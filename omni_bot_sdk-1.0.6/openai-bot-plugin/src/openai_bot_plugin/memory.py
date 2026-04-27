"""对话记忆存储：按会话 key 维护多轮对话，支持 TTL 过期、轮数裁剪和可选 JSON 持久化。

设计目标：
- 每个会话（私聊 = contact.username，群聊 = room.username）独立一条历史
- 只保留最近 N 轮（user + assistant 计为一轮）
- 超过 TTL 分钟的消息自动丢弃，避免拿着昨天的话题尬聊
- 进程内用 dict + threading.Lock 保证线程安全
- 可选指定 persist_path，落盘为 JSON，Bot 重启后历史不丢
"""

from __future__ import annotations

import json
import threading
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional


@dataclass
class MemoryTurn:
    role: str
    content: str
    ts: float = field(default_factory=time.time)

    def to_dict(self) -> dict:
        return {"role": self.role, "content": self.content, "ts": self.ts}

    @classmethod
    def from_dict(cls, data: dict) -> "MemoryTurn":
        return cls(
            role=str(data.get("role") or "user"),
            content=str(data.get("content") or ""),
            ts=float(data.get("ts") or time.time()),
        )


class ConversationMemory:
    """按会话 key 维护历史对话。线程安全。"""

    def __init__(
        self,
        max_turns: int = 10,
        ttl_minutes: int = 60,
        persist_path: Optional[str] = None,
    ) -> None:
        self.max_turns = max(0, int(max_turns or 0))
        self.ttl_seconds = max(0, int(ttl_minutes or 0)) * 60
        self.persist_path = Path(persist_path).expanduser() if persist_path else None
        self._store: Dict[str, List[MemoryTurn]] = {}
        self._lock = threading.RLock()
        self._load_from_disk()

    # ---- 读取 ----

    def get_messages(self, key: str) -> List[dict]:
        """返回形如 [{"role":"user"/"assistant","content":...}] 的最近历史，已按 TTL 过滤。"""
        if not key:
            return []
        with self._lock:
            turns = self._store.get(key, [])
            fresh = self._drop_expired(turns)
            if len(fresh) != len(turns):
                self._store[key] = fresh
            return [{"role": t.role, "content": t.content} for t in fresh]

    # ---- 写入 ----

    def append_exchange(self, key: str, user_content: str, assistant_content: str) -> None:
        """追加一轮 user + assistant 对话，自动按 max_turns 裁剪并持久化。"""
        if not key:
            return
        with self._lock:
            turns = self._drop_expired(self._store.get(key, []))
            turns.append(MemoryTurn(role="user", content=user_content))
            turns.append(MemoryTurn(role="assistant", content=assistant_content))
            self._store[key] = self._trim_to_max_turns(turns)
        self._save_to_disk()

    def clear(self, key: Optional[str] = None) -> None:
        """清空指定 key 的记忆；传 None 则清空所有会话。"""
        with self._lock:
            if key is None:
                self._store.clear()
            else:
                self._store.pop(key, None)
        self._save_to_disk()

    # ---- 工具方法 ----

    def _drop_expired(self, turns: List[MemoryTurn]) -> List[MemoryTurn]:
        if self.ttl_seconds <= 0:
            return list(turns)
        cutoff = time.time() - self.ttl_seconds
        return [t for t in turns if t.ts >= cutoff]

    def _trim_to_max_turns(self, turns: List[MemoryTurn]) -> List[MemoryTurn]:
        if self.max_turns <= 0:
            return []
        max_messages = self.max_turns * 2  # 每轮 = user + assistant
        if len(turns) <= max_messages:
            return turns
        return turns[-max_messages:]

    # ---- 持久化 ----

    def _load_from_disk(self) -> None:
        if self.persist_path is None or not self.persist_path.exists():
            return
        try:
            with self.persist_path.open("r", encoding="utf-8") as fh:
                raw = json.load(fh)
        except Exception:
            return
        if not isinstance(raw, dict):
            return
        restored: Dict[str, List[MemoryTurn]] = {}
        for key, turns in raw.items():
            if not isinstance(turns, list):
                continue
            restored[str(key)] = [MemoryTurn.from_dict(item) for item in turns if isinstance(item, dict)]
        with self._lock:
            self._store = restored

    def _save_to_disk(self) -> None:
        if self.persist_path is None:
            return
        try:
            self.persist_path.parent.mkdir(parents=True, exist_ok=True)
        except Exception:
            return
        with self._lock:
            snapshot = {
                key: [t.to_dict() for t in turns]
                for key, turns in self._store.items()
                if turns
            }
        try:
            tmp = self.persist_path.with_suffix(self.persist_path.suffix + ".tmp")
            with tmp.open("w", encoding="utf-8") as fh:
                json.dump(snapshot, fh, ensure_ascii=False, indent=2)
            tmp.replace(self.persist_path)
        except Exception:
            return

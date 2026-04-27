"""
消息工厂服务模块。
提供消息工厂相关的服务接口。
"""

import hashlib
import logging
from pathlib import Path

from omni_bot_sdk.models import UserInfo
from omni_bot_sdk.services.core.database_service import DatabaseService
from omni_bot_sdk.weixin.message_classes import Message, MessageType
from omni_bot_sdk.weixin.message_factory import FACTORY_REGISTRY


class MessageFactoryService:
    def __init__(self, user_info: UserInfo, db: DatabaseService):
        self.logger = logging.getLogger(__name__)
        self.user_info = user_info
        self.db = db
        self._session_username_md5_cache: dict[str, dict[str, str]] = {}

    def _get_session_db_path(self, message_db_path: str) -> Path:
        db_storage_path = Path(message_db_path).resolve().parents[1]
        return db_storage_path / "session" / "session.db"

    def _load_session_username_md5_map(self, session_db_path: Path) -> dict[str, str]:
        cache_key = str(session_db_path)
        if cache_key in self._session_username_md5_cache:
            return self._session_username_md5_cache[cache_key]

        username_md5_map: dict[str, str] = {}
        try:
            rows = self.db.execute_query(
                session_db_path,
                'select username from "SessionTable" where username is not null and username != ""',
            )
            for (username,) in rows:
                if not username:
                    continue
                username_md5_map[hashlib.md5(username.encode("utf-8")).hexdigest()] = username
            self.logger.info(
                f"已加载 SessionTable 映射缓存: db={session_db_path}, count={len(username_md5_map)}"
            )
        except Exception as exc:
            self.logger.warning(f"加载 SessionTable 映射失败: db={session_db_path}, error={exc}")

        self._session_username_md5_cache[cache_key] = username_md5_map
        return username_md5_map

    def _resolve_contact_from_table_suffix(
        self, table_suffix: str, message_db_path: str
    ):
        session_db_path = self._get_session_db_path(message_db_path)
        username_md5_map = self._load_session_username_md5_map(session_db_path)
        username = username_md5_map.get(table_suffix)
        if not username:
            return None
        try:
            return self.db.get_contact_by_username(username)
        except Exception as exc:
            self.logger.warning(
                f"通过 table_suffix 反查联系人失败: suffix={table_suffix}, username={username}, error={exc}"
            )
            return None

    def create_message(self, message: tuple) -> Message:
        """将消息转换为Message对象"""
        # TODO 加缓存，考虑到复杂程度，先不加了，腾讯在sqlite中索引加的不少，测试直接查询速度不慢
        table_name, msg_with_db = message
        table_suffix = table_name.replace("Msg_", "")
        type_ = msg_with_db[2]
        self.logger.info(f"消息类型: {MessageType.name(type_)}")
        room = self.db.get_room_by_md5(table_suffix)
        if type_ not in FACTORY_REGISTRY:
            type_ = -1
        if type_ == -1:
            self.logger.error(f"该消息类型: {type_} 未找到对应的工厂")
            return None
        contact = self.db.get_contact_by_sender_id(msg_with_db[4], msg_with_db[17])
        if not contact:
            self.logger.warn(f"未找到联系人: {msg_with_db[4]}")
            # TODO 有些消息是允许没有发送人的？这个时候怎么搞？是不是把他当作系统呢？
        corrected_contact = None
        if room is None and table_name.startswith("Msg_"):
            resolved_contact = self._resolve_contact_from_table_suffix(
                table_suffix, msg_with_db[17]
            )
            if resolved_contact:
                current_username = getattr(contact, "username", None)
                resolved_username = getattr(resolved_contact, "username", None)
                if current_username != resolved_username:
                    corrected_contact = resolved_contact
                    self.logger.warning(
                        "联系人映射修正: "
                        f"table_name={table_name}, "
                        f"table_suffix={table_suffix}, "
                        f"sender_id={msg_with_db[4]}, "
                        f"old_contact_username={current_username!r}, "
                        f"new_contact_username={resolved_username!r}, "
                        f"new_contact_display_name={getattr(resolved_contact, 'display_name', None)!r}"
                    )
                    contact = resolved_contact
        self.logger.info(
            "消息映射详情: "
            f"table_name={table_name}, "
            f"table_suffix={table_suffix}, "
            f"sender_id={msg_with_db[4]}, "
            f"db_path={msg_with_db[17]}, "
            f"room_display_name={getattr(room, 'display_name', None)!r}, "
            f"room_username={getattr(room, 'username', None)!r}, "
            f"contact_display_name={getattr(contact, 'display_name', None)!r}, "
            f"contact_username={getattr(contact, 'username', None)!r}, "
            f"contact_alias={getattr(contact, 'alias', None)!r}, "
            f"contact_corrected={bool(corrected_contact)}"
        )
        msg = FACTORY_REGISTRY[type_].create(
            msg_with_db, self.user_info, self.db, contact, room
        )
        msg.room = room
        if contact:
            msg.contact = contact
        self.logger.info(
            "消息对象详情: "
            f"target={getattr(msg, 'target', '')!r}, "
            f"is_chatroom={getattr(msg, 'is_chatroom', False)}, "
            f"real_sender_name={getattr(msg, 'real_sender_name', '')!r}, "
            f"parsed_content={getattr(msg, 'parsed_content', '')!r}"
        )
        return msg

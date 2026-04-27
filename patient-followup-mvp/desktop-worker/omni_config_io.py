"""读写 omni_bot_sdk 的 config.yaml，专门服务于 GUI 的「微信自动回复」面板。

设计目标：
- 只动 plugins.openai-bot-plugin 这一段，其它字段（OCR、MCP、其它插件）原样保留
- 用 ruamel.yaml round-trip 模式保留注释、引号风格、缩进，避免改坏用户原文件
- 失败时抛出明确的异常，让 GUI 弹窗给用户看
"""

from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import List


PLUGIN_KEY = "openai-bot-plugin"


DEFAULT_PROMPT = (
    "你是「{{self_nickname}}」本人，正在用微信和「{{contact_nickname}}」聊天"
    "（如果在群里，群名：{{room_nickname}}）。\n"
    "请当成真人朋友之间发微信来回复，严格遵守下面的语气规则：\n"
    "1. 口语化、自然，像在打字聊天；不要用书面语，不要用“您”，直接用“你”。\n"
    "2. 回复要短，一般 1~2 句就够了，能一句说完绝不拆成三句；一次别发一大段。\n"
    "3. 不要使用 Markdown：不要 **加粗**、不要 #标题、不要有序列表、不要代码块。\n"
    "4. 表情克制：一次最多一个 emoji 或者一个“哈哈”、“嗯”、“OK”，不要连发。\n"
    "5. 不要出戏：绝对不说“作为 AI”、“我是一个人工智能”、“根据您的问题”这种话。\n"
    "6. 不确定的事就直接说“这个我不太确定”或者“我回头帮你问下”，不要瞎编。\n"
    "7. 不要每句都问“还有什么可以帮你的吗”这种客服口吻，就自然结束对话。\n"
    "8. 如果对方只是打招呼（在吗/你好/嗨），就短短回一句招呼，别主动长篇发挥。\n\n"
    "参考信息：\n"
    "- 最近的聊天记录（可能为空）：{{chat_history}}\n"
    "- 当前时间：{{time_now}}\n"
)


@dataclass
class OpenAiPluginView:
    """GUI 关心的字段子集。"""

    enabled: bool = False
    only_private: bool = False
    allowed_targets: List[str] = field(default_factory=list)
    model: str = ""
    base_url: str = ""
    raw_present: bool = False  # config.yaml 里是否本来就有这一段
    prompt: str = ""

    # 对话记忆
    memory_enabled: bool = True
    memory_max_turns: int = 10
    memory_ttl_minutes: int = 60
    memory_persist_path: str = ""


def _load_yaml():
    try:
        from ruamel.yaml import YAML  # type: ignore
    except ImportError as exc:  # pragma: no cover
        raise RuntimeError(
            "缺少 ruamel.yaml，请先在运行 desktop-worker 的 Python 环境里执行：\n"
            "    pip install ruamel.yaml"
        ) from exc
    yaml = YAML()
    yaml.preserve_quotes = True
    yaml.width = 4096
    yaml.indent(mapping=2, sequence=4, offset=2)
    return yaml


def read_openai_plugin(omni_config_path: str | Path) -> OpenAiPluginView:
    path = Path(omni_config_path).expanduser()
    if not path.exists():
        raise FileNotFoundError(f"omni config.yaml 不存在: {path}")

    yaml = _load_yaml()
    with path.open("r", encoding="utf-8") as fh:
        data = yaml.load(fh) or {}

    plugins = data.get("plugins") or {}
    plugin_cfg = plugins.get(PLUGIN_KEY)
    if plugin_cfg is None:
        return OpenAiPluginView(raw_present=False)

    allowed_raw = plugin_cfg.get("allowed_targets") or []
    allowed = [str(item).strip() for item in allowed_raw if str(item).strip()]

    return OpenAiPluginView(
        enabled=bool(plugin_cfg.get("enabled", False)),
        only_private=bool(plugin_cfg.get("only_private", False)),
        allowed_targets=allowed,
        model=str(plugin_cfg.get("openai_model", "") or ""),
        base_url=str(plugin_cfg.get("openai_base_url", "") or ""),
        raw_present=True,
        prompt=str(plugin_cfg.get("prompt", "") or ""),
        memory_enabled=bool(plugin_cfg.get("memory_enabled", True)),
        memory_max_turns=int(plugin_cfg.get("memory_max_turns", 10) or 0),
        memory_ttl_minutes=int(plugin_cfg.get("memory_ttl_minutes", 60) or 0),
        memory_persist_path=str(plugin_cfg.get("memory_persist_path", "") or ""),
    )


def write_openai_plugin(
    omni_config_path: str | Path,
    *,
    enabled: bool,
    only_private: bool,
    allowed_targets: List[str],
    prompt: str | None = None,
    memory_enabled: bool = True,
    memory_max_turns: int = 10,
    memory_ttl_minutes: int = 60,
    memory_persist_path: str = "",
) -> None:
    """把 GUI 上的开关/白名单写回 config.yaml，保留其它配置和注释。"""
    path = Path(omni_config_path).expanduser()
    if not path.exists():
        raise FileNotFoundError(f"omni config.yaml 不存在: {path}")

    yaml = _load_yaml()
    with path.open("r", encoding="utf-8") as fh:
        data = yaml.load(fh) or {}

    plugins = data.get("plugins")
    if plugins is None:
        # 极少数情况：用户的 config.yaml 完全没有 plugins 段，给它建一个最小可用结构
        from ruamel.yaml.comments import CommentedMap  # type: ignore

        plugins = CommentedMap()
        data["plugins"] = plugins

    plugin_cfg = plugins.get(PLUGIN_KEY)
    if plugin_cfg is None:
        from ruamel.yaml.comments import CommentedMap  # type: ignore

        plugin_cfg = CommentedMap()
        plugin_cfg["enabled"] = enabled
        plugin_cfg["priority"] = 1497
        plugin_cfg["openai_model"] = "gpt-4o-mini"
        plugin_cfg["openai_base_url"] = "https://api.openai.com/v1"
        plugin_cfg["openai_api_key"] = "请在此处填写你的 API key"
        plugin_cfg["prompt"] = DEFAULT_PROMPT
        plugins[PLUGIN_KEY] = plugin_cfg

    plugin_cfg["enabled"] = bool(enabled)
    plugin_cfg["only_private"] = bool(only_private)

    if prompt is not None:
        text = str(prompt).strip()
        if text:
            from ruamel.yaml.scalarstring import LiteralScalarString  # type: ignore

            # 用 |- 多行字符串保存，yaml 里好看，也保留换行
            plugin_cfg["prompt"] = LiteralScalarString(text)

    cleaned_targets = []
    seen: set[str] = set()
    for item in allowed_targets:
        normalized = str(item).strip()
        if not normalized or normalized in seen:
            continue
        seen.add(normalized)
        cleaned_targets.append(normalized)

    from ruamel.yaml.comments import CommentedSeq  # type: ignore

    seq = CommentedSeq(cleaned_targets)
    plugin_cfg["allowed_targets"] = seq

    plugin_cfg["memory_enabled"] = bool(memory_enabled)
    plugin_cfg["memory_max_turns"] = max(0, int(memory_max_turns or 0))
    plugin_cfg["memory_ttl_minutes"] = max(0, int(memory_ttl_minutes or 0))
    plugin_cfg["memory_persist_path"] = str(memory_persist_path or "").strip()

    tmp_path = path.with_suffix(path.suffix + ".tmp")
    with tmp_path.open("w", encoding="utf-8") as fh:
        yaml.dump(data, fh)
    tmp_path.replace(path)

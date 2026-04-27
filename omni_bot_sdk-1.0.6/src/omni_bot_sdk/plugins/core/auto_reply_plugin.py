from typing import TYPE_CHECKING, List

from pydantic import BaseModel, Field, field_validator

from omni_bot_sdk.plugins.interface import (
    Plugin,
    PluginExcuteContext,
    PluginExcuteResponse,
    MessageType,
    SendTextMessageAction,
)

if TYPE_CHECKING:
    from omni_bot_sdk.bot import Bot


class AutoReplyPluginConfig(BaseModel):
    enabled: bool = False
    priority: int = 1490
    reply_text: str = "你好，我已收到你的消息。"
    only_private: bool = True
    only_at_in_group: bool = False
    mention_sender_in_group: bool = False
    stop_after_reply: bool = True
    match_keywords: List[str] = Field(default_factory=list)
    ignore_keywords: List[str] = Field(default_factory=list)
    allowed_contacts: List[str] = Field(default_factory=list)
    allowed_usernames: List[str] = Field(default_factory=list)
    allowed_aliases: List[str] = Field(default_factory=list)

    @field_validator(
        "match_keywords",
        "ignore_keywords",
        "allowed_contacts",
        "allowed_usernames",
        "allowed_aliases",
        mode="before",
    )
    @classmethod
    def _coerce_string_list(cls, value):
        if value is None:
            return []
        if isinstance(value, (str, int, float, bool)):
            return [str(value).strip()]
        if isinstance(value, list):
            return [str(item).strip() for item in value if str(item).strip()]
        return value


class AutoReplyPlugin(Plugin):
    priority = 1490
    name = "auto-reply-plugin"

    def __init__(self, bot: "Bot" = None):
        super().__init__(bot)
        self.priority = getattr(self.plugin_config, "priority", self.__class__.priority)

    def get_priority(self) -> int:
        return self.priority

    @staticmethod
    def _is_effective_chatroom(message) -> bool:
        if getattr(message, "is_chatroom", False):
            return True
        sender_contact = getattr(message, "contact", None)
        sender_username = getattr(sender_contact, "username", "") if sender_contact else ""
        return bool(sender_username and sender_username.endswith("@chatroom"))

    @staticmethod
    def _message_meta(message) -> dict:
        sender_contact = getattr(message, "contact", None)
        return {
            "is_self": getattr(message, "is_self", False),
            "is_chatroom": getattr(message, "is_chatroom", False),
            "effective_is_chatroom": AutoReplyPlugin._is_effective_chatroom(message),
            "is_at": getattr(message, "is_at", False),
            "local_type": getattr(message, "local_type", None),
            "sender_display_name": (getattr(message, "real_sender_name", "") or "").strip(),
            "sender_username": getattr(sender_contact, "username", "") if sender_contact else "",
            "sender_alias": getattr(sender_contact, "alias", "") if sender_contact else "",
            "target": (getattr(message, "target", "") or "").strip(),
            "content": (
                getattr(message, "parsed_content", "")[:100]
                if isinstance(getattr(message, "parsed_content", ""), str)
                else "<non-text>"
            ),
        }

    def _should_handle_by_scene(self, message) -> tuple[bool, str]:
        if message.is_self:
            return False, "消息来自自己"
        if message.local_type not in (MessageType.Text, MessageType.Text2, MessageType.Quote):
            return False, f"消息类型不支持: {message.local_type}"
        if not isinstance(message.parsed_content, str):
            return False, "消息内容不是字符串"

        content = message.parsed_content.strip()
        if not content:
            return False, "消息内容为空"

        if self._is_effective_chatroom(message):
            if self.get_plugin_config("only_private", True):
                return False, "当前配置仅回复私聊，且该消息被识别为群聊"
            if self.get_plugin_config("only_at_in_group", False) and not message.is_at:
                return False, "群聊消息未@自己"
        return True, "场景命中"

    def _match_keywords(self, content: str) -> tuple[bool, str]:
        match_keywords = self.get_plugin_config("match_keywords", [])
        ignore_keywords = self.get_plugin_config("ignore_keywords", [])

        hit_ignore = [keyword for keyword in ignore_keywords if keyword and keyword in content]
        if hit_ignore:
            return False, f"命中忽略关键词: {hit_ignore}"
        hit_match = [keyword for keyword in match_keywords if keyword and keyword in content]
        if match_keywords and not hit_match:
            return False, f"未命中指定关键词: {match_keywords}"
        if hit_match:
            return True, f"命中关键词: {hit_match}"
        return True, "关键词检查通过"

    def _match_sender(self, message) -> tuple[bool, str]:
        allowed_contacts = self.get_plugin_config("allowed_contacts", [])
        allowed_usernames = self.get_plugin_config("allowed_usernames", [])
        allowed_aliases = self.get_plugin_config("allowed_aliases", [])

        if not allowed_contacts and not allowed_usernames and not allowed_aliases:
            return True, "未配置联系人白名单，默认放行"

        sender_display_name = (message.real_sender_name or "").strip()
        sender_contact = getattr(message, "contact", None)
        sender_username = getattr(sender_contact, "username", "") if sender_contact else ""
        sender_alias = getattr(sender_contact, "alias", "") if sender_contact else ""
        target_name = (message.target or "").strip()

        normalized_contacts = {name.strip() for name in allowed_contacts if name and name.strip()}
        normalized_usernames = {
            username.strip() for username in allowed_usernames if username and username.strip()
        }
        normalized_aliases = {
            alias.strip() for alias in allowed_aliases if alias and alias.strip()
        }

        if sender_display_name and sender_display_name in normalized_contacts:
            return True, f"命中 allowed_contacts(display_name): {sender_display_name}"
        if target_name and target_name in normalized_contacts:
            return True, f"命中 allowed_contacts(target): {target_name}"
        if sender_username and sender_username in normalized_usernames:
            return True, f"命中 allowed_usernames: {sender_username}"
        if sender_alias and sender_alias in normalized_aliases:
            return True, f"命中 allowed_aliases: {sender_alias}"
        return (
            False,
            "未命中发送者白名单 "
            f"display_name={sender_display_name!r}, "
            f"username={sender_username!r}, "
            f"alias={sender_alias!r}, "
            f"target={target_name!r}, "
            f"allowed_contacts={sorted(normalized_contacts)!r}, "
            f"allowed_usernames={sorted(normalized_usernames)!r}, "
            f"allowed_aliases={sorted(normalized_aliases)!r}",
        )

    async def handle_message(self, context: PluginExcuteContext) -> None:
        message = context.get_message()
        self.logger.info(
            f"[auto-reply-plugin] 收到消息，准备判断是否自动回复: {self._message_meta(message)}"
        )

        scene_ok, scene_reason = self._should_handle_by_scene(message)
        if not scene_ok:
            self.logger.info(f"[auto-reply-plugin] 跳过回复，场景不匹配: {scene_reason}")
            return

        content = message.parsed_content.strip()
        keyword_ok, keyword_reason = self._match_keywords(content)
        if not keyword_ok:
            self.logger.info(f"[auto-reply-plugin] 跳过回复，关键词不匹配: {keyword_reason}")
            return
        sender_ok, sender_reason = self._match_sender(message)
        if not sender_ok:
            self.logger.info(f"[auto-reply-plugin] 跳过回复，发送者不匹配: {sender_reason}")
            return

        self.logger.info(
            f"[auto-reply-plugin] 命中自动回复条件: scene={scene_reason}, "
            f"keyword={keyword_reason}, sender={sender_reason}"
        )

        reply_template = self.get_plugin_config("reply_text", "你好，我已收到你的消息。")
        reply_text = reply_template.format(
            message=content,
            sender=message.real_sender_name or "",
            target=message.target or "",
        )
        self.logger.info(
            f"[auto-reply-plugin] 准备回复消息，target={message.target!r}, reply_text={reply_text!r}"
        )

        at_user_name = None
        if self._is_effective_chatroom(message) and self.get_plugin_config(
            "mention_sender_in_group", False
        ):
            at_user_name = message.real_sender_name or None

        action = SendTextMessageAction(
            content=reply_text,
            target=message.target,
            is_chatroom=self._is_effective_chatroom(message),
            at_user_name=at_user_name,
        )
        context.add_response(
            PluginExcuteResponse(
                plugin_name=self.get_plugin_name(),
                handled=True,
                should_stop=self.get_plugin_config("stop_after_reply", True),
                response={"reply_text": reply_text},
                actions=[action],
                message=message,
            )
        )
        if self.get_plugin_config("stop_after_reply", True):
            context.should_stop = True

    def get_plugin_name(self) -> str:
        return self.name

    def get_plugin_description(self) -> str:
        return "收到别人发来的文本消息后，自动回复预设内容"

    @classmethod
    def get_plugin_config_schema(cls):
        return AutoReplyPluginConfig

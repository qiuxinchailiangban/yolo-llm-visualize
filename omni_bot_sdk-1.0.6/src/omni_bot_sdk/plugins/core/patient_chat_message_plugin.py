from typing import TYPE_CHECKING, Optional

import requests
from pydantic import BaseModel, Field, field_validator

from omni_bot_sdk.plugins.interface import MessageType, Plugin, PluginExcuteContext

if TYPE_CHECKING:
    from omni_bot_sdk.bot import Bot


class PatientChatMessagePluginConfig(BaseModel):
    enabled: bool = False
    priority: int = 1590
    backend_base_url: str = "http://127.0.0.1:8080"
    worker_token: str = "followup-worker-token"
    worker_id: str = "omni-patient-chat"
    timeout_seconds: int = 5
    report_self_messages: bool = False
    allowed_types: list[int] = Field(default_factory=lambda: [MessageType.Text, MessageType.Text2, MessageType.Quote])

    @field_validator("backend_base_url", "worker_token", "worker_id", mode="before")
    @classmethod
    def _normalize_string(cls, value):
        if value is None:
            return ""
        return str(value).strip()


class PatientChatMessagePlugin(Plugin):
    priority = 1590
    name = "patient-chat-message-plugin"

    def __init__(self, bot: "Bot" = None):
        super().__init__(bot)
        self.priority = getattr(self.plugin_config, "priority", self.__class__.priority)
        self._session = requests.Session()

    def get_priority(self) -> int:
        return self.priority

    def get_plugin_name(self) -> str:
        return self.name

    def get_plugin_description(self) -> str:
        return "将已绑定患者群里的消息自动上报并归属到患者"

    @classmethod
    def get_plugin_config_schema(cls):
        return PatientChatMessagePluginConfig

    async def handle_message(self, context: PluginExcuteContext) -> None:
        message = context.get_message()
        if not self._should_handle(message):
            return

        payload = self._build_payload(message)
        if not payload:
            return

        try:
            response = self._session.post(
                self._build_endpoint(),
                json=payload,
                headers={
                    "Content-Type": "application/json",
                    "X-Worker-Token": self.get_plugin_config("worker_token", ""),
                },
                timeout=max(int(self.get_plugin_config("timeout_seconds", 5)), 1),
            )
            if response.ok:
                self.logger.info(
                    "[patient-chat-message-plugin] 已上报群消息: chatroom=%s sender=%s",
                    payload["chatroomUsername"],
                    payload.get("senderDisplayName", ""),
                )
            else:
                self.logger.warning(
                    "[patient-chat-message-plugin] 上报失败 status=%s body=%s",
                    response.status_code,
                    response.text[:300],
                )
        except Exception as exc:
            self.logger.error(
                "[patient-chat-message-plugin] 上报异常: %s",
                exc,
                exc_info=True,
            )

    def _should_handle(self, message) -> bool:
        if not getattr(message, "is_chatroom", False):
            return False
        if not self.get_plugin_config("report_self_messages", False) and getattr(message, "is_self", False):
            return False
        if getattr(message, "local_type", None) not in self.get_plugin_config("allowed_types", []):
            return False
        return bool(getattr(getattr(message, "room", None), "username", ""))

    def _build_payload(self, message) -> Optional[dict]:
        room = getattr(message, "room", None)
        if room is None:
            return None

        parsed_content = getattr(message, "parsed_content", "")
        if isinstance(parsed_content, bytes):
            parsed_content = parsed_content.decode("utf-8", errors="ignore")
        content = str(parsed_content).strip()
        if not content:
            return None

        sender_contact = getattr(message, "contact", None)
        sender_username = getattr(sender_contact, "username", "") if sender_contact else ""
        return {
            "workerId": self.get_plugin_config("worker_id", "omni-patient-chat"),
            "chatroomUsername": getattr(room, "username", "").strip(),
            "chatroomDisplayName": (getattr(room, "display_name", "") or "").strip(),
            "chatroomName": (getattr(message, "target", "") or "").strip(),
            "senderDisplayName": (getattr(message, "real_sender_name", "") or "").strip(),
            "senderUsername": sender_username.strip(),
            "direction": "OUTBOUND" if getattr(message, "is_self", False) else "INBOUND",
            "messageType": str(getattr(message, "local_type", "") or ""),
            "content": content,
            "localMessageId": getattr(message, "local_id", None),
            "serverMessageId": getattr(message, "server_id", None),
            "messageEpochSeconds": int(getattr(message, "create_time", 0) or 0),
        }

    def _build_endpoint(self) -> str:
        base_url = self.get_plugin_config("backend_base_url", "http://127.0.0.1:8080").rstrip("/")
        return f"{base_url}/api/worker/patient-chat-messages/report"

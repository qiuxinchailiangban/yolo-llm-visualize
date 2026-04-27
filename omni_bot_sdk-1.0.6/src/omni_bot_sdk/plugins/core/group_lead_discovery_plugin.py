import logging
from typing import TYPE_CHECKING, Optional

import requests
from pydantic import BaseModel, Field, field_validator

from omni_bot_sdk.plugins.interface import MessageType, Plugin, PluginExcuteContext

if TYPE_CHECKING:
    from omni_bot_sdk.bot import Bot


class GroupLeadDiscoveryPluginConfig(BaseModel):
    enabled: bool = False
    priority: int = 1600
    backend_base_url: str = "http://127.0.0.1:8080"
    worker_token: str = "followup-worker-token"
    worker_id: str = "omni-group-discovery"
    timeout_seconds: int = 5
    report_self_messages: bool = False
    allowed_types: list[int] = Field(default_factory=lambda: [MessageType.Text, MessageType.Text2, MessageType.Quote])

    @field_validator("backend_base_url", "worker_token", "worker_id", mode="before")
    @classmethod
    def _normalize_string(cls, value):
        if value is None:
            return ""
        return str(value).strip()


class GroupLeadDiscoveryPlugin(Plugin):
    priority = 1600
    name = "group-lead-discovery-plugin"

    def __init__(self, bot: "Bot" = None):
        super().__init__(bot)
        self.priority = getattr(self.plugin_config, "priority", self.__class__.priority)
        self._session = requests.Session()

    def get_priority(self) -> int:
        return self.priority

    def get_plugin_name(self) -> str:
        return self.name

    def get_plugin_description(self) -> str:
        return "发现微信群并按群名规则上报患者线索"

    @classmethod
    def get_plugin_config_schema(cls):
        return GroupLeadDiscoveryPluginConfig

    async def handle_message(self, context: PluginExcuteContext) -> None:
        message = context.get_message()
        if not self._should_handle(message):
            return

        payload = self._build_payload(message)
        if not payload:
            return

        endpoint = self._build_endpoint()
        headers = {
            "Content-Type": "application/json",
            "X-Worker-Token": self.get_plugin_config("worker_token", ""),
        }

        try:
            response = self._session.post(
                endpoint,
                json=payload,
                headers=headers,
                timeout=max(int(self.get_plugin_config("timeout_seconds", 5)), 1),
            )
            if response.ok:
                self.logger.info(
                    "[group-lead-discovery-plugin] 已上报微信群线索: chatroom=%s raw=%s",
                    payload["chatroomUsername"],
                    payload.get("rawGroupName", ""),
                )
            else:
                self.logger.warning(
                    "[group-lead-discovery-plugin] 上报失败 status=%s body=%s",
                    response.status_code,
                    response.text[:300],
                )
        except Exception as exc:
            self.logger.error(
                "[group-lead-discovery-plugin] 上报微信群线索异常: %s",
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
        room = getattr(message, "room", None)
        if room is None:
            return False
        return bool(getattr(room, "username", ""))

    def _build_payload(self, message) -> Optional[dict]:
        room = getattr(message, "room", None)
        if room is None:
            return None
        raw_group_name = (getattr(room, "display_name", "") or getattr(message, "target", "") or "").strip()
        parsed_content = getattr(message, "parsed_content", "")
        if isinstance(parsed_content, bytes):
            try:
                parsed_content = parsed_content.decode("utf-8", errors="ignore")
            except Exception:
                parsed_content = ""
        snippet = str(parsed_content).strip()[:255] if parsed_content else ""
        return {
            "workerId": self.get_plugin_config("worker_id", "omni-group-discovery"),
            "chatroomUsername": getattr(room, "username", "").strip(),
            "chatroomDisplayName": raw_group_name,
            "rawGroupName": raw_group_name,
            "firstMessageSnippet": snippet,
            "lastMessageSnippet": snippet,
        }

    def _build_endpoint(self) -> str:
        base_url = self.get_plugin_config("backend_base_url", "http://127.0.0.1:8080").rstrip("/")
        return f"{base_url}/api/worker/wechat-group-leads/discover"

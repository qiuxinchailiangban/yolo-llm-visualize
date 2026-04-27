import time
from typing import List, Optional

import openai
from openai import OpenAI
from pydantic import BaseModel, Field, field_validator
from omni_bot_sdk.plugins.interface import (
    Bot,
    Plugin,
    PluginExcuteContext,
    PluginExcuteResponse,
    MessageType,
    SendTextMessageAction,
)

from .memory import ConversationMemory


class OpenAIBotPluginConfig(BaseModel):
    """
    OpenAI Bot 插件配置
    enabled: 是否启用该插件
    openai_api_key: OpenAI API密钥
    openai_base_url: OpenAI API基础URL
    openai_model: OpenAI模型名称
    priority: 插件优先级，数值越大优先级越高
    prompt: 系统提示词，支持 {{chat_history}}、{{time_now}}、{{self_nickname}}、{{room_nickname}}、{{contact_nickname}} 变量占位符
    allowed_targets: 白名单。可以填【私聊好友显示名】或【群聊名称】。
        - 为空表示「不开启白名单」，所有消息都会被 LLM 自动回复（保留旧行为）
        - 非空时，只有 target 命中列表里的会话才会被回复，其它一律跳过
    only_private: 是否只回复私聊。默认 False = 私聊和群聊都可以回复
    """

    enabled: bool = False
    openai_api_key: str = "unknown"
    openai_base_url: str = "https://api.openai.com/v1"
    openai_model: str = "gpt-3.5-turbo"
    priority: int = 100
    prompt: str = (
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
    allowed_targets: List[str] = Field(default_factory=list)
    only_private: bool = False

    # --- 对话记忆 ---
    # memory_enabled: 启用多轮对话记忆；关闭后每条消息都是独立上下文
    # memory_max_turns: 每个会话最多保留的 user+assistant 轮数
    # memory_ttl_minutes: 超过多久未更新的历史会自动丢弃，防止拿旧话题尬聊
    # memory_persist_path: 可选，指向一个 json 文件，写进去后 Bot 重启记忆不丢
    # memory_reset_keywords: 用户在微信里发这些内容之一，就会清空本会话的记忆
    memory_enabled: bool = True
    memory_max_turns: int = 10
    memory_ttl_minutes: int = 60
    memory_persist_path: str = ""
    memory_reset_keywords: List[str] = Field(
        default_factory=lambda: ["/reset", "/清空记忆", "/忘了吧"]
    )

    @field_validator("allowed_targets", "memory_reset_keywords", mode="before")
    @classmethod
    def _coerce_string_list(cls, value):
        if value is None:
            return []
        if isinstance(value, (str, int, float, bool)):
            return [str(value).strip()]
        if isinstance(value, list):
            return [str(item).strip() for item in value if str(item).strip()]
        return value


class OpenAIBotPlugin(Plugin):
    """
    OpenAI 聊天机器人插件实现类
    """

    priority = 100
    name = "openai-bot-plugin"

    def __init__(self, bot: "Bot"):
        super().__init__(bot)
        self.api_key = self.plugin_config.openai_api_key
        self.base_url = self._normalize_base_url(self.plugin_config.openai_base_url)
        self.model = self.plugin_config.openai_model
        self.enabled = self.plugin_config.enabled
        self.priority = getattr(self.plugin_config, "priority", self.__class__.priority)
        self.user = bot.user_info
        self.prompt = self.plugin_config.prompt
        self.allowed_targets = list(getattr(self.plugin_config, "allowed_targets", []) or [])
        self.only_private = bool(getattr(self.plugin_config, "only_private", False))

        self.memory_enabled = bool(getattr(self.plugin_config, "memory_enabled", True))
        self.memory_reset_keywords = list(
            getattr(self.plugin_config, "memory_reset_keywords", []) or []
        )
        self.memory = ConversationMemory(
            max_turns=int(getattr(self.plugin_config, "memory_max_turns", 10) or 0),
            ttl_minutes=int(getattr(self.plugin_config, "memory_ttl_minutes", 60) or 0),
            persist_path=(getattr(self.plugin_config, "memory_persist_path", "") or None),
        )

        openai.api_key = self.api_key
        openai.base_url = self.base_url
        self.client = OpenAI(api_key=self.api_key, base_url=self.base_url)
        self.logger.info(
            f"[openai-bot-plugin] init enabled={self.enabled} only_private={self.only_private} "
            f"allowed_targets={self.allowed_targets!r} memory_enabled={self.memory_enabled} "
            f"memory_max_turns={self.memory.max_turns} memory_ttl_minutes={self.memory.ttl_seconds // 60} "
            f"memory_persist_path={self.memory.persist_path}"
        )

    def _resolve_memory_key(self, message) -> str:
        """用会话唯一 ID 作为记忆 key：群聊用 room.username，私聊用 contact.username。"""
        if getattr(message, "is_chatroom", False) and getattr(message, "room", None):
            username = getattr(message.room, "username", "") or ""
            if username:
                return f"room:{username.strip()}"
        contact = getattr(message, "contact", None)
        if contact is not None:
            username = getattr(contact, "username", "") or ""
            if username:
                return f"private:{username.strip()}"
        return ""

    def _is_reset_command(self, content: str) -> bool:
        if not content or not self.memory_reset_keywords:
            return False
        normalized = content.strip()
        return any(normalized == kw.strip() for kw in self.memory_reset_keywords if kw)

    def _resolve_target_name(self, message) -> str:
        """统一拿到一个用于白名单匹配的会话名：群名 / 好友显示名 / target 兜底。"""
        if getattr(message, "is_chatroom", False) and getattr(message, "room", None):
            name = getattr(message.room, "display_name", "") or ""
            if name:
                return name.strip()
        contact = getattr(message, "contact", None)
        if contact is not None:
            name = getattr(contact, "display_name", "") or ""
            if name:
                return name.strip()
        return (getattr(message, "target", "") or "").strip()

    def _is_target_allowed(self, message) -> tuple[bool, str]:
        """检查白名单。返回 (是否放行, 原因)。"""
        if self.only_private and getattr(message, "is_chatroom", False):
            return False, "仅回复私聊，但当前消息来自群聊"
        if not self.allowed_targets:
            return True, "未配置白名单，默认放行"
        target_name = self._resolve_target_name(message)
        normalized = {item.strip() for item in self.allowed_targets if item and str(item).strip()}
        if target_name and target_name in normalized:
            return True, f"命中白名单: {target_name}"
        return (
            False,
            f"未命中白名单: target={target_name!r}, allowed={sorted(normalized)!r}",
        )

    @staticmethod
    def _normalize_base_url(base_url: str) -> str:
        normalized = (base_url or "").rstrip("/")
        if normalized and not normalized.endswith("/v1"):
            normalized = f"{normalized}/v1"
        return normalized

    def _extract_response_text(self, response) -> Optional[str]:
        if response is None:
            return None
        if isinstance(response, str):
            return response.strip()
        if isinstance(response, dict):
            choices = response.get("choices") or []
            if choices:
                message = choices[0].get("message") or {}
                content = message.get("content")
                if isinstance(content, str):
                    return content.strip()
            content = response.get("content")
            if isinstance(content, str):
                return content.strip()
        if hasattr(response, "choices") and response.choices:
            content = response.choices[0].message.content
            if isinstance(content, str):
                return content.strip()
        if hasattr(response, "output_text") and isinstance(response.output_text, str):
            return response.output_text.strip()
        self.logger.error(
            f"OpenAI 返回格式无法识别: type={type(response).__name__}, value={response!r}"
        )
        return None

    def _extract_user_content(self, msg) -> str:
        if msg.local_type == MessageType.Quote:
            return msg.content or ""
        return (
            (msg.parsed_content or "")
            .replace(f"@{self.user.nickname}", "")
            .replace("\u2005", "")
            .strip()
        )

    def get_ai_response(self, msg, chat_history) -> Optional[str]:
        if not self.enabled:
            return None
        try:
            content = self._extract_user_content(msg)

            memory_key = self._resolve_memory_key(msg) if self.memory_enabled else ""
            history_messages: List[dict] = (
                self.memory.get_messages(memory_key) if memory_key else []
            )

            time_now = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
            system_prompt = self.prompt
            system_prompt = system_prompt.replace(
                "{{chat_history}}", chat_history or ""
            )
            system_prompt = system_prompt.replace("{{time_now}}", time_now)
            system_prompt = system_prompt.replace(
                "{{self_nickname}}", self.user.nickname
            )
            system_prompt = system_prompt.replace(
                "{{room_nickname}}", msg.room.display_name if msg.room else ""
            )
            system_prompt = system_prompt.replace(
                "{{contact_nickname}}", msg.contact.display_name if msg.contact else ""
            )

            messages: List[dict] = [{"role": "system", "content": system_prompt}]
            messages.extend(history_messages)
            messages.append({"role": "user", "content": content})

            if history_messages:
                self.logger.info(
                    f"[openai-bot-plugin] 带入历史 {len(history_messages)} 条（memory_key={memory_key}）"
                )

            response = self.client.chat.completions.create(
                model=self.model,
                messages=messages,
                user=msg.room.username if msg.is_chatroom else msg.contact.username,
            )
            answer = self._extract_response_text(response)

            if answer and memory_key:
                self.memory.append_exchange(memory_key, content, answer)

            return answer
        except Exception as e:
            self.logger.error(f"获取AI响应时出错: {e}")
            return None

    def get_priority(self) -> int:
        return self.priority

    async def handle_message(self, plusginExcuteContext: PluginExcuteContext) -> None:
        """
        处理接收到的消息
        文本消息，引用消息处理，其他都先不处理
        文本消息要判断是不是 at 我，或者是不是引用了我
        前面的上下文插件会在上下文中添加 not_for_bot 字段，如果为True，则不进行AI回复
        """
        if not self.enabled:
            return
        message = plusginExcuteContext.get_message()
        if (
            message.local_type != MessageType.Text
            and message.local_type != MessageType.Quote
        ):
            return
        context = plusginExcuteContext.get_context()
        not_for_bot = context.get("not_for_bot", False)
        if (
            not_for_bot
        ):  # 用户可能没有前置判断流程，这里需要采用一般逻辑，也就是私聊消息全部回复，群聊消息除了@和引用不回复，这是典型的机器人特征
            return
        allowed, reason = self._is_target_allowed(message)
        if not allowed:
            self.logger.info(f"[openai-bot-plugin] 跳过自动回复: {reason}")
            return
        self.logger.info(f"[openai-bot-plugin] 命中自动回复条件: {reason}")

        # 识别「清空记忆」指令，不走 LLM，直接回一句并清空本会话历史
        if self.memory_enabled:
            user_text = self._extract_user_content(message)
            if self._is_reset_command(user_text):
                memory_key = self._resolve_memory_key(message)
                if memory_key:
                    self.memory.clear(memory_key)
                    self.logger.info(
                        f"[openai-bot-plugin] 收到重置指令，已清空记忆 key={memory_key}"
                    )
                plusginExcuteContext.add_response(
                    PluginExcuteResponse(
                        message=message,
                        plugin_name=self.name,
                        should_stop=True,
                        actions=[
                            SendTextMessageAction(
                                content="好的，已忘掉之前的对话，我们重新开始吧。",
                                target=(
                                    message.room.display_name
                                    if message.room
                                    else message.contact.display_name
                                ),
                                is_chatroom=message.is_chatroom,
                            )
                        ],
                    )
                )
                plusginExcuteContext.should_stop = True
                return

        chat_history = context.get("chat_history", "")
        # 增加判断条件，如果是私聊，直接可以响应，如果是群聊，必须引用或者@
        if message.is_chatroom:
            if message.local_type == MessageType.Text:
                if message.is_at:
                    pass
                else:
                    return
            elif message.local_type == MessageType.Quote:
                if message.quote_message and message.quote_message.is_self:
                    pass
                else:
                    return
            response = self.get_ai_response(msg=message, chat_history=chat_history)
            if not response:
                return
            if message.local_type == MessageType.Quote:
                search_text = message.content
            else:
                search_text = f"{message.parsed_content.replace('\u2005', ' ').strip()}"
            plusginExcuteContext.add_response(
                PluginExcuteResponse(
                    message=message,
                    plugin_name=self.name,
                    should_stop=True,
                    actions=[
                        SendTextMessageAction(
                            content=response,
                            target=(
                                message.room.display_name
                                if message.room
                                else message.contact.display_name
                            ),
                            is_chatroom=message.is_chatroom,
                            at_user_name=None,
                            quote_message=search_text,
                            random_at_quote=True,  # 随机在@，引用，和不操作之间选择，在rpa里面有策略，实际上可以在操作的时候读取一下数据库，就会很方便
                        )
                    ],
                )
            )
        else:
            # 私聊的消息，直接使用Dify的工作流回复
            response = self.get_ai_response(msg=message, chat_history=chat_history)
            if not response:
                return
            plusginExcuteContext.add_response(
                PluginExcuteResponse(
                    message=message,
                    plugin_name=self.name,
                    should_stop=True,
                    actions=[
                        SendTextMessageAction(
                            content=response,
                            target=(
                                message.room.display_name
                                if message.room
                                else message.contact.display_name
                            ),
                            is_chatroom=message.is_chatroom,
                        )
                    ],
                )
            )
        plusginExcuteContext.should_stop = True

    def get_plugin_name(self) -> str:
        return self.name

    def get_plugin_description(self) -> str:
        return "OpenAI 聊天机器人插件"

    @classmethod
    def get_plugin_config_schema(cls):
        return OpenAIBotPluginConfig

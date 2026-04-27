"""
辅助查找图片AES密钥
"""

from typing import TYPE_CHECKING

from pydantic import BaseModel

from omni_bot_sdk.plugins.interface import (
    MessageType,
    Plugin,
    PluginExcuteContext,
)

if TYPE_CHECKING:
    from omni_bot_sdk.bot import Bot


class ImageAesPluginConfig(BaseModel):
    """
    图片 AES 辅助插件配置
    enabled: 是否启用该插件
    priority: 插件优先级，数值越大优先级越高
    """

    enabled: bool = True
    priority: int = 2000


class ImageAesPlugin(Plugin):
    """
    当自己给自己发送图片时，触发图片解密服务的延迟初始化。
    """

    priority = 2000
    name = "image-aes-plugin"

    def __init__(self, bot: "Bot" = None):
        super().__init__(bot)
        self.priority = getattr(self.plugin_config, "priority", self.__class__.priority)

    def get_priority(self) -> int:
        return self.priority

    async def handle_message(self, context: PluginExcuteContext) -> None:
        message = context.get_message()

        if message.local_type == MessageType.Image and message.is_self:
            if not self.bot.dat_decrypt_service._init_done:
                self.logger.info("图片解密服务未初始化，启动延迟初始化操作")
                self.bot.dat_decrypt_service.setup_lazy()

    def get_plugin_name(self) -> str:
        return self.name

    def get_plugin_description(self) -> str:
        return "这是一个用于辅助查找图片 AES 密钥的插件"

    @classmethod
    def get_plugin_config_schema(cls):
        return ImageAesPluginConfig

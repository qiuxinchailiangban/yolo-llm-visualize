from __future__ import annotations

import argparse
import logging
import sys
import time
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent
SRC_DIR = REPO_ROOT / "src"

# 在源码目录直接运行脚本时，补充 SDK 源码路径。
src_dir_str = str(SRC_DIR)
if src_dir_str not in sys.path:
    sys.path.insert(0, src_dir_str)

import pyautogui

from omni_bot_sdk.common.config import Config
from omni_bot_sdk.rpa.image_processor import ImageProcessor
from omni_bot_sdk.rpa.message_sender import MessageSender
from omni_bot_sdk.rpa.ocr_processor import OCRProcessor
from omni_bot_sdk.rpa.window_manager import WindowManager
from omni_bot_sdk.utils.helpers import copy_file_to_clipboard, ensure_dir_exists
from omni_bot_sdk.utils.logging_setup import setup_logging


DEFAULT_CONFIG_PATH = REPO_ROOT / "config.yaml"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="使用 omni-bot-sdk 向指定会话发送一条文本消息。"
    )
    parser.add_argument(
        "--target",
        default="文件传输助手",
        help="目标会话名称，默认是 文件传输助手",
    )
    parser.add_argument(
        "--content",
        default="你好，这是通过 omni-bot-sdk 发出的测试消息。",
        help="要发送的文本内容",
    )
    parser.add_argument(
        "--config",
        default=str(DEFAULT_CONFIG_PATH),
        help="配置文件路径，默认使用当前 SDK 目录下的 config.yaml",
    )
    parser.add_argument(
        "--wait-seconds",
        type=float,
        default=8,
        help="消息入队后额外等待的秒数，给 RPA 留出执行时间",
    )
    parser.add_argument(
        "--at-user-name",
        default=None,
        help="群聊中要 @ 的成员名称，仅群聊有效",
    )
    parser.add_argument(
        "--send-mode",
        choices=("enter", "button"),
        default="enter",
        help="发送方式，默认使用回车发送；如需测试按钮点击可改为 button",
    )
    parser.add_argument(
        "--image",
        action="append",
        default=[],
        help="文本发送成功后，再额外发送一张图片（可重复传多次，按顺序发送）。"
        "留空则只发文字。",
    )
    parser.add_argument(
        "--keep-mqtt",
        action="store_true",
        help="保留参数兼容性；当前最小发送脚本不会使用 MQTT",
    )
    return parser.parse_args()


def send_image_after_text(window_manager: WindowManager, image_path: str) -> bool:
    """把指定图片通过剪贴板粘贴到【当前】会话里并按回车发送。

    调用前必须已经 switch_session 到目标会话 + 发完了文本消息。
    这里没有再切会话，直接复用上一步的上下文，避免切错窗口。
    """
    path = Path(image_path).expanduser().resolve()
    if not path.exists():
        print(f"图片不存在，跳过发送: {path}")
        return False

    step_delay = max(getattr(window_manager, "action_delay", 0.3), 0.6)
    try:
        if not copy_file_to_clipboard(str(path)):
            print(f"复制图片到剪贴板失败: {path}")
            return False
        time.sleep(step_delay)
        if not window_manager.activate_input_box():
            print("激活输入框失败，图片未发送")
            return False
        time.sleep(step_delay)
        pyautogui.hotkey("ctrl", "v")
        # 微信需要一点时间把图片从剪贴板渲染到输入框
        time.sleep(max(step_delay, 1.2))
        pyautogui.press("enter")
        time.sleep(step_delay)
        print(f"图片已发送: {path}")
        return True
    except Exception as exc:  # noqa: BLE001
        print(f"发送图片时出错: {exc}")
        return False


def send_images_after_text(window_manager: WindowManager, image_paths: list[str]) -> bool:
    if not image_paths:
        return True
    all_ok = True
    for image_path in image_paths:
        if not send_image_after_text(window_manager, image_path):
            all_ok = False
    return all_ok


def build_runtime_context(config_path: Path):
    if not config_path.exists():
        raise FileNotFoundError(f"配置文件不存在: {config_path}")

    config = Config(str(config_path))
    setup_logging(
        log_dir=config.get("logging.path", "logs"),
        log_level=config.get("logging.level", logging.INFO),
    )
    ensure_dir_exists("runtime_images")

    image_processor = ImageProcessor()
    ocr_processor = OCRProcessor(config.get("rpa.ocr", {}))
    ocr_processor.setup()

    window_manager = WindowManager(
        image_processor=image_processor,
        ocr_processor=ocr_processor,
        rpa_config=config.get("rpa", {}),
    )
    sender = MessageSender(window_manager)
    return config, window_manager, sender


def initialize_window(window_manager: WindowManager, max_retries: int = 3) -> bool:
    for attempt in range(1, max_retries + 1):
        if window_manager.init_chat_window():
            return True
        print(f"初始化聊天窗口失败，{attempt}/{max_retries}，2 秒后重试...")
        time.sleep(2)
    return False


def main() -> int:
    args = parse_args()
    config_path = Path(args.config).expanduser().resolve()
    _, window_manager, sender = build_runtime_context(config_path)

    if not initialize_window(window_manager):
        print("聊天窗口初始化失败，请确认微信已登录并位于前台。")
        return 1

    if not window_manager.switch_session(args.target):
        print(f"切换会话失败: {args.target}")
        return 1
    time.sleep(max(getattr(window_manager, "switch_contact_delay", 0.3), 1.0))

    if args.at_user_name:
        sender.clear_input_box()
        if not sender.mention_user(args.at_user_name):
            print(f"@用户失败: {args.at_user_name}")
            return 1
        time.sleep(max(getattr(window_manager, "action_delay", 0.3), 0.8))

    sent_text = False
    if args.content:
        success = sender.send_message(
            args.content,
            clear_input_box=args.at_user_name is None,
            send_mode=args.send_mode,
        )
        if not success:
            print("消息发送失败，请检查微信窗口状态和目标会话名称。")
            return 1
        sent_text = True
        print(f"消息已发送到: {args.target}")
    else:
        print(f"本次仅发送图片到: {args.target}")

    if args.image:
        time.sleep(max(getattr(window_manager, "action_delay", 0.3), 1.0))
        image_ok = send_images_after_text(window_manager, args.image)
        if not image_ok:
            if sent_text:
                print("警告：部分附加图片发送失败，但文字提醒已送达。")
            else:
                print("警告：部分图片发送失败。")

    time.sleep(args.wait_seconds)
    print("脚本执行完成。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

from __future__ import annotations

import argparse
import sys
import time
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent
SRC_DIR = REPO_ROOT / "src"

src_dir_str = str(SRC_DIR)
if src_dir_str not in sys.path:
    sys.path.insert(0, src_dir_str)

from send_once import build_runtime_context, initialize_window, send_images_after_text


DEFAULT_CONFIG_PATH = REPO_ROOT / "config.yaml"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="倒计时结束后，自动向指定会话发送一条文本消息。"
    )
    parser.add_argument(
        "--target",
        default=None,
        help="目标会话名称；不传时会在运行时提示输入",
    )
    parser.add_argument(
        "--content",
        default=None,
        help="要发送的文本内容；不传时会在运行时提示输入",
    )
    parser.add_argument(
        "--countdown-seconds",
        type=int,
        default=None,
        help="倒计时秒数；不传时会在运行时提示输入",
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
        help="消息发送后额外等待的秒数，给 RPA 留出执行时间",
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
        help="倒计时结束后文字发送成功，再额外发送图片（可重复传多次，按顺序发送）。"
        "留空则只发文字。",
    )
    return parser.parse_args()


def prompt_if_missing(value: str | None, prompt_text: str) -> str:
    if value is not None and value.strip():
        return value.strip()
    while True:
        user_input = input(prompt_text).strip()
        if user_input:
            return user_input
        print("输入不能为空，请重新输入。")


def resolve_countdown_seconds(raw_value: int | None) -> int:
    if raw_value is not None:
        if raw_value <= 0:
            raise ValueError("倒计时秒数必须大于 0。")
        return raw_value

    while True:
        user_input = input("请输入倒计时秒数: ").strip()
        try:
            countdown_seconds = int(user_input)
        except ValueError:
            print("请输入正整数秒数，例如 60。")
            continue

        if countdown_seconds <= 0:
            print("倒计时秒数必须大于 0。")
            continue
        return countdown_seconds


def run_countdown(total_seconds: int) -> None:
    while total_seconds > 0:
        minutes, seconds = divmod(total_seconds, 60)
        print(f"\r倒计时中: {minutes:02d}:{seconds:02d}", end="", flush=True)
        time.sleep(1)
        total_seconds -= 1
    print("\r倒计时中: 00:00")


def main() -> int:
    args = parse_args()
    target = prompt_if_missing(args.target, "请输入要发送的联系人/会话名称: ")
    content = prompt_if_missing(args.content, "请输入要发送的消息内容: ")

    try:
        countdown_seconds = resolve_countdown_seconds(args.countdown_seconds)
    except ValueError as exc:
        print(str(exc))
        return 1

    config_path = Path(args.config).expanduser().resolve()
    _, window_manager, sender = build_runtime_context(config_path)

    send_time = datetime.now().timestamp() + countdown_seconds
    print(f"目标会话: {target}")
    if args.at_user_name:
        print(f"群聊 @ 成员: {args.at_user_name}")
    print(f"发送方式: {args.send_mode}")
    print(f"预计发送时间: {datetime.fromtimestamp(send_time).strftime('%Y-%m-%d %H:%M:%S')}")

    run_countdown(countdown_seconds)

    if not initialize_window(window_manager):
        print("聊天窗口初始化失败，请确认微信已登录并位于前台。")
        return 1

    if not window_manager.switch_session(target):
        print(f"切换会话失败: {target}")
        return 1
    time.sleep(max(getattr(window_manager, "switch_contact_delay", 0.3), 1.0))

    if args.at_user_name:
        sender.clear_input_box()
        if not sender.mention_user(args.at_user_name):
            print(f"@用户失败: {args.at_user_name}")
            return 1
        time.sleep(max(getattr(window_manager, "action_delay", 0.3), 0.8))

    sent_text = False
    if content:
        success = sender.send_message(
            content,
            clear_input_box=args.at_user_name is None,
            send_mode=args.send_mode,
        )
        if not success:
            print("消息发送失败，请检查微信窗口状态和目标会话名称。")
            return 1
        sent_text = True
        print(f"消息已发送到: {target}")
    else:
        print(f"本次仅发送图片到: {target}")

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

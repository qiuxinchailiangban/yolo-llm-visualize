from __future__ import annotations

import argparse
import socket
import sys
from pathlib import Path
from tempfile import TemporaryDirectory

REPO_ROOT = Path(__file__).resolve().parent
SRC_DIR = REPO_ROOT / "src"

# 允许在源码目录下直接启动，不依赖先安装 editable 包。
src_dir_str = str(SRC_DIR)
if src_dir_str not in sys.path:
    sys.path.insert(0, src_dir_str)

from ruamel.yaml import YAML
from omni_bot_sdk.bot import Bot


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="启动 omni-bot-sdk Bot。")
    parser.add_argument(
        "--config",
        default="config.yaml",
        help="配置文件路径，默认使用当前目录下的 config.yaml",
    )
    parser.add_argument(
        "--keep-mqtt",
        action="store_true",
        help="保留配置中的 MQTT 设置；默认会临时关闭 MQTT，避免本机未启动 broker 时启动失败",
    )
    parser.add_argument(
        "--mcp-port",
        type=int,
        default=None,
        help="指定 MCP 监听端口；未指定时会自动选择可用端口",
    )
    return parser.parse_args()


def is_port_available(host: str, port: int) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            sock.bind((host, port))
            return True
        except OSError:
            return False


def find_available_port(host: str, preferred_port: int, max_attempts: int = 20) -> int:
    for port in range(preferred_port, preferred_port + max_attempts):
        if is_port_available(host, port):
            return port
    raise RuntimeError(
        f"从端口 {preferred_port} 开始，连续 {max_attempts} 个端口都不可用，请手动指定 --mcp-port"
    )


def build_runtime_config(
    source_config_path: Path,
    temp_dir: Path,
    keep_mqtt: bool,
    mcp_port: int | None,
) -> Path:
    yaml = YAML()
    with source_config_path.open("r", encoding="utf-8") as file:
        config = yaml.load(file) or {}

    if not keep_mqtt:
        config["mqtt"] = {}

    mcp_config = config.get("mcp", {}) or {}
    mcp_host = mcp_config.get("host", "127.0.0.1")
    preferred_port = mcp_port or mcp_config.get("port", 8000)
    actual_port = find_available_port(mcp_host, preferred_port)
    config["mcp"] = {**mcp_config, "host": mcp_host, "port": actual_port}

    runtime_config_path = temp_dir / "config.runtime.yaml"
    with runtime_config_path.open("w", encoding="utf-8") as file:
        yaml.dump(config, file)

    print(f"MCP 将监听在 {mcp_host}:{actual_port}")
    return runtime_config_path


def main() -> int:
    args = parse_args()
    config_path = Path(args.config).expanduser().resolve()

    if not config_path.exists():
        print(f"配置文件不存在: {config_path}")
        return 1

    with TemporaryDirectory(prefix="omni-run-bot-") as temp_dir_str:
        temp_dir = Path(temp_dir_str)
        runtime_config_path = build_runtime_config(
            source_config_path=config_path,
            temp_dir=temp_dir,
            keep_mqtt=args.keep_mqtt,
            mcp_port=args.mcp_port,
        )

        bot = Bot(config_path=str(runtime_config_path))
        bot.start()
        return 0


if __name__ == "__main__":
    raise SystemExit(main())

from __future__ import annotations

import contextlib
import importlib
import io
import json
import sys
import time
import traceback
from pathlib import Path


def emit(message_type: str, **payload) -> None:
    sys.stdout.write(json.dumps({"type": message_type, **payload}, ensure_ascii=False) + "\n")
    sys.stdout.flush()


class JsonLogWriter(io.TextIOBase):
    def __init__(self, sink) -> None:
        super().__init__()
        self._sink = sink
        self._buffer = ""

    def write(self, data) -> int:
        text = str(data or "")
        if not text:
            return 0
        self._buffer += text.replace("\r\n", "\n")
        while "\n" in self._buffer:
            line, self._buffer = self._buffer.split("\n", 1)
            line = line.rstrip("\r")
            if line:
                self._sink(line)
        return len(text)

    def flush(self) -> None:
        line = self._buffer.strip()
        if line:
            self._sink(line)
        self._buffer = ""


class RuntimeTaskError(RuntimeError):
    def __init__(self, message: str, *, output: str = "", command_line: str = "") -> None:
        super().__init__(message)
        self.output = output
        self.command_line = command_line


class ResidentSendRuntime:
    def __init__(self, config: dict) -> None:
        self.config = config
        self.sdk_root = Path(str(config.get("sdk_root") or "")).expanduser().resolve()
        self.omni_config_path = Path(str(config.get("config_path") or "")).expanduser().resolve()
        self.wait_seconds = float(config.get("wait_seconds") or 8)
        self.send_mode = str(config.get("send_mode") or "enter")

        self.send_once = None
        self.window_manager = None
        self.sender = None
        self._runtime_ready = False

    def initialize(self, log_sink) -> None:
        if not self.sdk_root.exists():
            raise FileNotFoundError(f"sdk_root 不存在: {self.sdk_root}")
        if not self.omni_config_path.exists():
            raise FileNotFoundError(f"omni config 不存在: {self.omni_config_path}")

        sdk_root_str = str(self.sdk_root)
        if sdk_root_str not in sys.path:
            sys.path.insert(0, sdk_root_str)

        stdout_writer = JsonLogWriter(log_sink)
        stderr_writer = JsonLogWriter(log_sink)
        with contextlib.redirect_stdout(stdout_writer), contextlib.redirect_stderr(stderr_writer):
            self.send_once = importlib.import_module("send_once")
            _, self.window_manager, self.sender = self.send_once.build_runtime_context(self.omni_config_path)
            if not self.send_once.initialize_window(self.window_manager):
                raise RuntimeError("聊天窗口初始化失败，请确认微信已登录并处于可操作状态。")
        stdout_writer.flush()
        stderr_writer.flush()
        self._runtime_ready = True

    def execute(self, payload: dict, log_sink) -> dict:
        if not self._runtime_ready:
            self.initialize(log_sink)

        target = str(payload.get("targetConversation") or "").strip()
        content = str(payload.get("content") or "").strip()
        if not target:
            raise ValueError("targetConversation 不能为空")
        if not content:
            raise ValueError("content 不能为空")

        countdown_seconds = max(0, int(payload.get("countdownSeconds") or 0))
        qr_image_path = str(payload.get("qrImagePath") or "").strip()
        output_lines: list[str] = []

        def capture(line: str) -> None:
            output_lines.append(line)
            log_sink(line)

        stdout_writer = JsonLogWriter(capture)
        stderr_writer = JsonLogWriter(capture)
        command_line = (
            f"resident-runtime target={target} countdown={countdown_seconds}s "
            f"image={'yes' if bool(qr_image_path) else 'no'}"
        )
        try:
            with contextlib.redirect_stdout(stdout_writer), contextlib.redirect_stderr(stderr_writer):
                self._execute_once(target, content, countdown_seconds, qr_image_path)
        except Exception as exc:
            stdout_writer.flush()
            stderr_writer.flush()
            raise RuntimeTaskError(
                str(exc),
                output="\n".join(output_lines).strip(),
                command_line=command_line,
            ) from exc
        stdout_writer.flush()
        stderr_writer.flush()

        result_json = json.dumps(
            {
                "exitCode": 0,
                "taskNo": payload.get("taskNo"),
                "reminderTaskId": payload.get("reminderTaskId"),
            },
            ensure_ascii=False,
        )
        return {
            "commandLine": command_line,
            "output": "\n".join(output_lines).strip(),
            "resultJson": result_json,
        }

    def _execute_once(
        self,
        target: str,
        content: str,
        countdown_seconds: int,
        qr_image_path: str,
    ) -> None:
        assert self.window_manager is not None
        assert self.sender is not None
        assert self.send_once is not None

        if countdown_seconds > 0:
            self._run_countdown(countdown_seconds)

        if not self.window_manager.switch_session(target):
            print(f"切换会话失败，尝试重新初始化窗口后重试: {target}")
            self._runtime_ready = False
            self.initialize(print)
            if not self.window_manager.switch_session(target):
                raise RuntimeError(f"切换会话失败: {target}")
        time.sleep(max(getattr(self.window_manager, "switch_contact_delay", 0.3), 1.0))

        success = self.sender.send_message(
            content,
            clear_input_box=True,
            send_mode=self.send_mode,
        )
        if not success:
            raise RuntimeError("消息发送失败，请检查微信窗口状态和目标会话名称。")

        print(f"消息已发送到: {target}")

        if qr_image_path:
            image_file = Path(qr_image_path).expanduser().resolve()
            if image_file.exists():
                time.sleep(max(getattr(self.window_manager, "action_delay", 0.3), 1.0))
                image_ok = self.send_once.send_image_after_text(self.window_manager, str(image_file))
                if not image_ok:
                    print("警告：附加图片发送失败，但文字提醒已送达。")
            else:
                print(f"警告：图片不存在，跳过发送: {image_file}")

        time.sleep(self.wait_seconds)
        print("脚本执行完成。")

    @staticmethod
    def _run_countdown(total_seconds: int) -> None:
        while total_seconds > 0:
            minutes, seconds = divmod(total_seconds, 60)
            print(f"倒计时中: {minutes:02d}:{seconds:02d}")
            time.sleep(1)
            total_seconds -= 1
        print("倒计时中: 00:00")


def main() -> int:
    runtime: ResidentSendRuntime | None = None

    for raw_line in sys.stdin:
        line = raw_line.strip()
        if not line:
            continue
        try:
            command = json.loads(line)
        except json.JSONDecodeError as exc:
            emit("error", message=f"无法解析命令: {exc}")
            continue

        command_type = str(command.get("command") or "").strip().lower()
        if command_type == "init":
            try:
                runtime = ResidentSendRuntime(dict(command.get("config") or {}))
                runtime.initialize(lambda text: emit("log", message=text))
                emit("ready")
            except Exception as exc:  # noqa: BLE001
                emit("error", message=str(exc), traceback=traceback.format_exc())
                return 1
            continue

        if command_type == "execute":
            if runtime is None:
                emit("error", message="runtime 尚未初始化")
                continue
            try:
                result = runtime.execute(dict(command.get("payload") or {}), lambda text: emit("log", message=text))
                emit("result", ok=True, **result)
            except RuntimeTaskError as exc:
                emit(
                    "result",
                    ok=False,
                    error=str(exc),
                    output=exc.output,
                    commandLine=exc.command_line,
                    traceback=traceback.format_exc(),
                )
            except Exception as exc:  # noqa: BLE001
                emit("result", ok=False, error=str(exc), traceback=traceback.format_exc())
            continue

        if command_type == "shutdown":
            emit("stopped")
            return 0

        emit("error", message=f"未知命令: {command_type}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())

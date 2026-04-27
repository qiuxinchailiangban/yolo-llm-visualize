from __future__ import annotations

import math
import os
import queue
import subprocess
import threading
import time
import tkinter as tk
from datetime import datetime
from pathlib import Path
from tkinter import filedialog, messagebox, simpledialog
from tkinter.scrolledtext import ScrolledText
from tkinter import ttk

from worker import DEFAULT_CONFIG_PATH, load_config, run_worker_loop

try:
    import psutil  # type: ignore
except Exception:  # noqa: BLE001
    psutil = None  # type: ignore[assignment]

try:
    from omni_config_io import (
        DEFAULT_PROMPT,
        OpenAiPluginView,
        read_openai_plugin,
        write_openai_plugin,
    )
except Exception as _omni_io_exc:  # noqa: BLE001
    DEFAULT_PROMPT = ""  # type: ignore[assignment]
    OpenAiPluginView = None  # type: ignore[assignment]
    read_openai_plugin = None  # type: ignore[assignment]
    write_openai_plugin = None  # type: ignore[assignment]
    _OMNI_IO_IMPORT_ERROR: Exception | None = _omni_io_exc
else:
    _OMNI_IO_IMPORT_ERROR = None


class WorkerGuiApp:
    def __init__(self) -> None:
        self.root = tk.Tk()
        self.root.title("Desktop Worker")
        self.root.geometry("980x720")
        self.root.minsize(880, 620)
        self.root.attributes("-topmost", True)

        self.log_queue: queue.Queue[str] = queue.Queue()
        self.status_queue: queue.Queue[str] = queue.Queue()
        self.event_queue: queue.Queue[dict] = queue.Queue()
        self.stop_event = threading.Event()
        self.worker_thread: threading.Thread | None = None
        self.recent_tasks: list[dict] = []
        self.success_count = 0
        self.failure_count = 0
        self.current_countdown_job_no: str | None = None
        self.current_countdown_target = ""
        self.current_countdown_deadline: float | None = None
        self.current_countdown_waiting_result = False

        self.config_path_var = tk.StringVar(value=str(DEFAULT_CONFIG_PATH))
        self.status_var = tk.StringVar(value="STOPPED")
        self.topmost_var = tk.BooleanVar(value=True)
        self.success_var = tk.StringVar(value="0")
        self.failure_var = tk.StringVar(value="0")
        self.countdown_var = tk.StringVar(value="空闲")

        self.omni_config_path_var = tk.StringVar(value="")
        self.auto_reply_enabled_var = tk.BooleanVar(value=False)
        self.auto_reply_only_private_var = tk.BooleanVar(value=False)
        self.auto_reply_status_var = tk.StringVar(value="未加载")
        self.auto_reply_model_var = tk.StringVar(value="")
        self.auto_reply_summary_var = tk.StringVar(value="白名单 0 条 · Prompt 默认 · 记忆 开")

        # 白名单/Prompt 真正的数据源。GUI 里的 Listbox/Text 组件是一次性的
        # （弹窗关闭后就销毁），所以用缓存字段保存它们的值。
        self._whitelist_items: list[str] = []
        self._prompt_cache: str = ""

        # 弹窗里创建时才赋值；关闭后置回 None
        self.whitelist_listbox: tk.Listbox | None = None
        self.prompt_text: ScrolledText | None = None
        self._settings_win: tk.Toplevel | None = None

        self.memory_enabled_var = tk.BooleanVar(value=True)
        self.memory_max_turns_var = tk.StringVar(value="10")
        self.memory_ttl_minutes_var = tk.StringVar(value="60")
        self.memory_persist_path_var = tk.StringVar(value="")

        # omni run_bot.py 子进程相关
        self.bot_process: subprocess.Popen | None = None
        self.bot_reader_thread: threading.Thread | None = None
        self.bot_status_var = tk.StringVar(value="未启动")
        self.bot_start_button: tk.Button | None = None
        self.bot_stop_button: tk.Button | None = None

        self._build_ui()
        self._load_initial_gui_settings()
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)
        self.root.after(150, self._flush_queues)

    def _build_ui(self) -> None:
        container = tk.Frame(self.root, padx=10, pady=10)
        container.pack(fill=tk.BOTH, expand=True)

        header = tk.Frame(container)
        header.pack(fill=tk.X)

        tk.Label(header, text="配置文件").pack(side=tk.LEFT)
        tk.Entry(header, textvariable=self.config_path_var).pack(side=tk.LEFT, fill=tk.X, expand=True, padx=8)
        tk.Button(header, text="选择", width=8, command=self.choose_config).pack(side=tk.LEFT)

        controls = tk.Frame(container, pady=8)
        controls.pack(fill=tk.X)

        tk.Button(controls, text="启动 Worker", width=12, command=self.start_worker).pack(side=tk.LEFT)
        tk.Button(controls, text="停止 Worker", width=12, command=self.stop_worker).pack(side=tk.LEFT, padx=8)
        tk.Button(controls, text="清空日志", width=10, command=self.clear_logs).pack(side=tk.LEFT)

        topmost = tk.Checkbutton(
            controls,
            text="始终置顶",
            variable=self.topmost_var,
            command=self.toggle_topmost,
        )
        topmost.pack(side=tk.RIGHT)

        status_bar = tk.Frame(container)
        status_bar.pack(fill=tk.X, pady=(0, 8))

        tk.Label(status_bar, text="状态").pack(side=tk.LEFT)
        tk.Label(
            status_bar,
            textvariable=self.status_var,
            fg="#409eff",
            font=("Microsoft YaHei UI", 10, "bold"),
        ).pack(side=tk.LEFT, padx=(8, 0))

        summary = tk.Frame(container)
        summary.pack(fill=tk.X, pady=(0, 8))
        self._build_metric(summary, "成功任务", self.success_var, "#67c23a").pack(side=tk.LEFT, fill=tk.X, expand=True)
        self._build_metric(summary, "失败任务", self.failure_var, "#f56c6c").pack(
            side=tk.LEFT,
            fill=tk.X,
            expand=True,
            padx=8,
        )
        self._build_metric(summary, "当前倒计时", self.countdown_var, "#409eff").pack(side=tk.LEFT, fill=tk.X, expand=True)

        self._build_auto_reply_panel(container)

        tasks_frame = tk.Frame(container)
        tasks_frame.pack(fill=tk.X, pady=(0, 8))
        tk.Label(tasks_frame, text="最近 20 条任务", font=("Microsoft YaHei UI", 10, "bold")).pack(anchor="w")

        columns = ("created_at", "job_no", "target", "status", "countdown")
        self.task_tree = ttk.Treeview(tasks_frame, columns=columns, show="headings", height=8)
        self.task_tree.heading("created_at", text="时间")
        self.task_tree.heading("job_no", text="任务号")
        self.task_tree.heading("target", text="目标会话")
        self.task_tree.heading("status", text="状态")
        self.task_tree.heading("countdown", text="倒计时")
        self.task_tree.column("created_at", width=140, anchor="center")
        self.task_tree.column("job_no", width=180, anchor="w")
        self.task_tree.column("target", width=220, anchor="w")
        self.task_tree.column("status", width=100, anchor="center")
        self.task_tree.column("countdown", width=120, anchor="center")
        self.task_tree.pack(side=tk.LEFT, fill=tk.X, expand=True)

        task_scrollbar = ttk.Scrollbar(tasks_frame, orient=tk.VERTICAL, command=self.task_tree.yview)
        task_scrollbar.pack(side=tk.RIGHT, fill=tk.Y)
        self.task_tree.configure(yscrollcommand=task_scrollbar.set)

        tips = tk.Label(
            container,
            text="建议：点击网页发送后，把这个小窗口放在角落，倒计时结束前切回微信前台。",
            fg="#666666",
            anchor="w",
        )
        tips.pack(fill=tk.X, pady=(0, 8))

        self.log_text = ScrolledText(container, wrap=tk.WORD, font=("Consolas", 10))
        self.log_text.pack(fill=tk.BOTH, expand=True)
        self.log_text.configure(state=tk.DISABLED)

    def _build_metric(self, parent: tk.Widget, title: str, value_var: tk.StringVar, color: str) -> tk.Frame:
        frame = tk.Frame(parent, bd=1, relief=tk.GROOVE, padx=10, pady=8)
        tk.Label(frame, text=title, fg="#666666").pack(anchor="w")
        tk.Label(
            frame,
            textvariable=value_var,
            fg=color,
            font=("Microsoft YaHei UI", 14, "bold"),
            anchor="w",
        ).pack(anchor="w", pady=(4, 0))
        return frame

    def _build_auto_reply_panel(self, parent: tk.Widget) -> None:
        panel = tk.LabelFrame(
            parent,
            text="微信自动回复（LLM）",
            font=("Microsoft YaHei UI", 10, "bold"),
            padx=10,
            pady=8,
        )
        panel.pack(fill=tk.X, pady=(0, 8))

        path_row = tk.Frame(panel)
        path_row.pack(fill=tk.X)
        tk.Label(path_row, text="omni 配置").pack(side=tk.LEFT)
        tk.Entry(path_row, textvariable=self.omni_config_path_var, state="readonly").pack(
            side=tk.LEFT, fill=tk.X, expand=True, padx=8
        )
        tk.Button(path_row, text="重载", width=8, command=self.reload_auto_reply).pack(side=tk.LEFT)

        toggles_row = tk.Frame(panel)
        toggles_row.pack(fill=tk.X, pady=(6, 4))
        tk.Checkbutton(
            toggles_row,
            text="启用 LLM 自动回复（openai-bot-plugin）",
            variable=self.auto_reply_enabled_var,
        ).pack(side=tk.LEFT)
        tk.Checkbutton(
            toggles_row,
            text="仅回复私聊",
            variable=self.auto_reply_only_private_var,
        ).pack(side=tk.LEFT, padx=(16, 0))
        tk.Label(toggles_row, textvariable=self.auto_reply_status_var, fg="#888888").pack(side=tk.RIGHT)

        model_row = tk.Frame(panel)
        model_row.pack(fill=tk.X)
        tk.Label(model_row, text="当前模型", fg="#888888").pack(side=tk.LEFT)
        tk.Label(model_row, textvariable=self.auto_reply_model_var, fg="#409eff").pack(side=tk.LEFT, padx=(6, 0))
        tk.Label(model_row, text="·", fg="#cccccc").pack(side=tk.LEFT, padx=8)
        tk.Label(model_row, textvariable=self.auto_reply_summary_var, fg="#909399").pack(side=tk.LEFT)

        action_row = tk.Frame(panel)
        action_row.pack(fill=tk.X, pady=(10, 0))
        tk.Button(
            action_row,
            text="自动回复设置…",
            width=18,
            command=self.open_auto_reply_settings,
        ).pack(side=tk.LEFT)
        tk.Button(
            action_row,
            text="保存到 config.yaml",
            width=18,
            command=self.save_auto_reply,
        ).pack(side=tk.LEFT, padx=(8, 0))
        tk.Label(
            action_row,
            text="保存后需重启 Bot 才生效",
            fg="#f56c6c",
        ).pack(side=tk.LEFT, padx=(8, 0))

        bot_row = tk.Frame(panel)
        bot_row.pack(fill=tk.X, pady=(10, 0))
        tk.Label(
            bot_row,
            text="Bot 进程",
            font=("Microsoft YaHei UI", 9, "bold"),
        ).pack(side=tk.LEFT)
        self.bot_start_button = tk.Button(
            bot_row, text="启动 Bot", width=12, command=self.start_bot
        )
        self.bot_start_button.pack(side=tk.LEFT, padx=(10, 0))
        self.bot_stop_button = tk.Button(
            bot_row, text="停止 Bot", width=12, command=self.stop_bot, state=tk.DISABLED
        )
        self.bot_stop_button.pack(side=tk.LEFT, padx=(6, 0))
        tk.Label(bot_row, text="状态：").pack(side=tk.LEFT, padx=(16, 0))
        tk.Label(
            bot_row,
            textvariable=self.bot_status_var,
            fg="#409eff",
            font=("Microsoft YaHei UI", 9, "bold"),
        ).pack(side=tk.LEFT)
        tk.Label(
            bot_row,
            text="启动后日志会以 [bot] 前缀打到下方日志区",
            fg="#909399",
        ).pack(side=tk.LEFT, padx=(12, 0))

    def choose_config(self) -> None:
        selected = filedialog.askopenfilename(
            title="选择 worker 配置文件",
            filetypes=[("JSON", "*.json"), ("All files", "*.*")],
            initialdir=str(DEFAULT_CONFIG_PATH.parent),
        )
        if selected:
            self.config_path_var.set(selected)
            self._load_initial_gui_settings()

    def _load_initial_gui_settings(self) -> None:
        config_path = Path(self.config_path_var.get().strip()).expanduser()
        if not config_path.exists():
            return
        try:
            config = load_config(config_path)
        except Exception:
            return
        self.topmost_var.set(bool(config.get("always_on_top", True)))
        self.toggle_topmost()

        omni_path = str(config.get("config_path") or "").strip()
        self.omni_config_path_var.set(omni_path)
        self.reload_auto_reply()

    def _set_auto_reply_status(self, message: str, color: str = "#888888") -> None:
        self.auto_reply_status_var.set(message)

    def reload_auto_reply(self) -> None:
        if read_openai_plugin is None:
            self._set_auto_reply_status(
                f"无法加载 omni 配置：{_OMNI_IO_IMPORT_ERROR}", "#f56c6c"
            )
            return

        omni_path = self.omni_config_path_var.get().strip()
        if not omni_path:
            self._set_auto_reply_status("worker 配置里没有 config_path，无法加载", "#f56c6c")
            return
        if not Path(omni_path).exists():
            self._set_auto_reply_status(f"omni config.yaml 不存在: {omni_path}", "#f56c6c")
            return

        try:
            view = read_openai_plugin(omni_path)
        except Exception as exc:  # noqa: BLE001
            self._set_auto_reply_status(f"读取失败: {exc}", "#f56c6c")
            return

        self.auto_reply_enabled_var.set(view.enabled)
        self.auto_reply_only_private_var.set(view.only_private)
        self.auto_reply_model_var.set(view.model or "(未配置)")

        self.memory_enabled_var.set(bool(view.memory_enabled))
        self.memory_max_turns_var.set(str(view.memory_max_turns))
        self.memory_ttl_minutes_var.set(str(view.memory_ttl_minutes))
        self.memory_persist_path_var.set(view.memory_persist_path or "")

        self._whitelist_items = list(view.allowed_targets)
        self._prompt_cache = view.prompt or DEFAULT_PROMPT

        # 如果设置弹窗此刻正开着，把新值也同步到里面的控件
        if self.whitelist_listbox is not None:
            self.whitelist_listbox.delete(0, tk.END)
            for item in self._whitelist_items:
                self.whitelist_listbox.insert(tk.END, item)
        if self.prompt_text is not None:
            self.prompt_text.delete("1.0", tk.END)
            self.prompt_text.insert("1.0", self._prompt_cache)

        self._update_summary()

        if not view.raw_present:
            self._set_auto_reply_status(
                "config.yaml 里还没有 openai-bot-plugin 段，保存后会自动创建一个空模板", "#e6a23c"
            )
        else:
            stamp = datetime.now().strftime("%H:%M:%S")
            self._set_auto_reply_status(f"已加载 ({stamp})", "#67c23a")

    def _update_summary(self) -> None:
        count = len(self._whitelist_items)
        wl_text = f"白名单 {count} 条" if count else "白名单 未设置"

        if self._prompt_cache.strip() and self._prompt_cache.strip() != DEFAULT_PROMPT.strip():
            prompt_text = "Prompt 已自定义"
        else:
            prompt_text = "Prompt 默认"

        mem_text = "记忆 开" if self.memory_enabled_var.get() else "记忆 关"

        self.auto_reply_summary_var.set(f"{wl_text} · {prompt_text} · {mem_text}")

    def _sync_settings_into_cache(self) -> None:
        """把弹窗里控件当前的内容写回缓存字段，供 save_auto_reply 使用。"""
        if self.whitelist_listbox is not None:
            self._whitelist_items = [
                str(item).strip()
                for item in self.whitelist_listbox.get(0, tk.END)
                if str(item).strip()
            ]
        if self.prompt_text is not None:
            text = self.prompt_text.get("1.0", tk.END).strip()
            self._prompt_cache = text or DEFAULT_PROMPT
        self._update_summary()

    def open_auto_reply_settings(self) -> None:
        # 已经打开就拉到最前
        if self._settings_win is not None and self._settings_win.winfo_exists():
            self._settings_win.deiconify()
            self._settings_win.lift()
            self._settings_win.focus_force()
            return

        win = tk.Toplevel(self.root)
        win.title("自动回复 · 高级设置")
        win.geometry("760x640")
        win.transient(self.root)
        win.grab_set()
        self._settings_win = win

        nb = ttk.Notebook(win)
        nb.pack(fill=tk.BOTH, expand=True, padx=10, pady=(10, 0))

        # Tab 1: 白名单
        tab_wl = tk.Frame(nb, padx=10, pady=10)
        nb.add(tab_wl, text="白名单")
        tk.Label(
            tab_wl,
            text=(
                "只回复列表里的【私聊好友显示名】或【群聊名称】。\n"
                "名字必须和微信里看到的完全一致（区分空格和大小写）。\n"
                "留空 = 不限制，所有人都会被 LLM 自动回复。"
            ),
            fg="#666666",
            justify="left",
            anchor="w",
        ).pack(fill=tk.X)

        list_row = tk.Frame(tab_wl)
        list_row.pack(fill=tk.BOTH, expand=True, pady=(8, 0))

        self.whitelist_listbox = tk.Listbox(
            list_row, activestyle="dotbox", selectmode=tk.EXTENDED
        )
        self.whitelist_listbox.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        scrollbar = ttk.Scrollbar(list_row, orient=tk.VERTICAL, command=self.whitelist_listbox.yview)
        scrollbar.pack(side=tk.LEFT, fill=tk.Y)
        self.whitelist_listbox.configure(yscrollcommand=scrollbar.set)

        for item in self._whitelist_items:
            self.whitelist_listbox.insert(tk.END, item)

        button_col = tk.Frame(list_row)
        button_col.pack(side=tk.LEFT, padx=(8, 0), fill=tk.Y)
        tk.Button(button_col, text="添加…", width=10, command=self.add_whitelist_entry).pack(fill=tk.X)
        tk.Button(button_col, text="删除选中", width=10, command=self.remove_whitelist_entries).pack(fill=tk.X, pady=(4, 0))
        tk.Button(button_col, text="清空", width=10, command=self.clear_whitelist).pack(fill=tk.X, pady=(4, 0))

        # Tab 2: 人设 / Prompt
        tab_prompt = tk.Frame(nb, padx=10, pady=10)
        nb.add(tab_prompt, text="人设 / Prompt")
        tk.Label(
            tab_prompt,
            text=(
                "决定 LLM 用什么语气回复。支持下面这些变量：\n"
                "  {{self_nickname}} 你的昵称   {{contact_nickname}} 对方显示名   {{room_nickname}} 群名\n"
                "  {{chat_history}} 最近聊天   {{time_now}} 当前时间"
            ),
            fg="#909399",
            justify="left",
            anchor="w",
        ).pack(fill=tk.X)

        self.prompt_text = ScrolledText(
            tab_prompt,
            wrap=tk.WORD,
            font=("Microsoft YaHei UI", 10),
        )
        self.prompt_text.pack(fill=tk.BOTH, expand=True, pady=(6, 0))
        self.prompt_text.insert("1.0", self._prompt_cache or DEFAULT_PROMPT)

        prompt_action_row = tk.Frame(tab_prompt)
        prompt_action_row.pack(fill=tk.X, pady=(6, 0))
        tk.Button(
            prompt_action_row,
            text="恢复成默认 Prompt（模仿真人语气）",
            command=self.reset_prompt_to_default,
        ).pack(side=tk.LEFT)

        # Tab 3: 对话记忆
        tab_mem = tk.Frame(nb, padx=10, pady=10)
        nb.add(tab_mem, text="对话记忆")

        mem_row1 = tk.Frame(tab_mem)
        mem_row1.pack(fill=tk.X)
        tk.Checkbutton(
            mem_row1,
            text="启用多轮记忆（每个好友/群各自保留上下文）",
            variable=self.memory_enabled_var,
            command=self._update_summary,
        ).pack(side=tk.LEFT)
        tk.Button(
            mem_row1,
            text="清空所有会话记忆",
            width=16,
            command=self.clear_memory_store,
        ).pack(side=tk.RIGHT)

        mem_row2 = tk.Frame(tab_mem)
        mem_row2.pack(fill=tk.X, pady=(8, 0))
        tk.Label(mem_row2, text="最多保留轮数").pack(side=tk.LEFT)
        tk.Entry(mem_row2, textvariable=self.memory_max_turns_var, width=6).pack(side=tk.LEFT, padx=(4, 16))
        tk.Label(mem_row2, text="记忆超时（分钟）").pack(side=tk.LEFT)
        tk.Entry(mem_row2, textvariable=self.memory_ttl_minutes_var, width=6).pack(side=tk.LEFT, padx=(4, 16))
        tk.Label(mem_row2, text="0 = 永不过期", fg="#909399").pack(side=tk.LEFT)

        mem_row3 = tk.Frame(tab_mem)
        mem_row3.pack(fill=tk.X, pady=(8, 0))
        tk.Label(mem_row3, text="持久化文件").pack(side=tk.LEFT)
        tk.Entry(mem_row3, textvariable=self.memory_persist_path_var).pack(
            side=tk.LEFT, fill=tk.X, expand=True, padx=(4, 8)
        )
        tk.Label(
            tab_mem,
            text="持久化文件：留空=只在 Bot 进程内存里；填 json 路径则 Bot 重启也不丢记忆",
            fg="#909399",
        ).pack(anchor="w", pady=(4, 0))

        # 底部按钮
        bottom = tk.Frame(win)
        bottom.pack(fill=tk.X, padx=10, pady=10)
        tk.Label(
            bottom,
            text="改动会保留在界面里；回到主面板点「保存到 config.yaml」才会真正写入文件。",
            fg="#909399",
        ).pack(side=tk.LEFT)
        tk.Button(
            bottom,
            text="保存到 config.yaml 并关闭",
            command=lambda: self._save_and_close_settings(),
        ).pack(side=tk.RIGHT, padx=(8, 0))
        tk.Button(
            bottom,
            text="关闭",
            width=10,
            command=lambda: self._close_settings_window(),
        ).pack(side=tk.RIGHT)

        win.protocol("WM_DELETE_WINDOW", self._close_settings_window)

    def _close_settings_window(self) -> None:
        if self._settings_win is None:
            return
        self._sync_settings_into_cache()
        try:
            self._settings_win.grab_release()
        except Exception:
            pass
        self._settings_win.destroy()
        self._settings_win = None
        self.whitelist_listbox = None
        self.prompt_text = None

    def _save_and_close_settings(self) -> None:
        self._sync_settings_into_cache()
        self.save_auto_reply()
        self._close_settings_window()

    def add_whitelist_entry(self) -> None:
        if self.whitelist_listbox is None:
            return
        new_value = simpledialog.askstring(
            "添加白名单",
            "请输入要加入白名单的【私聊好友显示名】或【群聊名称】\n"
            "（必须和微信里看到的名字完全一致，区分空格和大小写）",
            parent=self.root,
        )
        if new_value is None:
            return
        new_value = new_value.strip()
        if not new_value:
            return
        existing = set(self.whitelist_listbox.get(0, tk.END))
        if new_value in existing:
            messagebox.showinfo("提示", f"「{new_value}」已经在白名单里了")
            return
        self.whitelist_listbox.insert(tk.END, new_value)
        self._set_auto_reply_status("有未保存改动，记得点「保存到 config.yaml」", "#e6a23c")
        self._sync_settings_into_cache()

    def remove_whitelist_entries(self) -> None:
        if self.whitelist_listbox is None:
            return
        selection = list(self.whitelist_listbox.curselection())
        if not selection:
            messagebox.showinfo("提示", "请先在列表里选中要删除的条目")
            return
        for index in reversed(selection):
            self.whitelist_listbox.delete(index)
        self._set_auto_reply_status("有未保存改动，记得点「保存到 config.yaml」", "#e6a23c")
        self._sync_settings_into_cache()

    def _parse_int_var(
        self,
        var: tk.StringVar,
        *,
        default: int,
        field: str,
        min_value: int,
        max_value: int,
    ) -> int:
        raw = (var.get() or "").strip()
        if not raw:
            return default
        try:
            value = int(raw)
        except ValueError as exc:
            raise ValueError(f"{field}必须是整数，当前 = {raw!r}") from exc
        if value < min_value or value > max_value:
            raise ValueError(
                f"{field}范围必须在 {min_value} ~ {max_value}，当前 = {value}"
            )
        return value

    def clear_memory_store(self) -> None:
        """清空记忆文件的内容（只在配置了 persist_path 时才有意义）。"""
        persist_path = self.memory_persist_path_var.get().strip()
        if not persist_path:
            messagebox.showinfo(
                "提示",
                "当前记忆只在 Bot 进程的内存里，没有落盘文件。\n"
                "要彻底清空内存里的记忆，只需重启 Bot 即可。",
            )
            return

        path = Path(persist_path).expanduser()
        if not path.exists():
            messagebox.showinfo("提示", f"持久化文件不存在，无需清空：{path}")
            return
        if not messagebox.askyesno(
            "确认清空",
            f"将删除记忆文件：\n{path}\n\n"
            "同时 Bot 进程内的记忆还在，要全部清掉请重启 Bot。\n继续吗？",
        ):
            return
        try:
            path.unlink()
        except Exception as exc:  # noqa: BLE001
            messagebox.showerror("删除失败", str(exc))
            self._append_log(f"[memory] 删除失败: {exc}")
            return
        self._append_log(f"[memory] 已删除记忆文件 {path}")
        messagebox.showinfo("完成", "已删除记忆文件。重启 Bot 后内存也会清空。")

    def reset_prompt_to_default(self) -> None:
        if self.prompt_text is None:
            return
        if not messagebox.askyesno(
            "恢复默认 Prompt",
            "会用内置的「模仿真人微信语气」默认 Prompt 覆盖当前编辑框里的内容。\n"
            "（此时还没写回 config.yaml，点「保存到 config.yaml」才会落盘。）\n继续吗？",
        ):
            return
        self.prompt_text.delete("1.0", tk.END)
        self.prompt_text.insert("1.0", DEFAULT_PROMPT)
        self._set_auto_reply_status("已重置 Prompt（尚未保存）", "#e6a23c")

    def clear_whitelist(self) -> None:
        if self.whitelist_listbox is None:
            return
        if self.whitelist_listbox.size() == 0:
            return
        if not messagebox.askyesno("确认", "确定清空白名单吗？\n清空后【任何人】给你发消息都会被 LLM 自动回复"):
            return
        self.whitelist_listbox.delete(0, tk.END)
        self._set_auto_reply_status("有未保存改动，记得点「保存到 config.yaml」", "#e6a23c")
        self._sync_settings_into_cache()

    def save_auto_reply(self) -> None:
        if write_openai_plugin is None:
            messagebox.showerror(
                "缺少依赖",
                f"无法保存：{_OMNI_IO_IMPORT_ERROR}\n请安装 ruamel.yaml: pip install ruamel.yaml",
            )
            return

        omni_path = self.omni_config_path_var.get().strip()
        if not omni_path:
            messagebox.showerror("配置缺失", "worker config.json 里没有 config_path，无法保存")
            return

        # 如果此刻弹窗是开着的，先把它里面的内容同步到缓存
        self._sync_settings_into_cache()

        targets = list(self._whitelist_items)
        enabled = bool(self.auto_reply_enabled_var.get())

        if enabled and not targets:
            confirm = messagebox.askyesno(
                "白名单为空",
                "白名单为空 = 任何人给你发消息都会被 LLM 自动回复。\n确认要这样保存吗？",
            )
            if not confirm:
                return

        try:
            memory_max_turns = self._parse_int_var(
                self.memory_max_turns_var, default=10, field="最多保留轮数", min_value=0, max_value=100
            )
            memory_ttl_minutes = self._parse_int_var(
                self.memory_ttl_minutes_var, default=60, field="记忆超时（分钟）", min_value=0, max_value=10080
            )
        except ValueError as exc:
            messagebox.showerror("参数不合法", str(exc))
            return

        prompt_value = (self._prompt_cache or DEFAULT_PROMPT).strip() or DEFAULT_PROMPT

        try:
            write_openai_plugin(
                omni_path,
                enabled=enabled,
                only_private=bool(self.auto_reply_only_private_var.get()),
                allowed_targets=targets,
                prompt=prompt_value,
                memory_enabled=bool(self.memory_enabled_var.get()),
                memory_max_turns=memory_max_turns,
                memory_ttl_minutes=memory_ttl_minutes,
                memory_persist_path=self.memory_persist_path_var.get().strip(),
            )
        except Exception as exc:  # noqa: BLE001
            messagebox.showerror("保存失败", str(exc))
            self._set_auto_reply_status(f"保存失败: {exc}", "#f56c6c")
            return

        stamp = datetime.now().strftime("%H:%M:%S")
        self._set_auto_reply_status(f"已保存 ({stamp})，重启 run_bot.py 后生效", "#67c23a")
        self._append_log(f"[auto-reply] 已写入 {omni_path}，enabled={enabled}，白名单 {len(targets)} 条")
        self._update_summary()
        messagebox.showinfo(
            "保存成功",
            "已写入 config.yaml。\n请到运行 omni 的命令行里按 Ctrl+C 停掉 run_bot.py，再重新启动它，配置才会生效。",
        )

    def start_worker(self) -> None:
        if self.worker_thread and self.worker_thread.is_alive():
            self._append_log("worker 已在运行")
            return

        config_path = Path(self.config_path_var.get().strip()).expanduser()
        if not config_path.exists():
            messagebox.showerror("配置不存在", f"找不到配置文件：{config_path}")
            return

        self.stop_event.clear()
        self.status_var.set("STARTING")
        self._append_log(f"starting worker with config: {config_path}")

        self.worker_thread = threading.Thread(
            target=self._run_worker,
            args=(config_path,),
            daemon=True,
        )
        self.worker_thread.start()

    def _run_worker(self, config_path: Path) -> None:
        try:
            run_worker_loop(
                config_path,
                log_func=self.log_queue.put,
                should_stop=self.stop_event.is_set,
                status_callback=self.status_queue.put,
                event_callback=self.event_queue.put,
            )
        except Exception as exc:  # noqa: BLE001
            self.log_queue.put(f"[gui-error] {exc}")
            self.status_queue.put("WORKER_CRASHED")

    def stop_worker(self) -> None:
        self.stop_event.set()
        self.status_var.set("STOPPING")
        self._append_log("stopping worker...")

    # --- omni bot 子进程控制 -----------------------------------------------

    def _is_bot_running(self) -> bool:
        return self.bot_process is not None and self.bot_process.poll() is None

    def _set_bot_buttons(self, running: bool) -> None:
        if self.bot_start_button is not None:
            self.bot_start_button.configure(state=tk.DISABLED if running else tk.NORMAL)
        if self.bot_stop_button is not None:
            self.bot_stop_button.configure(state=tk.NORMAL if running else tk.DISABLED)

    def start_bot(self) -> None:
        if self._is_bot_running():
            self._append_log("[bot] 已在运行，忽略重复启动")
            return

        worker_config_path = Path(self.config_path_var.get().strip()).expanduser()
        if not worker_config_path.exists():
            messagebox.showerror("配置不存在", f"找不到 worker 配置：{worker_config_path}")
            return
        try:
            worker_config = load_config(worker_config_path)
        except Exception as exc:  # noqa: BLE001
            messagebox.showerror("配置读取失败", str(exc))
            return

        sdk_root_str = str(worker_config.get("sdk_root") or "").strip()
        omni_config_str = str(worker_config.get("config_path") or "").strip()
        python_command = str(worker_config.get("python_command") or "python").strip() or "python"

        if not sdk_root_str:
            messagebox.showerror(
                "缺少配置",
                "worker config.json 里没有 sdk_root，无法启动 omni bot。",
            )
            return
        sdk_root = Path(sdk_root_str).expanduser().resolve()
        if not sdk_root.exists():
            messagebox.showerror("路径不存在", f"sdk_root 路径不存在：{sdk_root}")
            return
        run_bot_py = sdk_root / "run_bot.py"
        if not run_bot_py.exists():
            messagebox.showerror("脚本不存在", f"找不到 {run_bot_py}")
            return

        cmd = [python_command, "-u", str(run_bot_py)]
        if omni_config_str:
            cmd.extend(["--config", omni_config_str])

        env = os.environ.copy()
        env.setdefault("PYTHONIOENCODING", "utf-8")
        env.setdefault("PYTHONUNBUFFERED", "1")

        creationflags = 0
        if os.name == "nt":
            creationflags = subprocess.CREATE_NEW_PROCESS_GROUP  # type: ignore[attr-defined]

        self._append_log(f"[bot] 启动: {' '.join(cmd)} (cwd={sdk_root})")

        try:
            self.bot_process = subprocess.Popen(
                cmd,
                cwd=str(sdk_root),
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                stdin=subprocess.DEVNULL,
                env=env,
                bufsize=1,
                text=True,
                encoding="utf-8",
                errors="replace",
                creationflags=creationflags,
            )
        except Exception as exc:  # noqa: BLE001
            self.bot_process = None
            messagebox.showerror("启动失败", str(exc))
            self._append_log(f"[bot] 启动失败: {exc}")
            return

        self.bot_status_var.set(f"运行中 (PID {self.bot_process.pid})")
        self._set_bot_buttons(True)

        self.bot_reader_thread = threading.Thread(
            target=self._read_bot_output,
            args=(self.bot_process,),
            daemon=True,
        )
        self.bot_reader_thread.start()

    def _read_bot_output(self, process: subprocess.Popen) -> None:
        try:
            assert process.stdout is not None
            for raw_line in process.stdout:
                line = raw_line.rstrip("\r\n")
                if line:
                    self.log_queue.put(f"[bot] {line}")
        except Exception as exc:  # noqa: BLE001
            self.log_queue.put(f"[bot] 日志读取异常: {exc}")
        finally:
            return_code = process.wait()
            self.log_queue.put(f"[bot] 进程结束，exit code = {return_code}")
            # 切换 GUI 状态需要回到主线程
            self.root.after(0, self._on_bot_exited)

    def _on_bot_exited(self) -> None:
        self.bot_process = None
        self.bot_status_var.set("已停止")
        self._set_bot_buttons(False)

    def stop_bot(self) -> None:
        if not self._is_bot_running():
            self._append_log("[bot] 未在运行")
            self._on_bot_exited()
            return

        process = self.bot_process
        assert process is not None
        self.bot_status_var.set("停止中…")
        self._append_log(f"[bot] 正在停止 PID {process.pid} …")

        terminated = False
        if psutil is not None:
            try:
                parent = psutil.Process(process.pid)
                children = parent.children(recursive=True)
                for child in children:
                    try:
                        child.terminate()
                    except Exception:  # noqa: BLE001
                        pass
                parent.terminate()
                gone, alive = psutil.wait_procs([parent, *children], timeout=5)
                for proc in alive:
                    try:
                        proc.kill()
                    except Exception:  # noqa: BLE001
                        pass
                terminated = True
            except Exception as exc:  # noqa: BLE001
                self._append_log(f"[bot] psutil 终止失败，回退到 subprocess.terminate: {exc}")

        if not terminated:
            try:
                process.terminate()
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    process.kill()
            except Exception as exc:  # noqa: BLE001
                self._append_log(f"[bot] 终止失败: {exc}")

    def clear_logs(self) -> None:
        self.log_text.configure(state=tk.NORMAL)
        self.log_text.delete("1.0", tk.END)
        self.log_text.configure(state=tk.DISABLED)

    def toggle_topmost(self) -> None:
        self.root.attributes("-topmost", bool(self.topmost_var.get()))

    def _append_log(self, message: str) -> None:
        self.log_text.configure(state=tk.NORMAL)
        self.log_text.insert(tk.END, message + "\n")
        self.log_text.see(tk.END)
        self.log_text.configure(state=tk.DISABLED)

    def _record_task(self, job_no: str, target: str, status: str, countdown_text: str) -> None:
        for task in self.recent_tasks:
            if task["job_no"] == job_no:
                task["target"] = target
                task["status"] = status
                task["countdown"] = countdown_text
                task["created_at"] = datetime.now().strftime("%H:%M:%S")
                self._refresh_task_tree()
                return

        self.recent_tasks.insert(
            0,
            {
                "created_at": datetime.now().strftime("%H:%M:%S"),
                "job_no": job_no,
                "target": target,
                "status": status,
                "countdown": countdown_text,
            },
        )
        self.recent_tasks = self.recent_tasks[:20]
        self._refresh_task_tree()

    def _refresh_task_tree(self) -> None:
        for item_id in self.task_tree.get_children():
            self.task_tree.delete(item_id)
        for task in self.recent_tasks:
            self.task_tree.insert(
                "",
                tk.END,
                values=(
                    task["created_at"],
                    task["job_no"],
                    task["target"],
                    task["status"],
                    task["countdown"],
                ),
            )

    def _handle_event(self, event: dict) -> None:
        event_type = event.get("type")
        job_no = str(event.get("jobNo") or "")
        target = str(event.get("targetConversation") or "")

        if event_type == "claimed":
            countdown_seconds = max(0, int(event.get("countdownSeconds") or 0))
            countdown_text = f"{countdown_seconds}s" if countdown_seconds > 0 else "-"
            self._record_task(job_no, target, "运行中", countdown_text)
            self.current_countdown_job_no = job_no
            self.current_countdown_target = target
            if countdown_seconds > 0:
                self.current_countdown_deadline = time.time() + countdown_seconds
                self.current_countdown_waiting_result = False
            else:
                self.current_countdown_deadline = None
                self.current_countdown_waiting_result = False
                self.countdown_var.set(f"{job_no} 无倒计时，执行中")
            return

        if event_type == "success":
            self.success_count += 1
            self.success_var.set(str(self.success_count))
            self._record_task(job_no, target, "成功", "完成")
            if self.current_countdown_job_no == job_no:
                self.current_countdown_job_no = None
                self.current_countdown_target = ""
                self.current_countdown_deadline = None
                self.current_countdown_waiting_result = False
                self.countdown_var.set("空闲")
            return

        if event_type == "failed":
            self.failure_count += 1
            self.failure_var.set(str(self.failure_count))
            self._record_task(job_no, target, "失败", "结束")
            if self.current_countdown_job_no == job_no:
                self.current_countdown_job_no = None
                self.current_countdown_target = ""
                self.current_countdown_deadline = None
                self.current_countdown_waiting_result = False
                self.countdown_var.set("空闲")
            return

        if event_type == "idle" and self.current_countdown_job_no is None:
            self.countdown_var.set("空闲")

    def _update_countdown_status(self) -> None:
        if not self.current_countdown_job_no:
            return
        if self.current_countdown_deadline is None:
            if self.current_countdown_waiting_result:
                self.countdown_var.set(f"{self.current_countdown_job_no} 倒计时结束，等待发送结果")
            return

        remaining_seconds = max(0, math.ceil(self.current_countdown_deadline - time.time()))
        if remaining_seconds > 0:
            minutes, seconds = divmod(remaining_seconds, 60)
            countdown_text = f"剩余 {minutes:02d}:{seconds:02d}"
            self.countdown_var.set(f"{self.current_countdown_job_no} {countdown_text}")
            self._record_task(self.current_countdown_job_no, self.current_countdown_target, "倒计时中", countdown_text)
            return

        self.current_countdown_deadline = None
        self.current_countdown_waiting_result = True
        self.countdown_var.set(f"{self.current_countdown_job_no} 倒计时结束，等待发送结果")
        self._record_task(self.current_countdown_job_no, self.current_countdown_target, "发送中", "00:00")

    def _flush_queues(self) -> None:
        while True:
            try:
                message = self.log_queue.get_nowait()
            except queue.Empty:
                break
            self._append_log(message)

        while True:
            try:
                event = self.event_queue.get_nowait()
            except queue.Empty:
                break
            self._handle_event(event)

        while True:
            try:
                status = self.status_queue.get_nowait()
            except queue.Empty:
                break
            self.status_var.set(status)

        self._update_countdown_status()
        self.root.after(150, self._flush_queues)

    def on_close(self) -> None:
        self.stop_event.set()
        if self._is_bot_running():
            try:
                self.stop_bot()
            except Exception:  # noqa: BLE001
                pass
        self.root.after(200, self.root.destroy)

    def run(self) -> None:
        self.root.mainloop()


def main() -> int:
    app = WorkerGuiApp()
    app.run()
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

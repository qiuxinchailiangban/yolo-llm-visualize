const state = {
  configPath: "",
  workerSummary: null,
  adminUrl: "",
  backendBaseUrl: "",
  workerRunning: false,
  backendRunning: false,
  backendManagedByElectron: false,
  workerStatus: "STOPPED",
  backendStatus: "STOPPED",
  botRunning: false,
  botStatus: "STOPPED",
  logs: [],
  recentTasks: [],
  successCount: 0,
  failureCount: 0,
  currentCountdownJobNo: null,
  currentCountdownTarget: "",
  currentCountdownDeadline: null,
  currentCountdownWaitingResult: false,
  activeTab: "admin",
  autoReply: {
    omniConfigPath: "",
    resolvedPythonCommand: "",
    enabled: false,
    onlyPrivate: false,
    model: "",
    status: "未加载",
    summary: "白名单 0 条 · Prompt 默认 · 记忆 开",
    whitelistItems: [],
    prompt: "",
    defaultPrompt: "",
    memoryEnabled: true,
    memoryMaxTurns: 10,
    memoryTtlMinutes: 60,
    memoryPersistPath: "",
  },
};

const refs = {
  adminUrl: document.getElementById("adminUrl"),
  chooseConfigButton: document.getElementById("chooseConfigButton"),
  workerToggleButton: document.getElementById("workerToggleButton"),
  startBackendButton: document.getElementById("startBackendButton"),
  stopBackendButton: document.getElementById("stopBackendButton"),
  reloadAdminButton: document.getElementById("reloadAdminButton"),
  clearLogsButton: document.getElementById("clearLogsButton"),
  topmostToggle: document.getElementById("topmostToggle"),
  workerStatusValue: document.getElementById("workerStatusValue"),
  backendStatusValue: document.getElementById("backendStatusValue"),
  successValue: document.getElementById("successValue"),
  failureValue: document.getElementById("failureValue"),
  countdownValue: document.getElementById("countdownValue"),
  backendValue: document.getElementById("backendValue"),
  workerIdValue: document.getElementById("workerIdValue"),
  jobTypeValue: document.getElementById("jobTypeValue"),
  pythonEnvValue: document.getElementById("pythonEnvValue"),
  taskTableBody: document.getElementById("taskTableBody"),
  logOutput: document.getElementById("logOutput"),
  adminFrame: document.getElementById("adminFrame"),
  tabAdminButton: document.getElementById("tabAdminButton"),
  tabWorkerButton: document.getElementById("tabWorkerButton"),
  adminTab: document.getElementById("adminTab"),
  workerTab: document.getElementById("workerTab"),
  omniConfigPathValue: document.getElementById("omniConfigPathValue"),
  autoReplyModelValue: document.getElementById("autoReplyModelValue"),
  autoReplyStatusValue: document.getElementById("autoReplyStatusValue"),
  autoReplyEnabledToggle: document.getElementById("autoReplyEnabledToggle"),
  autoReplyOnlyPrivateToggle: document.getElementById("autoReplyOnlyPrivateToggle"),
  whitelistInput: document.getElementById("whitelistInput"),
  promptInput: document.getElementById("promptInput"),
  memoryEnabledToggle: document.getElementById("memoryEnabledToggle"),
  memoryMaxTurnsInput: document.getElementById("memoryMaxTurnsInput"),
  memoryTtlInput: document.getElementById("memoryTtlInput"),
  memoryPersistInput: document.getElementById("memoryPersistInput"),
  reloadAutoReplyButton: document.getElementById("reloadAutoReplyButton"),
  saveAutoReplyButton: document.getElementById("saveAutoReplyButton"),
  clearMemoryButton: document.getElementById("clearMemoryButton"),
  botToggleButton: document.getElementById("botToggleButton"),
  openWorkspaceSettingsButton: document.getElementById("openWorkspaceSettingsButton"),
  workspaceOverlay: document.getElementById("workspaceOverlay"),
  closeDrawerButton: document.getElementById("closeDrawerButton"),
  drawerTitle: document.getElementById("drawerTitle"),
  drawerSubtitle: document.getElementById("drawerSubtitle"),
  workerConfigPanel: document.getElementById("workerConfigPanel"),
  autoReplyPanel: document.getElementById("autoReplyPanel"),
  workerConfigPathValue: document.getElementById("workerConfigPathValue"),
};

function pushLog(message) {
  const stamp = new Date().toLocaleTimeString("zh-CN", { hour12: false });
  state.logs.push(`[${stamp}] ${message}`);
  state.logs = state.logs.slice(-800);
  refs.logOutput.textContent = state.logs.join("\n");
  refs.logOutput.scrollTop = refs.logOutput.scrollHeight;
}

function setActiveTab(tab) {
  state.activeTab = tab;
  const isAdmin = tab === "admin";
  refs.tabAdminButton.classList.toggle("active", isAdmin);
  refs.tabWorkerButton.classList.toggle("active", !isAdmin);
  refs.adminTab.classList.toggle("active", isAdmin);
  refs.workerTab.classList.toggle("active", !isAdmin);
}

function computeAutoReplySummary() {
  const count = state.autoReply.whitelistItems.length;
  const whitelistText = count > 0 ? `白名单 ${count} 条` : "白名单 未设置";
  const defaultPrompt = (state.autoReply.defaultPrompt || "").trim();
  const prompt = (state.autoReply.prompt || "").trim();
  const promptText = prompt && defaultPrompt && prompt !== defaultPrompt ? "Prompt 已自定义" : "Prompt 默认";
  const memoryText = state.autoReply.memoryEnabled ? "记忆 开" : "记忆 关";
  state.autoReply.summary = `${whitelistText} · ${promptText} · ${memoryText}`;
}

function renderAutoReply() {
  computeAutoReplySummary();
  refs.omniConfigPathValue.textContent = state.autoReply.omniConfigPath || "-";
  refs.autoReplyModelValue.textContent = state.autoReply.model || "(未配置)";
  refs.autoReplyStatusValue.textContent = state.autoReply.status || "未加载";
  refs.autoReplyEnabledToggle.checked = Boolean(state.autoReply.enabled);
  refs.autoReplyOnlyPrivateToggle.checked = Boolean(state.autoReply.onlyPrivate);
  refs.whitelistInput.value = state.autoReply.whitelistItems.join("\n");
  refs.promptInput.value = state.autoReply.prompt || state.autoReply.defaultPrompt || "";
  refs.memoryEnabledToggle.checked = Boolean(state.autoReply.memoryEnabled);
  refs.memoryMaxTurnsInput.value = String(state.autoReply.memoryMaxTurns ?? 10);
  refs.memoryTtlInput.value = String(state.autoReply.memoryTtlMinutes ?? 60);
  refs.memoryPersistInput.value = state.autoReply.memoryPersistPath || "";
}

function renderSummary() {
  refs.workerStatusValue.textContent = state.workerStatus;
  refs.backendStatusValue.textContent = state.backendStatus;
  refs.successValue.textContent = String(state.successCount);
  refs.failureValue.textContent = String(state.failureCount);
  refs.startBackendButton.disabled = state.backendRunning;
  refs.stopBackendButton.disabled = !state.backendRunning || !state.backendManagedByElectron;
  refs.workerConfigPathValue.textContent = state.configPath || "-";
  refs.adminUrl.textContent = state.adminUrl || "-";

  const summary = state.workerSummary || {};
  refs.backendValue.textContent = state.backendBaseUrl || summary.backendBaseUrl || "-";
  refs.workerIdValue.textContent = summary.workerId || "-";
  refs.jobTypeValue.textContent = summary.jobType || "-";
  refs.pythonEnvValue.textContent = summary.resolvedPythonCommand || summary.pythonCommand || "-";
  refs.topmostToggle.checked = Boolean(summary.alwaysOnTop);
  refs.workerToggleButton.classList.toggle("is-on", state.workerRunning);
  refs.workerToggleButton.setAttribute("aria-checked", String(state.workerRunning));
  refs.workerToggleButton.setAttribute("title", state.workerRunning ? "停止 Worker" : "启动 Worker");
  refs.botToggleButton.classList.toggle("is-on", state.botRunning);
  refs.botToggleButton.setAttribute("aria-checked", String(state.botRunning));
  refs.botToggleButton.setAttribute("title", state.botRunning ? "停止 Bot" : "启动 Bot");
  renderAutoReply();
}

function openDrawer() {
  refs.workspaceOverlay.classList.add("active");
  refs.workerConfigPanel.classList.add("active");
  refs.autoReplyPanel.classList.add("active");
  refs.drawerTitle.textContent = "工作区设置";
  refs.drawerSubtitle.textContent = "集中管理 Worker 配置、置顶选项和微信自动回复参数";
}

function closeDrawer() {
  refs.workspaceOverlay.classList.remove("active");
}

function statusClass(status) {
  if (status.includes("成功")) return "success";
  if (status.includes("失败")) return "failed";
  if (status.includes("运行") || status.includes("发送") || status.includes("倒计时")) return "running";
  return "pending";
}

function renderTasks() {
  refs.taskTableBody.innerHTML = "";

  if (state.recentTasks.length === 0) {
    const row = document.createElement("tr");
    row.className = "empty-row";
    row.innerHTML = '<td colspan="5">暂无任务</td>';
    refs.taskTableBody.appendChild(row);
    return;
  }

  state.recentTasks.forEach((task) => {
    const row = document.createElement("tr");
    row.innerHTML = `
      <td>${task.createdAt}</td>
      <td title="${task.jobNo}">${task.jobNo}</td>
      <td title="${task.target}">${task.target || "-"}</td>
      <td><span class="status-pill ${statusClass(task.status)}">${task.status}</span></td>
      <td>${task.countdown}</td>
    `;
    refs.taskTableBody.appendChild(row);
  });
}

function recordTask(jobNo, target, status, countdown) {
  const existing = state.recentTasks.find((task) => task.jobNo === jobNo);
  const createdAt = new Date().toLocaleTimeString("zh-CN", { hour12: false });

  if (existing) {
    existing.target = target;
    existing.status = status;
    existing.countdown = countdown;
    existing.createdAt = createdAt;
  } else {
    state.recentTasks.unshift({
      createdAt,
      jobNo,
      target,
      status,
      countdown,
    });
    state.recentTasks = state.recentTasks.slice(0, 20);
  }

  renderTasks();
}

function renderCountdown() {
  if (!state.currentCountdownJobNo) {
    refs.countdownValue.textContent = state.workerRunning ? "空闲" : "未运行";
    return;
  }

  if (state.currentCountdownDeadline) {
    const remainingMs = state.currentCountdownDeadline - Date.now();
    const remainingSeconds = Math.max(0, Math.ceil(remainingMs / 1000));
    if (remainingSeconds > 0) {
      const minutes = String(Math.floor(remainingSeconds / 60)).padStart(2, "0");
      const seconds = String(remainingSeconds % 60).padStart(2, "0");
      const text = `剩余 ${minutes}:${seconds}`;
      refs.countdownValue.textContent = `${state.currentCountdownJobNo} ${text}`;
      recordTask(state.currentCountdownJobNo, state.currentCountdownTarget, "倒计时中", text);
      return;
    }

    state.currentCountdownDeadline = null;
    state.currentCountdownWaitingResult = true;
    refs.countdownValue.textContent = `${state.currentCountdownJobNo} 倒计时结束，等待发送结果`;
    recordTask(state.currentCountdownJobNo, state.currentCountdownTarget, "发送中", "00:00");
    return;
  }

  if (state.currentCountdownWaitingResult) {
    refs.countdownValue.textContent = `${state.currentCountdownJobNo} 倒计时结束，等待发送结果`;
    return;
  }

  refs.countdownValue.textContent = `${state.currentCountdownJobNo} 无倒计时，执行中`;
}

function applyWorkerConfig(summary) {
  state.workerSummary = summary || null;
  if (summary && summary.configPath) {
    state.configPath = summary.configPath;
  }
  if (summary?.omniConfigPath) {
    state.autoReply.omniConfigPath = summary.omniConfigPath;
  }
  if (summary?.resolvedPythonCommand) {
    state.autoReply.resolvedPythonCommand = summary.resolvedPythonCommand;
  }
  renderSummary();
}

async function refreshAutoReply(options = {}) {
  const silent = Boolean(options.silent);
  try {
    const response = await window.desktopWorker.loadOmniSettings(state.configPath);
    const data = response?.data || response || {};
    state.autoReply = {
      ...state.autoReply,
      omniConfigPath: data.omniConfigPath || "",
      resolvedPythonCommand: data.resolvedPythonCommand || state.autoReply.resolvedPythonCommand,
      enabled: Boolean(data.enabled),
      onlyPrivate: Boolean(data.only_private),
      model: data.model || "",
      status: data.raw_present === false
        ? "config.yaml 里还没有 openai-bot-plugin，保存时会自动创建"
        : `已加载 ${new Date().toLocaleTimeString("zh-CN", { hour12: false })}`,
      whitelistItems: Array.isArray(data.allowed_targets) ? data.allowed_targets : [],
      prompt: data.prompt || data.default_prompt || "",
      defaultPrompt: data.default_prompt || state.autoReply.defaultPrompt || "",
      memoryEnabled: data.memory_enabled !== false,
      memoryMaxTurns: Number(data.memory_max_turns ?? 10),
      memoryTtlMinutes: Number(data.memory_ttl_minutes ?? 60),
      memoryPersistPath: data.memory_persist_path || "",
    };
    renderAutoReply();
    if (!silent) {
      pushLog(`[auto-reply] 已加载 ${state.autoReply.omniConfigPath}`);
    }
  } catch (error) {
    state.autoReply.status = `读取失败: ${error.message || error}`;
    renderAutoReply();
    pushLog(`[auto-reply-error] ${error.message || error}`);
  }
}

function syncAutoReplyFromInputs() {
  state.autoReply.enabled = refs.autoReplyEnabledToggle.checked;
  state.autoReply.onlyPrivate = refs.autoReplyOnlyPrivateToggle.checked;
  state.autoReply.whitelistItems = refs.whitelistInput.value
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean);
  state.autoReply.prompt = refs.promptInput.value.trim() || state.autoReply.defaultPrompt || "";
  state.autoReply.memoryEnabled = refs.memoryEnabledToggle.checked;
  state.autoReply.memoryMaxTurns = Number(refs.memoryMaxTurnsInput.value || 10);
  state.autoReply.memoryTtlMinutes = Number(refs.memoryTtlInput.value || 60);
  state.autoReply.memoryPersistPath = refs.memoryPersistInput.value.trim();
  computeAutoReplySummary();
}

async function saveAutoReply() {
  syncAutoReplyFromInputs();
  if (state.autoReply.enabled && state.autoReply.whitelistItems.length === 0) {
    const confirmed = window.confirm("白名单为空时，任何人给你发消息都会被 LLM 自动回复。确定继续保存吗？");
    if (!confirmed) {
      return;
    }
  }
  try {
    await window.desktopWorker.saveOmniSettings({
      configPath: state.configPath,
      settings: {
        enabled: state.autoReply.enabled,
        only_private: state.autoReply.onlyPrivate,
        allowed_targets: state.autoReply.whitelistItems,
        prompt: state.autoReply.prompt,
        memory_enabled: state.autoReply.memoryEnabled,
        memory_max_turns: state.autoReply.memoryMaxTurns,
        memory_ttl_minutes: state.autoReply.memoryTtlMinutes,
        memory_persist_path: state.autoReply.memoryPersistPath,
      },
    });
    state.autoReply.status = `已保存 ${new Date().toLocaleTimeString("zh-CN", { hour12: false })}，重启 Bot 后生效`;
    renderAutoReply();
    pushLog(`[auto-reply] 已写入 ${state.autoReply.omniConfigPath}，白名单 ${state.autoReply.whitelistItems.length} 条`);
    window.alert("已保存到 config.yaml。\n请重启 Bot，让最新自动回复配置生效。");
  } catch (error) {
    state.autoReply.status = `保存失败: ${error.message || error}`;
    renderAutoReply();
    pushLog(`[auto-reply-save-error] ${error.message || error}`);
  }
}

async function clearMemoryStore() {
  syncAutoReplyFromInputs();
  if (!state.autoReply.memoryPersistPath) {
    window.alert("当前没有配置记忆持久化文件。\n如果只是清空内存中的记忆，直接重启 Bot 即可。");
    return;
  }
  const confirmed = window.confirm(`将删除记忆文件：\n${state.autoReply.memoryPersistPath}\n\n继续吗？`);
  if (!confirmed) {
    return;
  }
  try {
    const result = await window.desktopWorker.clearOmniMemory(state.autoReply.memoryPersistPath);
    pushLog(result?.deleted ? `[memory] 已删除 ${state.autoReply.memoryPersistPath}` : `[memory] 记忆文件不存在：${state.autoReply.memoryPersistPath}`);
    window.alert(result?.deleted ? "已删除记忆文件。重启 Bot 后内存记忆也会清空。" : "记忆文件不存在，无需清空。");
  } catch (error) {
    pushLog(`[memory-error] ${error.message || error}`);
  }
}

function handleWorkerBridgePayload(payload) {
  if (payload.type === "worker:config") {
    applyWorkerConfig(payload.summary || null);
    refreshAutoReply({ silent: true });
    return;
  }

  if (payload.type === "status") {
    state.workerStatus = payload.status || state.workerStatus;
    state.workerRunning = !["STOPPED", "CONFIG_MISSING"].includes(state.workerStatus);
    renderSummary();
    return;
  }

  if (payload.type === "lifecycle") {
    state.workerStatus = payload.status || state.workerStatus;
    if (payload.status === "STOPPED") {
      state.workerRunning = false;
      state.currentCountdownJobNo = null;
      state.currentCountdownTarget = "";
      state.currentCountdownDeadline = null;
      state.currentCountdownWaitingResult = false;
    }
    renderSummary();
    renderCountdown();
    return;
  }

  if (payload.type === "log") {
    pushLog(`[worker] ${payload.message || ""}`);
    return;
  }

  if (payload.type === "error") {
    pushLog(`[worker-error] ${payload.message || "unknown error"}`);
    if (payload.traceback) {
      pushLog(payload.traceback);
    }
    return;
  }

  if (payload.type !== "event") {
    return;
  }

  const event = payload.event || {};
  const eventType = event.type;
  const jobNo = String(event.jobNo || "");
  const target = String(event.targetConversation || "");

  if (eventType === "claimed") {
    const countdownSeconds = Math.max(0, Number(event.countdownSeconds || 0));
    recordTask(jobNo, target, "运行中", countdownSeconds > 0 ? `${countdownSeconds}s` : "-");
    state.currentCountdownJobNo = jobNo;
    state.currentCountdownTarget = target;
    state.currentCountdownWaitingResult = false;
    state.currentCountdownDeadline = countdownSeconds > 0 ? Date.now() + countdownSeconds * 1000 : null;
    renderCountdown();
    return;
  }

  if (eventType === "success") {
    state.successCount += 1;
    recordTask(jobNo, target, "成功", "完成");
    if (state.currentCountdownJobNo === jobNo) {
      state.currentCountdownJobNo = null;
      state.currentCountdownTarget = "";
      state.currentCountdownDeadline = null;
      state.currentCountdownWaitingResult = false;
    }
    renderSummary();
    renderCountdown();
    return;
  }

  if (eventType === "failed") {
    state.failureCount += 1;
    recordTask(jobNo, target, "失败", "结束");
    if (state.currentCountdownJobNo === jobNo) {
      state.currentCountdownJobNo = null;
      state.currentCountdownTarget = "";
      state.currentCountdownDeadline = null;
      state.currentCountdownWaitingResult = false;
    }
    renderSummary();
    renderCountdown();
    if (event.errorMessage) {
      pushLog(`[task-error] ${jobNo} ${event.errorMessage}`);
    }
    return;
  }

  if (eventType === "idle") {
    renderCountdown();
  }
}

function handleDesktopEvent(payload) {
  if (!payload || typeof payload !== "object") {
    return;
  }

  if (payload.type === "ui:switch-tab") {
    if (payload.tab === "worker" || payload.tab === "admin") {
      setActiveTab(payload.tab);
    }
    return;
  }

  if (["log", "status", "event", "lifecycle", "error", "worker:config"].includes(payload.type)) {
    handleWorkerBridgePayload(payload);
    return;
  }

  if (payload.type === "backend:status") {
    state.backendStatus = payload.status || state.backendStatus;
    state.backendRunning = !["STOPPED", "START_TIMEOUT"].includes(state.backendStatus);
    renderSummary();
    return;
  }

  if (payload.type === "backend:log") {
    pushLog(`[backend] ${payload.message || ""}`);
    return;
  }

  if (payload.type === "backend:error") {
    pushLog(`[backend-error] ${payload.message || ""}`);
    return;
  }

  if (payload.type === "bot:status") {
    state.botStatus = payload.status || state.botStatus;
    state.botRunning = !["STOPPED"].includes(state.botStatus);
    renderSummary();
    if (payload.status === "STOPPED" && typeof payload.exitCode === "number") {
      pushLog(`[bot] 已退出，exitCode=${payload.exitCode}`);
    }
    return;
  }

  if (payload.type === "bot:log") {
    pushLog(`[bot] ${payload.message || ""}`);
    return;
  }

  if (payload.type === "bot:error") {
    pushLog(`[bot-error] ${payload.message || ""}`);
  }
}

async function boot() {
  const initialState = await window.desktopWorker.getInitialState();
  state.configPath = initialState.configPath || "";
  state.workerSummary = initialState.workerSummary || null;
  state.workerRunning = Boolean(initialState.workerRunning);
  state.backendRunning = Boolean(initialState.backendRunning);
  state.backendManagedByElectron = Boolean(initialState.backendManagedByElectron);
  state.backendBaseUrl = initialState.backendBaseUrl || "";
  state.adminUrl = initialState.adminUrl || "";
  state.botRunning = Boolean(initialState.botRunning);
  state.botStatus = initialState.botStatus || "STOPPED";
  state.workerStatus = state.workerRunning ? "RUNNING" : "STOPPED";
  state.backendStatus = state.backendRunning ? "READY" : "STOPPED";

  if (state.adminUrl) {
    refs.adminFrame.src = state.adminUrl;
  }

  renderSummary();
  renderTasks();
  renderCountdown();
  setActiveTab("admin");
  await refreshAutoReply({ silent: true });

  window.desktopWorker.onEvent(handleDesktopEvent);

  refs.tabAdminButton.addEventListener("click", () => setActiveTab("admin"));
  refs.tabWorkerButton.addEventListener("click", () => setActiveTab("worker"));

  refs.chooseConfigButton.addEventListener("click", async () => {
    const selected = await window.desktopWorker.chooseConfig();
    if (!selected) {
      return;
    }
    state.configPath = selected.configPath;
    applyWorkerConfig(selected.summary || null);
    pushLog(`已切换 worker 配置文件: ${state.configPath}`);
    await refreshAutoReply({ silent: false });
  });

  refs.workerToggleButton.addEventListener("click", async () => {
    if (state.workerRunning) {
      try {
        await window.desktopWorker.stopWorker();
        pushLog("正在请求停止 worker...");
      } catch (error) {
        pushLog(`[stop-worker-error] ${error.message || error}`);
      }
      return;
    }
    try {
      const result = await window.desktopWorker.startWorker(state.configPath);
      state.workerRunning = true;
      state.workerStatus = "STARTING";
      applyWorkerConfig(result.summary || state.workerSummary);
      renderCountdown();
      pushLog(`worker 已启动，配置文件: ${state.configPath}`);
    } catch (error) {
      pushLog(`[start-worker-error] ${error.message || error}`);
    }
  });

  refs.startBackendButton.addEventListener("click", async () => {
    try {
      const result = await window.desktopWorker.startBackend();
      state.backendRunning = true;
      state.backendManagedByElectron = !result?.info?.reusedExisting;
      state.backendStatus = result?.info?.reusedExisting ? "READY" : "STARTING";
      renderSummary();
      pushLog(result?.info?.reusedExisting ? "已连接到现有 backend" : "正在启动 backend...");
    } catch (error) {
      pushLog(`[start-backend-error] ${error.message || error}`);
    }
  });

  refs.stopBackendButton.addEventListener("click", async () => {
    try {
      await window.desktopWorker.stopBackend();
      pushLog("正在请求停止 backend...");
    } catch (error) {
      pushLog(`[stop-backend-error] ${error.message || error}`);
    }
  });

  refs.reloadAdminButton.addEventListener("click", () => {
    if (refs.adminFrame.src) {
      refs.adminFrame.contentWindow?.location.reload();
      pushLog("已刷新管理后台页");
    }
  });

  refs.clearLogsButton.addEventListener("click", () => {
    state.logs = [];
    refs.logOutput.textContent = "";
  });

  refs.openWorkspaceSettingsButton.addEventListener("click", () => openDrawer());
  refs.closeDrawerButton.addEventListener("click", closeDrawer);
  refs.workspaceOverlay.addEventListener("click", (event) => {
    if (event.target === refs.workspaceOverlay) {
      closeDrawer();
    }
  });

  refs.topmostToggle.addEventListener("change", async (event) => {
    const topmost = Boolean(event.target.checked);
    await window.desktopWorker.setTopmost(topmost);
    if (state.workerSummary) {
      state.workerSummary.alwaysOnTop = topmost;
    }
  });

  [
    refs.autoReplyEnabledToggle,
    refs.autoReplyOnlyPrivateToggle,
    refs.whitelistInput,
    refs.promptInput,
    refs.memoryEnabledToggle,
    refs.memoryMaxTurnsInput,
    refs.memoryTtlInput,
    refs.memoryPersistInput,
  ].forEach((element) => {
    element.addEventListener("input", syncAutoReplyFromInputs);
    element.addEventListener("change", syncAutoReplyFromInputs);
  });

  refs.reloadAutoReplyButton.addEventListener("click", async () => {
    await refreshAutoReply({ silent: false });
  });

  refs.saveAutoReplyButton.addEventListener("click", async () => {
    await saveAutoReply();
  });

  refs.clearMemoryButton.addEventListener("click", async () => {
    await clearMemoryStore();
  });

  refs.botToggleButton.addEventListener("click", async () => {
    if (state.botRunning) {
      try {
        await window.desktopWorker.stopBot();
        pushLog("[bot] 正在请求停止...");
      } catch (error) {
        pushLog(`[stop-bot-error] ${error.message || error}`);
      }
      return;
    }
    try {
      const result = await window.desktopWorker.startBot(state.configPath);
      state.botRunning = true;
      state.botStatus = "RUNNING";
      renderSummary();
      pushLog(`[bot] 已启动，Python=${result?.data?.resolvedPythonCommand || state.workerSummary?.resolvedPythonCommand || "-"}`);
    } catch (error) {
      pushLog(`[start-bot-error] ${error.message || error}`);
    }
  });

  window.setInterval(() => {
    renderCountdown();
  }, 250);
}

boot().catch((error) => {
  pushLog(`[boot-error] ${error.message || error}`);
});

# openai-bot-plugin

基于 OpenAI 官方 SDK 的聊天机器人插件。

## 依赖
- openai>=1.0.0

## 配置项
- `enabled`: 是否启用插件
- `openai_api_key`: OpenAI API 密钥
- `openai_base_url`: OpenAI API 基础 URL（可选，默认为官方）
- `openai_model`: OpenAI 模型名称（如 gpt-3.5-turbo）
- `priority`: 插件优先级
- `prompt`: 系统提示词，支持 {{chat_history}}、{{time_now}}、{{self_nickname}}、{{room_nickname}}、{{contact_nickname}} 变量占位符

## 用法
1. 在配置文件中添加 openai-bot-plugin 配置项。
2. 安装依赖：`pip install -e .`（在插件目录下）
3. 启动主程序，插件会自动加载。 
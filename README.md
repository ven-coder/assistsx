# Assistsx
- 用于[assistsx-js](https://github.com/ven-coder/assistsx-js)开发的自动化插件运行平台App，支持插件的本地安装、局域网安装、在线加载方式运行
- 该App基于 [assists-web](https://github.com/ven-coder/assists) 开源库开发（Maven Central：`io.github.ven-coder`）
- **开源版说明**：本仓库不包含插件商店与节点分析服务相关源码

## 🤖 Assists MCP · 让 AI Agent 成为开发协作者

> **新增 MCP 支持，全面增强 AI Agent 与无障碍自动化的协作能力。**  
> 通过 MCP，AI Agent 从辅助工具提升为自动化开发流程中的核心协作者。

接入 **Cursor、Claude Code、Codex、OpenCode** 等支持 MCP 的 AI 编程助手后，Agent 可全程参与开发与调试：不只是「遥控手机」，而是覆盖**基于无障碍服务的自动化**——读节点、写逻辑、查 Bug，让 AI Agent 真正进入自动化落地全流程。

与 AssistsX 协作时，可进一步发挥 MCP 的全部潜能：覆盖插件**创建、实现与排障**，显著提升效率。

1. 支持 Cursor、Claude Code、Codex、OpenCode 等 MCP 客户端接入  
2. 支持凡基于无障碍服务的自动化协作（读节点、写逻辑、查 Bug）  
3. 与 AssistsX 搭配时，支持插件创建、逻辑编写与调试辅助  
4. 支持原生 Android 无障碍服务节点读取与逻辑分析  
5. 支持运行异常分析与 Bug 排查  

**详细介绍及配置教程** → [https://assists.cn/mcp](https://assists.cn/mcp)

## 依赖集成（assists 库）

当前使用 Assists **3.5.4**，从 Maven Central 拉取：

```gradle
repositories {
    mavenCentral()
}

dependencies {
    api "io.github.ven-coder:assists-base:3.5.4"
    api "io.github.ven-coder:assists-web:3.5.4"
    api "io.github.ven-coder:assists-mp:3.5.4"
    api "io.github.ven-coder:assists-log:3.5.4"
    api "io.github.ven-coder:assists-ime:3.5.4"
}
```

本地联调：若同级目录存在 `../assists` 工程，`settings.gradle` 会通过 composite build 自动替换为源码模块；也可使用仓库内 `assists.gradle` 覆盖远程坐标。

### 发布 assistsxkit 到 Maven Central

版本号在根目录 `build.gradle` 的 `assistsxkitVersion`（当前 **0.0.5**）中维护。

```bash
./gradlew publishAllToMavenCentral
```

发布后坐标：

```gradle
implementation "io.github.ven-coder:assistsxkit:0.0.5"
```

##### 作者Wechat
<img width="250" alt="9ee0d2c0a6a2a46825c6e75f209be1c9" src="https://github.com/user-attachments/assets/cb0eb725-8120-49db-b805-1a579f98c5b4" />

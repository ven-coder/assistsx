# Assistsx
- 用于[assistsx-js](https://github.com/ven-coder/assistsx-js)开发的自动化插件运行平台App，支持插件的本地安装、局域网安装、在线加载方式运行
- 该App基于 [assists-web](https://github.com/ven-coder/assists) 开源库开发（Maven Central：`io.github.ven-coder`）
- **开源版说明**：本仓库不包含插件商店与节点分析服务相关源码

## 依赖集成（assists 库）

当前使用 Assists **3.5.3**，从 Maven Central 拉取：

```gradle
repositories {
    mavenCentral()
}

dependencies {
    api "io.github.ven-coder:assists-base:3.5.3"
    api "io.github.ven-coder:assists-web:3.5.3"
    api "io.github.ven-coder:assists-mp:3.5.3"
    api "io.github.ven-coder:assists-log:3.5.3"
    api "io.github.ven-coder:assists-ime:3.5.3"
}
```

本地联调：若同级目录存在 `../assists` 工程，`settings.gradle` 会通过 composite build 自动替换为源码模块；也可使用仓库内 `assists.gradle` 覆盖远程坐标。

### 发布 assistsxkit 到 Maven Central

版本号在根目录 `build.gradle` 的 `assistsxkitVersion`（当前 **0.0.2**）中维护。

```bash
./gradlew publishAllToMavenCentral
```

发布后坐标：

```gradle
implementation "io.github.ven-coder:assistsxkit:0.0.2"
```

##### 作者Wechat
<img width="250" alt="9ee0d2c0a6a2a46825c6e75f209be1c9" src="https://github.com/user-attachments/assets/cb0eb725-8120-49db-b805-1a579f98c5b4" />

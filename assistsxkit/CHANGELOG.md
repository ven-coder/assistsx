# 更新日志（assistsxkit）

> 该 CHANGELOG 仅追踪 `io.github.ven-coder:assistsxkit` 库的变更，与 AssistsX-Open App 的版本号无关。
>
> **版本号必须跟随 `assistsxkitVersion`**：本文件「当前最新已发布版本」与最新版本标题，必须与根目录 `build.gradle` 中的 `assistsxkitVersion` 完全一致。
> 发布 assistsxkit 时，以下三处必须在同一次提交中一起改，禁止只改其中一处：
>
> 1. 根目录 `build.gradle` → `assistsxkitVersion`
> 2. 本文件 → 更新「当前最新已发布版本」，并新增对应版本标题与条目
> 3. 各宿主项目引用的版本（如 `assistsx-platform/build.gradle` 的 `assistsxkitVersion` 与 README 中的依赖坐标）

## 待发布

> 当前最新已发布版本：**0.0.7**。以下为下一版本计划。

（暂无）

---

## 0.0.7（2026-10-20）· 已发布

- **新增**：插件主页与浮窗页面之间可通过事件总线互相收发事件，浮窗里的操作能直接刷新主页内容
- **新增**：插件页面加载失败时显示内置错误页，支持「重试」与「关闭」，并标识本地/线上插件
- **新增**：插件详情对话框显示插件类型（本地 / 在线）
- **新增**：扫描页支持一键安装插件，页面固定竖屏
- **修复**：首次安装默认插件兼容 `config.json` / `plugin.json` 两种配置命名，并自动补齐插件入口地址

---

## 0.0.6（2026-08-26）· 已发布

- **新增**：插件页顶部状态栏占位条支持网页控制显隐与背景色
- **新增**：`IndexActivity` 支持设置标题栏、悬浮按钮的默认显隐
- **优化**：贴边悬浮按钮默认位置上移，避让底部区域

---

## 0.0.5（2026-08-19）· 已发布

- **新增**：插件 MMKV 存储目录隔离，多插件互不干扰
- **新增**：贴边悬浮按钮，支持吸附、拖至底部，按钮顺序调整（refresh 紧邻 close，back/forward 顺序固定）
- **新增**：插件宿主 Chrome 控制——`PluginChromeController` 支持 ActionBar/悬浮按钮显隐、WebView 后退/前进/刷新、退出插件
- **调整**：WebView Bridge 注册移至应用启动时初始化，简化宿主接入流程
- **调整**：`IndexActivity` 改为 `open`，宿主 App 可继承扩展启动逻辑

## 0.0.4（2026-07-08）· 已发布

- 首次发布至 Maven Central（`io.github.ven-coder:assistsxkit`）

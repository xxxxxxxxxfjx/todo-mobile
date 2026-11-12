# todo-mobile · 番茄钟 App（调研 + Android 实现）

> 建立时间：2026-11-12
> 当前状态：**MVP 已实现并在 Android 17 / 16KB page size 模拟器上完成端到端验收**

## 一、这个文件夹是什么

这里是一套完整的「番茄钟 App」立项 + 落地资料：

1. **产品调研**：市面主流番茄钟 App 的功能拆解、竞品对照、优先级、商业化与差评点。
2. **工程环境**：无 USB / 无无线网卡 / 无蓝牙的 Windows 电脑上，用 Android 模拟器完成安装与调试。
3. **逆向基准**：对番茄ToDo APK 做静态取证（清单/资源/布局/字符串）与官方截图分析，提炼可借鉴的产品结构。
4. **可运行实现**：Kotlin + Jetpack Compose 的「番茄专注」Android App，含源码、APK、截图与验收报告。

## 二、文档与交付物索引

| 文件 | 内容 | 适合谁看 |
|---|---|---|
| [`01-番茄钟App功能调研.md`](./01-番茄钟App功能调研.md) | 14 个功能模块详解、7 个竞品对照、P0/P1/P2 优先级、商业化、Android 技术要点、数据表草案 | 产品 / 开发 |
| [`02-安卓模拟器方案-无USB无无线.md`](./02-安卓模拟器方案-无USB无无线.md) | 环境自检、AVD / MuMu / 雷电 / BlueStacks / 云真机方案、adb 命令、故障排查 | 开发 / 测试 |
| [`03-功能优先级清单.csv`](./03-功能优先级清单.csv) | 可直接导入 Excel / 项目管理工具的功能清单（含优先级与验收标准） | 产品 / 项目经理 |
| [`04-番茄ToDo逆向分析.md`](./04-番茄ToDo逆向分析.md) | 番茄ToDo APK 身份、16KB 兼容性、清单结构、功能版图、界面取证与「借鉴/不复制」边界 | 产品 / 开发 |
| [`05-实现与验收报告.md`](./05-实现与验收报告.md) | 交付物、技术架构、功能清单、实测矩阵、缺陷修复、已知限制、构建命令 | 开发 / 测试 |
| [`screenshots/`](./screenshots/) | 9 张自测截图（含白噪音/深色）+ 5 张番茄ToDo 官方参考截图 | 产品 / 设计 |
| `app/build/outputs/apk/debug/app-debug.apk` / `TomatoFocus-1.0.0-debug.apk` | 可安装 APK（debug 签名，约 15.9MB） | 体验 / 测试 |

## 三、已实现功能（摘要）

- **计时核心**：25/5/15 默认节奏、自定义工作/短休/长休、长休间隔、开始/暂停/继续/重置/跳过、+5 分钟、自动开始下一段开关；
- **可靠计时**：前台服务 + 通知栏倒计时、精确闹钟兜底、杀进程恢复、真实重启恢复（BOOT_COUNT + 墙上时钟）；
- **任务**：今日清单、增删改查、预估番茄、实际番茄自动累计、优先级、备注、完成状态；
- **统计**：今日/本周专注、番茄数、连续天数、最近 7 天柱状图、近 12 周热力图、任务分布、4 个成就；
- **体验**：大圆环极简首页、渐变任务卡、深色模式、通知/精确闹钟/电池优化权限引导、本地优先无网络权限；
- **P1 增强**：6 种本机实时合成白噪音（雨声/森林/海浪/咖啡馆/篝火/棕色噪音，支持音量与自动停止）、桌面小组件（倒计时 + 开始/暂停）；
- **兼容**：APK 内 **0 个原生库**，在 16KB page size 设备上可正常安装运行。

> 未实现（按 P1/P2 后置）：学霸模式/应用拦截、白噪音混音器、锁屏实时活动、云同步、自习室、AI、会员支付。详见验收报告第 6 节。

## 四、快速开始

```powershell
# 1) 启动模拟器（示例：本机 AVD Pixel_7）
$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe -avd Pixel_7

# 2) 安装
$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe install -r .\TomatoFocus-1.0.0-debug.apk

# 3) 授予必要权限（可选，用于通知/精确闹钟验收）
adb shell pm grant com.focus.tomato android.permission.POST_NOTIFICATIONS
adb shell appops set com.focus.tomato SCHEDULE_EXACT_ALARM allow
adb shell am start -n com.focus.tomato/.MainActivity
```

构建：

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
$env:ANDROID_HOME="$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat :app:assembleDebug --console=plain --no-daemon
```

> 本机 Gradle 在受限沙箱下无法加载 `native-platform.dll`，构建需在完全权限下执行；这是宿主环境限制，不是项目问题。

## 五、项目结构

```
todo-mobile/
├─ app/                         # Android 应用模块
│  └─ src/main/
│     ├─ java/com/focus/tomato/
│     │  ├─ data/               # Room 实体/DAO、设置、计时状态持久化
│     │  ├─ timer/              # 前台服务、通知、AlarmManager、开机广播
│     │  ├─ audio/              # 白噪音合成、AudioTrack 引擎、NoiseService
│     │  ├─ widget/             # 桌面小组件 Provider/RemoteViews
│     │  └─ ui/                 # Compose 主题、四个页面、组件与 ViewModel
│     ├─ res/                   # 图标、字符串、主题
│     └─ AndroidManifest.xml
├─ 01..05-*.md                  # 调研 / 环境 / 逆向 / 验收文档
├─ screenshots/                 # 自测截图 + 参考截图
├─ ref/                         # 番茄ToDo APK、清单/资源取证、官方截图
└─ artifacts/                   # 测试过程截图与日志（可清理）
```

## 六、一句话结论

- **产品**：番茄钟不是计时器，而是「计划 → 执行 → 记录 → 复盘」的闭环；纯计时没有生存空间，差异化在**防干扰能力 + 数据资产 + 情绪激励**。
- **本次实现**：把文档里的 P0 全部落地，并把「后台/杀进程/重启恢复」这条最容易翻车的链路真正跑通；没有堆砌学霸模式等重权限功能，先保证计时可靠与体验完整。
- **环境**：模拟器**不需要无线设备**，adb 走 `127.0.0.1` 回环，联网走宿主机 NAT；当前 AVD 为 Android 17 / 16KB page size，已作为兼容性验收环境。

## 七、更新记录

| 日期 | 变更 |
|---|---|
| 2026-11-12 | 初版：功能调研 + 模拟器方案 + 功能清单 |
| 2026-11-12 | 新增：番茄ToDo 逆向分析、Kotlin/Compose MVP、APK、截图与验收报告 |

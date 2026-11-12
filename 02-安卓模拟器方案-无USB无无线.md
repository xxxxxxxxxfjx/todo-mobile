# 安卓模拟器方案（无 USB、无无线网卡、无蓝牙）

> 适用环境：Windows 10/11 开发机，**没有 USB 口接手机、没有无线网卡、没有蓝牙**，需要在电脑上安装并调试 Android APK。
> 结论：**模拟器不需要任何无线设备**。它跑在本机虚拟化上，adb 走 `127.0.0.1` 回环，联网走宿主机 NAT（有网线即可）。
> 更新日期：2026-11-12

---

## 0. 一句话结论

| 需求 | 推荐方案 |
|---|---|
| 正经开发调试（断点、Logcat、布局检查） | **Android Studio 自带 AVD**（首选） |
| 只想快速装个 APK 看看 | **MuMu 模拟器 12**（安卓 15）或 **雷电模拟器 14**（安卓 14） |
| 机器必须常开 Hyper-V（WSL2 / Docker） | **Android Studio AVD（WHPX）** 或 **BlueStacks 5 Hyper-V 版** |
| 游戏项目 | Google Play Games 开发者模拟器（需 Hyper-V） |
| BIOS 开不了 VT / 机器太老 | **云真机**（WeTest / 阿里云 EMAS / Firebase Test Lab） |

---

## 1. 为什么"没有无线"不影响模拟器

```
┌────────────────────────── 你的 Windows 电脑 ──────────────────────────┐
│                                                                       │
│   Android Studio / adb ──127.0.0.1──► 模拟器（QEMU 虚拟机）           │
│                                          │                            │
│                                          ├─ 虚拟网卡 10.0.2.x（NAT）  │
│                                          │        │                   │
│   以太网（网线）──────────────────────────┴────────┘                   │
│                                                                       │
└───────────────────────────────────────────────────────────────────────┘
```

三个关键点：

1. **adb 不走网络**：模拟器在宿主机上开一个本地端口（如 `5554`、`16384`），adb 通过 `127.0.0.1` 连接，跟无线网卡无关。
2. **模拟器上网走 NAT**：虚拟网卡把流量转发到宿主机的默认网络出口。**只要有网线（以太网）就能上网**，没有 Wi-Fi 也不影响。
3. **完全离线也能跑**：模拟器可以离线启动、离线安装 APK，只是不能登录账号、不能拉在线数据。

> 因此前面提到的"手机无线调试（adb pair）"方案在你这里不可用，直接忽略。所有调试都在模拟器内完成。

---

## 2. 开工前自检（1 分钟）

### 2.1 检查 CPU 虚拟化

任务管理器 → 性能 → CPU → 看"虚拟化：已启用"。

或用 PowerShell：

```powershell
(Get-CimInstance Win32_Processor).VirtualizationFirmwareEnabled
```

返回 `True` 才正常。若是 `False`：

1. 重启进 BIOS/UEFI（开机按 Del / F2 / F10，视主板而定）；
2. 找 `Intel VT-x` / `Intel Virtualization Technology` / `SVM Mode`（AMD）；
3. 设为 `Enabled`，保存重启。

### 2.2 检查 Hyper-V 状态

```powershell
(Get-CimInstance Win32_ComputerSystem).HypervisorPresent
```

| 结果 | 含义 | 选择 |
|---|---|---|
| `True` | Hyper-V 正在运行（WSL2 / Docker / 沙盒会打开它） | 用 **Android Studio AVD（WHPX）** 或 BlueStacks Hyper-V 版；不要用 MuMu / 雷电 |
| `False` | Hyper-V 未运行 | 首选 AVD（AEHD 加速），也可用 MuMu / 雷电 |

### 2.3 检查内存与磁盘

```powershell
Get-CimInstance Win32_ComputerSystem | Select-Object TotalPhysicalMemory
Get-PSDrive C | Select-Object Used,Free
```

- 内存：建议 **16GB**，最低 8GB（只能跑一个轻量实例）
- 磁盘：预留 **20–30GB**（Android Studio + 1~2 个系统镜像）

### 2.4 检查是否装了 adb

```powershell
adb version
```

没有就装 Android Studio（自带 platform-tools），或单独下载 platform-tools 并加入 PATH。

---

## 3. 方案 A：Android Studio 自带模拟器（首选）

### 3.1 安装

1. 下载 Android Studio：https://developer.android.com/studio
2. 安装时勾选 **Android SDK**、**Android SDK Platform-Tools**、**Android Emulator**。
3. 首次启动按向导下载 SDK（默认在 `%LOCALAPPDATA%\Android\Sdk`）。

### 3.2 创建 AVD

1. 打开 **Device Manager**（右侧栏或 Tools → Device Manager）。
2. **Create Virtual Device** → 选机型（建议 **Pixel 8** 或 Pixel 7）。
3. 选系统镜像（System Image）：
   - **Google APIs**：可 `adb root`，适合开发调试；
   - **Google Play**：带 Play 商店，更接近真实用户环境，但**不能 root**；
   - API 版本建议 **API 35 / 36 各建一个**（Android 15 / 16）。
4. 高级设置建议：
   - RAM：`2048–4096 MB`
   - VM Heap：`512 MB`
   - Internal Storage：`4096 MB` 以上
   - Graphics：`Hardware - GLES 2.0`（花屏/黑屏时改 `Software`）
5. 完成，点 ▶ 启动。

### 3.3 命令行操作（推荐）

```powershell
# 路径（按实际安装位置调整）
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$sdk\platform-tools\adb.exe"
$emu = "$sdk\emulator\emulator.exe"

# 列出所有 AVD
& $emu -list-avds

# 启动指定 AVD
& $emu -avd Pixel_8_API_36

# 查看设备（应出现 emulator-5554）
& $adb devices
```

### 3.4 安装与调试 APK

```powershell
& $adb install -r .\app-debug.apk
& $adb shell am start -n com.example.todo/.MainActivity
& $adb logcat -s TodoTimer:D AndroidRuntime:E
```

### 3.5 常用技巧

| 操作 | 方法 |
|---|---|
| 快速截屏 | 模拟器工具栏相机按钮；或 `adb exec-out screencap -p > shot.png` |
| 模拟旋转 | 工具栏旋转按钮；或 `adb shell settings put system accelerometer_rotation 0` + `adb shell settings put system user_rotation 1` |
| 模拟来电/短信 | 模拟器 Extended Controls → Phone |
| 模拟位置 | Extended Controls → Location |
| 模拟网速 | Extended Controls → Cellular |
| 快照（秒开） | 关闭时勾选 Save quick-boot snapshot；或用 `-no-snapshot-save` 强制冷启动 |
| 冷启动排障 | `emulator -avd <名字> -no-snapshot -wipe-data`（清数据，慎用） |
| 修改 hosts / 代理 | 模拟器内 设置 → 网络；或启动参数 `-http-proxy` |

### 3.6 加速方式（二选一）

| Hyper-V | 加速器 | 说明 |
|---|---|---|
| 关闭 | **AEHD**（Android Emulator hypervisor driver） | SDK Manager → SDK Tools → 安装 "Android Emulator hypervisor driver"；装完 `sc query aehd` 应显示 RUNNING |
| 开启 | **WHPX**（Windows Hypervisor Platform） | 需启用 Windows 功能：`HypervisorPlatform`；管理员 PowerShell：`Enable-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform -All`，重启 |

> 如果装了 Docker Desktop / WSL2，Hyper-V 一定是开的，直接用 WHPX，不要去关 Hyper-V。

---

## 4. 方案 B：MuMu 模拟器 12（最省事）

官网：https://mumu.163.com/ （当前版本 **V6.8.11**，支持 **Android 15 / 12**）

### 4.1 安装

1. 下载 Windows 版安装包，安装到空间充足的盘（建议非系统盘）。
2. 首次启动前确认 BIOS 已开 VT。
3. 启动后可在设置里调整分辨率、帧率、性能模式。

### 4.2 连接 adb

1. 模拟器右上角菜单 → **问题诊断** → 查看 **ADB 调试端口**（本体一般是 `16384`）。
2. 连接：

```powershell
# MuMu 自带 adb，通常在安装目录 shell 下，例如：
# C:\Program Files\Netease\MuMuPlayer-12.0\shell\adb.exe
adb connect 127.0.0.1:16384
adb devices
adb install -r D:\test.apk
```

3. 多开端口规则（官方说明）：

| 实例 | 端口 |
|---|---|
| 0（本体） | 16384 |
| 1 | 16416 |
| 2 | 16448 |
| … | 每个 +32 |

> 端口被占用时会 +1（如 16384 → 16385）；多开实例仍按 16384 + 32×n 计算。

### 4.3 传 APK 的三种方式

1. **拖拽**：把 `.apk` 直接拖进模拟器窗口，自动安装。
2. **adb**：`adb install -r 路径\app.apk`（推荐，能看报错）。
3. **共享文件夹**：右上角 `···` → 更多工具 → 共享文件夹，设一个本机目录，模拟器内直接访问。

### 4.4 注意

- MuMu 与 Hyper-V 通常不兼容；开了 WSL2/Docker 的机器可能启动失败。
- 安装时注意取消捆绑勾选。
- 游戏向产品，部分金融/政务类 App 会检测模拟器。

---

## 5. 方案 C：雷电模拟器 14（脚本/自动化友好）

官网：https://www.ldmnq.com/ （当前 **雷电14 V14.0.34.1**，基于 Android 14；另保留雷电9 V9.5.42）

### 5.1 安装与连接

1. 下载安装，建议装到非系统盘（如 `E:\leidian\LDPlayer14`）。
2. 打开安装目录，在地址栏输入 `cmd` 回车，直接使用目录内的 `adb.exe`。
3. 连接：

```powershell
cd E:\leidian\LDPlayer14
.\adb.exe devices
# 应看到 emulator-5554
.\adb.exe -s emulator-5554 install -r D:\test.apk
.\adb.exe -s emulator-5554 shell
```

### 5.2 端口规则

| 实例 | 端口 / 串号 |
|---|---|
| 0 | 5554（`emulator-5554`） |
| 1 | 5556 |
| 2 | 5558 |
| … | 依次 +2 |

> **坑**：同一台机器同时运行雷电9 和雷电14 的实例 0，两者都会抢 `5555`，后启动的实例在 `adb devices` 里会消失。只保留一个版本运行即可。

### 5.3 开 ROOT

设置中心 → 其他 → **开启 ROOT 权限**，然后：

```powershell
adb root
adb shell   # 提示符应变成 #
```

### 5.4 适用场景

- 需要多开、脚本、自动化点击（自带操作录制/宏）
- 需要 root 做权限/数据测试
- 缺点：广告较多，和 Hyper-V 冲突

---

## 6. 方案 D：BlueStacks 5 / Google Play Games 开发者模拟器

### 6.1 BlueStacks 5（Hyper-V 环境可用）

- 官网：https://www.bluestacks.com/
- 有专门的 **Hyper-V 版**安装包；提供 Android 9 / 11 实例。
- 开启 ADB：设置 → 高级 → **Android Debug Bridge**，然后 `adb connect 127.0.0.1:5555`。
- 优点：省事、兼容性好；缺点：广告多、资源占用大。

### 6.2 Google Play Games 开发者模拟器（只建议游戏项目）

- 官方页：https://developer.android.com/games/playgames/emulator
- **必须开启 Hyper-V**；支持启动到普通 Android 桌面、Vulkan/DirectX 切换、强制宽高比、鼠标/触摸切换、**旁加载与调试**。
- 限制：面向游戏；Play Integrity 不返回 `MEETS_VIRTUAL_INTEGRITY`；会收集性能/崩溃数据且不能关闭。
- 对番茄钟 App 来说没必要，了解即可。

---

## 7. 方案 E：云真机（BIOS 开不了 VT 时的兜底）

本地模拟器跑不动时，用浏览器里的云真机，**不需要本机任何 USB / 无线硬件**：

| 平台 | 特点 |
|---|---|
| 腾讯 WeTest 云真机 | 国内机型全，按分钟计费，适合兼容性抽测 |
| 阿里云 EMAS 移动测试 | 真机兼容性测试 + 远程调试 |
| Firebase Test Lab |  Google 官方，适合出海应用，Robo 测试可自动跑 |
| AWS Device Farm | 机型多，适合大规模自动化 |

流程：上传 APK → 选机型/系统版本 → 远程安装运行 → 看录屏/日志。缺点是按量付费、交互有延迟，不适合日常高频调试。

---

## 8. 如果这台机器完全不能上网

需要"离线装 Android Studio + 系统镜像"：

1. 在另一台能上网的电脑下载：
   - Android Studio 安装包（Windows 64-bit）
   - **SDK Command-line tools**
   - 需要的 **system image** 压缩包（如 `system-images;android-35;google_apis;x86_64`）
2. 拷到目标机安装 Android Studio，把 command-line tools 解压到 `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\latest`。
3. 把 system image 解压到：
   ```
   %LOCALAPPDATA%\Android\Sdk\system-images\android-35\google_apis\x86_64\
   ```
   （目录结构要包含 `system.img`、`userdata.img`、`package.xml` 等）
4. 打开 Android Studio → Device Manager → Create Virtual Device，镜像应能被识别。
5. MuMu / 雷电 / BlueStacks 的安装包本身可离线安装，装完即可离线使用。

> 如果连"另一台能上网的电脑"都没有，只能用云真机（但云真机也需要网络）——这种情况建议先解决网络。

---

## 9. APK 传入模拟器的 4 种方式

| 方式 | 操作 | 优点 | 缺点 |
|---|---|---|---|
| **adb install** | `adb install -r app.apk` | 能看到完整报错，可脚本化 | 需要命令行 |
| **拖拽** | 把 APK 拖进模拟器窗口 | 最快 | 报错信息不明确 |
| **共享文件夹** | MuMu/雷电的共享目录 | 适合频繁替换 | 需要配置 |
| **模拟器内下载** | 模拟器浏览器打开下载链接 | 无需电脑传文件 | 依赖网络、来源不可控 |

补充：也可以 `adb push app.apk /sdcard/Download/`，再在模拟器文件管理器里点击安装。

---

## 10. 本项目的调试命令清单（可直接复制）

```powershell
# 基础
adb devices
adb install -r .\app-debug.apk
adb uninstall com.example.todo
adb shell pm clear com.example.todo

# 启动 / 停止
adb shell am start -n com.example.todo/.MainActivity
adb shell am force-stop com.example.todo
adb shell dumpsys window | findstr mCurrentFocus

# 日志（只看自己的 tag + 崩溃）
adb logcat -s TodoTimer:D AndroidRuntime:E

# 截图 / 录屏
adb exec-out screencap -p > shot.png
adb shell screenrecord /sdcard/demo.mp4
adb pull /sdcard/demo.mp4 .

# 模拟操作
adb shell input keyevent KEYCODE_HOME
adb shell input keyevent KEYCODE_BACK
adb shell input tap 540 1200
adb shell input swipe 540 1600 540 400 300
adb shell input text "hello"

# 包信息
adb shell pm list packages -3
adb shell dumpsys package com.example.todo | findstr version
```

### 番茄钟专项可靠性测试

| 测试 | 操作 | 期望 |
|---|---|---|
| 后台计时 | 启动 25 分钟 → 按 Home → 锁屏 10 分钟 → 回来 | 剩余时间准确，不漂移 |
| 杀进程恢复 | 计时中 `adb shell am force-stop com.example.todo` → 重开 | 恢复剩余时间/状态 |
| 重启恢复 | 模拟器重启 → 打开 App | 计时状态恢复（或明确提示已结束） |
| Doze 压测 | `adb shell dumpsys battery unplug` +（需 root）`adb shell dumpsys deviceidle force-idle` | 到点仍能提醒（AlarmManager 兜底） |
| 低电量模式 | 设置 → 电池 → 省电模式 | 计时不受影响 |
| 通知权限拒绝 | 首次启动拒绝通知 | App 有引导，不崩溃 |
| 时区/时间修改 | 改系统时区或时间 | 剩余时间按 elapsedRealtime 计算，不乱跳 |
| 精确闹钟权限 | Android 12+ 关闭"闹钟和提醒"权限 | 有引导，降级为普通通知 |

---

## 11. 故障排查表

| 现象 | 原因 | 解决 |
|---|---|---|
| 启动报 "VT is not enabled" / "HAXM not installed" | BIOS 未开虚拟化 / 加速器未装 | 开 VT；SDK Manager 装 AEHD 或用 WHPX |
| 提示 "Hyper-V detected" 后 MuMu/雷电启动失败 | 游戏模拟器与 Hyper-V 冲突 | 改用 AVD（WHPX）或 BlueStacks Hyper-V 版；或关闭 Hyper-V（会关掉 WSL2/Docker） |
| AVD 启动黑屏/花屏 | GPU 渲染问题 | AVD 设置 Graphics 改 `Software`；或命令行加 `-gpu swiftshader_indirect` |
| `adb devices` 为空 | adb server 异常 / 驱动 | `adb kill-server` → `adb start-server`；确认模拟器已启动 |
| `adb connect` 被拒绝 | 端口不对 / 模拟器未开 ADB | MuMu 看"问题诊断"端口；雷电用安装目录 adb；BlueStacks 在设置里开 ADB |
| `INSTALL_FAILED_INSUFFICIENT_STORAGE` | 模拟器存储不足 | AVD 扩容（Edit → Internal Storage）；或卸载无用 App |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 签名不一致 | `adb uninstall 包名` 后重装 |
| `INSTALL_FAILED_NO_MATCHING_ABIS` | APK 只有 arm64 原生库，x86_64 镜像跑不了 | 换 ARM 镜像（很慢）或用云真机/真机 |
| 模拟器无网络 | 宿主机网络 / DNS | 检查网线；启动参数 `-dns-server 8.8.8.8`；关闭代理/VPN 再试 |
| 模拟器很卡 | 内存/显卡不足、未开加速 | 提高 AVD RAM；确认 AEHD/WHPX 生效；关闭其他虚拟机 |
| 端口被占用 | 多个模拟器抢同一端口 | 只保留一个实例；或改 AVD/模拟器端口 |
| 中文输入不了 | `adb shell input text` 不支持中文 | 用模拟器内输入法；或 adb 装 ADBKeyboard |

---

## 12. 模拟器能测什么、不能测什么

### 能测（覆盖日常开发 90%）

- 安装/启动/崩溃/ANR
- 界面布局、深色模式、多分辨率、横竖屏
- 通知、权限流程、精确闹钟、前台服务
- 后台/锁屏计时、Doze、低电量模式
- 本地数据库、SharedPreferences、文件读写
- 网络请求（NAT 联网）
- 深链接、Intent、多 Activity
- 无障碍服务（应用拦截逻辑）
- 桌面小组件（Glance）
- 多 API 版本兼容（建多个 AVD）

### 不能测 / 测不准

| 项目 | 原因 | 替代 |
|---|---|---|
| 蓝牙 / BLE | 模拟器无蓝牙栈 | 真机 / 云真机 |
| NFC | 无 NFC 硬件 | 真机 |
| SIM 卡 / 通话 / 短信 | 仅模拟，不是真实网络 | 真机 |
| 指纹 / 人脸 | 仅模拟一次验证 | 真机 |
| 部分传感器（陀螺仪、气压计） | 模拟数据 | 真机 |
| Play Integrity / SafetyNet 强校验 | 模拟器基本过不了 | 真机 |
| 真实性能 / 功耗 / 发热 | 与 PC 性能无关 | 真机 |
| 厂商推送（华为/小米/OPPO/vivo） | 需要厂商服务 | 真机 |
| 加固/风控检测 | 会识别模拟器 | 真机 / 云真机 |

---

## 13. 推荐执行顺序（给这台机器的落地清单）

1. **今天**：自检 VT / Hyper-V（第 2 节）→ 装 Android Studio → 建 1 个 Pixel 8 + API 35 的 AVD → `adb devices` 看到 `emulator-5554`。
2. **明天**：`adb install` 一个现成 APK 验证链路；配置 Logcat 过滤；试一次截图/录屏。
3. **本周**：建第二个 AVD（API 36 / Google Play 镜像）做兼容性对比；把 `adb` 加入 PATH。
4. **备用**：装一个 MuMu 12，用于"双击拖 APK 快速看效果"；若 Hyper-V 已开则跳过。
5. **兜底**：注册一个云真机账号（WeTest / EMAS），用于真机保真验证。

---

## 14. 参考来源

| 来源 | 链接 |
|---|---|
| Android 官方模拟器文档（系统要求、AVD、操作） | https://developer.android.com/studio/run/emulator |
| Android 16 SDK / 系统镜像 | https://developer.android.com/about/versions/16 |
| MuMu 官网（V6.8.11，Android 15/12） | https://mumu.163.com/ |
| MuMu ADB 连接说明（端口 16384 起，多开 +32） | https://mumu.163.com/help/20230214/35047_1073151.html |
| 雷电官网（雷电14 V14.0.34.1，Android 14） | https://www.ldmnq.com/ |
| 雷电 ADB 命令说明（端口 5554/5556，ROOT） | https://help.ldmnq.com/docs/lei-dian-mo-ni-qi-ADB-ming-ling-shi-yong-shuo-ming |
| BlueStacks 5（Hyper-V 版 / Android 11） | https://www.bluestacks.com/ |
| Google Play Games 开发者模拟器（需 Hyper-V） | https://developer.android.com/games/playgames/emulator |
| WSA 已于 2025-03-05 下架（不要再用） | https://learn.microsoft.com/en-us/previous-versions/windows/android/wsa/ |

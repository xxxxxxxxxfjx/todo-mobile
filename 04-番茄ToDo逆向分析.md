# 番茄ToDo 逆向分析（参考基准）

> 分析日期：2026-11-12
> 分析对象：番茄ToDo（TomaTodo）Android `10.2.9.519`
> 分析方式：APK 静态取证（清单/资源/布局/字符串）+ 官方应用商店截图 + 16KB 模拟器运行观察
> 用途：为「番茄专注」App 提供产品结构、交互层级与 Android 技术取舍的参考；**不复制其代码、图片、文案或品牌素材**。

---

## 0. 一句话结论

番茄ToDo 的本质不是“倒计时器”，而是 **「待办清单 → 番茄计时 → 学霸模式/锁机 → 数据统计 → 自习室/成就」** 的闭环。它的体量（85.8MB、111 个 Activity、395 个布局）主要来自大量业务分支、第三方 SDK 和加固壳；对 MVP 来说，真正值得借鉴的是**信息架构与专注闭环**，而不是功能数量。

---

## 1. APK 身份与运行兼容性

| 项 | 值 |
|---|---|
| 包名 | `com.plan.kot32.tomatotime` |
| 版本 | `10.2.9.519`（versionCode 968） |
| 应用名 | TomaTodo / 番茄ToDo |
| 大小 | 85,786,093 bytes（约 85.8 MB） |
| MD5 | `B3F6D552D2664693D85A5C2622DF5CD4`（与应用宝 CDN 标称一致） |
| SHA-256 | `682B72385175F793D628813D061BCCF79A038325351F4A9A15FACEB38C2656DD` |
| minSdk / targetSdk | 23 / 30（targetSdk 较旧） |
| compileSdk | 31 |
| 原生库 ABI | arm64-v8a / armeabi-v7a / armeabi / x86 |
| 加固迹象 | `libjgcpkt_64.so`、`.jiagu` 目录（360 加固），类名大量混淆 |

### 1.1 16KB page size 兼容性（重要发现）

当前开发机 AVD 为 **Android 17 / x86_64 / 16KB page size**。番茄ToDo 首次启动时：

- 系统弹出 “Android App Compatibility — This app isn't 16 KB compatible…”，列出 `libimagepipeline.so`、`libgifimage.so`、`libnative-filters.so` 等未按 16KB 对齐的库；
- `logcat` 随后出现 `UnsatisfiedLinkError`：
  `dlopen failed: ".../lib/arm64/libimagepipeline.so" program alignment (4096) cannot be smaller than system page size (16384)`；
- 因此该 APK 在本 AVD 上**无法完成启动**，只能做静态取证和官方截图取证。

这条事实反向约束了我们自己的实现：**「番茄专注」刻意不引入任何 `.so` 原生库**，APK 内 `so_count=0`，因而天然通过 16KB 兼容检查（详见验收报告）。

---

## 2. 清单结构：一个“重装”App 的规模

用 `aapt2 dump xmltree` 解析 `AndroidManifest.xml`（原始输出：`ref/AndroidManifest.xmltree.txt`）：

| 组件 | 数量 |
|---|---:|
| Activity | 111 |
| activity-alias | 1 |
| Service | 19 |
| BroadcastReceiver | 21 |
| ContentProvider | 4 |
| uses-permission | 21 |
| `res/layout` 布局文件 | 395 |

### 2.1 关键组件（证明功能存在，而不是只靠文案推测）

| 组件 | 作用推断 |
|---|---|
| `remind.keeprunning.CountService` / `InnerService` | 计时/后台保活的前台服务组合 |
| `remind.keeprunning.notify.BlockNotifyService` | 通知监听服务（`BIND_NOTIFICATION_LISTENER_SERVICE`） |
| `remind.keeprunning.MyAccessibilityService` | 无障碍服务（`BIND_ACCESSIBILITY_SERVICE`），学霸模式/应用拦截的核心 |
| `remind.keeprunning.KeepRunningWallpaperService` | 动态壁纸保活（`BIND_WALLPAPER`），对抗后台清理 |
| `widget.todowidget.ToDoListWidgetService` | 待办桌面小组件 |
| `widget.collectionwidget.CollectionWidgetService` | 合集/清单小组件 |
| `widget.futuretasklistwidget.FutureTaskListWidgetService` | 未来任务小组件 |
| `remind.lock.LockPermissionActivity` / `activity_lock` | 锁机权限与锁屏页 |
| `activity.studyroom.*` | 自习室、公开自习室、排行榜 |
| `activity.statistic` / `fragment_statistic` | 统计页 |
| `activity.xueba_help` / `white_list` | 学霸模式与白名单说明 |
| `activity.relax_and_sleep` / `activity_sleep` | 白噪音/睡眠/放松 |
| `activity.theme` / `custom_main_page` | 主题、皮肤、首页定制 |

### 2.2 权限画像（从 `aapt2 dump badging`）

`VIBRATE`、`INTERNET`、`ACCESS_NETWORK_STATE`、`WAKE_LOCK`、`ACCESS_WIFI_STATE`、`QUERY_ALL_PACKAGES`、`FOREGROUND_SERVICE`、`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`、`SYSTEM_ALERT_WINDOW`、`SYSTEM_OVERLAY_WINDOW`、`REQUEST_INSTALL_PACKAGES`、`RECEIVE_BOOT_COMPLETED`、`SET_WALLPAPER`、`PACKAGE_USAGE_STATS`、`POST_NOTIFICATIONS` 等。

> 解读：它把“真的能拦住手机”当作核心卖点，因此申请了**悬浮窗、使用统计、通知监听、无障碍、动态壁纸、忽略电池优化**一整套重权限；这也是 Google Play 审核与应用商店合规的主要风险来源。我们的 MVP 只保留**计时可靠性所需的权限**，不申请无障碍/使用统计/悬浮窗。

---

## 3. 功能版图（从资源与布局反推）

> 资源字符串原始输出：`artifacts/tomato-resources.txt`（29,775 行）；布局名来自 `res/layout` 列表。

### 3.1 计时核心

- 倒计时 / 正计时 / 翻页时钟（`activity_count_down`、`view_simple_clock`、`view_countdown_clock_digit`）
- 工作/短休/长休、番茄时长设置（`activity_setting_tomato`、`fragment_tomato_advance_settings`）
- 计时稳定性设置（`TimingStabilitySettingsActivity`、`setting_prevent_kill`）
- 悬浮窗（`float_time_window`、`BubbleActivity`）
- 桌面小组件（待办、今日专注、热力图、未来任务、智慧语录等）

### 3.2 任务 / 待办

- 待办集、待办列表、今日待办（字符串：“待办集”“当前没有待办，点击右上角 + 号创建一个吧”）
- 新增/编辑任务、子任务、重复、提醒、排序（`activity_add_task`、`activity_edit_task`、`activity_sort_task`、`dialog_add_todo_more`）
- 未来任务/日历/周视图（`activity_futrue_task`、`activity_calender`、`activity_week_view`）
- 预估番茄与实际消耗（字符串：“预估番茄”“实际番茄”一类字段）

### 3.3 防干扰 / 学霸模式

- 学霸模式、严格模式、白名单/黑名单（`setting_xueba`、`setting_xueba_control_self`、`activity_white_list`、`activity_xueba_help`）
- 定时锁机、锁屏页、退出拦截（`activity_lock`、`activity_lock_bottom`、`LockPermissionActivity`、`JumpToWhiteAppActivity`）
- 通知屏蔽（`BlockNotifyService`、`BlockNotifyHelpActivity`）
- 防被杀：前台服务 + 动态壁纸 + 电池优化白名单（`KeepRunningWallpaperService`、`setting_prevent_kill`）

### 3.4 统计 / 复盘

- 累计/今日专注：次数、时长、日均时长
- 饼图分布（`view_today_pie`、`pie_entry_item`）
- 柱状图/折线图/月视图（`view_month_line`、`view_month_break`、`view_day_app`）
- 热力图与热力图皮肤（`heatmap_widget_*`、`dialog_heatmap`）
- 中断原因、高效时段、宏观统计（字符串与布局均有对应入口）
- 历史记录、周视图、数据导出（`activity_history`、`dialog_export`）

### 3.5 激励 / 社交 / 声音 / 个性化

- 成就徽章（`activity_achieve`、`activity_new_achieve_activity`）
- 等级/经验（`activity_add_exp`）
- 皮肤/主题/背景海报（`activity_theme`、`activity_custom_main_page`、`dialog_theme_selection`）
- 白噪音/背景音/放松睡眠（`activity_relax`、`activity_relax_and_sleep`、字符串“背景音乐正在播放中”）
- 自习室/排行榜/学习小组（`activity_study_room`、`activity_public_studyroom`、`ranking_item`）
- 会员/季卡/皮肤付费（`dialog_membership_prompt`、`season_crad_item`、`activity_remove_ad`）

### 3.6 其他

- 账号/云同步/换机迁移（`activity_login`、`activity_my`）
- 手表/PC 连接（`dialog_watch_*`、`dialog_pc_connection`）
- 意见反馈/客服（美洽 SDK `MeiQiaService`）

---

## 4. 界面取证（官方应用商店截图）

截图来源：应用宝 `com.plan.kot32.tomatotime` 详情页；已保存到 `screenshots/reference/`。

| 截图 | 观察 | 可借鉴点 |
|---|---|---|
| `tomato-todo-1.jpg` | 任务页：蓝色渐变 Header，任务卡片带渐变/背景图，右侧“开始” | 卡片承载“任务 + 预计时长 + 完成度”，一键开始 |
| `tomato-todo-2.jpg` | 计时页：全屏背景图，顶部语录，中央大圆环 24:50，任务名/状态/今日进度 | 计时页要有“氛围 + 进度 + 当前任务”三件事 |
| `tomato-todo-3.jpg` | 统计页：深蓝 Header，白卡片“累计/今日”，环形图 + 排行 + 柱状图 | 统计先给“今日/累计”数字，再给分布和趋势 |
| `tomato-todo-4.jpg` | 学霸模式锁屏：黑底、语录、任务名、劝阻文案、“点击返回番茄钟” | 锁屏是“防逃逸”的一部分，不是单纯计时 |
| `tomato-todo-5.jpg` | 自习室：房间信息、成员榜、专注时长、学霸模式标签 | 社交是留存层，适合 P2，不适合 MVP |

### 4.1 视觉语言总结

- **浅色主界面 + 高饱和强调色**（蓝/番茄红），深色页用于锁机/沉浸；
- **大圆环 + 超大数字**是计时页的绝对视觉中心；
- **卡片化**承载任务与统计，圆角大、阴影轻；
- 文案偏“陪伴/激励”，但锁机页会用损失厌恶与劝阻式文案；
- 首页操作路径极短：任务卡片“开始”或大圆环下的主按钮。

---

## 5. 技术实现推断（仅基于清单/资源/运行观察）

1. **前台服务 + 多保活手段**：`CountService`/`InnerService` + 动态壁纸 + 通知监听 + 电池优化引导；这解释了它为什么能长期驻留，也解释了合规风险。
2. **学霸模式依赖无障碍**：`MyAccessibilityService` 是应用拦截的核心；国内商店可接受，Google Play 审核风险高。
3. **任务与统计是本地 + 云同步双轨**：`activity_login`、`activity_my`、`dialog_export` 等说明有账号体系；我们的 MVP 先做本地优先。
4. **体量来自 SDK 与皮肤体系**：395 个布局、19 个 Service、加固壳、推送/支付/客服/统计 SDK；MVP 不应复制这种复杂度。
5. **targetSdk 30 的历史包袱**：在 Android 14/15 上会受前台服务类型、精确闹钟、通知权限等新规影响；新项目应从 targetSdk 34 起做对。

---

## 6. 对我们「番茄专注」的映射

| 番茄ToDo 能力 | 我们的 MVP 决策 | 状态 |
|---|---|---|
| 自定义工作/短休/长休、长休间隔 | 实现，设置页可调 | ✅ P0 |
| 开始/暂停/继续/重置/跳过/+5 分钟 | 实现 | ✅ P0 |
| 自动开始下一段（可开关） | 实现，默认关闭 | ✅ P0 |
| 结束提醒（声音/震动/通知） | 实现，独立高优先级通道 + 手动铃声/震动 | ✅ P0 |
| 后台/锁屏可靠计时 | 前台服务 + `elapsedRealtime` + 精确闹钟兜底 | ✅ P0 |
| 杀进程恢复 | SharedPreferences 持久化 + 重开对账 | ✅ P0 |
| 重启恢复 | `BOOT_COUNT` 识别重启 + 墙上时钟截止时间 | ✅ P0（实测修复） |
| 任务增删改查、预估/实际番茄 | 实现，Room 本地存储 | ✅ P0 |
| 今日清单、完成状态 | 实现 | ✅ P0 |
| 今日/本周专注、番茄数 | 实现 | ✅ P0 |
| 深色模式 | 实现（跟随系统/浅色/深色） | ✅ P0 |
| 热力图、连续天数 | 实现（近 12 周） | ✅ P1 部分 |
| 成就徽章 | 实现 4 个本地成就 | ✅ P1 部分 |
| 学霸模式/应用拦截 | 未做：需要无障碍/使用统计，合规与范围都不适合 MVP | ⏳ P1 |
| 白噪音/混音 | 6 种本机实时合成白噪音已完成；混音器未做 | ✅ P1 部分 |
| 桌面小组件 | 已实现倒计时 + 开始/暂停小组件 | ✅ P1 |
| 云同步/账号/自习室/AI | 未做：服务端与账号体系 | ⏳ P2 |
| 皮肤/会员/支付 | 未做：商业化后置 | ⏳ P2 |

### 6.1 明确不复制

- 不复制番茄ToDo 的图片、插画、背景、图标、文案、音效；
- 不复制其代码/资源命名/包结构；
- 不使用其品牌名“番茄ToDo/TomaTodo”；
- 仅借鉴**信息架构、交互顺序、视觉层级**这类不受版权保护的产品思路。

---

## 7. 证据索引

| 证据 | 路径 |
|---|---|
| 参考 APK（校验后） | `ref/tomato-todo-10.2.9.519.apk` |
| Manifest 完整 xmltree | `ref/AndroidManifest.xmltree.txt` |
| 资源字符串/颜色/布局索引 | `artifacts/tomato-resources.txt` |
| 官方商店截图 | `screenshots/reference/tomato-todo-1..5.jpg` |
| 16KB 运行失败记录 | 本文件 §1.1；系统兼容性弹窗与 `logcat` 观察于 2026-11-12 |
| REA 归档 | `ev_23342f34c43f4a23be2d8847fe3e4cb3338837dc3d164212eaf90aa0971f8835`（APK 清单/摘要） |

> 说明：由于 APK 使用 360 加固且类名混淆，本次没有把“深度反编译业务逻辑”作为目标；对 MVP 而言，清单 + 资源 + 官方截图已足以还原产品结构与关键交互。需要进一步做代码级审计时，应在合规授权下另行进行。

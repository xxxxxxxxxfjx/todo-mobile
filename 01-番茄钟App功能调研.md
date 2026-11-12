# 番茄钟类 App 功能调研

> 调研时间：2026-11-12
> 调研对象：Forest、番茄ToDo、潮汐 Tide、专注清单 Focus To-Do、滴答清单 TickTick、Pomofocus、Microsoft Windows 时钟「专注会话」，并参考 Flora / Study Bunny / Focusmate / Tiimo / Be Focused / Session 等细分产品。
> 用途：todo-mobile 立项、PRD 编写、MVP 范围裁剪。

---

## 0. 摘要

番茄钟 App 早已不是「一个倒计时器」。它本质是一个 **「计划 → 执行 → 记录 → 复盘」的闭环产品**，计时只是入口。市面产品可拆成四层能力：

| 层 | 解决的问题 | 代表功能 |
|---|---|---|
| **计时层**（工具） | 什么时候开始、什么时候停 | 25/5/15 计时、自定义、循环、提醒 |
| **任务层**（对象） | 这段时间干什么 | 待办、子任务、预估番茄、项目标签 |
| **防干扰层**（保障） | 怎么不被手机打断 | 学霸模式、应用拦截、锁机、勿扰 |
| **数据与激励层**（留存） | 为什么明天还来 | 统计、热力图、种树、成就、自习室 |

结论：**纯计时器没有生存空间**——系统（Windows 专注会话、iOS 专注模式）、任务工具（滴答清单）、白噪音工具（潮汐）都内置了番茄钟。新产品的差异化只能落在「**真的能拦住手机**」「**数据能沉淀**」「**情绪上有反馈**」这三件事上。

---

## 1. 番茄工作法的标准流程

Francesco Cirillo 提出的番茄工作法，标准循环是：

1. **计划**：每天早上列出今天要完成的任务，估算每个任务需要几个番茄（1 番茄 = 25 分钟）。
2. **执行**：选一个任务，启动 25 分钟计时，期间只做这一件事。
3. **记录**：每完成一个番茄，记录实际消耗与中断情况。
4. **处理**：比较「预估番茄数」与「实际番茄数」，找出估时偏差。
5. **可视化**：用图表/森林/热力图把专注过程呈现出来，形成正反馈。

标准节奏：**25 分钟工作 → 5 分钟短休 → 每 4 个番茄 → 15–30 分钟长休**。

> 产品启示：只做第 2 步（计时）的产品，用户用两周就会流失；把第 1、3、4、5 步做顺，才有长期留存。

---

## 2. 功能全景（14 个模块）

### 2.1 计时核心

- 默认 **25 分钟工作 / 5 分钟短休 / 每 4 个番茄 15–30 分钟长休**
- 自定义：工作时长、短休、长休、长休间隔、循环轮数
- 控制：开始 / 暂停 / 跳过 / 提前结束 / 延长 5 分钟 / 重置
- **自动开始下一段**（可开关；很多人反感强制自动，必须给开关）
- 正计时（stopwatch）与倒计时两种模式
- 结束提醒：铃声、震动、语音报时、渐强、结束前 1 分钟预告
- 展示位置：锁屏、悬浮窗、画中画、通知栏、灵动岛/实时活动、桌面小组件
- **后台可靠计时**（差评重灾区，见第 6 节）

### 2.2 任务 / 待办管理

- 收集箱、今日清单、项目/清单、标签、优先级
- 截止日期、提醒、**重复规则**（每天 / 每周 / 工作日）
- 子任务、备注、附件、检查项
- **预估番茄数 vs 实际消耗**（番茄工作法的灵魂：估 3 个番茄、实际用了 5 个 → 复盘依据）
- 拖拽排序、自然语言快速添加（"明天 10 点写周报 2 个番茄"）
- 日历视图 / 看板视图 / 时间块（time blocking）
- 与系统日历、Google Calendar、Outlook 双向同步
- 任务完成率、逾期任务统计

### 2.3 防干扰 / 自律（国内产品最卷的一块）

- **白名单 / 黑名单**：专注期间只允许白名单 App
- **学霸模式 / 严格模式**：退出即判失败、禁止切 App、防逃逸
  - 番茄ToDo 在 HarmonyOS 上基于 Screen Time Guard Kit 做系统级拦截（智能拦截、场景化锁机策略、防逃逸）
- **定时锁机**：整点锁、睡前锁、自定义时段锁
- 网站屏蔽（浏览器扩展，如 Forest 的 Chrome/Firefox 插件）
- 翻转手机 / 屏幕朝下 / 离开检测
- 与系统勿扰联动（iOS Focus、Android DND）
- 家长监督 / 青少年模式
- 中断记录：手动标记"内部打断 / 外部打断"，用于复盘

### 2.4 数据统计与复盘

- 今日 / 周 / 月 / 年 专注时长、番茄数、任务完成数
- 按**标签 / 项目 / 时段**分布（上午 vs 晚上效率）
- **热力图**（GitHub 式）、彩色时间方块、时间轴
- 连续专注天数（streak）、最长记录、日均专注
- 目标设定与达成率（"本周 20 小时"）
- 周报 / 月报 / 年度报告
- 导出 CSV / PDF；写入 Apple Health「正念训练」、Google Fit
- 最佳专注时段分析、专注中断率

### 2.5 激励与游戏化

- **种树**（Forest：专注长树，切 App 树就枯死；官方称已种下 210 万棵真树）
- 养宠物、建城市、养鱼、种花（Flora、Study Bunny 等）
- 金币 / 积分、商店、皮肤、树种 / 场景解锁
- 成就徽章、等级、称号（番茄ToDo v10.2.9 新增成就徽章页）
- 排行榜、好友 PK、班级 / 团队榜
- 损失厌恶设计：连续中断、树枯死、森林荒芜
- 公益联动：真树、捐款、联名

### 2.6 社交与陪伴

- **自习室**：加入码进房、连麦 / 视频、专注时长榜（番茄ToDo 的自习室已扩展到陌生人社交）
- 学习小组 / 班级 / 公司团队
- 好友关注、动态、打卡分享（微信 / 小红书）
- **1 对 1 真人陪专注**（Focusmate 模式，开视频互相监督）
- 直播自习、云监工

### 2.7 声音与氛围

- 白噪音库：雨、火、咖啡厅、森林、海浪、键盘声
- 音乐歌单：Spotify / Apple Music / 网易云（Windows 时钟的专注会话可直接连 Spotify）
- 双耳节拍（binaural）、ASMR、冥想引导、呼吸练习（潮汐）
- 混音器：多音轨叠加、单独音量、渐弱、定时关闭
- 番茄结束音效可换（"叮" / 鸟叫 / 钟声）

### 2.8 个性化与外观

- 主题色、深色模式、背景图、字体、图标
- 桌面 / 锁屏小组件、Apple Watch 表盘、Wear OS
- 灵动岛 / 实时活动（iOS）、通知栏常驻（Android）
- Siri / 快捷指令 / URL Scheme / 快捷键
- 多语言、农历 / 时区

### 2.9 多端与同步

- iOS / Android / Windows / macOS / Web / 浏览器扩展 / HarmonyOS
- 账号体系、云同步、离线可用、冲突合并
- 数据导入导出（CSV / JSON）、本地备份、换机迁移

### 2.10 集成与开放能力

- 日历集成（Google / Outlook / 系统日历）
- 任务工具互通（Todoist、Notion、Things）
- 自动化：Webhook、Zapier、IFTTT、快捷指令
- API / 团队看板 / 数据回传
- 会议联动：日历上开会前自动进入专注

### 2.11 团队 / 教育 / 企业版

- 教师端：班级管理、学生专注报告、作业番茄统计
- 企业端：团队专注时长、工时 / 项目归属、OKR 关联
- SSO、数据隔离、合规审计
- 按席位收费（番茄钟最现实的第二增长曲线）

### 2.12 AI 能力（2025–2026 新增趋势）

- AI 把大任务**拆成 N 个番茄**
- AI 生成"今日计划"并按优先级排序
- 根据历史数据推荐个人最佳专注时长（有人 25 分钟，有人 50 分钟）
- AI 周报 / 复盘总结（"你本周 60% 专注在上午，晚上效率低"）
- 语音添加任务、自然语言改设置
- 智能识别拖延模式并提醒

### 2.13 平台系统级能力（决定体验上限）

| 平台 | 可用能力 |
|---|---|
| **iOS** | Screen Time API（FamilyControls / DeviceActivity）做应用拦截、Live Activities、Focus filters、Shortcuts、WidgetKit、HealthKit |
| **Android** | 无障碍服务拦截、UsageStats 使用统计、悬浮窗、前台服务、精确闹钟、DND 权限、电池优化白名单、Glance 小组件 |
| **Windows** | 时钟 App「专注会话」：计时器浮层、自动开勿扰、任务栏不闪 / 不显示角标、结束通知、接 Microsoft To Do、接 Spotify |
| **macOS** | 菜单栏计时、勿扰联动、日历集成 |

### 2.14 番茄工作法的"方法论功能"

- 每日计划（今天要完成哪几个番茄）
- 追踪（实际执行）
- 记录（中断、完成情况）
- 处理（改进估时）
- 可视化（图表 / 森林 / 热力图）
- 每日 / 每周复盘模板

---

## 3. 竞品功能对照

### 3.1 功能对照表

| 功能 | Forest | 番茄ToDo | 潮汐 | 专注清单 | 滴答清单 | Pomofocus | Win 时钟 |
|---|---|---|---|---|---|---|---|
| 自定义计时 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 任务 / 待办 | 标签 + 备注 | ✅ 强 | ❌ | ✅ 强 | ✅ 最强 | ✅ 基础 | ✅ 接 To Do |
| 子任务 / 重复 | 部分 | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ |
| 应用拦截 | ✅ Deep Focus / Time Guard | ✅ 学霸模式 | ❌ | ❌ | ❌ | ❌ | 系统勿扰 |
| 白噪音 | 部分 | ✅ | ✅ 最强 | ✅ | ✅ | ✅ | 接 Spotify |
| 数据统计 | ✅ | ✅ 热力图 | ✅ | ✅ 报表 | ✅ 专注报告 | ✅ 日 / 周 / 月 | ✅ 每日进度 |
| 游戏化 | ✅ 种树 + 真树 | 成就 / 皮肤 | ❌ | ❌ | ❌ | ❌ | ❌ |
| 社交 / 自习室 | 好友 / 森林 | ✅ 自习室 | ❌ | ❌ | 协作 | ❌ | ❌ |
| 多端 | 手机 + 浏览器 | 手机 + 手表 | 全平台 | 全平台 | 全平台 | Web + 桌面 | Windows |
| 付费模式 | 免费 + 订阅 | VIP 季卡 / 三年卡 | 订阅 | 免费 + Pro | 会员 | 免费 + Premium | 系统自带 |

### 3.2 各产品一句话定位

| 产品 | 定位 | 最值得抄的点 |
|---|---|---|
| **Forest** | 用"种树"把专注变成可见资产 | 损失厌恶（切 App 树枯死）+ 公益叙事（真树） |
| **番茄ToDo** | 学生党的自律神器 | 学霸模式 + 自习室 + 热力图，国内最懂"防玩手机" |
| **潮汐** | 睡眠 / 专注 / 冥想一体的氛围 App | 声音场景与番茄钟的融合、极简美学 |
| **专注清单** | 番茄 + 任务 + 多端同步 | 功能克制、跨端完整（含 Apple Watch / Windows / Chrome） |
| **滴答清单** | 全能任务管理内置专注 | 任务 → 番茄 → 统计的完整闭环 |
| **Pomofocus** | 打开网页就能用的极简番茄钟 | 基础免费 + Premium 把"数据资产"锁付费墙 |
| **Windows 专注会话** | 系统级免打扰 | 与 To Do / Spotify / 勿扰深度联动 |

### 3.3 其他值得看的细分产品

- **Flora**：种树 + 好友，社交激励更强
- **Study Bunny**：宠物养成 + 学习计时，学生向
- **Focusmate**：1 对 1 真人视频陪专注，解决"一个人坚持不下去"
- **Tiimo**：ADHD 友好，视觉化日程 + 计时
- **Be Focused / Session**：macOS 菜单栏极简派，适合桌面办公场景

---

## 4. 功能优先级（可直接当 PRD 用）

### P0 — MVP 必须有（没有就没人用）

1. 计时：自定义工作 / 短休 / 长休 + 循环 + 自动开始开关
2. 结束提醒：通知 + 声音 + 震动（含权限引导）
3. **后台 / 锁屏可靠计时**（前台服务 + 精确闹钟 + 重启恢复）
4. 任务清单：添加 / 完成 / 删除 + 预估番茄数
5. 基础统计：今日 / 本周专注时长、番茄数
6. 本地存储 + 深色模式 + 极简首页（一个大圆环 + 开始键）

### P1 — 决定留存（3 个月内做）

7. 应用拦截 / 学霸模式（Android 无障碍 + iOS Screen Time）
8. 白噪音（5–10 个音源 + 混音）
9. 热力图 + 连续天数 + 成就
10. 桌面小组件 / 锁屏实时活动
11. 云同步 + 换机迁移
12. 标签 / 项目 + 周报

### P2 — 差异化与商业化（6 个月后）

13. 自习室 / 好友 / 团队
14. AI 任务拆解 + AI 周报
15. 日历双向同步
16. CSV / PDF 导出、API / Webhook
17. Apple Watch / Wear OS / 桌面端
18. 教育版 / 企业版（按席位收费）
19. 公益联动（真树 / 捐款）

> 详细的"功能点—优先级—验收标准"见同目录 `03-功能优先级清单.csv`。

---

## 5. 商业化模式（真实案例）

| 模式 | 案例 | 要点 |
|---|---|---|
| 免费 + 订阅 | Forest（官网有 Plans 页）、潮汐 | 高级统计、主题、音源包、跨端 |
| 免费 + 增值包 | Pomofocus：基础免费；Premium 才有**项目、年度报告、CSV 导出、无限模板、Todoist 集成、Webhook、去广告** | 把"数据资产"锁在付费墙后非常有效 |
| VIP 时长卡 | 番茄ToDo：季度卡 / 6 个月 / 三年卡 | 国内学生党接受度高，客单价低但量大 |
| 买断制 | 部分独立开发者产品 | 口碑好，但难持续 |
| 团队 / 教育版 | 班级、企业专注报告 | 按席位，B 端更稳 |
| 广告 | 多数免费工具 | 番茄钟用户对广告极度敏感，慎用 |

---

## 6. 用户真正在意什么（也是差评来源）

### 6.1 真需求

- 计时**绝对不能不准 / 被杀**（后台、锁屏、省电模式）
- 开始专注的路径 ≤ 2 次点击
- 没有广告、没有弹窗、没有"社交绑架"
- 可视化反馈（树、圆环、热力图）——这是坚持的动力
- 数据能导出、能跨设备

### 6.2 高频差评 / 坑

1. 后台计时被系统杀掉 → 必须前台服务 + `AlarmManager` 精确闹钟 + 开机 / 重启恢复
2. 通知权限被拒 → 首启引导，说明用途
3. 无障碍服务被系统清理 → 引导加白名单 / 自启动
4. 统计和真实使用对不上（多设备、跨天、时区）
5. 订阅后功能缩水 / 涨价 → 口碑崩得很快
6. 社交变攀比、自习室变广告位
7. 数据锁死不能导出 → 用户不敢长期用
8. 隐私：屏幕使用数据极敏感，建议**本地优先 + 明示授权 + 不上传原始 App 使用记录**

---

## 7. Android 实现要点

### 7.1 技术栈建议

- 语言 / UI：Kotlin + Jetpack Compose
- 本地数据：Room（+ DataStore 存设置）
- 后台：Foreground Service + WorkManager + AlarmManager
- 小组件：Glance（Android 12+ 推荐）
- 依赖注入：Hilt（可选）
- 图表：Compose Canvas 自绘或 Vico
- 同步：自建 REST / Supabase / Firebase（按合规要求选）

### 7.2 权限清单

| 权限 | 用途 | 备注 |
|---|---|---|
| `POST_NOTIFICATIONS` | 结束提醒 | Android 13+ 运行时权限 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | 保证计时不被杀 | Android 14+ 需声明类型 |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | 精确结束提醒 | Android 12+ 需引导授权 |
| `PACKAGE_USAGE_STATS` | 读取前台 App / 统计 | 需跳设置手动授权，非运行时弹窗 |
| `SYSTEM_ALERT_WINDOW` | 悬浮窗计时 | 需手动授权 |
| 无障碍服务（AccessibilityService） | 拦截其他 App | Google Play 审核严格，必须声明为核心功能 |
| `ACCESS_NOTIFICATION_POLICY` | 开启勿扰 | 需引导授权 |
| `RECEIVE_BOOT_COMPLETED` | 重启恢复计时 | 配合前台服务 |
| `WAKE_LOCK` | 保持 CPU 唤醒 | 谨慎使用，注意功耗 |

### 7.3 计时正确性（最容易翻车）

- **不要**用 `delay()` 或 `Handler.postDelayed()` 累加计时，进程被杀就全错
- 用 `SystemClock.elapsedRealtime()` + 目标结束时间戳，每次回到前台重新计算剩余
- 进程被杀后从 DataStore / Room 恢复剩余时间与状态
- 处理跨时区、用户手动改系统时间、夏令时
- 结束时用 `AlarmManager.setExactAndAllowWhileIdle()` 兜底
- 进入 Doze 前保存状态；退出 Doze 后校正

### 7.4 数据模型草案（Room）

```sql
-- 任务
task(
  id INTEGER PRIMARY KEY,
  title TEXT NOT NULL,
  note TEXT,
  list_id INTEGER,
  priority INTEGER DEFAULT 0,
  due_at INTEGER,              -- epoch millis
  repeat_rule TEXT,            -- RRULE 或自定义
  estimated_pomodoros INTEGER DEFAULT 1,
  completed_at INTEGER,
  sort_order INTEGER,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  deleted_at INTEGER           -- 软删除
)

-- 专注记录
focus_session(
  id INTEGER PRIMARY KEY,
  task_id INTEGER,
  started_at INTEGER NOT NULL,
  ended_at INTEGER,
  planned_seconds INTEGER NOT NULL,
  actual_seconds INTEGER,
  session_type TEXT NOT NULL,  -- work / short_break / long_break
  status TEXT NOT NULL,        -- running / completed / abandoned
  interruptions INTEGER DEFAULT 0,
  tag_id INTEGER
)

-- 标签 / 清单
tag(id INTEGER PRIMARY KEY, name TEXT, color INTEGER)
task_tag(task_id INTEGER, tag_id INTEGER, PRIMARY KEY(task_id, tag_id))

-- 每日汇总（加速统计查询）
daily_stat(
  date TEXT PRIMARY KEY,       -- yyyy-MM-dd（本地时区）
  focus_seconds INTEGER DEFAULT 0,
  pomodoro_count INTEGER DEFAULT 0,
  completed_tasks INTEGER DEFAULT 0,
  updated_at INTEGER
)

-- 成就
achievement(id TEXT PRIMARY KEY, unlocked_at INTEGER)

-- 设置
setting(key TEXT PRIMARY KEY, value TEXT)
```

索引建议：`focus_session(started_at)`、`task(completed_at)`、`task(deleted_at)`。

### 7.5 上架风险

- Google Play 对"无障碍拦截其他 App"审核严格，可能要求改用 `UsageStats` + 悬浮提示
- 国内应用商店对"锁机 / 设备管理员"权限审核也严，建议做成可选功能
- 隐私政策必须说明屏幕使用数据的采集范围与用途；能本地处理就不要上云
- 订阅需走平台内购（Google Play Billing / 国内各商店支付）

---

## 8. 趋势判断

1. **番茄钟正在被"吸收"**：任务管理（滴答清单）、系统（Windows 专注会话、iOS 专注模式）、白噪音（潮汐）都在内置番茄钟，纯计时器没有生存空间。
2. **卖点从"计时"转向"防干扰 + 数据"**：谁能真正拦住手机，谁就有付费理由。
3. **AI 化**：任务拆解、个性化时长、自动复盘是 2026 年新产品的标配差异点。
4. **无广告 / 极简是口碑护城河**：番茄ToDo 早期正是靠"无广告、秒启动"起量。
5. **B 端（学校 / 企业）比 C 端更好收钱**：班级专注报告、团队专注统计。

---

## 9. 参考来源

> 以下页面均于 2026-11-12 实际打开核对。

| 来源 | 链接 | 关键信息 |
|---|---|---|
| Forest 官网 | https://www.forestapp.cc/ | 60M+ 下载、4.8 分、种树机制、Deep Focus / Time Guard、210 万棵真树 |
| 番茄ToDo 百度百科（2026-09 更新） | https://baike.baidu.com/item/番茄ToDo | 功能列表、学霸模式（HarmonyOS Screen Time Guard Kit）、自习室、v10.2.9 新功能、VIP 模式 |
| 潮汐官网 | https://tide.fm/ | 睡眠 / 专注 / 冥想 / 呼吸、声音场景、番茄 + 自然声 |
| 专注清单官网 | https://www.focustodo.cn/ | 番茄 + 任务 + 报表 + 多端 + 子任务 / 重复 / 提醒 |
| Pomofocus | https://pomofocus.io/ | 基础功能 vs Premium 功能划分（项目、年度报告、CSV、Todoist、Webhook） |
| Microsoft Windows 专注会话 | https://www.microsoft.com/en-us/windows/tips/focus-sessions | 计时浮层、自动勿扰、To Do / Spotify 集成 |
| Android 官方模拟器文档 | https://developer.android.com/studio/run/emulator | AVD 创建、系统要求（16GB 内存 / 16GB 磁盘） |

---

## 10. 下一步建议

1. 用 `03-功能优先级清单.csv` 过一遍，划掉本期不做的功能，确定 MVP 边界。
2. 画 4 个核心页面：首页（计时）、任务列表、统计、设置。
3. 先做"计时可靠性"技术验证（后台 2 小时不漂移、杀进程能恢复），再堆功能。
4. 用同目录 `02-安卓模拟器方案-无USB无无线.md` 搭好调试环境，边开发边 `adb install` 验证。

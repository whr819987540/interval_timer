# 设计系统

本应用的视觉方向由 [ui-ux-pro-max](https://github.com/nextlevelbuilder/ui-ux-pro-max-skill) 设计知识库推导而来（v2.13.0，数据表 `products.csv`、`colors.csv`、`ui-reasoning.csv`、`styles.csv`、`stacks/jetpack-compose.csv`、`references/pro-rules.md`）。修改 UI 前先读本文件；改动 token 时同步更新这里的表格。

## 1. 定位

| 项 | 取值 | 来源 |
|---|---|---|
| 产品类型 | Timer & Pomodoro（#107） | `products.csv` |
| 推荐模式 | Minimal & Direct：大号居中数字 + 环形进度 | `ui-reasoning.csv` #107 |
| 风格 | Minimalism & Swiss Style，辅以 Dark Mode (OLED) 与微交互 | `products.csv` / `styles.csv` |
| 关键要求 | 大号居中倒计时、环形进度、声音 + 触觉提醒、自定义时长 | `products.csv` #107 |
| 反模式 | 过度装饰；仅靠颜色传达状态；emoji 当图标 | `ui-reasoning.csv`、`pro-rules.md` |

使用场景：手机放在一旁或口袋里、经常锁屏，用户只想“看一眼、按一下”。因此：
- 一屏只做一件事（设定 / 计时中 / 时间到），没有多余导航。
- 时长输入：点选“小时 / 分钟 / 秒”格子，再用键盘输入该项；选中格子用 2dp primary 描边 + secondaryContainer 底色（描边粗细变化保证不只靠颜色区分），并以 `Role.Tab` + selected 语义告知读屏。
- 主操作按钮 ≥ 64dp，“时间到”的“重置”按钮 80dp，单手可达（放在下半屏）。
- 锁屏界面（`AlarmActivity`）与应用内“时间到”界面共用同一个 `FinishedPanel`，体验一致。

## 2. 颜色 token

基于 `colors.csv` #107 “Focus red on dark + break green”。深色主题把文字用红提亮为 `#F87171`，因为原色 `#DC2626` 在 `#0F172A` 上只有 3.70:1，达不到正文 4.5:1。

| 角色 | 浅色 | 深色 | 用途 |
|---|---|---|---|
| background / surface | `#F8FAFC` | `#0F172A` | 页面底色 |
| onBackground / onSurface | `#0F172A` | `#F8FAFC` | 主要文字、数字 |
| onSurfaceVariant | `#475569` | `#94A3B8` | 次要文字、占位数字、暂停时的进度环 |
| primary | `#DC2626` | `#F87171` | 运行中的进度环、强调文字、分组标题 |
| primaryContainer / on | `#DC2626` / `#FFFFFF` | `#DC2626` / `#FFFFFF` | 开始、暂停/继续、重置等主按钮 |
| secondaryContainer / on | `#FEE2E2` / `#991B1B` | `#4A1414` / `#FECACA` | 选中的时/分/秒格子与预设、警示横幅 |
| tertiary | `#047857` | `#34D399` | “已开启”状态 |
| tertiaryContainer / on | `#D1FAE5` / `#065F46` | `#064E3B` / `#A7F3D0` | “时间到”图标底 |
| surfaceContainer | `#FFFFFF` | `#192134` | 设置卡片 |
| surfaceContainerHigh | `#EEF2F7` | `#222C42` | 键盘按键、轮次徽标、次要圆按钮 |
| outlineVariant | `#E2E8F0` | `#334155` | 分隔线、卡片描边、进度环底轨 |

对比度（WCAG 2.x，脚本计算）：

| 组合 | 浅色 | 深色 | 要求 |
|---|---|---|---|
| 正文 on 背景 | 17.06 | 17.06 | 4.5 |
| 次要文字 on 背景 | 7.24 | 6.96 | 4.5 |
| 次要文字 on 卡片 | 7.58 | 6.26 | 4.5 |
| 数字 on 按键 | 15.88 | 13.32 | 4.5 |
| 次要文字 on 选中格子 | 6.20 | 5.84 | 4.5 |
| 数字 on 选中格子 | 14.61 | 14.32 | 4.5 |
| 选中描边 vs 背景 | 4.62 | 6.45 | 3.0（非文字） |
| primary 文字 on 背景 | 4.62 | 6.45 | 4.5 |
| 白字 on 主按钮 | 4.83 | 4.83 | 4.5 |
| tertiary 文字 on 背景 | 5.24 | 9.29 | 4.5 |
| 进度弧 vs 底轨 | 3.92 | 3.74 | 3.0（非文字） |
| 暂停进度弧 vs 背景 | 7.24 | 6.96 | 3.0（非文字） |

规则：组件里只用 `MaterialTheme.colorScheme.*`，不写十六进制颜色（唯一例外是 `Theme.kt` 的调色板和启动图标矢量）。

## 3. 字体

- 系统字体（Roboto / 系统中文字体），不额外打包字体，保证中文渲染与安装体积。
- 倒计时数字：`TimerDigits`（Light，`fontFeatureSettings = "tnum"` 等宽数字），数字跳动时不抖动。
- 字号随容器宽度计算（环直径 × 0.23，带小时时 × 0.17；时/分/秒格子宽度 × 0.44，上限 48sp），用 `Dp.toSp()` 换算，大字体设置下不会溢出；其余文字使用 Material 3 字阶并随系统字体缩放。

## 4. 间距、尺寸与动效

| Token | 值 |
|---|---|
| Spacing | 4 / 8 / 12 / 16 / 24 / 32 / 48 dp（4/8 节奏） |
| 图标 | 20 / 24 / 32 / 56 dp |
| 按钮 | 次要圆按钮 56dp，主按钮高度 64dp，英雄按钮 80dp；所有可点区域 ≥ 48dp |
| 进度环 | 描边 12dp，最大直径 340dp |
| 内容最大宽度 | 480dp（平板、横屏不铺满） |
| 动效 | FAST 150ms（退出）、MEDIUM 250ms（进入）、PULSE 700ms（时间到图标呼吸） |

动效原则：退出比进入快；只用淡入淡出与 `graphicsLayer` 缩放，不改变布局尺寸；系统关闭动画（`ANIMATOR_DURATION_SCALE = 0`）时，页面切换与呼吸动画全部关闭（`rememberReducedMotion()`）。

## 5. 图标

统一使用 Material Icons（实心系）矢量，存放在 `res/drawable/ic_*.xml`，不引入 emoji，也不依赖 `material-icons-extended`。通知小图标 `ic_stat_timer` 为纯白剪影。启动图标是自适应图标，带单色层（Android 13 主题图标）。

## 6. 无障碍

- 图标按钮都有可读名称；`LabeledIconButton` 下方的可见文字即名称，读屏只读一次。
- 状态不只用颜色表达：设置页状态行同时变化图标、文字（已开启/未开启）和颜色。
- “时间到”标题是 heading 并带 polite live region；倒计时数字提供“剩余 X 分钟”描述，但不设 live region，避免每秒播报。
- 时/分/秒格子读作“5 分钟”等完整值并带选中状态；键盘“清零”“删除”键有明确的读屏名称（“清零当前项”“删除一位”）。
- 边到边显示，内容使用 Scaffold / `safeDrawing` 内边距，避开状态栏、刘海和手势条。

## 7. 交付前检查（摘自 `pro-rules.md`）

- [x] 无 emoji 图标；同一图标族；按压不改变布局
- [x] 触控目标 ≥ 48dp；按压有涟漪反馈；禁用态使用原生 disabled 语义
- [x] 浅色、深色分别核对对比度（见上表）
- [x] 安全区：Scaffold insets + `safeDrawing`
- [x] 横屏布局：设定页、计时页分栏；时间到页可滚动
- [x] 矮屏（小屏 + 通知横幅、大字体）：设定页字段与键盘可滚动，“开始”按钮固定可见
- [x] 关闭动画时无动效
- [ ] 真机/模拟器上走查小屏、大屏、最大字体（本机无 KVM，已用 Paparazzi 截图在 JVM 上核对，见 README）

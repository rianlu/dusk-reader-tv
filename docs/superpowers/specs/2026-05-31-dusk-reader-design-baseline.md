# 暮阅 TV 设计基准（以阅读页为准）

**日期**: 2026-05-31
**地位**: 全局视觉与交互的**唯一事实来源**。本文档优先级高于
`2026-04-30-dusk-reader-apple-tv-visual-design.md` 中的具体 token 规则。
**基准来源**: 阅读页（`ReaderScreen` + `ReaderSettingsOverlay`）是已被确认认可的实现，
其实际代码即为设计标准。其它页面以此为准对齐，**不再反向要求阅读页去迎合旧规格**。

> 写法说明：本文档的 token 全部从**真实代码反推**，不是理想值。凡与旧
> `apple-tv-visual-design.md` 冲突处（圆角 ≤12dp、聚焦缩放 1.06、间距仅限
> 8/12/16/24/32/48/64、禁止硬编码颜色、否决默认封面渐变），**以本文档为准**。

---

## 1. 基准来源（参考实现）

- 阅读正文与三层覆盖层：[ReaderScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/reader/ReaderScreen.kt)
- 设置抽屉与原子组件（StepperField / OptionCard / ThemeOption / StepperButton）：[ReaderSettingsOverlay.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/reader/ReaderSettingsOverlay.kt)
- 主题与亮度：[ReaderTheme.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/reader/ReaderTheme.kt)
- 通用按钮：[DuskTvButton.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/common/DuskTvButton.kt)
- 全局主题（深色 colorScheme + Inter 字体 + TV Material 默认 shapes）：[Theme.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/theme/Theme.kt) / [Type.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/theme/Type.kt)

---

## 2. 设计 Token（全部为代码实测值）

### 2.1 颜色

阅读页的「界面铬层（chrome）」**刻意不走 `colorScheme`**，而是用一组固定的近黑面板 +
白色反相聚焦。这是认可的风格，作为标准：

| 角色 | 值 | 出处 |
|---|---|---|
| 设置抽屉面板底 | `Color(0xFF111111)` | `ReaderSettingsOverlay.kt:96` |
| 目录抽屉面板底 | `Color(0xFF171717)` | `ReaderScreen.kt:576` |
| 选项卡 / 主题卡 静息底 | `Color(0xFF222222)` | `ReaderSettingsOverlay.kt:396,453` |
| 选中（未聚焦）填充 | `Color.White.copy(alpha = 0.16f)`（目录当前项 `0.14f`） | `:396` / `ReaderScreen.kt:610` |
| 小卡 / 步进按钮 静息底 | `Color.White.copy(alpha = 0.08f)` | `ReaderScreen.kt:419` |
| **聚焦填充** | `Color.White`（实底） | 全部可聚焦组件 |
| **聚焦内容色** | `Color.Black` | 反相 |
| 聚焦边框 | `BorderStroke(2.dp, Color.White)` | 全部 |
| 控制层遮罩 | 顶 `黑0.78→透明`，底 `透明→黑0.88` 竖向渐变 | `ReaderScreen.kt:395,460` |
| 正文背景 | 取自 `ReaderTheme.bgColor`（4 主题），切换 `320ms` 渐变 | `ReaderScreen.kt:135` |
| 铬层文字 | `Color.White`，次级按 `0.92 / 0.82 / 0.72 / 0.66 / 0.58 / 0.56` 递减 alpha | 全文 |

**结论性规则**：
- 沉浸式铬层（阅读控制层、目录、设置）用上表的近黑硬编码色，**允许且应当**硬编码，不要求改走 `colorScheme`。
- 浏览类页面（首页/书库/详情/传书/设置）的页面底色与卡片可继续用各自的深色硬编码（如 `0xFF05070B`），只要聚焦表现统一即可（见 2.4）。
- 4 组阅读主题：`ForestNight 0xFF101E19` / `CinemaGray 0xFF15171A` / `WarmParchment 0xFFD8C9AC` / `HighContrast 0xFF050607`。

### 2.2 圆角（采用 TV Material 默认 shapes，不另设上限）

实测 `MaterialTheme.shapes`（TV Material 1.0.0 默认）：`small = 8dp`、`medium = 12dp`、
`large = 16dp`、`extraLarge = 28dp`。阅读页的用法即标准：

| 组件 | 圆角 | 出处 |
|---|---|---|
| 选项卡 / 主题卡 / 通用按钮 | `shapes.large` = **16dp** | `ReaderSettingsOverlay.kt:394,451` |
| 列表项 / 目录项 / 主题色块 | `shapes.medium` = **12dp** | `ReaderScreen.kt:619,622`；`:468` |
| 进度/时间小标签等 chip | `shapes.small` = **8dp** | `ReaderScreen.kt:420` |
| 步进 +/- 按钮 | `CircleShape` | `ReaderSettingsOverlay.kt:346` |
| 全高侧边抽屉（设置/目录） | `RectangleShape`（直角贴边） | `ReaderSettingsOverlay.kt:97` |

> ⚠️ 旧文档「次级卡片圆角 ≤12dp」作废。**16dp 的选项卡/按钮是标准**。

### 2.3 间距（实测节奏）

阅读页用的是「4 的倍数」节奏，而非旧文档限定的 `8/12/16/24/32/48/64`：

- 面板内边距：水平 `32dp`、垂直 `28dp`（设置）；`24dp`（目录）— `ReaderSettingsOverlay.kt:103`、`ReaderScreen.kt:583`
- 分组之间：`28dp`；分组内步进之间：`24dp`；标签→控件：`12dp`；小间隙：`8dp` — `ReaderSettingsOverlay.kt:119,133,160,111`
- 控制层内边距：水平 `52dp`、垂直 `24dp` — `ReaderScreen.kt:398,463`
- 页面级正文呼吸：水平 `72dp`、垂直 `56dp`（`READER_HORIZONTAL/VERTICAL_PADDING`）— `ReaderScreen.kt:686-687`
- 侧抽屉宽度：设置 `460dp`、目录 `420dp`；选项卡高 `60dp`、主题卡高 `92dp`、步进按钮 `48dp`

**规则**：保持 4 的倍数；面板内统一 `28/24/12/8`，页面级用 `52~72` 拉开留白。不再硬性限制为旧的七档。

### 2.4 焦点交互（签名规范，最重要）

所有可聚焦元件必须满足这套「白色反相」表现，与阅读页一致：

1. **聚焦 = 白色实底 + 黑色内容 + `2dp` 白色边框**（`ClickableSurfaceDefaults.colors` 的
   `focusedContainerColor = Color.White` / `focusedContentColor = Color.Black`）。
2. **聚焦缩放 `focusedScale = 1.04f`**（不是 1.06；书库当前用 1.05，属待对齐的轻微偏差）。
3. **选中但未聚焦** = `White @0.16` 的轻微填充，不抢焦点表现。
4. 每个可滚动容器内的可聚焦行/卡必须挂 `BringIntoViewRequester` + `bringIntoView()`，保证聚焦项滚入视野（`ReaderSettingsOverlay.kt:287-292`）。
5. **显式焦点图**：用 `focusProperties { up/down/left/right }` 明确邻居，边界处用 `FocusRequester.Cancel` 封口（防止焦点逃逸）。侧抽屉整体 `left/right = Cancel`。
6. 列表/网格用 `focusRestorer()` 记忆并恢复上次焦点（目录已做：`ReaderScreen.kt:596`）。

### 2.5 排版

- **正文**：`FontFamily.Serif`，两端对齐（`TextAlign.Justify`），`letterSpacing 0.4sp`，
  `lineHeight = fontSize × lineSpacing`，字号区间 `18–80px`，带极轻文字阴影。
  正文用衬线体是刻意的阅读风格，**与铬层区分**。
- **铬层**：用全局 `Inter` 字体的 type scale：面板标题 `headlineSmall(24)` 加粗、
  书名 `titleLarge(22)` 加粗、分组标签 `titleMedium(16)`（白 `0.72`）、
  辅助信息 `labelLarge/Medium/Small`、说明 `bodySmall`。

### 2.6 动效

- 主题切换：背景/文字色 `320ms` `tween` 渐变。
- 覆盖层入场：控制层上/下 `slide + fade`；设置层从右 `slideInHorizontally`；目录层从左；退出反向。
- 不做额外花哨动画（与 `reader-fullscreen-design.md` 非目标一致）。

### 2.7 层级模型（沉浸 + 三层覆盖）

- 沉浸态 `immersive = !showControls && !showToc && !showSettings`，`HorizontalPager`
  承载单页且 `userScrollEnabled=false`，纯方向键驱动。
- `DPAD_CENTER/ENTER`（沉浸态）→ 呼出控制层；翻页 `L/R`（横）或 `U/D`（竖）或 AUTO 定时。
- **分层返回键**：目录 → 关目录；设置 → 关设置并回到控制层；控制层 → 关控制层；否则退出阅读（`ReaderScreen.kt:291-301`）。
- 浏览类页面复用顶栏 + 内容舞台 + 列表的结构，但聚焦表现必须与本节一致。

---

## 3. 各页面对齐现状（也是「其它页面完成到什么程度」的回答）

「风格对齐」= 是否符合 §2 基准；「功能完成度」= 能力是否真的可用。

| 页面 | 风格对齐 | 功能完成度 | 主要差距（需处理） |
|---|---|---|---|
| **阅读页** | ★ 基准本身 | 高 | 文档错标「草稿态」（实为即时生效）；AUTO 行动态插入导致焦点连线脆弱 |
| **设置页** | **高**（同款白底反白聚焦 / 16dp / 1.04，已对齐） | **低** | 8 个入口全是 `Toast("该入口尚未启用")`，无 ViewModel，无实际动作；列表无初始焦点/`focusRestorer` |
| **首页 + 书库**（同一 `BookshelfScreen` 双模式） | 中高（海报 12dp 合理；聚焦缩放 `1.05` 与基准 `1.04` 略偏） | 中 | 首页顶栏永不隐藏 + 初始焦点可能落空；书库网格缺 `focusRestorer`/pivot，末行 Down 边界算错 |
| **详情页** | 中高（深色氛围 + `10.5:16` 封面 + 默认焦点在阅读按钮） | 中 | 进度% 分母在「覆盖重传后/再次打开前」用的是字节数 → 显示约 0%；导入时间是写死字符串 |
| **传书页** | 中（颜色 token 对，但 4 张状态卡堆叠像后台面板） | 中 | 二维码卡不可聚焦、无上下邻居；内嵌 Ktor 服务 `@Singleton` 从不 `stop()`，离页/退出后仍在 8080 常驻 |
| **Dashboard 顶栏** | 中（白色胶囊指示器） | — | 选中态 vs 聚焦态仅靠白色透明度区分，区分度弱；72% 不透明偏重 |

> 真实结构提醒：**不存在** `screens/home/`、`screens/library/`、`common/BooksRow.kt`、
> `common/PosterWallRow.kt`、`common/BookBackdrop.kt`、`data/reader/ReaderProgressCodec.kt`。
> 首页与书库是 `BookshelfScreen` 用 `BookshelfScreenMode.Home/Library` 复用的同一个 composable。

---

## 4. 待对齐清单（按性价比）

1. **设置页接 ViewModel + 落地真实动作**（重扫书库、跳系统权限、阅读偏好入口），去掉占位 Toast。风格已对齐，补功能即可。
2. **首页焦点/顶栏**：让首页在滚动时上报 `onScroll`，并给内容一个确定的初始焦点；书库网格补 `focusRestorer()` 与末行边界修正。
3. **聚焦缩放统一为 `1.04`**：把书库的 `1.05` 收敛到基准，必要时抽一个共享常量 `FocusScale = 1.04f`。
4. **详情页进度分母** bug、**传书服务生命周期**、传书页可聚焦化与信息层级收敛。
5. 顶栏选中/聚焦态做出可见区分。

> 注意：以上多为**功能/细节**问题。整体视觉语言已与阅读页基本一致，无需大规模重做。

---

## 5. 与其它文档的关系

- 本文档**取代** `2026-04-30-dusk-reader-apple-tv-visual-design.md` 中的所有量化 token 规则；该文档保留其「内容优先 / 海报浏览」的产品方向描述，但 token 以本文为准。
- `2026-04-30-reader-fullscreen-design.md` 仍是阅读页的详细行为规格，但其中「草稿态 + 确认 + 取消」模型与实际不符（实际为即时生效），已在该文档内标注。
- `2026-04-30-dusk-reader-functional-design.md` 的页面功能状态已据实修正。

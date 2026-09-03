# 暮阅 TV 设计系统（DESIGN.md）

> **基准**：阅读页（`ReaderScreen` + `ReaderSettingsOverlay` + `ReaderTheme`）——已被产品认可的参考实现，其代码即标准。
> **地位**：本文件是全 App 视觉/交互的唯一权威设计文档，取代 `docs/superpowers/specs/2026-07-20-dusk-reader-ui-design.md`。
> **写法**：所有 token 从当前真实代码逐值反推（2026-09 核实，含 tv-material 1.1.0 源码解包验证）。与代码冲突时，先核实代码，再修文档。
> **产品方向**：本地小说版的 Apple TV。流媒体气质的浏览页 + 编辑器气质的沉浸阅读页，两种场景一套语言。

---

## 1. 设计哲学

- **内容优先**：封面与正文是主角，说明文字和功能按钮退后。每屏只有一个主层级。
- **聚焦即突出，静息即收敛**：电视遥控器是唯一输入方式，聚焦态是视觉设计的中心，其余一切让路。
- **两种气质，一套语言**：浏览页（首页/书库/详情/传书/设置）是内容浏览态；阅读页是沉浸态。风格反差是刻意的（tvOS 的 Movies app vs Reader app），但聚焦表现、圆角、面板色共享同一套 token，切换不突兀。
- **tvOS 血统**：深色玻璃感、白底反相聚焦、内容横幅（hero）、顶部轻量导航——这些是参考 Apple TV 的核心成分，不是“深色主题”的泛化。

## 2. 颜色

### 2.1 核心原则：白与近黑，不引入彩色

全 App 的交互语言建立在 **白 ↔ 近黑** 的反相关系上，品牌色不参与交互层。这是基准阅读页定下的规则，浏览页沿用它。

### 2.2 聚焦签名（最重要，全 App 唯一）

| 状态 | 定义 |
|---|---|
| **聚焦** | `containerColor = Color.White` + `contentColor = Color.Black` + `2dp White` 描边 |
| **选中·未聚焦** | `White @ 0.14~0.16` 轻填充 + ✓ 前缀标记 |
| **选中·聚焦** | 白底黑字反相 + ✓（反色为黑，仍可辨） |
| **静息** | `White @ 0.04~0.10` 或近黑实底（见 2.3），`1dp` 低透明白描边或无边 |

> ✓ 标记是选中态的必要条件：聚焦白底会盖掉 0.16 的选中填充，没有 ✓ 时无法区分“聚焦在生效项”和“聚焦在其它项”。

### 2.3 面板与背景（近黑阶）

| Token | 值 | 用途 |
|---|---|---|
| 沉浸铬层面板 | `0xFF111111` / `0xFF171717` / `0xFF222222` | 阅读设置抽屉 / 目录抽屉 / 静息选项卡（三层近黑阶，实底不透明） |
| 浏览页页面底 | `0xFF070D15` + 双层渐变 | 所有浏览页统一背景（径向 `0xFF17263A@0.52` + 垂直三段 `0xFF0B1420→0xFF08111B→0xFF070D15`） |
| 浏览页面板底 | `White @ 0.07`（描边 `White @ 0.12`） | PrimaryPanel |
| 浏览页小卡静息 | `White @ 0.04~0.10` | DuskTvButton Secondary / OptionCard 静息 / 步进按钮 |
| 控制层遮罩 | 顶 `黑0.78→透明` / 底 `透明→黑0.88` | 阅读控制层 |
| 详情页底 | `0xFF05070B` + 水平渐变 | BookDetails 专用舞台底 |

### 2.4 白色 alpha 阶梯（文字/图标）

`1.0 → 0.92 → 0.82 → 0.72 → 0.66 → 0.58 → 0.56 → 0.50 → 0.42`（代码实测的九级，同一屏内同类文字用同一档）

### 2.5 阅读主题（基准页资产，四组）

| 主题 | 背景文字 | 文字 | 气质 |
|---|---|---|---|
| 墨绿夜读 ForestNight | `0xFF101E19` | `0xFFDCE6D8` | 默认 |
| 影院暗灰 CinemaGray | `0xFF15171A` | `0xFFD8D5CC` | 中性 |
| 暖纸柔光 WarmParchment | `0xFFD8C9AC` | `0xF FF2F281E`（即 `0xFF2F281E`） | 唯一亮底 |
| 高对比 HighContrast | `0xFF050607` | `0xFFF0F0E8` | 弱视 |

文字亮度三档（乘数）：柔和 0.88 / 标准 1.0 / 清晰 1.12（`Color.scaleRgb`）。

### 2.6 强调色（仅非交互处）

`TXT=0xFFFBBF24`（琥珀）/ `EPUB=0xFF7DD3FC`（天蓝）——只用于格式 chip 边框与文字，聚焦反相后随内容变黑，**不承担任何状态语义**。

## 3. 圆角（tv-material 1.1.0 ShapeDefaults，已解包核实）

| Shape | 值 | 用途（全 App 唯一映射） |
|---|---|---|
| `ExtraSmall` | 4dp | （不用——官方样例的取值，暮阅刻意放大） |
| `Small` | 8dp | chip / 小标签 / 进度条 |
| `Medium` | 12dp | 列表项 / 目录项 / 海报卡 / 主题预览块 |
| `Large` | 16dp | 选项卡 / 通用按钮（DuskTvButton）/ OptionCard / 网格书卡 |
| `ExtraLarge` | 28dp | PrimaryPanel / 图标容器 |
| `CircleShape` | — | 步进 +/- 按钮 |
| `RectangleShape` | — | 全高侧抽屉（贴边直角） |

> 规则：**卡片与选项 Large(16)，列表行 Medium(12)，chip Small(8)，面板 ExtraLarge(28)**。`JetStreamCardShape = ShapeDefaults.Large` 全局生效，新组件按此表选型，禁止第三种映射。

## 4. 聚焦缩放（两档，不得有第三种值）

| 档位 | 值 | 适用 |
|---|---|---|
| 铬层档 | `1.04f` | 阅读设置抽屉 / 目录 / 控制层按钮 / 步进按钮 / OptionCard / ThemeOption |
| 浏览档 | `1f` | 书库网格卡 / DuskTvButton / 设置行 / 网格 tile（低端盒子上缩放是卡顿源，白底反相+描边已足够表达焦点） |

## 5. 排版

### 5.1 字体双轨制

- **阅读正文**：`FontFamily.Serif`（衬线，刻意与铬层区分）+ `TextAlign.Justify` + `letterSpacing 0.4sp` + 极轻文字阴影（`Shadow(黑@0.08, blur 4)`；HighContrast 主题 `0.28`）
- **铬层/浏览页**：`Inter` 全字重族（本地 ttf）。**注**：`LexendExa`（logo 字体）是官方样例残留，暮阅未自衬 brand 字体，顶栏 logo 可保留但不算设计系统成员

### 5.2 type scale（Inter，tv-material 默认）

| 用途 | style | 备注用法 |
|---|---|---|
| 首页 Hero 书名 | `displayMedium + Bold` | HomeBookshelf |
| 详情页书名 | `displaySmall + Bold` | BookDetails |
| 设置抽屉标题/页面主标题 | `headlineSmall + Bold/SemiBold` | ReaderSettingsOverlay / PageHeader |
| 舞台区小标 | `labelLarge` | 「最近阅读」 |
| 分组标签 | `titleMedium`（白0.72） | 「阅读主题」「翻页方式」 |
| 卡片标题 | `titleSmall + SemiBold` | 书卡书名 |
| 数值/URL | `titleLarge + SemiBold/Bold` | 步进值 / 地址块 |
| 步进标签 | `titleMedium`（白0.72） | StepperField |
| 正文说明 | `bodyMedium`（白0.58~0.66） | 副标题/说明 |
| chip | `labelSmall + SemiBold` | BookKindChip |

## 6. 间距与尺寸

### 6.1 4/8 节奏

| Token | 值 | 出处 |
|---|---|---|
| 浏览页呼吸 | 水平 `58+8` 垂直 `16` | ParentPadding/childPadding |
| 阅读正文呼吸 | 水平 `72` 垂直 `56` | ReaderScreen |
| 控制层 | 水平 `52` 垂直 `24` | 阅读控制层 |
| 设置抽屉内边距 | `32 / 28` | ReaderSettingsOverlay |
| 目录抽屉内边距 | `24` | 阅读目录 |
| 面板内 | `30 / 24` | PrimaryPanel |
| 分组间距 | `28`（组间） `24`（行距） `12`（标签→控件） `8`（小间隙） | 设置抽屉节奏 |
| 卡片留白 | `6`（tile 内）/ `18~24`（行内） | 网格/设置行 |

### 6.2 组件尺寸

| 组件 | 尺寸 |
|---|---|
| 侧抽屉宽 | 设置 `460dp` / 目录 `420dp` |
| 选项卡高 | `60dp`（OptionCard）/ `40dp`（顶栏 Tab） |
| 主题卡高 | `92dp`（内含 `60×64` 预览块） |
| 步进按钮 | `48dp` 圆 |
| 海报卡比例 | `3:4`（网格）/ `10.5:16`（Hero/详情） |
| 顶栏高度 | ~64dp（40 tab + 2×12 padding） |
| 标准按钮高 | `heightIn(min 48)`（DuskTvButton） |

## 7. 动效

- **主题切换**：背景/文字 `animateColorAsState` 320ms tween——基准页唯一动效，全局色彩过渡沿用此值
- **覆盖层**：控制层上/下 `slide+fade`；设置右滑入/目录左滑入，退出反向（阅读页已实现）
- **内容变化**：列表数据变化应有 `AnimatedContent` 过渡（官方样例模式，暮阅欠账，见 §10 待办）
- **克制原则**：新增动效先在低端盒子验证帧率，卡顿即砍。**禁止自定义滚动 tween**（`BringIntoViewSpec.scrollAnimationSpec` 保持默认弹簧——连发按键需重定向）

## 8. 焦点交互规范（签名规范）

1. **聚焦 = 白底黑字 + 2dp 白描边**（§2.2），缩放按 §4 两档
2. **显式焦点图**：每个可聚焦区域的四邻必须可预测；边界用 `FocusRequester.Cancel` 封口；抽屉整体 left/right Cancel
3. **列表容器统一模式**（对齐官方 MoviesRow / ImmersiveListScreen）：
   - 容器挂 `focusRestorer { 首项或恒组合锚点 }`
   - index 0 项挂 `focusRequester(锚点)`
   - 点击进入下一页前 `saveFocusedChild()`
   - **首项锚点需恒组合**：fallback 指向列表外的稳定元素（如 toolbar），指向 Lazy 项的 requester 在回收后 `requestFocusSafely()` 兜底，不崩不跳
4. **滚动**：聚焦项永远滚入视野——`BringIntoViewRequester + bringIntoView()`（§2.4-4，官方 MovieDetails 模式）；长列表用 `PositionFocusedItemInLazyLayout` pivot（parentFraction 0.3，公式照搬 TvMaterialCatalog，不覆写动画 spec）
5. **首焦**：一屏一个 `LaunchedEffect` 请求主 CTA/首项焦点；禁止跨屏版本号协议（已废弃机制）
6. **网格 per-item 规则最小化**：仅封不可恢复边界（如网格整体左/右界），行内移动交给默认 2D 搜索——规则越多，与动态列表重组的交互面越大，越易跳焦
7. **onScroll 纯滚动驱动**：顶栏显隐只由滚动位置决定，不掺焦点状态（官方 HomeScreen 模式）

## 9. 页面骨架

### 9.1 导航

- 外层 NavHost：`Dashboard → BookDetails/{bookId} → Reader/{bookId}`
- Dashboard 内 Tab：首页 · 书库 · 传书 · 设置（书源启用后插「找书」）
- 返回键逐层回退：覆盖层 → 页面 → Tab → Home → 退出；页面不得自创返回语义
- 顶栏：内容滚动到顶部时显示，向下滚自动收起；选中态（胶囊指示器）与聚焦态必须可区分

### 9.2 浏览页统一骨架

`PageHeader（eyebrow+title+subtitle）→ PrimaryPanel → 内容层`，全页统一背景（§2.3）。书库 = 轻筛选行 + 海报网格；详情 = 舞台背景 + 左海报 + 右元数据 + 主 CTA；设置 = 分组行列表。

### 9.3 阅读页（沉浸态）

- 单页全屏正文，HorizontalPager 纯方向键驱动；沉浸态无任何 chrome
- DPAD_CENTER 呼出控制层（顶书名/章节/进度/时间 + 底目录/设置/退出）
- 设置右抽屉**即时生效**无确认按钮；目录左抽屉跳章
- 六项验收底线：打开、翻页、跳章、调设置、退出、进度恢复——任何阅读页改动必须全回归

## 10. 已知偏差与待办（动手前重新核实）

1. **网格聚焦边框 3dp**（BookshelfScreen L754）——应为 2dp，待修
2. **colorScheme 空转**（§A1）——颜色全部硬编码，theme 层是死配置。新代码建议定义 `DuskColors` object 沉淀常量，渐进替换散落值
3. **onScroll 焦点耦合、restorer fallback 缺失、版本号首焦**（§8 违规项）——焦点修复专项处理
4. **AnimatedContent 缺失**——列表数据变化瞬变，待补
5. **FilterChip 未用**——筛选仍走对话框（§8 之外的交互债）
6. **rememberSaveable 覆盖不足**——网格滚动位置/搜索词/对话框状态进程重建丢失
7. 死代码：BookCard、SectionHeader、SecondaryPanel、ModifierUtils 部分函数、onGoTransfer/onGoBookshelf 死参数

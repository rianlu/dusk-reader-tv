# 重构任务清单

> 依据：`docs/analysis/2026-09-01-vs-official-samples.md`（根因与路线）、`DESIGN.md`（样式规范）
> 规则：完成一项勾一项并注明提交号；新增发现追加到对应分级；每完成一个 P 级做一次实机回归

---

## ✅ P0 焦点急救（已完成 · `0215550`）

- [x] onScroll 去掉 `!gridHasFocus` 耦合（顶栏显隐纯滚动驱动）→ BookshelfScreen
- [x] 删除自定义滚动 tween；pivot 公式对齐官方 PositionFocusedItemInLazyLayout（30%）
- [x] focusRestorer fallback 改恒组合锚点（searchRequester）
- [x] 版本号首焦协议全链路废除（Tab 点击改 moveFocus(Down) 官方模式）
- [x] 网格卡聚焦边框 3dp → 2dp
- [x] per-item focusProperties 最小化（仅首列/末列 Cancel）
- [x] 验证：compileDebugKotlin ✅ + testDebugUnitTest 26/26 ✅
- [ ] **实机回归**：六项验收（顶栏↔内容 / 网格四向+快速滚动 / 对话框 / 返回恢复 / 首焦 / 阅读页六底线）

## 🔧 P1 结构治理（进行中）

### P1-1 BooksGrid 容器组件 ✅ 已完成
- [x] 新建 `presentation/common/BooksGrid.kt`：焦点治理内收（pivot spec + restorer 恒组合锚点 + 边界最小规则 + state 透传）
- [x] BookshelfScreen 网格切换为 BooksGrid（tile 外观经 bookTile 参数注入 LibraryBookTile）
- [x] 删除 BookshelfScreen 内的 pivot spec/LIBRARY_GRID_COLUMNS/grid 焦点代码
- [ ] 回归：网格四向 / 快速滚动 / 搜索过滤后焦点（待实机）

### P1-2 筛选改 FilterChip 平铺行（删两个 StandardDialog）✅ 已完成
- [x] Library 模式 toolbar：格式/排序改为 `tv.material3.FilterChip` 平铺（对齐官方 MovieFilterChipRow 模式）
- [x] 删除 `LibraryOptionDialog`（两个弹窗）及其调用
- [x] 搜索入口保留按钮（唯一弹窗：搜索输入）
- [ ] 回归：chip 即点即生效、焦点不丢、无弹窗层级（待实机）

### P1-3 rememberSaveable 状态恢复 ✅ 已完成
- [x] 书库：网格滚动位置——rememberLazyGridState 默认即 saveable（核实无需改动）
- [x] 书库：搜索词/筛选/排序——ViewModel 改 SavedStateHandle（getStateFlow，进程重建恢复）
- [x] 书库：showSearchDialog → rememberSaveable
- [x] 阅读器：overlay 三态（showControls/showToc/showSettings）→ rememberSaveable
- [ ] 回归：开发者选项「不保留活动」后重建，验证搜索词/chip 状态/阅读器菜单层级恢复（待实机）

## 🧹 P2 清理与体验（未开始）

- [x] 删死代码：BookCard / SectionHeader / SecondaryPanel / ModifierUtils 死函数（focusOnInitialVisibility、createInitialFocusRestorerModifiers、ifElse）
- [x] 首页 Hero 换官方 Carousel：最近阅读轮播（D-pad 左右切书、内建自动轮播、CarouselSaver 状态恢复、400ms fade 过渡）（`603b21f`）
- [x] 网格 item 级过渡：BooksGrid 加 Modifier.animateItem()（搜索/筛选/排序切换书位平滑过渡）
- [x] 详情页 BringIntoViewRequester：核实不需要——暮阅详情页是整屏居中布局不滚动（官方 MovieDetails 用它是因为 432dp 固定高度可滚），跳过
- [ ] stash@{0} 清理（P0 实机验证通过后 drop）
- [x] P2 全部完成（`603b21f`）：死代码清理 / Carousel / animateItem / BringIntoView 核实

## 🏗️ P3 架构卫生（未开始）

- [x] DuskColors 常量沉淀（`theme/DuskColors.kt`：聚焦签名/九级阶梯/近黑铬层/背景/强调色 26 个 token；BooksGrid、DuskTvButton 已迁移，旧文件渐进迁移）
- [x] 扫描入口互斥（BookRepositoryImpl scanMutex.withLock——四处调用方并发扫描不再 Room 写竞争）
- [ ] colorScheme 决策：继续硬编码 or token 化（DESIGN.md §10-2，倾向保持 DuskColors 硬编码 + 废弃 theme/Theme.kt 的空转 darkColorScheme）

---

## 回归测试结果（2026-09-04 · TV 模拟器 sdk_google_atv64_arm64 1280×720 · 306 本测试书库）

| 项 | 结果 |
|---|---|
| 首页 Hero 初始焦点 + CENTER 进详情 | ✅ |
| 详情页 → 阅读器 → 控制层 → 设置抽屉 → 分层返回链 | ✅ 设置→控制层→沉浸→详情页逐层正确 |
| 阅读器字号步进/主题选中(✓暖纸柔光)显示 | ✅ |
| 网格快速连按 DOWN×12 / UP×12 | ✅ 无跳焦无闪退，pivot 滚动平滑 |
| 网格压力 DOWN×15+UP×15 | ✅ 零崩溃 |
| 顶栏 tab → toolbar → 网格 → toolbar → 顶栏 焦点链 | ✅（本轮修复后通过） |
| FilterChip 即点即生效（仅 TXT）+ 焦点保持 | ✅ 网格即时只剩 TXT 书 |
| 搜索弹窗（标题/占位/完成按钮/live 回显） | ✅ |
| 传书页（服务就绪 8080/重启按钮/首焦） | ✅ |
| 进程重启（force-stop 后冷启动） | ✅ 恢复正常 |
| 崩溃日志 | ✅ 0 崩溃 |

**发现并修复**：网格首行 UP 越过 toolbar 飞顶栏（2D 搜索在顶栏显隐动画中不可靠）→ BooksGrid 增加 `upRequester` 显式定向（恒组合 chip 锚点，不违反"规则最小化"——首行是动态列表与静态 toolbar 的边界，必须封口）。

**遗留待办（低优先）**：
- ~~搜索弹窗内 BACK 被 IME 消费~~ → 已修复（`5b813f0` 后）：首焦改"完成"按钮（withFrameNanos 让过 dialogFocusable 首帧抢焦）、输入框聚焦/失焦均 hide IME、DOWN 双保险定向；Gboard 模拟器下仍拦 DOWN（IME 窗口层抢事件，Compose 修饰符无解），**真机遥控器无软键盘候选导航，预期三重保障全部生效，待真机验证**
- Carousel 多书轮播：测试库只有 1 本有阅读历史，单本退化静态 Hero 属设计内行为，多书轮播待真机/真实数据验证
- 设置页/传书页内容区 UP 到顶栏需多次按键绕 TabRow 循环（焦点落哪个 tab 由几何决定）——观感可再优化，非缺陷
- 书库 toolbar 搜索按钮与首个 chip 间 LEFT 移动偶需两键（2D 搜索间隙）——低优先

## 实机回归基线清单（每个 P 级完成后过一遍）

```
□ 首页: Hero 初始焦点, 上下滚动顶栏显隐平滑
□ 书库: 网格四向移动无跳焦, 快速连按 20 次不飞
□ 书库: 搜索栏↔网格双向移动, 返回键顶栏弹出无跳行
□ 顶栏: 点击当前 Tab 焦点落内容区首项
□ 设置/传书: 进入即首焦, 传书 Loading→就绪焦点自动落位
□ 阅读页: 打开/翻页/跳章/调设置/退出/进度恢复 六底线
□ 新增: 筛选 chip 即点即生效(无弹窗), 焦点保持
```

## 变更日志

- 2026-09-04 模拟器回归二轮：设置页/重扫/搜索弹窗修复（首焦+IME 抑制），新增 4 项结论
- 2026-09-04 模拟器回归：11 项通过，修复网格首行 UP 越界，2 项低优先遗留
- 2026-09-01 P3 主体完成：DuskColors token 沉淀 + 扫描互斥
- 2026-09-01 P2 收官（`603b21f`）：Hero→Carousel，P2 四项全部完成
- 2026-09-01 P2-3 完成：网格 animateItem 过渡；详情页 BringIntoView 核实为不适用
- 2026-09-01 P2-1 完成：死代码清理（BookCard/SectionHeader/SecondaryPanel/ModifierUtils 六个死函数）
- 2026-09-01 P1-1 完成：BooksGrid 容器组件，焦点治理内收（P1 收官）
- 2026-09-01 P1-3 完成：SavedStateHandle+rememberSaveable 状态恢复
- 2026-09-01 P1-2 完成：筛选/排序 FilterChip 平铺，删 LibraryOptionDialog
- 2026-09-01 P0 完成（`0215550`）：四项根因修复 + 版本号协议废除
- 2026-09-01 文档收口（`ba3864f`）：AGENTS/DESIGN/analysis 体系建立

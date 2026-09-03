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

### P1-1 BooksGrid 容器组件（搬官方 MoviesRow 模式）
- [ ] 新建 `presentation/common/BooksGrid.kt`：焦点治理内收（focusRestorer 恒组合锚点 + pivot spec + 可选 saveFocusedChild）
- [ ] BookshelfScreen 网格切换为 BooksGrid，删除散落的网格焦点代码
- [ ] 回归：网格四向 / 快速滚动 / 搜索过滤后焦点

### P1-2 筛选改 FilterChip 平铺行（删两个 StandardDialog）✅ 已完成
- [x] Library 模式 toolbar：格式/排序改为 `tv.material3.FilterChip` 平铺（对齐官方 MovieFilterChipRow 模式）
- [x] 删除 `LibraryOptionDialog`（两个弹窗）及其调用
- [x] 搜索入口保留按钮（唯一弹窗：搜索输入）
- [ ] 回归：chip 即点即生效、焦点不丢、无弹窗层级（待实机）

### P1-3 rememberSaveable 状态恢复
- [ ] 书库：网格滚动位置（LazyGridState via rememberLazyGridState saveable 默认行为核实）
- [ ] 书库：搜索词（ViewModel 已持状态，核实进程重建恢复）
- [ ] 设置/传书页状态恢复核实
- [ ] 阅读器 overlay 状态（showControls/showToc/showSettings）

## 🧹 P2 清理与体验（未开始）

- [ ] 删死代码：BookCard / SectionHeader / SecondaryPanel / ModifierUtils 死函数（focusOnInitialVisibility、createInitialFocusRestorerModifiers、ifElse）
- [ ] 首页 Hero 换官方 Carousel（CarouselSaver 模式）
- [ ] 列表数据变化 AnimatedContent 过渡（搜索过滤/排序切换）
- [ ] 详情页主按钮 BringIntoViewRequester 聚焦滚入
- [ ] stash@{0} 清理（P0 验证通过后 drop）

## 🏗️ P3 架构卫生（未开始）

- [ ] DuskColors 常量沉淀（渐进替换散落 Color.White.copy）
- [ ] 扫描入口单 owner（scanLocalStorage 四处调用收敛，加互斥）
- [ ] colorScheme 决策：继续硬编码 or token 化（DESIGN.md §10-2）

---

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

- 2026-09-01 P1-2 完成：筛选/排序 FilterChip 平铺，删 LibraryOptionDialog（+94/-189）
- 2026-09-01 P0 完成（`0215550`）：四项根因修复 + 版本号协议废除
- 2026-09-01 文档收口（`ba3864f`）：AGENTS/DESIGN/analysis 体系建立

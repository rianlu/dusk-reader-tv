# Dusk Reader TV 完全重构设计文档

**日期**: 2026-04-20
**版本**: 1.0
**状态**: 设计阶段

---

## 1. 项目背景

### 1.1 当前问题

Dusk Reader TV 是一个 Android TV 本地阅读器应用，目前存在以下核心问题：

1. **焦点系统混乱**
   - 焦点顺序不正确
   - 页面间焦点切换异常
   - 部分元素无法聚焦
   - 焦点记忆功能不完善

2. **UI 设计不一致**
   - TransferScreen、SettingsScreen、ReaderScreen 等页面布局不统一
   - 字体大小和间距适配问题
   - 动画效果不流畅
   - 缺少视觉层次

3. **交互体验不佳**
   - 部分页面功能缺失
   - 焦点导航不符合Apple TV标准
   - 用户体验不符合10-foot UI要求

### 1.2 改造目标

- **完全重构焦点系统**：建立全局焦点图，树状结构管理，统一导航规则
- **完全重新设计UI**：参考Apple TV流媒体应用和手机端小说阅读器
- **提升用户体验**：可预测的焦点流转，清晰的视觉层次，流畅的动画

---

## 2. 焦点系统方案 (基于 JetStream 最佳实践)

### 2.1 设计原则：原生防越界与重定向

弃用原有的在业务核心中构建重量级 `FocusManager` 树状节点的方式。全面采用 Jetpack Compose TV (`androidx.tv`) 原生的焦点管理能力，利用官方的最佳实践方案实现“代码极简、焦点绝对稳定、零越界”。

**核心策略：**
1. **组内自治**：对于连续列表（如书架、设置项），交由 `TvLazyRow`、`TvLazyColumn` 或 `focusGroup` 自动处理，减少硬编码。
2. **跨组强制导向**：利用 `Modifier.focusProperties { up = xxx ; down = xxx }` 将不可预知的 2D 空间搜索锁定，强制限定焦点的转移路径。
3. **原生组件依托**：直接使用 `androidx.tv.material3.NavigationDrawer`（控制侧边栏）和 `Surface`。
4. **无缝复原**：通过 `Modifier.focusRestorer()` 自动处理从下级页面返回时的焦点记忆。

### 2.2 核心区域焦点约束配置

在应用的主要页面中，我们通过预先定义的 `FocusRequester` 作为锚点，明确规划焦点的定向跳转。

#### 主要 FocusRequester 锚点定义
```kotlin
// 侧边栏与主区
val sidebarRequester = remember { FocusRequester() }

// Home页锚点
val heroCardRequester = remember { FocusRequester() }
val bookRailRequester = remember { FocusRequester() }

// Transfer页锚点
val qrCodeRequester = remember { FocusRequester() }
val ipButtonRequester = remember { FocusRequester() }
val copyButtonRequester = remember { FocusRequester() }
```

### 2.3 各页面焦点通信流转图

**HomeScreen (主页) 流转:**
- **Hero Card (主卡片)**: 
  - `left -> sidebarRequester` (回侧边栏)
  - `down -> bookRailRequester` (进下方第一排书架)
- **Book Rails (书架)**: 
  - 自动通过 `TvLazyRow` 处理内部左右移动。
  - `left -> sidebarRequester` (在第一本书向左时回侧边栏)
  - `up -> heroCardRequester` (在第一排向上时回 HeroCard)

**Sidebar (侧边栏) 滑动流转:**
- 利用原生的 `NavigationDrawer`。
- 其内部抽屉只需设置：抽屉关闭时 `right` 将焦点交给当前激活的内容主体的 Requester（如 `heroCardRequester` ）。

### 2.4 原生焦点记忆与分组隔离

**焦点分组隔离 (防穿透):**
在没有滚动连贯要求、但是彼此是独立逻辑块的地方，使用 `Modifier.focusGroup()` 打包：
```kotlin
// Settings 页面或 Reader 控制栏
Column(modifier = Modifier.focusGroup()) {
    // 焦点会在这里内部优先循环寻找，不会因为长按突然掉到全屏幕外面去
    ReaderSettingButton()
    ReaderTocButton()
}
```

**自适应的焦点恢复 (Focus Memory):**
舍弃手写的 `FocusMemory` 缓存策略，改用 Compose-TV 的 `@OptIn(ExperimentalTvFoundationApi::class)` 以及 `focusRestorer()`：
```kotlin
Column(modifier = Modifier.focusRestorer()) {
    // 当从内页(如阅读器)退回首页时，系统自动记住上次激活的是列表里的哪个卡片
    TvLazyRow { ... }
}
```

---

## 3. UI 完全重新设计方案

### 3.1 整体设计原则

**Apple TV 流媒体应用标准：**
- 侧边栏：240dp固定宽度
- 内容区：自适应填充
- 焦点动画：300ms缓动曲线
- 间距系统：XS(8dp), S(16dp), M(24dp), L(32dp), XL(48dp)

**手机端小说阅读器最佳实践：**
- 清晰的视觉层次
- 大字体提升可读性
- 直观的操作入口
- 流畅的翻页动画

### 3.2 Sidebar（侧边栏）重新设计

**目标：** 240dp宽度，清晰的导航，自然的焦点流转

**关键改进：**
- 添加App Logo和名称
- 选中项使用蓝色高亮指示器
- 图标颜色随选中状态变化
- 底部显示版本信息
- 优化间距和字体大小

**焦点配置：**
- `right = ContentAreaFocusRequester`：从侧边栏进入内容区
- `focusProperties` 定义上下导航

### 3.3 HomeScreen（首页）重新设计

**目标：** Hero卡片 + 书籍Rail，清晰的视觉层次

**关键改进：**
- Hero卡片高度固定为360dp（原280dp响应式）
- Hero卡片添加渐变背景（深蓝到黑色）
- 添加"继续阅读"标签
- 进度条可视化显示
- 书籍Rail间距增加到32dp
- 空状态界面添加"前往传书"按钮

**焦点配置：**
- `HomeHeroPrimary`：Hero卡片焦点
- `HomeBookRailFirst`：第一本书焦点
- `down = ContentRailFocusRequester`：向下进入Rail

### 3.4 TransferScreen（传书页面）完全重构

**目标：** 简洁清晰的传书界面，焦点友好

**关键改进：**
- QR码居中显示，360dp×360dp
- IP地址和复制按钮横向排列
- 添加3步操作说明，带数字标识
- IP地址按钮可聚焦，点击复制
- 复制按钮可聚焦
- 添加"快速传书"大标题

**焦点配置：**
- `TransferQrCode`：QR码焦点
- `TransferIpAddressButton`：IP地址按钮焦点
- `TransferCopyButton`：复制按钮焦点
- 下导航到说明文字区域

### 3.5 SettingsScreen（设置页面）完全重构

**目标：** 分组清晰，易于导航

**关键改进：**
- 4个设置分组：
  1. 应用信息
  2. 阅读偏好
  3. 数据管理
  4. 关于
- 每个分组：图标 + 标题 + 副标题 + 箭头
- 添加"返回首页"按钮
- 使用Surface组件，焦点时背景变深

**焦点配置：**
- `SettingsFirstItem`：第一个设置项
- `SettingsSecondItem`：第二个设置项
- `SettingsThirdItem`：第三个设置项
- `SettingsFourthItem`：第四个设置项
- `down = ContentRailFocusRequester`：向下进入Rail

### 3.6 ReaderScreen（阅读器）重新设计

**目标：** 沉浸式阅读，简洁的控制层

**关键改进：**
- **顶部控制栏**：
  - 左侧：书名 + 章节标题
  - 右侧：目录按钮 + 设置按钮 + 返回按钮
  - 半透明黑色渐变背景

- **底部控制栏**：
  - 左侧：页码 + 进度信息
  - 中间：翻页按钮（左翻页 + 右翻页）
  - 右侧：关闭按钮
  - 半透明黑色渐变背景

- **阅读设置弹窗**：
  - 440dp宽度
  - 字体大小滑块
  - 主题选择（3个圆形色块）
  - 行间距滑块
  - 段落间距滑块
  - 翻页方式（左右翻页 / 上下滚动）
  - 确认按钮 + 取消按钮

**焦点配置：**
- `ReaderContent`：阅读内容焦点
- `ReaderControlsTop`：顶部控制栏焦点
- `ReaderSettingsButton`：设置按钮
- `ReaderToCButton`：目录按钮
- `ReaderPrevPageButton`：上一页
- `ReaderNextPageButton`：下一页
- `ReaderCloseButton`：关闭按钮

---

## 4. 字体和动效优化

### 4.1 字体系统优化

**基于1920px基准，适配不同屏幕：**
- 小屏 (< 1280px)：字体×0.7
- 中屏 (1280-1920px)：字体×0.85
- 大屏 (1920-2560px)：字体×1.0
- 超大屏 (> 2560px)：字体×1.15

**关键字体大小：**
- DisplayLarge：56sp（标题）
- DisplayMedium：40sp（副标题）
- DisplaySmall：32sp（小标题）
- TitleLarge：28sp（章节标题）
- BodyLarge：17sp（正文）
- LabelLarge：13sp（标签）

### 4.2 动画优化

**焦点动画：**
- 卡片缩放：280ms，缓动曲线CubicBezier(0.4, 0.0, 0.2, 1.0)
- Hero缩放：400ms
- 阴影：0 → 32dp
- 透明度：0.95 → 1.0

**页面转场：**
- 淡入淡出：350ms
- 滑动：400ms

**滚动动画：**
- 列表滚动：400ms

---

## 5. 实施计划

### 5.1 阶段1：核心架构（1-2天）

**任务：**
1. 创建 `ui/navigation/FocusNode.kt`
   - 定义FocusNode类和对象
   - 创建所有FocusEntry枚举值对应的Node对象

2. 创建 `ui/navigation/FocusManager.kt`
   - 实现焦点管理器核心逻辑
   - 实现导航规则管理
   - 实现焦点记忆功能

3. 创建 `ui/navigation/FocusMemory.kt`（增强版）
   - 实现页面级和Rail级记忆
   - 实现恢复逻辑

4. 创建 `ui/navigation/NavigationRules.kt`
   - 定义NavigationRules接口
   - 实现所有页面的导航规则

### 5.2 阶段2：FocusEntry扩展（0.5天）

**任务：**
1. 扩展 `ui/navigation/FocusEntry.kt`
   - 添加所有新的FocusEntry值
   - 确保所有节点都有对应Entry

2. 创建所有FocusNode对象
   - SidebarNode及其子节点
   - HomeNode及其子节点
   - TransferNode及其子节点
   - SettingsNode及其子节点
   - ReaderNode及其子节点

### 5.3 阶段3：Sidebar重构（1天）

**任务：**
1. 重构 `ui/component/Sidebar.kt`
   - 使用新的焦点系统
   - 添加Logo和名称
   - 优化选中状态显示
   - 添加版本信息

2. 测试侧边栏导航
   - 左右导航
   - 上下导航
   - 选中状态切换

### 5.4 阶段4：HomeScreen重构（1天）

**任务：**
1. 重构 `ui/screen/HomeScreen.kt`
   - 使用新的焦点系统
   - 调整Hero卡片高度为360dp
   - 优化Rail间距为32dp

2. 重构 `ui/component/HomeHeroCard.kt`
   - 添加渐变背景
   - 添加"继续阅读"标签
   - 添加进度条
   - 优化按钮布局

3. 重构 `ui/component/BookCard.kt`
   - 确保焦点配置正确
   - 优化动画效果

4. 测试Home页面导航
   - Hero → Rail
   - Rail → Hero
   - Rail内部循环导航

### 5.5 阶段5：ReaderScreen重构（1天）

**任务：**
1. 重构 `ui/screen/ReaderScreen.kt`
   - 优化顶部控制栏布局
   - 优化底部控制栏布局
   - 确保所有按钮有焦点

2. 重构 `ui/component/ReaderSettingsOverlay.kt`
   - 确保所有控件可聚焦
   - 优化滑块和按钮布局

3. 测试阅读器导航
   - 内容 ↔ 控制层
   - 翻页按钮
   - 设置和目录按钮

### 5.6 阶段6：其他页面重构（1天）

**任务：**
1. 重构 `ui/screen/TransferScreen.kt`
   - 完全重构布局
   - 添加所有焦点
   - 优化说明文字

2. 重构 `ui/screen/SettingsScreen.kt`
   - 添加4个设置分组
   - 确保焦点顺序
   - 添加返回首页按钮

3. 测试Transfer和Settings页面导航

### 5.7 阶段7：测试和优化（1天）

**任务：**
1. 焦点验证测试
   - 验证所有页面是否有可到达的焦点
   - 验证焦点顺序正确
   - 验证页面间切换正常

2. 调试工具测试
   - 实现焦点调试界面
   - 显示当前焦点和页面信息

3. 性能优化
   - 优化Compose重绘
   - 减少不必要的重组
   - 确保60fps流畅度

4. 用户测试
   - 测试各种导航场景
   - 收集用户反馈
   - 调整细节

---

## 6. 风险和挑战

### 6.1 技术风险

**焦点系统复杂性：**
- 风险：焦点逻辑复杂，容易出现bug
- 缓解：建立完善的测试用例，使用调试工具

**状态管理：**
- 风险：FocusManager与Compose状态管理可能冲突
- 缓解：使用remember和LaunchedEffect协调

### 6.2 工作量风险

**预估工作量：**
- 设计：1天
- 核心架构：1-2天
- UI重构：3天
- 测试优化：1天
- **总计：6-7天**

### 6.3 用户体验风险

**焦点恢复：**
- 风险：焦点记忆可能不自然
- 缓解：充分测试各种场景，调整恢复逻辑

**学习成本：**
- 风险：用户需要时间适应新UI
- 缓解：保持部分一致性，渐进式引导

---

## 7. 成功标准

### 7.1 功能标准

- [ ] 所有页面焦点可正常导航
- [ ] 焦点顺序符合预期
- [ ] 焦点记忆功能正常
- [ ] 所有可交互元素可聚焦

### 7.2 体验标准

- [ ] 焦点动画流畅（60fps）
- [ ] 焦点流转自然可预测
- [ ] UI布局清晰，视觉层次分明
- [ ] 字体大小适配不同屏幕

### 7.3 性能标准

- [ ] 启动时间 < 2秒
- [ ] 内存占用 < 200MB
- [ ] 无内存泄漏
- [ ] 焦点操作响应 < 16ms

---

## 8. 后续优化方向

### 8.1 焦点系统优化

- 支持焦点动画预加载
- 添加焦点路径可视化
- 实现焦点预测

### 8.2 UI优化

- 添加更多主题选项
- 优化动画效果
- 添加手势支持（未来）

### 8.3 功能增强

- 添加书签功能
- 添加目录浏览
- 添加夜间模式自动切换

---

## 附录

### A. 参考资料

- Apple TV Human Interface Guidelines
- Android TV UI Guidelines
- Jetpack Compose Focus System
- Netflix TV App Design

### B. 术语表

- **FocusEntry**: 焦点入口枚举，定义所有可聚焦的位置
- **FocusNode**: 焦点节点，树状结构中的节点
- **FocusManager**: 焦点管理器，核心控制器
- **FocusMemory**: 焦点记忆，保存和恢复焦点位置
- **NavigationRules**: 导航规则，定义焦点导航逻辑
- **10-foot UI**: 电视端UI，针对3米观看距离优化

### C. 代码示例位置

- 焦点系统：`ui/navigation/`
- UI组件：`ui/component/`
- UI页面：`ui/screen/`
- 主题：`ui/theme/`

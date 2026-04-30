# 暮阅 TV 功能设计总览

**日期**: 2026-04-30  
**定位**: 面向产品梳理、功能评审、AI 开发参考  
**写法原则**: 以页面和功能为主，架构与代码入口作为辅助索引  

---

## 1. 文档目的

这份文档用于回答 3 个问题：

1. 当前项目一共有哪几个页面
2. 每个页面已经具备哪些功能、还缺哪些能力
3. 后续 AI 或开发者应该从哪里入手继续开发

文档同时覆盖：

- **当前已实现功能**
- **已知问题 / 当前缺口**
- **建议补充功能**

为了方便阅读，文档中统一使用以下状态标记：

- `已实现`：当前代码中已有明确能力
- `部分实现`：有基础能力，但体验、边界或稳定性不足
- `待实现`：当前没有，或只有很弱的占位能力
- `建议规划`：建议纳入后续迭代

---

## 2. 产品定位

`暮阅 TV` 是一个面向电视场景的本地阅读器，核心目标是让用户在电视上完成以下链路：

1. 给电视授权访问本地存储
2. 在 `Documents/暮阅` 目录中扫描本地书籍
3. 通过“无线传书”从手机/电脑向电视发送 TXT / EPUB 文件
4. 在首页或书库中找到书籍
5. 进入书籍详情页查看基础信息
6. 打开阅读页继续阅读，并保存本地阅读进度

从产品上看，它不是一个在线内容平台，而是一个 **本地书库 + 局域网传书 + TV 阅读器**。

---

## 3. 页面总览

当前项目的页面主干如下：

1. 启动与权限页
2. Dashboard 容器页
3. 首页
4. 书库页
5. 传书页
6. 设置页
7. 书籍详情页
8. 阅读页

页面关系如下：

```text
应用启动
  -> 存储权限校验
  -> Dashboard
       -> 首页
       -> 书库
       -> 传书
       -> 设置

首页 / 书库
  -> 书籍详情页
       -> 阅读页
```

当前路由入口：

- 外层路由：`Dashboard`、`BookDetails`、`Reader`
- Dashboard 内部 Tab：`Home`、`Library`、`Transfer`、`Settings`

代码入口：

- [App.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/App.kt)
- [Screens.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/Screens.kt)
- [DashboardScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/dashboard/DashboardScreen.kt)

---

## 4. 核心用户流程

这一节不按代码结构讲，而按用户实际使用顺序讲，方便快速理解项目的主链路。

### 4.1 首次使用流程

1. 打开应用
2. 看到存储权限说明
3. 跳转系统设置授权
4. 返回应用后自动扫描 `Documents/暮阅`
5. 进入 Dashboard
6. 如果书架为空，从首页空状态跳去传书页

### 4.2 导入书籍流程

1. 用户进入传书页
2. 看到电视局域网地址和二维码
3. 用手机或电脑打开地址
4. 上传 TXT / EPUB
5. 文件写入 `Documents/暮阅`
6. 应用更新数据库并重新扫描书库
7. 返回首页或书库可看到新增书籍

### 4.3 阅读流程

1. 用户从首页或书库选中一本书
2. 进入书籍详情页
3. 点击“开始阅读”或“继续阅读”
4. 进入阅读页
5. 阅读时进行翻页、调设置、跳章节
6. 退出阅读时保存阅读进度
7. 返回后首页/详情页可以显示继续阅读状态

### 4.4 日常回访流程

1. 用户再次打开应用
2. 首页“最近阅读”优先展示历史书籍
3. 进入详情页后显示当前进度
4. 点击“继续阅读”从上次位置恢复

---

## 5. 页面功能地图

### 5.1 启动与权限页

**页面角色**

- 应用的实际首个功能入口
- 负责检查是否具备访问外部存储的权限
- 权限通过后触发本地书籍扫描

**当前已有功能**

- `已实现` Android 11+ 的 `MANAGE_EXTERNAL_STORAGE` 检查
- `已实现` Android 10 及以下的写存储权限检查
- `已实现` 无权限时展示授权说明页
- `已实现` 授权成功后调用 `bookRepository.scanLocalStorage()`

**当前问题**

- `部分实现` 权限说明文案仍偏技术化，缺少更明确的用户指引
- `部分实现` 没有显示“已扫描多少本书 / 是否扫描成功”
- `部分实现` 权限失败后的恢复路径比较单一

**建议补充**

- `建议规划` 授权成功后增加一次扫描反馈
- `建议规划` 无权限时展示更明确的步骤说明
- `建议规划` 增加“重新扫描书库”能力

**代码入口**

- [MainActivity.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/MainActivity.kt)
- [StoragePermissionHandler.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/common/StoragePermissionHandler.kt)

---

### 5.2 Dashboard 容器页

**页面角色**

- 整个主应用的 Tab 容器
- 管理顶部导航栏、Tab 切换、返回键行为和顶栏显隐

**当前已有功能**

- `已实现` 顶部导航栏切换 `首页 / 书库 / 传书 / 设置`
- `已实现` 内容区域内嵌二级 NavHost
- `已实现` 顶栏根据滚动状态显示或隐藏
- `已实现` 返回键在顶栏与内容区之间做焦点和层级回退

**当前问题**

- `部分实现` 顶栏显隐和焦点恢复逻辑较复杂，可维护性一般
- `部分实现` 交互规则主要依靠当前代码习惯，没有形成统一文档

**建议补充**

- `建议规划` 给顶栏和内容区补一份明确的焦点流转规则
- `建议规划` 把返回键策略写成统一约定，避免后续页面各自处理

**代码入口**

- [DashboardScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/dashboard/DashboardScreen.kt)
- [DashboardTopBar.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/dashboard/DashboardTopBar.kt)

---

### 5.3 首页

**页面角色**

- 作为 Apple TV 风格内容首页
- 以舞台区 + 海报墙承接“继续阅读 / 最近导入 / 进入详情”

**当前已有功能**

- `已实现` 顶部舞台区展示聚焦书籍背景、标题、作者与主按钮
- `已实现` 首页按“继续阅读 / 最近导入 / 未开始阅读 / 全部书库”组织海报墙
- `已实现` 使用统一纵向海报卡浏览书籍
- `已实现` 点击书籍进入详情页
- `已实现` 空书架状态提示
- `已实现` 空状态下提供“前往传书”按钮

**当前问题**

- `部分实现` 舞台区信息仍偏克制，尚未接入更多元数据
- `部分实现` 缺少“最近一次导入结果”这类首页级系统反馈
- `部分实现` 海报默认图已统一，但真实封面来源仍有限

**建议补充**

- `建议规划` 增加“最近上传结果”或“书库更新”提示
- `建议规划` 丰富舞台区副标题与进度信息
- `建议规划` 后续接入真实封面 / backdrop 生成链路

**代码入口**

- [HomeScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/home/HomeScreen.kt)
- [HomeScreenViewModel.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/home/HomeScreenViewModel.kt)
- [BooksRow.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/common/BooksRow.kt)

---

### 5.4 书库页

**页面角色**

- 提供所有已导入书籍的完整总览
- 适合在书多时快速浏览和选择

**当前已有功能**

- `已实现` 从本地数据库读取全部书籍
- `已实现` 使用 6 列海报网格展示书籍
- `已实现` 提供排序：最近阅读 / 最近导入 / 标题
- `已实现` 提供格式筛选：全部 / TXT / EPUB
- `已实现` 点击书籍进入详情页
- `已实现` 空状态提供跳转传书入口

**当前问题**

- `部分实现` 仍未提供搜索入口
- `部分实现` 筛选和排序仍是轻量级，没有更多分类维度
- `部分实现` 海报网格只覆盖浏览，不含批量管理能力

**建议补充**

- `建议规划` 增加搜索
- `建议规划` 增加更细的分类维度
- `建议规划` 增加书库管理动作，例如删除 / 批量整理

**代码入口**

- [LibraryScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/library/LibraryScreen.kt)
- [LibraryScreenViewModel.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/library/LibraryScreenViewModel.kt)

---

### 5.5 传书页

**页面角色**

- 作为 TV 内容应用中的设备连接页
- 提供局域网无线传书能力，让手机或电脑向电视上传 TXT / EPUB

**当前已有功能**

- `已实现` 启动局域网 HTTP 传书服务
- `已实现` 展示访问地址与大尺寸二维码
- `已实现` 复制传书地址到剪贴板
- `已实现` 刷新传书服务状态
- `已实现` 展示书籍目录 `Documents/暮阅`
- `已实现` 展示最近一次上传结果
- `已实现` 展示 3 步上传说明
- `已实现` 网络不可用时展示不可用状态与重新探测入口

**上传链路当前能力**

- `已实现` 接收 `.txt` / `.epub`
- `已实现` 写入 `Documents/暮阅`
- `已实现` 同路径文件覆盖更新
- `已实现` 上传后触发书库重新扫描

**当前问题**

- `部分实现` 传书网页样式仍较简单，还是基础上传页
- `部分实现` 没有上传进度与失败原因明细
- `部分实现` 重复文件策略仍是默认覆盖，未提供交互选择

**建议补充**

- `建议规划` 优化 Web 上传页样式与文案
- `建议规划` 增加上传进度与失败原因反馈
- `建议规划` 增加重复文件处理策略：覆盖 / 跳过 / 另存

**代码入口**

- [TransferScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/transfer/TransferScreen.kt)
- [TransferScreenViewModel.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/transfer/TransferScreenViewModel.kt)
- [FileTransferServer.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/network/FileTransferServer.kt)

---

### 5.6 设置页

**页面角色**

- 作为 TV 应用的系统与维护入口页
- 用分组入口列表承载书库、权限、阅读偏好、关于信息

**当前已有功能**

- `已实现` 分组展示“书库与权限 / 阅读偏好 / 数据与维护 / 关于应用”
- `已实现` 提供“重新扫描书库”操作
- `已实现` 提供“前往系统权限设置”入口
- `已实现` 展示阅读页设置入口说明
- `已实现` 展示本地数据与传书服务说明
- `已实现` 展示版本号、包名与应用信息

**当前问题**

- `部分实现` 仍没有真正的二级设置页，当前是“入口式单页”
- `部分实现` 尚未提供清理缓存、清空阅读进度等维护动作
- `部分实现` 阅读偏好仍在阅读页内部维护，未全局持久化

**建议补充**

- `建议规划` 增加清空阅读进度 / 清理缓存
- `建议规划` 增加真正的二级设置页结构
- `建议规划` 将部分阅读默认设置挪到这里做全局偏好

**代码入口**

- [SettingsScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/settings/SettingsScreen.kt)

---

### 5.7 书籍详情页

**页面角色**

- 承接从首页/书库进入书籍后的信息确认页
- 展示书籍元数据，并提供开始/继续阅读入口

**当前已有功能**

- `已实现` 根据 `bookId` 读取书籍
- `已实现` 展示背景氛围层、封面、标题、作者、格式、大小
- `已实现` 有阅读进度时显示进度百分比
- `已实现` 根据是否已有进度切换“开始阅读 / 继续阅读”文案
- `已实现` 默认将焦点放在阅读按钮

**当前问题**

- `部分实现` 元数据仍然偏少，没有导入时间、文件路径、章节数等信息
- `部分实现` 没有删除书籍、重新解析、查看原文件信息等操作
- `部分实现` 描述信息通常为空，因为 TXT 本地书默认没有结构化元数据

**建议补充**

- `建议规划` 增加“删除书籍”操作
- `建议规划` 增加“重新解析 / 重新生成封面”
- `建议规划` 增加导入时间、最后阅读时间等元信息
- `建议规划` 增加“从详情直接跳传书”或“查看文件位置”能力

**代码入口**

- [BookDetailsScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/bookDetails/BookDetailsScreen.kt)
- [BookDetailsScreenViewModel.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/bookDetails/BookDetailsScreenViewModel.kt)

---

### 5.8 阅读页

**页面角色**

- 项目的核心页面
- 承担正文排版、翻页、目录跳转、设置调整、阅读进度保存

**当前已有功能**

- `已实现` 读取 TXT 全文
- `已实现` 基于文本测量做分页
- `已实现` 正文按单页全屏模型显示
- `已实现` 左右键翻页
- `已实现` 记录阅读进度
- `已实现` 目录扫描与章节跳转
- `已实现` 阅读设置抽屉
- `已实现` 设置草稿态、确认应用、取消回退
- `已实现` 4 组背景主题切换
- `已实现` 字体大小、行距、段间距调整
- `已实现` 控制层、目录层、设置层基本结构
- `已实现` 退出按钮

**当前问题**

- `部分实现` 阅读设置还未持久化到全局偏好
- `部分实现` 需要继续做 TV 模拟器上的长时回归，验证分页窗口切换稳定性
- `部分实现` EPUB 前端阅读链路仍未补齐

**建议方向**

- `建议规划` 将阅读设置持久化为全局偏好
- `建议规划` 继续强化多分辨率 / 长文本回归
- `建议规划` 补齐 EPUB 阅读链路

**配套设计文档**

- [2026-04-30-reader-fullscreen-design.md](/Users/lu/AIProjects/dusk-reader-tv/docs/superpowers/specs/2026-04-30-reader-fullscreen-design.md)

**代码入口**

- [ReaderScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/reader/ReaderScreen.kt)
- [ReaderViewModel.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/reader/ReaderViewModel.kt)
- [ReaderSettingsOverlay.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/reader/ReaderSettingsOverlay.kt)
- [ReaderTheme.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/reader/ReaderTheme.kt)

---

## 6. 页面操作速览

这一节用于快速浏览“每个页面到底能操作什么”，方便你后续做删改。

### 6.1 首页

- 可见内容：舞台区、继续阅读、最近导入、未开始阅读、全部书库、空状态提示
- 可执行操作：选书、进入详情、舞台区开始阅读、空状态跳传书
- 当前缺少：最近上传结果等首页级反馈

### 6.2 书库页

- 可见内容：轻量筛选条、全部书籍海报网格
- 可执行操作：选书、进入详情、切换排序、切换格式筛选
- 当前缺少：搜索、更细分类

### 6.3 传书页

- 可见内容：二维码、地址、服务状态、目录、最近上传、说明
- 可执行操作：复制地址、刷新服务状态
- 当前缺少：Web 上传页优化、上传进度反馈

### 6.4 设置页

- 可见内容：分组设置入口、书库状态、应用信息
- 可执行操作：重扫书库、打开权限设置
- 当前缺少：缓存与阅读数据管理、二级设置页

### 6.5 书籍详情页

- 可见内容：封面、标题、格式、大小、阅读进度
- 可执行操作：开始阅读、继续阅读、返回
- 当前缺少：删除书籍、更多元数据、文件管理动作

### 6.6 阅读页

- 可见内容：单页正文、控制层、目录层、设置层
- 可执行操作：左右翻页、跳章节、调设置、退出
- 当前缺少：全局偏好持久化、EPUB 阅读链路

---

## 7. 核心数据与服务能力

### 7.1 书籍数据模型

核心实体是 `Book`，当前包含：

- 基础信息：标题、作者、格式、路径、封面路径、描述
- 文件信息：文件大小、总大小、导入时间
- 阅读信息：最后阅读章节、最后阅读位置、最后阅读时间

当前现实情况：

- TXT 本地书的作者、简介、封面通常并不完整
- 已新增 `backdropPath` 作为首页 / 详情页的横版背景字段
- 进度保存依赖 `lastReadPosition`
- 书库主要依赖本地文件路径防重

代码入口：

- [Book.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/data/entities/Book.kt)

### 7.2 书库仓储能力

`BookRepository` 当前负责：

- 读取全部书籍
- 读取最近阅读书籍
- 按 ID 查单本书
- 按文件路径查重
- 插入 / 更新 / 删除书籍
- 扫描本地 `Documents/暮阅`

代码入口：

- [BookRepository.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/data/repositories/BookRepository.kt)
- [BookRepositoryImpl.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/data/repositories/BookRepositoryImpl.kt)

### 7.3 阅读引擎能力

当前阅读引擎主要面向 TXT：

- 全文读取
- 分段
- 章节识别
- 保存/恢复阅读位置

当前状态：

- `部分实现` TXT 主流程可用
- `待实现` EPUB 阅读能力尚未形成完整前端阅读链路说明

代码入口：

- [TxtReaderEngine.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/data/reader/TxtReaderEngine.kt)
- [ReaderProgressCodec.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/data/reader/ReaderProgressCodec.kt)

### 7.4 传书服务能力

当前无线传书服务是一个内嵌的 Ktor HTTP 服务：

- 默认端口 `8080`
- 获取本机局域网 IP
- 提供上传页面
- 接收上传文件
- 写入 `Documents/暮阅`
- 更新或插入数据库记录
- 上传完成后重新扫描书库

当前状态：

- `已实现` 基础可用
- `部分实现` 缺少更强的错误处理和用户反馈

---

## 8. 当前已实现功能总表

从产品视角看，目前项目已经具备以下最小闭环：

1. 存储授权
2. 扫描本地书库
3. 首页和书库展示书籍
4. 无线传书上传 TXT / EPUB
5. 进入书籍详情页
6. 打开阅读页阅读 TXT
7. 保存与恢复阅读进度

这说明项目已经不是空壳，而是已经具备了 **可运行的本地阅读器闭环**。

---

## 9. 当前已知问题与缺口

### 9.1 高优先级

1. 阅读设置仍未持久化为全局偏好
2. EPUB 阅读前端链路仍未补齐
3. 阅读页需要继续做 TV 模拟器长时稳定性回归

### 9.2 中优先级

1. 书库页缺少搜索
2. 设置页缺少二级设置结构与数据清理能力
3. 传书 Web 页缺少更强反馈与样式优化

### 9.3 低优先级

1. 首页信息层次可以进一步加强
2. 详情页可补更多元数据和操作
3. 传书 Web 页可进一步美化

---

## 10. 建议迭代路线

### 第一阶段：阅读主链收口

目标：把已完成的全屏阅读模型做稳定收口。

建议内容：

1. 阅读设置全局持久化
2. TV 模拟器长时回归
3. EPUB 阅读链路补齐
4. 多分辨率分页稳定性验证

### 第二阶段：书库体验增强

目标：让书多时仍然易找、易选。

建议内容：

1. 搜索
2. 排序
3. 筛选
4. 空状态与操作捷径优化

### 第三阶段：传书和设置完善

目标：让导入和维护更自助。

建议内容：

1. 传书 Web 页优化
2. 上传进度 / 失败反馈
3. 缓存与阅读数据管理
4. 设置二级页化

### 第四阶段：内容元数据增强

目标：让书库信息更丰富。

建议内容：

1. 解析更多 EPUB 元数据
2. 书籍简介、作者、章节数补全
3. 封面策略优化

---

## 11. AI 开发参考索引

如果后续让 AI 继续开发，建议按以下入口理解项目：

### 11.1 路由与页面结构

- [App.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/App.kt)
- [Screens.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/Screens.kt)
- [DashboardScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/dashboard/DashboardScreen.kt)

### 11.2 页面实现入口

- 首页：[HomeScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/home/HomeScreen.kt)
- 书库：[LibraryScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/library/LibraryScreen.kt)
- 传书：[TransferScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/transfer/TransferScreen.kt)
- 设置：[SettingsScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/settings/SettingsScreen.kt)
- 详情：[BookDetailsScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/bookDetails/BookDetailsScreen.kt)
- 阅读：[ReaderScreen.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/presentation/screens/reader/ReaderScreen.kt)

### 11.3 数据和服务入口

- 仓储：[BookRepository.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/data/repositories/BookRepository.kt)
- 仓储实现：[BookRepositoryImpl.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/data/repositories/BookRepositoryImpl.kt)
- 数据库：[AppDatabase.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/data/local/AppDatabase.kt)
- DAO：[BookDao.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/data/local/BookDao.kt)
- 传书服务：[FileTransferServer.kt](/Users/lu/AIProjects/dusk-reader-tv/app/src/main/java/com/wzl/duskreader/tv/network/FileTransferServer.kt)

### 11.4 当前最值得优先处理的模块

1. `reader/*`
2. `bookDetails/*`
3. `transfer/*`
4. `home/*` 和 `library/*`

---

## 12. AI 修改注意点

这一节是给后续 AI 或开发者的直接提醒，避免修改时偏题或误伤已有逻辑。

### 12.1 阅读页是最高优先级区域

- 阅读页目前功能最多，但也最不稳定
- 任何阅读页修改都应该优先验证：打开、翻页、跳章、设置、退出、进度恢复

### 12.2 不要只看 UI，要同时看 ViewModel

- 很多页面看起来只是界面问题，实际状态都在 ViewModel 或 Repository
- 尤其是阅读页，UI、分页、进度保存、章节跳转是联动关系

### 12.3 首页与书库共享同一书籍源

- 首页的“最近阅读 / 全部书库”和书库页都来自 `BookRepository`
- 改动数据结构时要同时检查这两页

### 12.4 传书页修改不能只改前端

- 传书页本身只是展示层
- 真正上传逻辑在 `FileTransferServer`
- 改地址、端口、重复文件策略时，要同时检查服务层和书库扫描

### 12.5 设置页目前不是完整设置中心

- 不要假设设置页已经承担了全局配置管理
- 目前很多阅读设置仍在阅读页内部维护

### 12.6 权限与本地文件访问是底层前提

- 没有外部存储权限时，首页、书库、传书、阅读都会间接受影响
- 遇到“页面没数据”时要先排查权限和扫描，而不是直接改 UI

---

## 13. 结论

从功能上看，`暮阅 TV` 当前已经形成了“本地书库 + 传书 + 阅读”的最小闭环。  
从体验上看，当前主链已经切到 **Apple TV 风格的海报浏览 + 全屏阅读**。  
从开发规划上看，最合理的路线是：

1. 先收口阅读页持久化与稳定性
2. 再增强书库检索与管理
3. 最后补传书 Web 页和设置二级结构

如果这份文档作为后续开发基线，建议优先把阅读页方案与本文件一起看，再继续拆实现计划。

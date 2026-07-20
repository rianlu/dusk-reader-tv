# 暮阅 · 自定义书源接入 — 开发功能文档

> 状态:待评审 → 待开发
> 日期:2026-07-21
> 本文档是书源功能的**唯一权威规范**。UI 规范从属于 `2026-07-20-dusk-reader-ui-design.md`,不得另立体系。

---

## 1. 背景与产品定位

暮阅当前是纯本地 TV 阅读器(TXT/EPUB)。用户核心诉求:电视上看网络小说,本地导入链路太长。本功能引入**用户自定义书源**(兼容 Legado「开源阅读」书源 JSON 格式),实现:搜书 → 加入书架 → 在线逐章阅读 → 本地缓存离线阅读。

### 1.1 定位与合规护栏(硬性约束,不得违反)

产品自我定位:**本地阅读器,可选支持用户自定义内容来源**。不是网络小说聚合器。

| # | 护栏 | 落实方式 |
|---|------|----------|
| 1 | 不内置任何书源 | 安装后书源列表为空,无预置数据 |
| 2 | 不推荐、不链接任何书源 | App 内、README、发布渠道均不出现任何书源 URL 或获取指引 |
| 3 | 书源功能默认关闭 | 设置页总开关「启用自定义书源」,默认 OFF;关闭时所有书源相关 UI 不可见,App 形态 = 纯本地阅读器 |
| 4 | 免费、无广告、无内购 | 永久约束 |
| 5 | 首次开启弹免责声明 | 用户确认「仅用于访问自有授权内容,书源由本人提供并自负其责」后方可启用 |
| 6 | 分发只走 GitHub/侧载 | 不上架国内应用商店 |

### 1.2 不做的功能(明确排除)

- **订阅源(RSS 源)**:TV 场景弱且加重聚合器属性,不做。
- **发现页/分类浏览(`exploreUrl`)**:一期不做(该字段大量依赖 `@js:`,且"逛书城"心智偏聚合器);仅保留字段解析不报错。
- **书源编辑/调试器(TV 端)**:遥控器不可行;二期在 Ktor 网页端提供只读的体检报告,不做规则编辑器。
- **听书、全局模式切换、双书架**:不做。书架/阅读器统一,来源只是书的属性。

---

## 2. 总体架构

### 2.1 原则

- **阅读链路完全复用**:分页(`buildReaderPages`)、进度 `(chapter, position)`、主题、翻页模式、LruCache 一律不改。网络书对阅读器而言只是"另一种章节来源"。
- **来源抽象**:新增 `ChapterProvider` 接口,本地实现包装现有引擎,网络实现走书源规则引擎;`ReaderViewModel` 只面向接口。
- **纯逻辑下沉**:规则解析、URL 拼装、节流策略、缓存淘汰策略全部写成 `data/entities/` 或 `data/booksource/` 下的框架无关纯函数/`object`,保持单元可测(项目无 UI 测试,这是唯一测试防线)。

### 2.2 模块与包结构(新增)

```
data/booksource/
  BookSource.kt              // Room 实体:书源(原始 JSON + 解析后的元信息)
  BookSourceParser.kt        // Legado JSON → BookSource,含合法性校验(纯逻辑)
  rule/
    RuleParser.kt            // 规则字符串 → RuleStep 链(纯逻辑)
    RuleEvaluator.kt         // RuleStep 链 + 文档 → 结果(依赖 jsoup,可测)
    AnalyzeUrl.kt            // searchUrl/{{key}}/{{page}}/POST 参数拼装(纯逻辑)
  engine/
    BookSourceEngine.kt      // 搜索/详情/目录/正文 四段流程编排
    SourceHttpClient.kt      // HTTP 客户端封装:UA/header/charset/节流/超时
    ConcurrentRateLimiter.kt // "20/60000" 节流(纯逻辑 + 时钟注入,可测)
  cache/
    ChapterCacheStore.kt     // 网络章节落盘缓存(文件目录 + 索引)
data/reader/
  ChapterProvider.kt         // 接口:suspend getChapterText(index): Result<String>
  LocalChapterProvider.kt    // 包装 TxtReaderEngine/EpubReaderEngine
  NetworkChapterProvider.kt  // BookSourceEngine + ChapterCacheStore
presentation/screens/discover/   // 「找书」页(搜索 + 结果 + 详情预览)
presentation/screens/sourcemanage/ // 书源管理(列表/启停/删除/体检)——入口在设置页
network/FileTransferServer.kt    // 扩展:网页端书源导入/管理路由
```

### 2.3 新增依赖

| 依赖 | 用途 | Android 9 兼容性 |
|------|------|------------------|
| `ktor-client-core` + `ktor-client-cio` | HTTP 抓取(与现有 Ktor server 同版本族,不引 OkHttp 双栈) | ✅ CIO 纯 Kotlin,minSdk 28 无问题 |
| `jsoup` | HTML 解析 + CSS 选择器 | ✅ 纯 Java,API 28 兼容 |
| (二期)`quickjs-android` 或 `mozilla-rhino` | `@js:` 规则 | 选型时验证 API 28;Rhino 纯 Java 最稳 |

不引入 JSONPath 库:一期用 kotlinx-serialization `JsonElement` 手写点路径取值(`$.data.list` 级别),覆盖绝大多数 JSON 型书源。

---

## 3. 书源格式:Legado JSON 兼容子集

### 3.1 顶层字段支持矩阵

导入接受**单对象或对象数组**。字段解析策略:认识的用,不认识的原样保留在 `rawJson` 中(前向兼容,导出时不丢失)。

| 字段 | 一期 | 说明 |
|------|------|------|
| `bookSourceName/Url/Group/Comment` | ✅ | 名称为空 → 校验失败 |
| `enabled` | ✅ | |
| `bookSourceType` | ✅ | 仅支持 `0`(文本);`1` 音频/`2` 图片/`3` 文件类标记「不支持」并禁用 |
| `searchUrl` + `ruleSearch` | ✅ | 核心 |
| `ruleBookInfo` | ✅ | |
| `ruleToc`(含 `nextTocUrl`) | ✅ | 目录翻页必须支持(大量源目录分页) |
| `ruleContent`(含 `nextContentUrl`、`replaceRegex`) | ✅ | 正文翻页必须支持 |
| `header` | ✅ | JSON 字符串,含 UA |
| `concurrentRate` | ✅ | `"次数/毫秒"` 节流,缺省默认 `1/1000` |
| `bookUrlPattern`、`customOrder`、`weight` | ✅ | 排序/匹配辅助 |
| `enabledCookieJar` | ✅ | Ktor client CookiesStorage,按源隔离 |
| `loginUrl/loginUi` | ❌ | 标记「不支持登录源」 |
| `exploreUrl/ruleExplore` | ⏸ | 解析不报错,功能二期 |
| 任意 `@js:`/`<js></js>` 规则 | ⏸ | 一期遇到 → 该条规则视为不可用(见 3.3 降级策略) |

### 3.2 规则语法支持子集(一期)

Legado 规则是链式字符串。一期实现以下算子(覆盖社区书源主流写法):

```
选择器段(@ 分隔链式):
  class.xxx.N / id.xxx / tag.xxx.N / .css类 / #id     — jsoup 定位,.N 取第 N 个(负数从尾数)
  children / text.关键字                                — 子元素 / 按文本匹配元素
终值段:
  @text / @textNodes / @html / @href / @src / @content / @all / @属性名
后处理:
  ##正则                — 匹配删除(replaceAll 为空)
  ##正则##替换          — 替换
  ##正则##替换###       — 只替换第一个
组合:
  A||B    — A 无结果用 B(备选)
  A&&B    — 结果合并
  {{key}} / {{page}}    — searchUrl/翻页变量
  @json:$.a.b / $.a.b   — JSON 点路径(响应为 JSON 时)
URL 规则(searchUrl 附加):
  path,{"method":"POST","body":"kw={{key}}","charset":"gbk"}
```

**不支持(一期)**:`@XPath://`、`@js:`、`<js>`、`{{java.xxx}}`、`@css:`(与默认等价的除外)、正则捕获组模板 `$1` 以外的高级替换。

### 3.3 降级与容错策略

- 逐条规则独立降级:`ruleSearch.coverUrl` 用了 `@js:` → 封面为空,不影响搜索结果;`ruleContent.content` 不可用 → 该源标记「正文规则不支持」,搜索结果中该源置灰。
- 关键链路判定:`searchUrl + ruleSearch.bookList/name/bookUrl` + `ruleToc.chapterList/chapterName/chapterUrl` + `ruleContent.content` 均可解析 → 源「可用」;否则「部分支持」或「不支持」,导入时即静态标注。
- 网络失败:单章抓取重试 2 次(指数退避);同一源**连续 5 次**失败 → 自动禁用并在书源管理页提示,用户可手动重启。

### 3.4 字符集

响应解码顺序:`header/URL 规则声明的 charset` → HTTP `Content-Type` charset → HTML `<meta charset>` 嗅探 → UTF-8。GBK/GB18030 站点极多,**必须**实现嗅探(复用 `TxtReaderEngine` 的探测思路)。

---

## 4. 数据层设计

### 4.1 Room 变更(数据库版本 +1,提供 Migration,禁止破坏性重建)

**新表 `book_sources`**:

```kotlin
@Entity(tableName = "book_sources")
data class BookSource(
    @PrimaryKey val sourceUrl: String,     // bookSourceUrl 作为主键(去重依据)
    val name: String,
    val group: String?,
    val enabled: Boolean,
    val customOrder: Int,
    val supportLevel: Int,                 // 0 可用 / 1 部分支持 / 2 不支持
    val rawJson: String,                   // 原始 JSON 全文(导出/前向兼容)
    val lastCheckTime: Long?,              // 最近体检时间
    val lastCheckOk: Boolean?,
    val respondTimeMs: Long?,              // 体检耗时(排序用)
    val failureStreak: Int,                // 连续失败计数(自动禁用)
    val importedAt: Long,
)
```

**`books` 表新增列**(全部带默认值,老数据自动为本地书):

```
sourceType: Int = 0        // 0 LOCAL / 1 NETWORK
sourceUrl: String? = null  // 关联 book_sources.sourceUrl
remoteBookUrl: String? = null  // 书籍详情页 URL
remoteTocUrl: String? = null   // 目录页 URL(部分源与详情页不同)
latestChapterTitle: String? = null  // 最新章节(追更展示)
```

**不变式沿用**:网络书的 `totalSize` 同样存**章节总数**(目录抓取后写入),`progressRatio()` 逻辑零改动。`filePath` 对网络书存缓存目录路径。

**`book_chapters` 表**:网络书目录同样写入该表(chapterIndex/title/url 存 `BookChapter`,需评估现有字段,url 可复用偏移量字段或加列 `chapterUrl: String?`)。

### 4.2 章节缓存

- 位置:`context.filesDir/booksource_cache/{bookId}/{chapterIndex}.txt`(内部存储,卸载即清,不落公共目录)。
- 读取顺序:内存 LruCache(现有 3 章) → 磁盘缓存 → 网络抓取(抓到即落盘)。
- 缓存管理:设置页显示占用总量 + 「清空书源缓存」;单书从书架删除时同步删缓存目录。
- 上限:默认不设硬上限(小说文本量小),仅提供手动清理;二期再考虑 LRU 磁盘淘汰。

### 4.3 节流与预读

- `ConcurrentRateLimiter` 按源实例化,严格遵守 `concurrentRate`;缺省 `1/1000`。
- 预读窗口:当前章加载完成后,后台顺序预抓 **后 2 章**(受同一节流器约束);翻到新章即滑动窗口。预读失败静默,不打扰阅读。
- 手动缓存:书籍详情页提供「缓存后 50 章 / 缓存全本」,串行 + 节流执行,进度可见、可取消、断点续传(已缓存章节跳过)。整本缓存耗时长是**预期行为**,UI 明示「受书源站点限速,请耐心等待」。

---

## 5. 网络层与 Android 9 (minSdk 28) 约束

| 事项 | 现状/方案 |
|------|-----------|
| 明文 HTTP | Manifest 已有 `usesCleartextTraffic="true"` ✅(大量书源站为 http,保持不变) |
| TLS | API 28 默认启用 TLS 1.3,无老设备 TLS 问题 |
| UA | 默认 UA 用书源 `header` 声明;缺省用移动端 Chrome UA(TV 默认 UA 会被部分站拦截) |
| 超时 | 连接 10s / 读取 15s;正文页 `nextContentUrl` 循环拼接设最大 10 页防死循环 |
| 重定向 | 跟随,但记录最终 URL 用于相对路径解析(`baseUri`) |
| 相对 URL | 一律以响应最终 URL 为 base 解析(jsoup `absUrl` / 手动 `URI.resolve`) |
| 主线程 | 所有抓取在 `Dispatchers.IO`;规则解析(jsoup)在 `Dispatchers.Default` |
| 免安装依赖 | 不使用 WebView 抓取(一期无 JS 渲染需求;WebView on TV 不可靠) |

---

## 6. UI 设计(严格遵循设计基准)

所有 token 引用 `2026-07-20-dusk-reader-ui-design.md`:焦点态 = 白底黑字 + 2dp 白描边(沉浸铬层缩放 1.04 / 浏览页 1f);面板色 `0xFF111111/171717/222222`;圆角 8/12/16dp;正文衬线。**本节只定义结构与流程,不新增任何视觉 token。**

### 6.1 设置页 — 「自定义书源」区块

- 新增设置分组「自定义书源」,首项为总开关(默认关)。
- 首次开启 → `StandardDialog` 免责声明(标题「启用自定义书源」,正文含 1.1-#5 文案,确认/取消双按钮,焦点默认在「取消」)。
- 开启后该分组展开三行:「书源管理」(进入 6.4)、「书源缓存 · 已用 xx MB」(点按清空,二次确认)、「网页端管理 · http://<ip>:8080」(只读提示)。
- 关闭总开关:找书 Tab 隐藏、书源管理不可入;**书架上已有的网络书保留可见**,已缓存章节可继续读,未缓存章节提示「书源功能已关闭」。(用户的书不消失——开关控制功能入口,不隐藏内容。)

### 6.2 Dashboard — 「找书」Tab

- Tab 顺序:`Home · 书架 · 找书 · 传书 · 设置`;总开关关闭时不渲染「找书」。
- 页面结构(自上而下):
  1. 搜索框(复用书架搜索框样式)+ 右侧「搜索」按钮。TV 输入痛点缓解:搜索框旁提供「手机输入」入口,展示二维码(复用 zxing)指向 Ktor 网页端搜索页,手机输入关键字后电视端实时收到并执行搜索。
  2. 结果列表:`LazyColumn` 行式条目(封面缩略 + 书名 + 作者 + 来源源名 + 最新章节),同名同作者结果**聚合为一条**,展开显示「N 个来源」。行焦点态按基准(白底黑字)。
  3. 状态:逐源流式返回(某源慢不阻塞整体);顶部显示「已搜 x/y 源」;全部失败给重试。
- 搜索执行:并发查询所有 `enabled && supportLevel==0` 的源,单源超时 15s,受各自节流器约束。
- 焦点图:搜索框 ↓ 结果列表 ↑↓ 循环,`focusRestorer()` 保持回焦;左边界 `FocusRequester.Cancel` 防逃逸(与现有 Tab 页一致)。

### 6.3 网络书详情页(复用 BookDetails)

- 现有 `BookDetailsScreen` 扩展:网络书显示来源名角标、最新章节、以及三个动作:「开始阅读 / 换源 / 缓存管理(后 50 章 · 全本)」。
- 「加入书架」发生在找书结果页选中详情后:抓详情(`ruleBookInfo`)+ 目录(`ruleToc`)→ 写 `books` + `book_chapters` → 出现在书架(与本地书混排,卡片右上角「源」小角标,样式对齐现有封面角标规范)。
- 换源:弹 `FullScreenDialog` 列出其它源中按「书名+作者」精确匹配的结果(后台预搜),选中后替换 `sourceUrl/remoteBookUrl` 并重抓目录;进度按 `(chapter, position)` 保留,章节数不一致时 clamp 到目标源目录范围。

### 6.4 书源管理页(设置进入,全屏)

- 列表行:源名 + 分组 + 状态徽标(可用/部分支持/不支持/已禁用/失败自动禁用)+ 体检耗时。
- 行内动作(D-pad OK 弹菜单):启用/禁用、置顶、删除(二次确认)、体检此源。
- 顶部动作条:「全部体检」「从网页端导入」(展示二维码 + URL)。
- **TV 端不提供 URL 输入框**——书源导入的主路径是网页端(6.5),TV 端只做管理和体检。这是刻意的流程设计:遥控器输入 URL 的失败率高到不可接受。

### 6.5 Ktor 网页端扩展(FileTransferServer)

新增路由(沿用现有网页端风格):

- `GET /sources` — 书源管理页:列表(状态/体检结果)、启停、删除、导出全部(JSON 下载)。
- `POST /sources/import` — 三种导入:粘贴 JSON 文本 / 上传 .json 文件 / 提交 URL(**由 App 端发起抓取**该 URL,手机只传 URL 字符串)。导入即执行静态校验,返回逐源结果(成功 n / 部分支持 m / 失败 k + 原因)。
- `GET /search-remote` — 手机代输搜索页:输入关键字 → App 端「找书」页收到并执行(通过内存事件流,不落库)。
- 总开关关闭时以上路由返回「功能未启用」页。

### 6.6 阅读页(改动最小化)

- 新增中间态:章节网络加载中(居中小字「正在获取本章…」,沿用阅读主题前景色);失败态给「重试 / 换源」两按钮(焦点默认「重试」)。
- 其余(分页、翻页、设置浮层、进度)零改动。

## 6.7 必须保证的用户流程(验收基准)

以下 6 条流程为验收硬标准,任何一条走不通不得发布:

1. **开启**:设置 → 打开总开关 → 阅读并确认免责声明 → 「找书」Tab 出现。
2. **导入书源**:设置 → 书源管理 → 扫码打开网页端 → 粘贴书源 URL/JSON → 导入成功,TV 端列表实时出现,标注支持级别。
3. **搜书入架**:找书 → (可选手机代输)输入书名 → 结果聚合列表 → 选择 → 详情+目录抓取 → 加入书架 → 书架混排可见、带角标。
4. **在线阅读**:书架 → 网络书 → 阅读页正常分页/翻页/存进度;翻章时预读生效,正常网络下无感等待。
5. **离线兜底**:已缓存章节在断网时可读;未缓存章节提示明确、不崩溃。
6. **失效恢复**:正文抓取失败 → 重试/换源 → 换源后进度保留、继续阅读。

---

## 7. 分期实施计划

### Phase 1 — 规则引擎(纯逻辑,先行)
`BookSourceParser` / `RuleParser` / `RuleEvaluator` / `AnalyzeUrl` / `ConcurrentRateLimiter` + 完整单元测试(以「速读谷」等真实源 JSON + 本地保存的 HTML fixture 做离线测试,**测试不访问网络**)。

### Phase 2 — 数据与抓取
Room 迁移、`BookSource` 表、`SourceHttpClient`(charset/UA/cookie/节流)、`BookSourceEngine` 四段流程、`ChapterCacheStore`、`ChapterProvider` 抽象及双实现、`ReaderViewModel` 接入。

### Phase 3 — UI 主流程
设置总开关+免责声明、找书 Tab(搜索/结果/入架)、BookDetails 扩展、阅读页中间态、书架角标与来源筛选。

### Phase 4 — 管理与体验闭环
书源管理页、体检、网页端导入/管理/代输搜索、换源、手动缓存(50 章/全本)、自动禁用。

### Phase 5(二期,另立文档)
`@js:` 规则(Rhino/QuickJS 选型)、发现页、XPath、登录源、磁盘缓存淘汰。

### 测试策略
- 全部业务规则(规则解析、URL 拼装、节流、支持级别判定、换源进度 clamp、缓存路径策略)为纯函数,进 `app/src/test/`。
- 引擎集成测试用本地 HTML/JSON fixture,零网络依赖,CI 可跑。
- 无 UI 自动化(与项目现状一致),6.7 的 6 条流程为手工验收清单。

---

## 8. 风险与开放问题

| 风险 | 应对 |
|------|------|
| 社区书源大量使用 `@js:`,一期兼容率不足 | 导入时静态标注支持级别,预期管理;二期补 JS 引擎 |
| 书源站封禁/失效常态化 | 换源 + 体检 + 自动禁用是产品能力而非补丁,Phase 4 必做 |
| TV 遥控器输入 | 手机网页端代输 + 二维码为主路径,TV 输入仅兜底 |
| 合规 | 1.1 六条护栏为发布检查项 |
| `book_chapters` 表结构是否够放章节 URL | Phase 2 开工前确认,必要时加列(带默认值迁移) |

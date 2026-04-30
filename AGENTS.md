# 项目经验补充

## KSP 构建验证冲突

- 标题：KSP / Hilt / Room 生成链不要并行跑 `testDebugUnitTest` 和 `assembleDebug`
- 触发信号：同一轮验证里并行运行 `./gradlew testDebugUnitTest` 与 `./gradlew assembleDebug` 后，出现 `kspDebugKotlin` 的 `FileAlreadyExistsException`、`NoSuchFileException`、`byRounds` 目录异常，或 Kotlin/KSP 增量缓存伪失败
- 根因 / 约束：当前项目的 KSP 生成目录会被并行 Gradle 任务共享，`testDebugUnitTest` 与 `assembleDebug` 同时运行时，Hilt / Room / KSP 输出可能互相踩写，导致构建链失败，但不代表业务代码本身有编译错误
- 正确做法：验证时串行执行；先 `./gradlew --stop`，必要时 `./gradlew clean`，然后分别单独运行 `./gradlew testDebugUnitTest -Pksp.incremental=false -Dkotlin.incremental=false --no-daemon` 和 `./gradlew assembleDebug -Pksp.incremental=false -Dkotlin.incremental=false --no-daemon`
- 验证方式：以串行、无增量、无常驻 daemon 的命令结果为准；若两条命令都通过，再认定代码状态通过
- 适用范围：所有涉及 KSP / Hilt / Room 代码生成的本仓库验证流程

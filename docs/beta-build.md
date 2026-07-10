# Beta Build Runbook

## 目标

- 使用独立测试包名 `com.wzl.duskreader.tv.beta`.
- 使用固定测试签名, 保证测试版后续可覆盖升级.
- 启用 R8 与资源收缩, 避免未使用依赖和图标类进入测试包.
- 禁止提交 `keystore.properties`, `.jks`, `.keystore`.

## 首次配置

1. 创建本地 keystore 目录: `mkdir -p ~/.local/share/android-keystores`.
2. 生成测试签名: `keytool -genkeypair -v -keystore ~/.local/share/android-keystores/dusk-reader-tv-beta.jks -alias dusk-reader-tv-beta -keyalg RSA -keysize 2048 -validity 10000`.
3. 复制模板: `cp keystore.properties.example keystore.properties`.
4. 填写 `BETA_STORE_FILE`, `BETA_STORE_PASSWORD`, `BETA_KEY_ALIAS`, `BETA_KEY_PASSWORD`; PKCS12 默认让 `BETA_KEY_PASSWORD` 等于 `BETA_STORE_PASSWORD`.
5. 确认 `git status --short` 不显示密钥或真实签名配置.

## 打包

1. 运行单元测试: `./gradlew testDebugUnitTest`.
2. 构建测试包: `./gradlew assembleBeta`.
3. 分发 APK: `app/build/outputs/apk/beta/app-beta.apk`.
4. 下一版测试前递增 `versionCode`, 并更新 `versionNameSuffix`.

## 安装验证

1. 安装 APK: `adb install -r app/build/outputs/apk/beta/app-beta.apk`.
2. 确认包名: `adb shell pm list packages | grep com.wzl.duskreader.tv.beta`.
3. 确认应用名显示为 `暮阅 Beta`.

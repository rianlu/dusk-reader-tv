# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Ktor / Netty —— BlockHound 是 Netty 的可选 runtime 集成，不存在也没关系
-dontwarn reactor.blockhound.**
-dontwarn io.netty.util.internal.Hidden$NettyBlockHoundIntegration

# Ktor / Netty —— 以下都是 optional runtime 依赖（SSL native、log4j、slf4j、jetty-npn、JMX），
# 暮阅只用 Netty 裸 HTTP，用不到也不会在运行时加载
-dontwarn io.netty.internal.tcnative.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn org.slf4j.impl.**
-dontwarn java.lang.management.**
<img src="../image/leviathan_banner.png" alt="Leviathan">
<div align="center">

[![下载](https://img.shields.io/badge/releases-blue?label=%e4%b8%8b%e8%bd%bd&style=for-the-badge&colorA=19201a&colorB=298046)](https://gitlab.com/vospek/minecraft-dev/leviathan/-/releases)⠀
[![GitLab 构建](https://img.shields.io/gitlab/pipeline/vospek/minecraft-dev/leviathan/main?label=%e6%9e%84%e5%bb%ba&style=for-the-badge&colorA=19201a&colorB=298046)](https://gitlab.com/vospek/minecraft-dev/leviathan/-/pipelines)

**Leviathan** 是一个基于 [Paper](https://papermc.io/) 的分支，专为高自定义和高性能而设计，源自 [Leaf](https://github.com/Winds-Studio/Leaf)。
</div>

> [!WARNING]
> Leviathan 是一个面向性能的分支。在迁移到 Leviathan 之前，请务必**提前备份**。欢迎任何人贡献优化或报告问题来帮助我们改进。

[English](../../README.md) | **中文**

## 🐋 特点
- **基于 [Paper](https://papermc.io/)**，以获得更好的性能和灵活的 API
- **异步**寻路、生物生成和实体追踪
- **大量优化**融合自 [其他核心](#-致谢) 和我们自己的补丁
- **完全兼容** Spigot 和 Paper 插件
- **最新依赖**，保持所有依赖项为最新版本
- **允许用户名使用所有字符**，包括中文和其他字符
- **修复**一些 Minecraft 的 bug
- **模组协议**支持
- **更多自定义配置项**，源自 [Purpur](https://github.com/PurpurMC/Purpur) 的特性
- **线性区域文件格式**，节省磁盘空间
- **运维友好**，集成 [Pufferfish](https://github.com/pufferfish-gg/Pufferfish) 的 [Sentry](https://sentry.io/welcome/)，轻松详细追踪服务器的所有报错
- 以及更多...

## 📥 下载
在 [GitLab Releases](https://gitlab.com/vospek/minecraft-dev/leviathan/-/releases) 获取最新构建版本

## 📦 构建
构建用于分发的 Paperclip JAR：
```bash
./gradlew applyAllPatches && ./gradlew createPaperclipJar
```

## 📦 API
<details>
<summary>点击展开</summary>

Leviathan API 由 CI 流水线发布到 [GitLab Package Registry](https://gitlab.com/vospek/minecraft-dev/leviathan/-/packages)。

### Gradle
```kotlin
repositories {
  maven {
    url = uri("https://gitlab.com/api/v4/projects/vospek%2Fminecraft-dev%2Fleviathan/packages/maven")
    credentials(HttpHeaderCredentials::class) {
      name = "Private-Token"
      value = System.getenv("GITLAB_TOKEN")
    }
  }
}

dependencies {
    compileOnly("dev.vospek.leviathan:leviathan-api:26.2.local-SNAPSHOT")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}
```

### Maven
```xml
<repository>
    <id>gitlab</id>
    <url>https://gitlab.com/api/v4/projects/vospek%2Fminecraft-dev%2Fleviathan/packages/maven</url>
</repository>
```
```xml
<dependency>
    <groupId>dev.vospek.leviathan</groupId>
    <artifactId>leviathan-api</artifactId>
    <version>26.2.local-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```
</details>

## ⚖️ 许可证
Leviathan 根据其上游项目采用多种开源许可证授权。请参阅 [LICENSE.md](../../LICENSE.md) 了解完整的详细信息。

## 📜 致谢
感谢以下项目。Leviathan 包含了一些取自这些项目的补丁。<br>
如果没有这些优秀的项目，Leviathan 就不会变得如此出色。

- [Leaf](https://github.com/Winds-Studio/Leaf) (本项目所基于的 fork)
- [Gale](https://github.com/Dreeam-qwq/Gale) ([原始仓库](https://github.com/GaleMC/Gale))
- [Pufferfish](https://github.com/pufferfish-gg/Pufferfish)
- [Purpur](https://github.com/PurpurMC/Purpur)
- <details>
    <summary>🍴 展开查看 Leviathan 采用补丁的核心</summary>
    <p>
      • <a href="https://github.com/KeYiMC/KeYi">KeYi</a> (R.I.P.)
        <a href="https://github.com/MikuMC/KeYiBackup">(备份仓库)</a><br>
      • <a href="https://github.com/etil2jz/Mirai">Mirai</a><br>
      • <a href="https://github.com/Bloom-host/Petal">Petal</a><br>
      • <a href="https://github.com/fxmorin/carpet-fixes">Carpet Fixes</a><br>
      • <a href="https://github.com/Akarin-project/Akarin">Akarin</a><br>
      • <a href="https://github.com/Cryptite/Slice">Slice</a><br>
      • <a href="https://github.com/ProjectEdenGG/Parchment">Parchment</a><br>
      • <a href="https://github.com/LeavesMC/Leaves">Leaves</a><br>
      • <a href="https://github.com/KaiijuMC/Kaiiju">Kaiiju</a><br>
      • <a href="https://github.com/PlazmaMC/PlazmaBukkit">Plazma</a><br>
      • <a href="https://github.com/SparklyPower/SparklyPaper">SparklyPaper</a><br>
      • <a href="https://github.com/HaHaWTH/Polpot">Polpot</a><br>
      • <a href="https://github.com/plasmoapp/matter">Matter</a><br>
      • <a href="https://github.com/LuminolMC/Luminol">Luminol</a><br>
      • <a href="https://github.com/Gensokyo-Reimagined/Nitori">Nitori</a><br>
      • <a href="https://github.com/Tuinity/Moonrise">Moonrise</a> (在 1.21.1 期间)<br> 
      • <a href="https://github.com/Samsuik/Sakura">Sakura</a><br> 
    </p>
</details>

## 🔥 特别感谢
<table>
  <tr>
    <td colspan="2" align="center">
      <a href="https://www.yourkit.com/">
        <img src="https://www.yourkit.com/images/yklogo.png" alt="YourKit" width="300">
      </a>
      <p>YourKit 通过创新和智能的工具支持开源项目，用于监控和分析 Java 和 .NET 应用程序。YourKit 是 <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>、<a href="https://www.yourkit.com/dotnet-profiler/">YourKit .NET Profiler</a> 和 <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a> 的创造者。</p>
    </td>
  </tr>
</table>
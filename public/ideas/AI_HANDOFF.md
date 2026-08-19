# Leviathan — AI 交接文档

> 本文件为后续接手的 AI 提供完整上下文。配合根目录 `AGENTS.md`(工作区规则)与 `public/ideas/Phase0/`(开发规划)阅读。

## 1. 项目是什么

- **Leviathan**:基于 **Leaf**(Paper fork)的高性能 Minecraft 服务器核心的改名 fork,目标是长期自主开发(见 Phase 0/1 规划)。
- 上游仓库:`Winds-Studio/Leaf`,分支 `ver/26.2`;镜像 `git.yuemi.org/Repo-Mirror/Leaf`。
- 本项目仓库:`gitlab.com/vospek/minecraft-dev/leviathan`,默认分支 `main`,`MC_VERSION=26.2`(gradle.properties 的 `mcVersion`)。
- 构建产物:`leviathan-paperclip-26.2*.jar`;发布 jar 命名 `leviathan-26.2-<pipeline_iid>.jar`,GitLab Release tag `ver-26.2`。

## 2. 已完成状态

### 2.1 品牌化(核心工作)

Leaf → Leviathan 全量改名已完成并提交,包括:

- **全部 417 个 patch**(`leviathan-server/paper-patches/` 与 `leviathan-server/minecraft-patches/`)的 bulk 替换:包名 `dev.vospek.leviathan`、类名(`LeafConfig`→`LeviathanConfig` 等)、日志前缀、README。
- patch 中的 `-` 行/上下文行也被同步替换(bulk 品牌化产出的引用一致性是**正确且必要**的,与上游原始内容的失配会破坏文本应用)。
- 权限根常量:`LEAF_ROOT = "leaf"` → `LEVIATHAN_ROOT = "leviathan"`(`0007-Leviathan-Commands.patch` 定义 + `0012` 上下文行同步)。
- bStats 图表 id 品牌化(`leviathan_version`,见 `0003-Gale-metrics.patch`)。

**有意保留(不要"顺手修"):**

- `ca.spottedleaf.moonrise` / `ca.spottedleaf.*`(上游库包名)
- `leaf$` mixin 方法名、`leaf$availableGoals` 等
- `leaf.yml` / `leaf_config` 迁移逻辑(`LeviathanConfig.java`,为了兼容旧配置)
- `LeavesMC` 标记、上游项目链接、`maven.leafmc.one` 仓库、根 `build.gradle.kts` 的 `leafMavenPublicUrl` 变量名
- `public/` 下 banner/logo 图片内容(未替换)
- KDTree 注释中的 "leaf node"(算法术语)

### 2.2 CI/CD(GitLab,已跑通)

`.gitlab-ci.yml` 三个 job:

- **build**:`eclipse-temurin:25-jdk` 镜像,`applyAllPatches` + `createPaperclipJar`,产物 `leviathan-server/build/libs/`。before_script 需 `apt-get install git ca-certificates`(镜像无 git)。
- **publish-api**(手动):`./gradlew :leviathan-api:publish`。
- **release**(手动):alpine 镜像,上传 jar 到 Generic Packages,`release-cli` 创建 Release,`prepareRelease.sh` 生成 `release_notes.md`。

**已经修过的坑(不要再踩):**

1. patch 应用失败报 `invalid object`/`Repository lacks necessary blobs` → 不是 hash 问题,而是**文本应用失配**触发了 3-way merge。修复方式是让 patch 内容与上一 patch 应用后的实际内容一致(例:`0107` 第 298 行 `"PAPER OR LEAF"` 必须与 `0002-Rebrand` 应用后的 `"PAPER OR LEVIATHAN"` 一致)。patch 的 index hash 不更新没关系,文本能干净应用就不校验 hash。
2. `gradlew`/`scripts/*.sh` 在 Windows 提交丢失可执行位 → `git update-index --chmod=+x`。
3. release job 无 tag 时 `git describe --tags --abbrev=0` 直接 fatal → 需 `2>/dev/null || true` + fallback;且需要完整历史(`GIT_DEPTH: 0`)。
4. release-cli 安装必须用官方 URL:`https://gitlab.com/api/v4/projects/gitlab-org%2Frelease-cli/packages/generic/release-cli/latest/release-cli-linux-amd64`(releases 下载端点会返回 404 文本)。
5. release-cli 0.24.0 用 `--description release_notes.md`(文件路径,无 `--description-file` 参数)。
6. 凭据:`git push` 使用 Windows Git Credential Manager(`credential.helper=manager`);token 失效时需用户本地重新登录。GitHub 凭据是已 suspend 的账号,不要用 `git clone` 拉上游,用 webfetch 抓 raw 文件。

### 2.3 其他

- README(`README.md` + `public/readme/README_CN.md`)含 📊 bStats 节,链接 `https://bstats.org/plugin/server-implementation/Leviathan`(大小写敏感)。签名图 SVG 暂无数据(500),等数据出现后再加徽章。
- Phase 0 开发规划已拆分为 `public/ideas/Phase0/`(00-overview ~ 11-safety-and-rollback,共 12 篇)。

## 3. 开发规划(Phase 0 摘要)

Phase 0 目标:**建立测量和控制系统,不引入性能变量**。禁止提前实现:ZGC 参数、Linear V2、Zstd、mmap、RocksDB、DAB、Hopper Sleep、SIMD、Async Chunk、Region Tick、Plugin Async、Purpur 玩法改动。

计划落地的条目:

- P0-001~003:源码/构建基线、LV-0001~0315 Patch 编号体系、`levidathan/{bootstrap,config,observability,diagnostics,benchmark,...}` 模块目录
- P0-004/005:启动时 Runtime Detector + 硬件能力探测(`Capabilities.cpu().avx2()` 风格,结果进 `dev.vospek.leviathan.*`)
- P0-006~008:`config/leviathan.yml` 主配置 + Feature Flag(disabled/safe/enabled/experimental)+ 启动校验(类型/范围/冲突/依赖,错误要提前报告而不是中途崩溃)
- P0-009/010:统一日志命名空间 `[Leviathan/...]`,高频统计进 `logs/leviathan/*.log` 而非控制台
- P0-011~015:MetricRegistry(Counter/Gauge/Histogram/Timer/Rate)、Tick(TPS/MSPT/Overrun/P50~MAX)、CPU、Memory(Heap/Direct/GC/Allocation)、Thread 指标
- P0-016/017:`/leviathan` 命令框架(status/stats/metrics/...),未来 patch 统计都挂到 stats
- P0-018~020:Baseline Benchmark + 6 个压力场景(A 空服/B 生存/C 高实体/D 跑图/E 高网络/F 区块保存)+ 回归门槛机制(MSPT↑/TPS↓ → FAIL,Memory/Startup → REVIEW,Crash/Data Loss → FAIL)
- P0-021~023:单元测试(Config/Metrics/Runtime Detector/Feature Flags/Command Tree)、服务器级集成测试、Crash/Recovery 测试
- 0-I:CI 加 Unit Test / Integration Test / Static Analysis / Patch Validation
- 0-J:Patch Registry(YAML 化,字段:patch/name/tier/phase/status/dependencies/tests/benchmark)
- 0-K:Safe Mode(`leviathan.runtime.mode=safe`,只加载基础设施)、Feature Disable、Automatic Fallback

Phase 0 验收:能编译、能启动、vanilla 行为无明显改变、配置/Feature Flag/status/stats 正常、指标可见、Baseline 可重复、CI 自动构建、Patch Registry 可追踪、配置错误不模糊崩溃。

## 4. 待办/建议下一步

1. 开始 Phase 0 实施:建议顺序 P0-004/005(Runtime Detector)→ P0-006/007/008(Config + Feature Flag)→ P0-009/010(Logger)→ P0-011~015(Metrics)→ P0-016/017(命令)。
2. `publish-api` job 尚未验证(手动,需 API 仓库配置)。
3. bStats 签名图徽章待数据出现后补。
4. banner/logo 图片内容替换(如需)。
5. 决策:构建依赖 `cn.dreeam:quantumleaper:1.0.0-SNAPSHOT` 是否保留。

## 5. 工作区规则要点(详见 AGENTS.md)

- 只修改 applied Java 源码(`leaf-server` 即 `leviathan-server` 下的 applied 源、applied Paper API/server、Leaf 自己的 `dev.vospek.leviathan` 源码);其他文件只读。
- **不创建/修改/删除/重新生成 patch 文件**、不跑 applyAllPatches/rebuild 等任务——除非用户明确授权(本项目的品牌化修复即属用户授权的例外)。
- 不跑 Gradle 构建/测试/benchmark/服务器启动,验证由仓库主人工做;本地只做只读检查(git status/diff、读源码、grep)。
- 不改写 git 历史、不 commit 除非用户要求。
- CodeGraph 可用于跨文件导航,但以实际 checkout 源码为准。
- 性能敏感路径避免分配/装箱/stream/捕获 lambda。
- 完成报告需列出:改动的 applied 源文件、行为变化、线程/兼容性/性能考量、需人工验证的点。

## 6. 命令速查

```bash
# 推送(凭据经 GCM,失效时让用户本地跑一次 git push 重新登录)
git push origin main

# 品牌化扫描残留(区分有意保留项)
rg -n "Leaf|LEAF|leaf" leviathan-server/paper-patches leviathan-server/minecraft-patches leviathan-server/src

# 检查某 patch 是否可能与其他 patch 文本失配(改 patch 时)
rg -n "context行内容" leviathan-server/paper-patches leviathan-server/minecraft-patches
```

> 注意:`\bLeaf\b` 替换是大小写敏感的,历史教训是全大写 `LEAF`、小写驼峰 `leafVersion` 等变体需要单独处理。

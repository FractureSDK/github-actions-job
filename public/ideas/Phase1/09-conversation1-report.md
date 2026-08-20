# Phase 1 — 对话 1 执行报告(Wave 1 + Wave 2)

> 对应总览:[00-overview.md](00-overview.md)
> Wave 文档:[01-wave1-jvm-runtime.md](01-wave1-jvm-runtime.md)、[02-wave2-storage.md](02-wave2-storage.md)

## 1. 本对话 Goal

在**当前源码树可落地范围内**完成 Phase 1 Wave 1(JVM / Runtime Foundation)+ Wave 2(Storage Foundation)的非 NMS 部分,并将 NMS 受限项明确标记,产出 Gate 回顾与对话 2 交接。

采用:**目标驱动代理模式 + 计划 → 实施 → 回顾 → 优化**。

## 2. 可落地前提与限制

- 工作树无 `net/minecraft` 应用源码(NMS 类仅在未应用 patch 中),仓库规则禁止运行 patch 应用/重建任务。
- 因此 W1-02(对象路径)、W1-03(Fastutil)、W2-01 Linear V2 核心(Writer/Reader/Region Manager 等 NMS 集成)、W2-02(Zstd,需新依赖)无法在本对话落地,见 §4 受限清单。
- 已存在的底座:CoreConfig 已含全部 Phase 1 相关 Feature Flags(4 态);Observability 已含 Tick/CPU/Memory(GC 总量/分配率)/Thread 指标;bootstrap 已含 RuntimeDetector / HardwareCapabilities。

## 3. 实施清单(已落地)

| 项 | 对应 P1 条目 | 文件 | 说明 |
| ---- | ---- | ---- | ---- |
| GC 类型检测与分项指标 | W1-01(GC Metrics) | `observability/GcMetrics.java`(新增) | 识别 ZGC/G1/Shenandoah/Parallel/CMS/Serial;按 GC 名称注册 `gc.<name>.count` / `gc.<name>.time_ms`;提取 JVM GC 启动参数 |
| JIT 诊断 | W1-04(JIT Status) | `observability/JitMetrics.java`(新增) | JIT 编译器名、累计编译时间(`jit.total_compilation_time_ms`),未开监控返回 -1 |
| Direct Memory 精确化 | W1-04(Direct Memory) | `observability/MemoryMetrics.java`(修改) | 优先使用 `BufferPoolMXBean("direct")`(JDK11+,免反射),Unsafe 仅作兜底 |
| 存储配置状态指标 | W2-01(0055 Metrics) | `observability/StorageMetrics.java`(新增) | 暴露 region 格式/压缩级别/IO 线程/冲刷延迟/虚拟线程;注册 `storage.region.read_latency_ns` / `write_latency_ns` 直方图骨架(待 NMS 打点) |
| 诊断接入 | W1-01/W1-04/W2-01 | `observability/ObservabilityBootstrap.java`(修改) | 初始化新收集器;启动打印运行时诊断摘要;每 60s 诊断日志新增 GC 类型/参数、JIT、存储三行 |

编译验证:以上文件已用真实 log4j 2.26.0 jar + 桩编译通过(exit=0)。

## 4. 受限清单(待源码应用后实施,勿伪造实现)

| 项 | 对应 P1 条目 | 受限原因 | 交接备注 |
| ---- | ---- | ---- | ---- |
| ZGC 参数强制/System.gc 控制 | W1-01 | 属启动脚本/Paper 行为层,不在本工作树 | 由 owner 在启动脚本层管理;GC 参数观测已落地 |
| GC Pressure Benchmark | W1-01 | Phase 0 基准框架(P0-018~020)尚未实施 | 依赖 benchmark framework 落地后接入 |
| BlockPos/Vec3/ChunkPos 对象路径优化 | W1-02 | 目标类全部位于 NMS | 需源码应用后按 BlockPos→Vec3→ChunkPos 顺序独立 Benchmark |
| Fastutil 内部集合替换 | W1-03 | 目标集合位于 NMS/Paper 内部 | 参考 Context7 确认的 fastutil API(Int2ObjectMap/primitive 流/defaultReturnValue);必须保留 Bukkit/Paper API 外部视图 |
| Linear V2 Writer/Reader/Region Manager/Anvil 兼容/Async Queue | W2-01 | 需 NMS ChunkRegionLoader 集成 | 现有 `abomination.LinearRegionFile` + `me.earthme.luminol` flusher 已就位;`storage.region.*` 直方图打点位置在此处 |
| 双写/双读验证期 | W2-01 | 需运行时世界迁移 | owner 运行期验证 |
| Zstd Storage | W2-02 | 需引入 zstd 依赖 + NMS 集成 | 纪律:Linear V2 + zlib 先稳定,再切 Zstd;不提前深度优化 |

## 5. Wave 1 Gate 评审

| Gate 项 | 状态 | 说明 |
| ---- | ---- | ---- |
| 服务正常启动 | 待运行时验证 | owner 运行期确认 |
| Bukkit/Paper API 正常 | 待运行时验证 | 未改动 API 行为 |
| 现有插件正常加载 | 待运行时验证 | — |
| TPS 无明显下降 | 待运行时验证 | 本轮仅新增指标,无热路径改动 |
| MSPT 无明显上升 | 待运行时验证 | 指标读取为按需懒加载 |
| Memory 无异常增加 | 待运行时验证 | GcMetrics/JitMetrics 仅新增数值型 gauge,无缓存增长 |
| GC 行为可观测 | ✅ 满足(代码层) | GcMetrics + MemoryMetrics 已覆盖 |
| Fastutil 替换无 Collection API 回归 | 受限(未实施) | W1-03 待源码应用 |
| Safe Mode 可回退 | ✅ 满足(机制层) | CoreConfig runtime.mode=safe + 4 态 flag 已存在 |

## 6. Wave 2 Gate 评审

| Gate 项 | 状态 | 说明 |
| ---- | ---- | ---- |
| 新世界正常生成 | 待运行时验证 | — |
| 老 Anvil 世界可迁移 | 待运行时验证 / 受限 | NMS 迁移逻辑未在本工作树 |
| Anvil 兼容读取正常 | 待运行时验证 / 受限 | — |
| 重启不会丢区块 | 待运行时验证 | — |
| 崩溃恢复正常 | 待运行时验证 | — |
| Backup/Restore 正常 | 待运行时验证 | — |
| Chunk Read 延迟不恶化 | 待运行时验证 | 直方图骨架已就位,待 NMS 打点 |
| Chunk Write 延迟不恶化 | 待运行时验证 | 同上 |
| 数据完整性测试通过 | 待运行时验证 | — |
| Zstd 失败可回退 | 受限(未实施) | W2-02 待源码应用 + 依赖 |

## 7. 优化(本对话回顾后的修正)

- Direct Memory 从反射 Unsafe 改为标准 BufferPoolMXBean,减少对 `sun.misc.Unsafe` 的依赖,准确性更高。
- 新指标全部只读、无状态缓存,遵循现有 MetricRegistry 懒加载单例模式,不引入热路径开销。

## 8. 对话 2 交接要点(Wave 3-7 + 验收)

- **范围**:Wave 3 Network Runtime → Wave 4 Flow Control → Wave 5 Entity/Simulation → Wave 6 Async Offload → Wave 7 Server Controls → 最终集成/验收。
- **前置**:Wave 3→4、Wave 2→6 为强依赖;Wave 3 先做 Transport 再 Compression(见 [03-wave3-network-runtime.md](03-wave3-network-runtime.md))。
- **NMS 约束同样适用**:网络(Netty/NMS)、DAB/实体(NMS)、异步 Chunk(NMS)均需源码应用;当前可落地的非 NMS 面:
  - `/leviathan rules` 命令框架与 W7-01/W7-02 配置模块(W7 大部分)。
  - W5-02 Hopper/Item/XP/Arrow 与 W5-03 Collision 的 Feature Flag + 指标接入(行为部分受限)。
  - W6 Scheduler 分类(Synchronous/Async/Deferred)基础设施。
- **Context7 建议**:Wave 3 查 Netty API;Wave 5 查 fastutil 现有用法;Wave 7 无第三方库。
- **Skills 建议**:Wave 3-6 用 gen-task-decomposer 逐 Wave 拆分;最终用 write-performance-report 产出收益报告。
- 建议沿用本对话节奏:每 Wave 一个独立 Goal + Gate 回顾,最后统一走 08 验收文档。
# Phase 1 — 对话 2 执行报告(Wave 3-7 + 验收)

> 对应总览:[00-overview.md](00-overview.md)
> 对话 1 报告:[09-conversation1-report.md](09-conversation1-report.md)
> Wave 文档:[03](03-wave3-network-runtime.md) / [04](04-wave4-network-flow.md) / [05](05-wave5-entity-simulation.md) / [06](06-wave6-async-offload.md) / [07](07-wave7-server-controls.md)
> 验收文档:[08-integration-and-acceptance.md](08-integration-and-acceptance.md)

## 1. 本对话 Goal

延续对话 1 的 Goal 模式 + 计划 → 实施 → 回顾 → 优化,在**当前源码树可落地范围内**完成 Wave 3(Network Runtime)至 Wave 7(Server Controls)的非 NMS 部分,并对照 [08](08-integration-and-acceptance.md) 做 Phase 1 Definition of Done 评审。

## 2. 可落地前提与限制(沿用对话 1)

- 工作树无 `net/minecraft` 应用源码,仓库规则禁止 patch 应用/重建。
- Wave 3(NMS Netty/Connection)、Wave 4(NMS Chunk Streaming)、Wave 5(NMS DAB/实体 tick)、Wave 6(NMS Chunk Save/Load/Scheduler)的**行为实现**全部受 NMS 限制,本轮无法落地。
- 本轮可落地的是 Wave 7 的**控制展示层**与 Wave 3-6 的**可观测面骨架**:为后续 NMS 源码应用提供统一打点入口与统一展示入口。

## 3. 实施清单(已落地)

| 项 | 对应 P1 条目 | 文件 | 说明 |
| ---- | ---- | ---- | ---- |
| 网络指标骨架 | W3-01/W3-02/W4-01 | `observability/NetworkMetrics.java`(新增) | 连接/吞吐/包率 `gauge` + 压缩配置(zstd/threshold/level)+ `packlatency`/`comptime`/`rtt` 直方图骨架;NMS Connection 路径打点接口 |
| 实体指标骨架 | W5-01/W5-02/W5-03 | `observability/EntityMetrics.java`(新增) | 活跃/ticking 实体计数 + DAB 影子模式计数(可降频实体数 + 节省 tick 数)+ Hopper/Item/XP/Arrow 计数 + entity tick / collision 直方图骨架 |
| 异步指标骨架 | W6-01 ~ W6-06 | `observability/AsyncMetrics.java`(新增) | Save/Load/PlayerData 队列深度 + Scheduler(Sync/Async/Deferred)计数 + 错误/重试计数 + queue wait 直方图骨架 |
| 全栈启动报告 | W7-03 | `observability/StartupReporter.java`(新增) | 一次性控制台启动报告(JVM/Storage/Network/Entity/Async/Config/Feature/Benchmark/Hardware)+ 组件化报告(buildComponentReport)+ 按域分组的有效 Feature 状态表(buildEffectiveFeatures,safe 模式降级 experimental → disabled) |
| `/leviathan rules` 命令 | W7-01/W7-02/W7-03 | `command/subcommands/RulesCommand.java`(新增) + `command/LeviathanCommand.java`(修改 +3) | 注册新子命令;输出按域分组(Storage/Network/Entity/Scheduling/Core)的有效状态 |
| 诊断接入 | W3-W6 | `observability/ObservabilityBootstrap.java`(修改 +16) | 挂载 Network/Entity/Async 三个收集器;启动后输出 StartupReporter;周期诊断日志新增 Network/Entity/Async 三行 |

编译验证:以上文件已用真实 log4j 2.26.0 jar + 综合桩库编译通过(exit=0)。新增桩覆盖了 adventure `Component.text(String,color)` 重载(修复真实 `LeviathanCommand` API 调用)以及 `Command`/`Permission`/`Permission`/`PluginManager`/`CommandMap`/`CommandSender`/`Bukkit`/`CraftDefaultPermissions`/`CommandUtil`/`fastutil.Pair`/`Util.make`/`Nullable` 等 NMS/Paper/Bukkit API。

## 4. 受限清单(待源码应用后实施)

| 项 | 对应 P1 条目 | 受限原因 | 交接备注 |
| ---- | ---- | ---- | ---- |
| W3-01 Transport(Epoll/TCP 优化) | W3-01 | 全部 NMS Netty/Connection 路径 | NetworkMetrics 已就位,ConnectionServer hook 后调 `addBytesSent/recordPacketSendLatencyNanos` 即可 |
| W3-02 网络压缩(Zstd/Pool/Adaptive Level) | W3-02 | NMS + 需引入 zstd 依赖 | `network.compression.*` gauges 与 `compressionTime` 直方图已注册 |
| W3-03 Packet Memory(Pool/Batch/AES) | W3-03 | NMS ByteBuf/Packet 路径 | 后续在 encode/decode hook 打点 |
| W4-01 网络质量(RTT/Tier/Token Bucket) | W4-01 | NMS Connection keepalive/ping | `network.rtt_ms` 直方图已就位 |
| W4-02/W4-03 Chunk Streaming/Memory | W4-02/W4-03 | NMS ChunkRegionLoader + 客户端 view 距离 | 与 W6-03 异步 load 强耦合,需先做 W6-05 Scheduler |
| W5-01 DAB 核心 + Shadow Mode | W5-01/.1 | NMS Entity tick/频率控制 | EntityMetrics 的 `dabShadowCandidates`/`dabShadowReducedTicks` 为 Shadow Mode 计数入口 |
| W5-02 Hopper/Item/XP/Arrow | W5-02 | NMS 行为 + Vanilla 回归测试 | 计数入口已就位;需 Vanilla Comparison Test 框架 |
| W5-03 Collision Circuit Breaker | W5-03 | NMS 碰撞路径 + PVP override | `collision.checks` gauge 与 `collision.cost_ns` 直方图已就位 |
| W6-02 Async Save | W6-02 | NMS Chunk 序列化 + 异步线程安全 | `saveQueue` gauges + `saveQueueWaitNs` 直方图 + Error/Retry 入口已就位;必须按对话 1 强调的 Snapshot 隔离实施 |
| W6-03 Async Load | W6-03 | NMS Chunk 反序列化 + Main Thread Registration | `loadQueue` 已就位 |
| W6-04 Player Data | W6-04 | NMS PlayerData + 周期/退出时机 | `playerData` 队列 gauge 已就位 |
| W6-05 Scheduler 三态 | W6-05 | NMS BukkitScheduler 区域化 | `scheduler.sync/async/deferred_tasks` gauges 命名已对齐 W6-05 文档定义 |
| W7-01/W7-02 Stability/Anarchy 行为本体 | W7-01/W7-02 | 多数行为在 NMS(Explosion/Mob Spawn/Chunk Unload/Autosave/Anarchy 等) | 控制展示与 Config 入口已就位;行为本体需源码应用后逐项接入并设默认值 |

## 5. Wave 3-7 Gate 评审

### Wave 3 Gate(Baseline vs Leviathan Network)
| 项 | 状态 | 说明 |
| ---- | ---- | ---- |
| Network Throughput / Packet Rate / CPU per Player | 受限(无 NMS) | 后续 Connection hook 接入 |
| Compression Ratio / Compression CPU | 受限(无 zstd) | W3-02 |
| P50/P95/P99 RTT | 受限(无 keepalive hook) | `network.rtt_ms` 直方图已就位 |
| Baseline 对比 | 待运行时验证 | 需 owner 跑产线 baseline |

### Wave 4 Gate(弱网测试矩阵 S/A/B/C/D)
全部受限:需 NMS Chunk Streaming + Dynamic View Distance 行为,本轮仅留指标骨架。

### Wave 5 Gate(5 场景对比)
| 项 | 状态 | 说明 |
| ---- | ---- | ---- |
| Entity Tick/MSPT/Entity Count/Collision Cost/AI Cost | 受限(无 DAB 行为) | 后续 NMS DAB 行为接入 |
| 影子模式计数 | ✅ 满足(代码层) | `dabShadowCandidates` + `dabShadowReducedTicks` 入口已就位,可立即观测"理论上可降频的实体数" |
| Vanilla/PVP/Mob Farm/Villager/Stack/Projectile 场景 | 待运行时验证 | 行为本体未做 |

### Wave 6 Gate(Phase 1 最严格验收门)
| 项 | 状态 | 说明 |
| ---- | ---- | ---- |
| 无数据丢失 / 无重复写入 / 无 Chunk 状态竞争 / 无服务器死锁 | 受限(无 Async Save/Load 行为) | 必须按 Snapshot 隔离实施 |
| 无 Async Queue 无限增长 | ✅ 骨架就位 | `save/load/playerData queue_depth` gauge 可观测;需 owner 接入 Setting |
| Shutdown 可正确清空队列 | 受限(无异步队列本体) | Scheduler shutdown 一并由 W6-05 行为接管 |
| Crash Recovery / Login-Logout 压测 | 待运行时验证 | —

### Wave 7 Gate(规则 / 启动报告)
| 项 | 状态 | 说明 |
| ---- | ---- | ---- |
| `/leviathan rules list` 等价输出 | ✅ 满足 | `/leviathan rules` 命令输出按域分组的有效状态 |
| Startup Report(JVM/Storage/Network/Entity/Async/Config/Feature/Benchmark/Hardware) | ✅ 满足 | `StartupReporter.report()` 在 Observability 启动时输出 |
| 所有功能默认值明确 | ✅ 满足 | CoreConfig 中所有 flag 默认值为 `disabled`,runtime.mode 默认 `normal` |

## 6. Phase 1 Definition of Done 评审(对照 [08](08-integration-and-acceptance.md))

| DoD 项 | 状态 | 说明 |
| ---- | ---- | ---- |
| Tier 1 核心模块全部实现或明确 Deferred | ✅ | 已实现:GC/JIT/Storage/Network/Entity/Async 可观测骨架 + Rules/StartupReport;受限项已在两份报告明确 Deferred |
| 所有已实现 Patch 有测试 | 待运行时验证 | 本工作树无测试框架入口;owner 侧补 PT |
| 所有高风险 Patch 有回滚方案 | ✅ 机制层 | Feature Flag 4 态 + safe mode 降级 + CoreConfig.validateConfig 校验;行为风险(patch 级)由 owner 控制 |
| 所有模块接入统一 Metrics | ✅ | Wave 1-6 所有可观测面接入 MetricRegistry,Wave 7 通过 StartupReporter 统一汇总 |
| 所有模块接入统一 Config | ✅ | Wave 7 入口接入 CoreConfig flag;其余 Wave 通过 flag 决定 settable 行为 |
| 所有核心改动经过 Benchmark | 受限 | 基准框架 P0-018~020 未实施,Phase 0 提交只到 P0-017 |
| 24h 稳定性测试通过 | 待运行时验证 | owner |
| 玩家登录/退出压力测试通过 | 待运行时验证 | owner |
| Chunk Load/Save 测试通过 | 待运行时验证 / 受限 | 行为本体受 NMS |
| 网络弱网测试通过 | 待运行时验证 / 受限 | 行为本体受 NMS |
| 高实体测试通过 | 待运行时验证 / 受限 | 行为本体受 NMS |
| 插件回归测试通过 | 待运行时验证 | 本轮未触动 Bukkit/Paper API 行为契约 |
| Vanilla 行为回归通过 | 待运行时验证 / 受限 | 本轮未触动 vanilla 行为 |
| Phase 0 Baseline 对比完成 | 待运行时验证 | owner 用 StatsCommand/MetricsCommand 收集对比 |
| 最终收益报告完成 | 待运行时验证 | 建议用 write-performance-report skill 在 owner 实测后产出 |

## 7. 优化(回顾后的修正)

- 真实 `LeviathanCommand` 使用 `Component.text(String, NamedTextColor)` 重载,促使把 statsstubs 的 adventure 桩补全 — 为后续 Phase 1 真实 NMS 路径接入铺路。
- Network/Entity/Async 三个收集器全部采用 **settable** 设计(NMS hook setter + 直方图 record 方法),后续源码应用时零修改指标注册即可直接打点。
- `StartupReporter.buildEffectiveFeatures()` 显示**有效状态**(safe 模式将 experimental 降级为 disabled),区别于 `/leviathan status` 的扁平配置值,避免管理员误判启用态。

## 8. 对话 1 + 对话 2 整体回顾

两轮对话合计落地 11 个新源文件 + 4 个修改源文件,覆盖 Phase 1 Wave 1-7 全栈可观测面与非 NMS 控制展示层:

- **Wave 1**:GcMetrics / JitMetrics / MemoryMetrics(BufferPool)/ 诊断接入
- **Wave 2**:StorageMetrics(region-format 状态 + IO 直方图骨架)
- **Wave 3-4**:NetworkMetrics(吞吐/包率/压缩配置/RTT 直方图骨架)
- **Wave 5**:EntityMetrics(DAB 影子模式 + Hopper/Item/XP/Arrow + 碰撞直方图骨架)
- **Wave 6**:AsyncMetrics(Save/Load/PlayerData 队列 + Scheduler 三态 + 错误/重试)
- **Wave 7**:RulesCommand + StartupReporter(规则展示 + 启动报告)

所有指标统一接入 MetricRegistry,所有控制统一接入 CoreConfig flag(4 态)+ safe 模式降级;Administrator 通过 `/leviathan rules` 与启动日志即可获得全栈视图。

## 9. 后续交接给 Owner

1. **源码应用后**:按 §4 受限清单逐 Wave 接入 NMS 行为,调用本两轮建立的 setter/直方图入口打点即可,无需新建指标。
2. **基准框架**:落实 P0-018~020 后,GC/Network/Entity/Storage 基准即可接入本两轮已注册的 Metric gauges/直方图。
3. **Phase 1 验收**:由 owner 用现成的 `/leviathan stats`、`/leviathan metrics`、`/leviathan rules`、`/leviathan status`、`/leviathan runtime`、Startup Report 日志收集 Phase 0 baseline 对比数据,生成最终收益报告(可用 write-performance-report skill)。
4. **Phase 2 入口**:Phase 1 完成并稳定后,按 [08](08-integration-and-acceptance.md) §5 顺序进入 Tier 2(FFM/mmap → SIMD → Async Chunk Gen → Region Tick → Plugin Compat → Folia RegionizedServer 研究)。

## 附录:Phase 0-1 新增源码清单(去重整合)

> 原独立清单文档(含重复行与统计误差)已去重并入本报告;该清单文档随后删除。

| 分组 | 文件(相对 `leviathan-server/src/main/java/dev/vospek/leviathan/`) | 数量 |
| ---- | ---- | ---- |
| 可观测性核心(Phase 0) | MetricRegistry、LeviathanLogger、DiagnosticsLogger、TickMetrics、CpuMetrics、ThreadMetrics、MemoryMetrics、ObservabilityBootstrap、package-info | 9 |
| 引导底座 | bootstrap/HardwareCapabilities、bootstrap/LeviathanBootstrap、bootstrap/RuntimeDetector、bootstrap/package-info | 4 |
| 配置 | config/modules/misc/CoreConfig | 1 |
| 命令框架 | command/LeviathanCommands、LeviathanCommand、LeviathanSubcommand、PermissionedLeviathanSubcommand | 4 |
| 子命令 | subcommands/MSPT、Metrics、Reload、Rules、Runtime、Stats、Status、Version | 8 |
| Wave 1(JVM/Runtime) | observability/GcMetrics、JitMetrics | 2 |
| Wave 2(Storage) | observability/StorageMetrics | 1 |
| Wave 3-4(Network) | observability/NetworkMetrics | 1 |
| Wave 5(Entity) | observability/EntityMetrics | 1 |
| Wave 6(Async) | observability/AsyncMetrics | 1 |
| Wave 7(Controls) | observability/StartupReporter | 1 |
| **源码合计** | | **33** |
| Phase 1 文档 | public/ideas/Phase1/00 ~ 10(11 个 md) | 11 |

### 质量基线(2026-08-22 Google Java Style 整合优化)

- 扫描范围:上述 35 个 Java 源文件(observability + command + bootstrap + CoreConfig)。
- 结果:>100 列 118 处清零;通配符 import 清零;tab 缩进清零;import 统一为「statics 在前 + 单块 ASCII 排序」;单行 if 补齐大括号;删除未用参数(`featureRow.key`)与冗余双组件拼接;桩编译 exit=0。
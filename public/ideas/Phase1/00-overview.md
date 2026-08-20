# Project Leviathan — Phase 1 总览

> 本目录由 [P1.md](../P1.md) 拆分为按 Wave 组织的具体实施文件。

## 1. Phase 1 定位

**阶段名称：** Phase 1 — Core Performance Foundation

**阶段目标：**

> 在 Phase 0 已完成的基础设施之上，开始对 Leaf 核心进行第一轮正式性能改造，优先落地成熟、可回滚、可量化验证的 Tier 1 Patch。

Phase 1 不追求一次完成所有 173 个 Tier 1 Patch。

本阶段采用：

```text
Phase 0
基础设施 / Baseline
        ↓
Phase 1
核心性能改造
        ↓
Phase 1.x
性能回归 / 参数调整
        ↓
Phase 1 验收
        ↓
Phase 2
高风险架构改造
```

---

## 2. Phase 1 核心原则

### 2.1 一次只改变一个主要变量

严禁：

```text
ZGC
+
Linear V2
+
Zstd
+
DAB
+
Async Chunk
```

同时进入生产测试。

必须：

```text
Baseline
 ↓
一个模块
 ↓
Benchmark
 ↓
Regression
 ↓
记录结果
 ↓
继续下一个模块
```

这样 Phase 0 建立的基线才真正有价值。

### 2.2 所有 Patch 必须可关闭

每个性能 Patch 都必须接入 Phase 0 Feature Flag。

例如：

```yaml
features:
  zgc-tuning: true
  fastutil: true
  linear-storage: false
  storage-zstd: false
  dab: false
  async-chunk-save: false
```

出现问题时：

```text
禁用 Feature
    ↓
重启
    ↓
恢复基线行为
```

### 2.3 Patch 不允许跨层偷偷修改

一个 Patch 必须拥有明确边界。

例如：

```text
LV-0001 ZGC
→ JVM / Runtime

LV-0046 Linear V2
→ Storage

LV-0091 Epoll
→ Network

LV-0161 DAB
→ Entity

LV-0237 Async Chunk Save
→ Scheduler / Storage
```

不得为了实现某 Patch 顺便修改其他模块的行为。

需要跨模块时必须声明依赖。

---

## 3. Phase 1 总体结构

Phase 1 拆成 7 个实施 Wave：

```text
Wave 1
JVM / Runtime Foundation

Wave 2
Storage Foundation

Wave 3
Network Runtime

Wave 4
Network Flow Control

Wave 5
Entity / Simulation

Wave 6
Async Offload

Wave 7
Server Controls / Final Integration
```

依赖关系：

```text
Wave 1
   ↓
Wave 2
   ↓
Wave 3
   ↓
Wave 4
   ↓
Wave 5
   ↓
Wave 6
   ↓
Wave 7
```

其中 Wave 3 → Wave 4、Wave 2 → Wave 6 为强依赖。

---

## 4. 版本策略

不要把整个阶段最终只打一个版本。

采用：

```text
Leviathan 1.0
↓
Phase 1 Wave 1
Leviathan 1.1
↓
Wave 2
Leviathan 1.2
↓
Wave 3
Leviathan 1.3
↓
Wave 4
Leviathan 1.4
↓
Wave 5
Leviathan 1.5
↓
Wave 6
Leviathan 1.6
↓
Wave 7
Leviathan 1.7
```

最后：

```text
Leviathan Phase 1 Release
```

---

## 5. 子文档索引

| 文档 | 内容 | 对应 v2 Patch |
| ---- | ---- | ---- |
| [01-wave1-jvm-runtime.md](01-wave1-jvm-runtime.md) | Wave 1 JVM / Runtime Foundation（ZGC、对象路径、Fastutil、Runtime Diagnostics）+ Gate | 0001-0010, 0013-0020, 0031-0038, 0045 |
| [02-wave2-storage.md](02-wave2-storage.md) | Wave 2 Storage Foundation（Linear V2、Zstd Storage）+ Gate | 0046-0065 |
| [03-wave3-network-runtime.md](03-wave3-network-runtime.md) | Wave 3 Network Runtime（Transport、Compression、Packet Memory）+ Gate | 0091-0092, 0094-0108, 0116-0118, 0124, 0129, 0130 |
| [04-wave4-network-flow.md](04-wave4-network-flow.md) | Wave 4 Network Flow Control（网络质量、Chunk Streaming、Lazy Sending）+ Gate | 0131-0160 |
| [05-wave5-entity-simulation.md](05-wave5-entity-simulation.md) | Wave 5 Entity / Simulation（DAB、Hopper/Item/XP/Arrow、Collision）+ Gate | 0161-0175, 0191-0199, 0201-0203 |
| [06-wave6-async-offload.md](06-wave6-async-offload.md) | Wave 6 Async Offload（Observer、Async Save/Load、Player Data、Scheduler、监控）+ Gate | 0236-0250 |
| [07-wave7-server-controls.md](07-wave7-server-controls.md) | Wave 7 Server Controls（Stability、Anarchy、Final Runtime Report） | 0281-0290, 0304-0313, 0314, 0315 |
| [08-integration-and-acceptance.md](08-integration-and-acceptance.md) | 最终集成测试、性能验收矩阵、Definition of Done、目标架构、Phase 2 入口 | — |

---

## 6. 后续子文档结构

- 每个 Wave 文件包含：目标、Patch 列表、实施顺序、具体要求、验收重点。
- 每个 Wave 末尾都有独立 Gate，只有通过 Gate 才进入下一个 Wave。
- 最终验收（Full Stack Test、性能矩阵、DoD）统一在 [08-integration-and-acceptance.md](08-integration-and-acceptance.md)。
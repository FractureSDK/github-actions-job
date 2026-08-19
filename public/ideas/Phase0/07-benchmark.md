# Phase 0-G：性能基准体系

> 对应总览：[00-overview.md](00-overview.md)

Phase 0 不允许只靠"感觉变快了"。

必须建立：

```text
Baseline
  ↓
Patch
  ↓
Benchmark
  ↓
Compare
  ↓
Accept / Reject
```

---

## P0-018 Baseline Benchmark

在未修改核心性能行为的 Leaf 基线环境记录：

```text
Startup Time
Idle TPS
Idle MSPT
CPU Usage
Heap Usage
GC
Player Join
Chunk Load
Chunk Save
Entity Tick
Network Throughput
```

---

## P0-019 压力场景

至少准备：

```text
Scenario A
空服务器

Scenario B
普通生存

Scenario C
高实体密度

Scenario D
高速跑图

Scenario E
高网络流量

Scenario F
区块保存压力
```

后续每个核心 Patch 尽量在相同场景重复测试。

---

## P0-020 性能回归门槛

建立 Regression Rule。

例如：

```text
MSPT ↑ > X%  → FAIL
TPS ↓ > X%   → FAIL
Memory ↑ > X% → REVIEW
Startup ↑ > X% → REVIEW
Crash        → FAIL
Data Loss    → FAIL
```

具体阈值后续可以定，但机制必须在 Phase 0 建好。
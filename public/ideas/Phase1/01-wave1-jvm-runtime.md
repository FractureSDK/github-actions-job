# Phase 1-A：Wave 1 — JVM / Runtime Foundation

> 对应总览：[00-overview.md](00-overview.md)
> 通过 Wave 1 Gate 后才进入 Wave 2。

---

## 目标

首先建立 Leviathan 自己的运行时优化基础。

对应 v2：

```text
0001-0010
0013-0020
0031-0038
0045
```

源规划中第一卷的重点包括 ZGC、JIT/对象路径、Fastutil，以及 JVM 参数与运行时诊断。

---

## W1-01：ZGC

### Patch

```text
0001
0002
0003
0004
0005
```

### 实施顺序

```text
0001
 ↓
0002
 ↓
0003
 ↓
0004
 ↓
0005
```

### 目标

建立：

```text
JVM
 └─ ZGC
     ├─ 参数
     ├─ GC Metrics
     ├─ System.gc 控制
     └─ Pressure Benchmark
```

### 验收

重点观察：

```text
GC Pause
Allocation Rate
Heap Usage
MSPT
CPU
Startup
```

特别禁止只看 TPS。

---

## W1-02：对象路径优化

### Patch

```text
0006-0010
```

### 目标

围绕：

```text
BlockPos
Vec3
ChunkPos
MutableBlockPos
```

降低热路径对象创建。

### 要求

不要一次性修改所有热路径。

顺序：

```text
BlockPos
 ↓
Vec3
 ↓
ChunkPos
```

每个阶段独立 Benchmark。

---

## W1-03：Fastutil

### Patch

```text
0013-0020
```

### 重点

替换高频内部集合，但必须保留 Bukkit/Paper API 的外部视图兼容。

源规划明确把这一组作为成本较低、收益较稳定的 Tier 1 内容。

### 核心风险

```text
内部集合
     ↓
Bukkit API
     ↓
插件
```

因此：

> 内部实现可以改变，外部 API 行为不能随意改变。

---

## W1-04：Runtime Diagnostics

### Patch

```text
0031-0038
0045
```

### 重点

```text
JDK Check
CPU Capability
Threading
Direct Memory
Heap Snapshot
Allocation
JIT Status
GC Analysis
Integrity
```

这部分应直接接入 Phase 0 Observability。

---

## Wave 1 Gate

Wave 1 完成后必须进行完整回归。

```text
[ ] 服务正常启动
[ ] Bukkit/Paper API 正常
[ ] 现有插件正常加载
[ ] TPS 无明显下降
[ ] MSPT 无明显上升
[ ] Memory 无异常增加
[ ] GC 行为可观测
[ ] Fastutil 替换无 Collection API 回归
[ ] Safe Mode 可回退
```

只有通过 Gate 才进入 Wave 2。
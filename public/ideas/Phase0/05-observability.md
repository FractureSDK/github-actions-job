# Phase 0-E：可观测性核心

> 这是整个 Phase 0 最重要的部分。
> 对应总览：[00-overview.md](00-overview.md)

---

## P0-011 Metrics Registry

建立统一指标注册中心：

```text
MetricRegistry
```

所有未来模块使用同一体系。

支持：

```text
Counter
Gauge
Histogram
Timer
Rate
```

---

## P0-012 Tick Metrics

建立：

```text
TPS
MSPT
Tick Duration
Tick Overrun
Tick Spike
```

至少需要：

```text
P50
P95
P99
MAX
```

---

## P0-013 CPU Metrics

记录：

```text
Process CPU
System CPU
Main Thread CPU
Worker Thread CPU
```

---

## P0-014 Memory Metrics

记录：

```text
Heap Used
Heap Max
Heap Committed
Direct Memory
Native Memory
GC Count
GC Pause
Allocation Rate
```

这部分为后续 ZGC、内存优化 Patch 提供基线。

---

## P0-015 Thread Metrics

建立：

```text
Thread Count
Runnable
Blocked
Waiting
Parked
CPU Time
```

并为未来 Scheduler / Async / Region Tick 提供统一数据入口。
# Phase 1-C：Wave 3 — Network Runtime

> 对应总览：[00-overview.md](00-overview.md)
> 强依赖：Wave 3 → Wave 4。

---

## 目标

对应 v2：

```text
0091-0092
0094-0108
0116-0118
0124
0129
0130
```

v2 明确把 Epoll、TCP、Zstd 网络压缩以及 Packet/ByteBuf 优化列为 Tier 1。

---

## W3-01：Transport

### Patch

```text
0091
0092
0094-0100
```

### 目标

建立：

```text
Minecraft
 ↓
Netty
 ↓
Epoll
 ↓
TCP Optimization
 ↓
Connection Control
```

先做 Transport，再做 Compression。

---

## W3-02：Network Compression

### Patch

```text
0101-0108
```

### 范围

第一阶段只做：

```text
Zstd
Threshold
Compression Pool
Async Queue
Adaptive Level
Small Packet Bypass
Statistics
```

### 明确不提前加入

```text
Network Dictionary
Protocol Negotiation
高级差分编码
```

这些已经被 v2 明确后移。

---

## W3-03：Packet Memory

### Patch

```text
0116
0117
0118
0124
0129
0130
```

### 重点

```text
Packet Object Pool
PooledByteBuf
Batch
AES Acceleration
Packet Size Protection
Integrity Test
```

---

## Wave 3 Gate

必须比较：

```text
Baseline
vs
Leviathan Network
```

指标：

```text
Network Throughput
CPU / Player
Packet Rate
Compression Ratio
Compression CPU
P50 RTT
P95 RTT
P99 RTT
```

尤其需要验证：

> Zstd 节省的带宽是否值得增加的 CPU 成本。
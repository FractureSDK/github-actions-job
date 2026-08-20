# Phase 1-D：Wave 4 — Network Flow Control

> 对应总览：[00-overview.md](00-overview.md)
> 强依赖：Wave 3 → Wave 4（本 Wave 建立在 Network Runtime 之上）。

---

## 目标

对应：

```text
0131-0160
```

v2 把整卷列入 Tier 1，原因是弱网与跨国连接直接影响目标服务器体验。

这一 Wave 不能单独看成"防弱网"。

它应该被定义成：

> **Player Network Adaptation Layer**

---

## W4-01：网络质量

### Patch

```text
0131-0133
```

### 目标

建立：

```text
RTT
Packet Quality
Tier S/A/B/C/D
Token Bucket
```

---

## W4-02：Chunk Streaming

### Patch

```text
0134-0142
```

### 实现

```text
Chunk Rate Limit
Combat Packet Bypass
Smooth Transition
Weak Network Detection
Dynamic View Distance
Timeout Protection
```

---

## W4-03：Chunk Memory / Lazy Sending

### Patch

```text
0143-0159
```

### 实现

```text
Known Chunk Set
Duplicate Suppression
Chunk Delta Tracking
Boundary Jump Protection
Entity Lazy Update
Priority Queue
Spiral Send
Predictive Loading
Unload Delay
Bandwidth Metrics
Retry
Malicious Chunk Request Protection
Dynamic View Distance
```

这部分是整个 Phase 1 网络层中风险较高的区域。

因此建议：

```text
先监控
 ↓
后限速
 ↓
再主动丢弃/延迟
```

而不是一开始就激进控制。

---

## Wave 4 Gate

建立专门的弱网测试矩阵：

```text
S
A
B
C
D
```

并模拟：

```text
Low RTT
High RTT
Packet Loss
Jitter
Bandwidth Limit
Burst Traffic
Fast Movement
Chunk Border Crossing
```

验收重点：

```text
连接稳定性
Chunk Loading
Combat Responsiveness
视距变化
掉线率
带宽峰值
```
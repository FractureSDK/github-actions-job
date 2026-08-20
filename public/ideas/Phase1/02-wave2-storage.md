# Phase 1-B：Wave 2 — Storage Foundation

> 对应总览：[00-overview.md](00-overview.md)
> 强依赖：Wave 2 → Wave 6（异步 Chunk 系统建立在 Storage Runtime 之上）。

---

## 目标

对应：

```text
0046-0065
```

v2 将 Linear V2 与 Zstd Storage 全部列入 Tier 1。

这是 Phase 1 第二个最大核心。

---

## W2-01：Linear V2

### Patch

```text
0046-0055
```

### 实施顺序

```text
0046 Migration
 ↓
0047 Writer
 ↓
0048 Reader
 ↓
0049 Region Manager
 ↓
0050 Integrity
 ↓
0051 Snapshot
 ↓
0052 Anvil Compatibility
 ↓
0053 Async Queue
 ↓
0054 Rate Limit
 ↓
0055 Metrics
```

---

## W2-01.1：必须建立双写/双读验证期

不能直接：

```text
Anvil
 ↓
Linear V2
```

然后删除 Anvil。

必须经历：

```text
Anvil
 ↕
Compatibility Layer
 ↕
Linear V2
```

首先验证：

```text
生成
加载
保存
重载
服务器重启
崩溃恢复
备份
恢复
```

---

## W2-01.2：Storage Benchmark

重点指标：

```text
Chunk Read P50/P95/P99
Chunk Write P50/P95/P99
Region Open
Save Burst
Startup
Disk Usage
IOPS
Throughput
```

---

## W2-02：Zstd Storage

### Patch

```text
0056-0065
```

### 实施顺序

```text
Zstd Integration
 ↓
Compression Level
 ↓
Dictionary
 ↓
Versioning
 ↓
Multi-Dictionary
 ↓
Pipeline
 ↓
Cache
 ↓
Fallback
```

### 实施纪律

> **Zstd 不应该在 Linear V2 尚未稳定之前深度优化。**

先：

```text
Linear V2 + zlib fallback
```

确认正确性，再切换：

```text
Linear V2 + Zstd
```

---

## Wave 2 Gate

必须通过：

```text
[ ] 新世界正常生成
[ ] 老 Anvil 世界可迁移
[ ] Anvil 兼容读取正常
[ ] 重启不会丢区块
[ ] 崩溃恢复正常
[ ] Backup/Restore 正常
[ ] Chunk Read 延迟不恶化
[ ] Chunk Write 延迟不恶化
[ ] 数据完整性测试通过
[ ] Zstd 失败可回退
```

这一 Gate 是整个 Phase 1 极其重要的一道门。

因为后面的异步 Chunk 系统会建立在 Storage Runtime 之上。
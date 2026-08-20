# Phase 1-F：Wave 6 — Async Offload

> 对应总览：[00-overview.md](00-overview.md)
> 强依赖：Wave 2 → Wave 6（本 Wave 建立在 Storage Foundation 之上）。
> 这是 Phase 1 最危险的阶段之一。

---

## 目标

对应：

```text
0236-0250
```

v2 明确将这部分作为 Tier 1，属于 Phase 1 Async Offload Layer。

---

## W6-01：Observer First

### Patch

```text
0236
```

先确定：

```text
Main Thread
 ├── Save
 ├── Load
 ├── PlayerData
 ├── Plugin
 ├── Tick
 └── Other
```

到底谁最耗时。

---

## W6-02：Async Save

### Patch

```text
0237
```

这是第一项真正移出主线程的功能。

必须保证：

```text
Main Thread
    ↓
Serialize Snapshot
    ↓
Async Storage
```

而不是：

```text
Main Thread
    ↓
共享可变 Chunk
    ↓
Async Thread
```

后者极易出现数据竞争。

---

## W6-03：Async Load

### Patch

```text
0238
```

流程：

```text
Request
 ↓
IO Thread
 ↓
Decode
 ↓
Validation
 ↓
Main Thread Registration
```

绝不能让后台线程直接任意修改 Minecraft World State。

---

## W6-04：Player Data

### Patch

```text
0239
```

这一项必须基于 Phase 0/现有存储架构稳定后实施。

v2 本身把 RocksDB 玩家数据迁移放到了 Tier 3，因此这里 Phase 1 只处理已有玩家数据路径的异步化，不顺便引入 RocksDB。

---

## W6-05：任务分类与 Scheduler

### Patch

```text
0241-0247
```

建立：

```text
Synchronous
Async
Deferred
```

三个类别。

这是未来 Phase 2 Region Runtime 的前置条件。

---

## W6-06：监控与压力测试

### Patch

```text
0248-0250
```

### 重点

```text
Thread Stats
Concurrent Join
Async Queue Depth
Queue Latency
Error Rate
Data Race
```

---

## Wave 6 Gate

这是 Phase 1 最严格的验收门。

必须达到：

```text
[ ] 无数据丢失
[ ] 无重复写入
[ ] 无 Chunk 状态竞争
[ ] 无服务器死锁
[ ] 无 Async Queue 无限增长
[ ] Shutdown 可正确清空队列
[ ] Crash Recovery 正常
[ ] Login/Logout 压力测试通过
```
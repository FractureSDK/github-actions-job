# Phase 1 最终集成测试与验收

> 对应总览：[00-overview.md](00-overview.md)

---

## 1. Leviathan Full Stack Test

Phase 1 所有 Wave 完成后，不能直接宣布完成。

必须进行一次完整的：

```text
Client
 ↓
Network
 ↓
Packet
 ↓
Player
 ↓
Region / Chunk
 ↓
Entity
 ↓
World
 ↓
Storage
 ↓
Async Save
```

完整运行：

```text
24h Stability Test
```

---

## 2. 性能验收矩阵

至少比较：

| 指标                | Phase 0 Baseline | Phase 1 | 要求    |
| ----------------- | ---------------: | ------: | ----- |
| TPS               |         Baseline |      实测 | 不下降   |
| MSPT P50          |         Baseline |      实测 | 改善    |
| MSPT P95          |         Baseline |      实测 | 改善    |
| MSPT P99          |         Baseline |      实测 | 不恶化   |
| GC Pause          |         Baseline |      实测 | 改善    |
| Heap              |         Baseline |      实测 | 不异常增加 |
| CPU               |         Baseline |      实测 | 综合下降  |
| Chunk Read        |         Baseline |      实测 | 不恶化   |
| Chunk Write       |         Baseline |      实测 | 不恶化   |
| Network Bandwidth |         Baseline |      实测 | 改善    |
| Entity Tick       |         Baseline |      实测 | 改善    |
| Main Thread Load  |         Baseline |      实测 | 改善    |
| Startup           |         Baseline |      实测 | 可接受   |

具体数值阈值由 Phase 0 已建立的 Regression Policy 决定，不在 Patch 中临时修改。

---

## 3. Phase 1 Definition of Done

Phase 1 不是"173 个 Patch 全部打钩"。

真正的完成条件是：

```text
[ ] Tier 1 核心模块全部实现或明确 Deferred
[ ] 所有已实现 Patch 有测试
[ ] 所有高风险 Patch 有回滚方案
[ ] 所有模块接入统一 Metrics
[ ] 所有模块接入统一 Config
[ ] 所有核心改动经过 Benchmark
[ ] 24h 稳定性测试通过
[ ] 玩家登录/退出压力测试通过
[ ] Chunk Load/Save 测试通过
[ ] 网络弱网测试通过
[ ] 高实体测试通过
[ ] 插件回归测试通过
[ ] Vanilla 行为回归通过
[ ] Phase 0 Baseline 对比完成
[ ] 最终收益报告完成
```

---

## 4. Phase 1 完成后的目标架构

```text
                         Leviathan
                             │
              ┌──────────────┼──────────────┐
              │              │              │
        Observability    Configuration    Diagnostics
              │              │              │
              └──────────────┼──────────────┘
                             │
                    Leviathan Runtime
                             │
       ┌────────────┬────────┼───────────┐
       ▼            ▼        ▼           ▼
      JVM        Storage   Network    Scheduler
       │            │        │           │
       │            │        │           │
       └────────────┴────────┼───────────┘
                             ▼
                     Game Simulation
                             │
                 ┌───────────┼───────────┐
                 ▼           ▼           ▼
               Entity      Chunk      Physics
                 │           │           │
                 └───────────┼───────────┘
                             ▼
                      Async Runtime
```

此时 Leviathan 才真正从：

> **Leaf + Patch**

进化成：

> **Leaf Compatibility Base + Leviathan Runtime。**

---

## 5. Phase 2 的入口条件

只有 Phase 1 完成并稳定后，才允许进入 v2 原规划中的 Tier 2：

```text
0066-0075   FFM / mmap
0176-0185   SIMD
0211-0220   Async Chunk Generation
0251-0265   Region Tick
0266-0280   Plugin Compatibility
```

其中最重要的下一阶段仍然是：

```text
Folia RegionizedServer
        ↓
源码研究
        ↓
架构适配
        ↓
Leviathan Region Runtime
```

而不是从零重新设计 Region Tick。
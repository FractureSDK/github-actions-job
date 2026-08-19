# Project Leviathan — Phase 0 总览

## 1. Phase 0 定位

**阶段名称：** Phase 0 — Foundation / Baseline

**核心目标：**

> 在正式修改 Leaf 核心性能行为之前，建立完整的 Leviathan 开发基线、构建体系、配置体系、测试体系和可观测性基础设施。

Phase 0 不以性能提升为验收目标。

Phase 0 的最终结果应该是：

```text
Leaf
  ↓
Leviathan 基础框架
  ↓
统一配置系统
  ↓
统一日志/指标系统
  ↓
Benchmark / Regression Test
  ↓
CI / Build / Patch 管理
  ↓
可安全开始 Phase 1
```

---

## 2. Phase 0 原则

### 2.1 不修改核心游戏行为

Phase 0 不主动引入：

- ZGC 参数强制优化
- Linear V2
- Zstd
- DAB
- 网络流控
- 异步 Chunk
- 多线程 Tick
- Region Tick

这些功能属于后续阶段。

Phase 0 允许修改的内容主要是：

- 构建系统
- 模块结构
- 配置框架
- 日志
- 指标
- Benchmark
- 测试
- 启动诊断
- Patch 管理
- 开发工具

---

## 3. Phase 0 总体架构

```text
                   ┌─────────────────────┐
                   │   Leaf Base Source  │
                   └──────────┬──────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │ Leviathan Bootstrap │
                   └──────────┬──────────┘
                              │
         ┌────────────────────┼────────────────────┐
         ▼                    ▼                    ▼
  Config Runtime       Observability         Diagnostics
         │                    │                    │
         └────────────────────┼────────────────────┘
                              ▼
                   ┌─────────────────────┐
                   │ Test / Benchmark    │
                   │ Infrastructure      │
                   └──────────┬──────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │ Phase 1 Patch Layer │
                   └─────────────────────┘
```

Phase 0 的关键是建立一个所有后续 Patch 都能接入的公共底座。

---

## 子文档索引

| 文档 | 内容 | 条目 |
| ---- | ---- | ---- |
| [01-source-and-build.md](01-source-and-build.md) | Phase 0-A 源码与构建体系 | P0-001 ~ P0-003 |
| [02-runtime-detection.md](02-runtime-detection.md) | Phase 0-B 运行环境基线 | P0-004 ~ P0-005 |
| [03-config-system.md](03-config-system.md) | Phase 0-C 配置系统 | P0-006 ~ P0-008 |
| [04-logging.md](04-logging.md) | Phase 0-D 统一日志体系 | P0-009 ~ P0-010 |
| [05-observability.md](05-observability.md) | Phase 0-E 可观测性核心 | P0-011 ~ P0-015 |
| [06-commands.md](06-commands.md) | Phase 0-F /leviathan 命令基础 | P0-016 ~ P0-017 |
| [07-benchmark.md](07-benchmark.md) | Phase 0-G 性能基准体系 | P0-018 ~ P0-020 |
| [08-testing.md](08-testing.md) | Phase 0-H 测试体系 | P0-021 ~ P0-023 |
| [09-cicd.md](09-cicd.md) | Phase 0-I CI/CD | — |
| [10-patch-registry.md](10-patch-registry.md) | Phase 0-J Patch 管理工具 | — |
| [11-safety-and-rollback.md](11-safety-and-rollback.md) | Phase 0-K 安全与回滚机制 | — |

---

## 15. Phase 0 不实现的内容

以下全部明确禁止提前塞进 Phase 0：

```text
❌ Linear V2
❌ Zstd Storage
❌ mmap
❌ RocksDB
❌ Zstd Network
❌ DAB
❌ Hopper Sleep
❌ SIMD
❌ Async Chunk
❌ Region Tick
❌ Plugin Async
❌ Purpur Gameplay Changes
```

原因很简单：

> Phase 0 的任务是建立"测量和控制系统"，不是开始制造性能变量。

---

## 16. Phase 0 最终产物

Phase 0 完成后，仓库应该至少拥有：

```text
Leviathan
├── Runtime Detector
├── Configuration System
├── Feature Flag System
├── Logger
├── Metrics Registry
├── Diagnostics
├── /leviathan command framework
├── Benchmark Framework
├── Regression Framework
├── Unit Tests
├── Integration Tests
├── CI Pipeline
├── Patch Registry
└── Safe Mode
```

---

## 17. Phase 0 验收标准

必须全部满足：

```text
[ ] Leaf 能正常编译
[ ] Leaf 能正常启动
[ ] Vanilla 行为无明显改变
[ ] 插件加载正常
[ ] Runtime Detection 正常
[ ] Leviathan 配置正常加载
[ ] Feature Flag 正常工作
[ ] /leviathan status 正常
[ ] /leviathan stats 正常
[ ] Tick/CPU/Memory/GC 数据可见
[ ] Baseline Benchmark 可重复
[ ] Integration Test 可重复
[ ] CI 可自动构建
[ ] Patch Registry 可追踪
[ ] Safe Mode 可启动
[ ] 出现配置错误时不会直接产生模糊崩溃
```

---

## 18. Phase 0 完成后的状态

最终不是：

> "我们已经完成很多优化。"

而应该是：

> **"我们现在拥有一个可以安全修改 Leaf、可以测量修改结果、可以快速回滚修改，并且能够追踪 315 Patch 依赖关系的 Leviathan 开发底座。"**

此时才正式进入：

```text
Phase 1
↓
JVM / Memory
Storage
Network
Flow Control
DAB
Async I/O
配置调控
```

并且每个 Patch 都必须经过：

```text
Implement
↓
Test
↓
Benchmark
↓
Compare against Phase 0 Baseline
↓
Pass / Reject
```

这套机制将成为整个 Project Leviathan 的核心开发纪律。
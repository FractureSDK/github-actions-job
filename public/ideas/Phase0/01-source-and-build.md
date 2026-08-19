# Phase 0-A：源码与构建体系

> 对应总览：[00-overview.md](00-overview.md)

---

## P0-001 项目 Fork 基线

建立 Leviathan 独立源码仓库。

要求：

- Leaf 作为最初基线
- 保留 upstream 来源标识
- 记录 Leaf commit / version
- 建立 Leviathan 自己的版本号体系
- 建立 upstream merge 流程
- 禁止直接在混乱的临时分支上长期开发

最终形成：

```text
upstream Leaf
     │
     ▼
Leviathan Base
     │
     ├── leviathan-main
     ├── feature/*
     ├── patch/*
     └── experimental/*
```

---

## P0-002 Patch 编号体系

建立统一 Patch 编号：

```text
LV-0001
LV-0002
LV-0003
...
LV-0315
```

同时保留原始 Patch 编号：

```text
Leviathan Patch 0001
→ LV-0001
```

每个 Patch 必须有：

```text
ID
Title
Category
Risk
Dependencies
Source Files
Test Plan
Benchmark
Rollback
Status
```

状态统一：

```text
PLANNED
READY
IMPLEMENTING
TESTING
PASSED
FAILED
DEFERRED
REWORK
```

---

## P0-003 模块目录规范

建立统一代码结构。

建议：

```text
levidathan/
├── bootstrap/
├── config/
├── observability/
├── diagnostics/
├── benchmark/
├── testing/
├── scheduler/
├── storage/
├── network/
├── entity/
├── world/
├── plugin/
└── compatibility/
```

Phase 0 不要求所有模块立即实现，但目录和职责需要先确定。
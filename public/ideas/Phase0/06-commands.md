# Phase 0-F：/leviathan 命令基础

> 对应总览：[00-overview.md](00-overview.md)

建立统一命令树：

```text
/leviathan
├── version
├── info
├── status
├── stats
├── runtime
├── metrics
├── diagnostics
└── benchmark
```

Phase 0 不实现所有具体功能。

先建立命令框架。

---

## P0-016 `/leviathan status`

显示：

```text
Leviathan Version
Leaf Version
Java Version
OS
CPU
Memory
Active Features
Runtime Mode
```

---

## P0-017 `/leviathan stats`

第一版至少显示：

```text
TPS
MSPT
CPU
Memory
GC
Threads
Players
Entities
Chunks
```

未来所有 Patch 都把自己的统计挂到这里。
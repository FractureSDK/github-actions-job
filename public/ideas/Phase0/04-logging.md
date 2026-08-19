# Phase 0-D：统一日志体系

> 对应总览：[00-overview.md](00-overview.md)

---

## P0-009 Leviathan Logger

建立统一日志命名空间：

```text
[Leviathan]
[Leviathan/Runtime]
[Leviathan/Storage]
[Leviathan/Network]
[Leviathan/Entity]
[Leviathan/Region]
[Leviathan/Plugin]
```

禁止未来每个 Patch 各自定义日志体系。

---

## P0-010 Structured Diagnostics

所有关键模块至少支持：

```text
INFO
WARN
ERROR
DEBUG
TRACE
```

高频性能统计尽量不要刷控制台，而进入：

```text
logs/leviathan/
```

例如：

```text
runtime.log
performance.log
network.log
storage.log
entity.log
diagnostics.log
benchmark.log
```
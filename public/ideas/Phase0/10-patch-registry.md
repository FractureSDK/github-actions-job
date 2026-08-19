# Phase 0-J：Patch 管理工具

> 对应总览：[00-overview.md](00-overview.md)

建立一个内部 Patch Registry。

例如：

```yaml
patch: 0001
name: ZGC Bootstrap
tier: 1
phase: 1
status: planned

dependencies:
  - P0-004
  - P0-014

tests:
  - gc-baseline

benchmark:
  - startup
  - memory
```

这样以后 173 个 Tier 1 Patch 不再只是 Markdown 清单，而是真正成为可以追踪的工程对象。
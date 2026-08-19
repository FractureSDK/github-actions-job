# Phase 0-C：配置系统

> 对应总览：[00-overview.md](00-overview.md)

---

## P0-006 Leviathan 主配置文件

建立：

```text
config/leviathan.yml
```

建议结构：

```yaml
leviathan:
  version: 1

  runtime:
    mode: safe

  diagnostics:
    enabled: true

  observability:
    enabled: true

  benchmark:
    enabled: false

  experimental:
    enabled: false
```

---

## P0-007 Feature Flag 系统

所有后续实验性功能都必须支持：

```text
disabled
safe
enabled
experimental
```

而不是写死在代码中。

例如：

```yaml
features:
  linear-storage: false
  zstd-storage: false
  dab: false
  async-chunk: false
  region-tick: false
```

这样后续出现严重问题时可以直接关闭模块。

---

## P0-008 配置校验

服务器启动时检查：

- 类型
- 范围
- 冲突配置
- Runtime Capability
- 依赖模块

例如：

```text
region-tick = true
linear-storage = false
```

如果存在依赖关系，则提前报告，而不是运行到中途崩溃。
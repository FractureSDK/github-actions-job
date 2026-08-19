# Phase 0-K：安全与回滚机制

> 对应总览：[00-overview.md](00-overview.md)

建立三个核心能力：

## Safe Mode

```text
leviathan.runtime.mode=safe
```

只加载基础设施，不启用实验性优化。

## Feature Disable

任何高风险模块可以单独关闭。

## Automatic Fallback

发现：

```text
Crash
Deadlock
Data Corruption
Startup Failure
```

时至少能够进入 Safe Mode 或输出明确恢复建议。
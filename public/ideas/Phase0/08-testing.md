# Phase 0-H：测试体系

> 对应总览：[00-overview.md](00-overview.md)

---

## P0-021 单元测试框架

所有新的 Leviathan 基础设施优先单元测试。

重点：

```text
Config
Metrics
Runtime Detector
Feature Flags
Command Tree
Diagnostics
```

---

## P0-022 Integration Test

建立服务器级测试：

```text
Boot
Shutdown
Load World
Save World
Player Join
Player Quit
Plugin Load
Config Reload
```

---

## P0-023 Crash / Recovery Test

重点验证：

```text
Invalid Config
Missing Dependency
Unsupported JDK
Unsupported CPU Feature
Storage Error
Plugin Error
Background Task Error
```
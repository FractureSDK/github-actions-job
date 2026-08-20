# Phase 1-G：Wave 7 — Server Controls / Final Integration

> 对应总览：[00-overview.md](00-overview.md)

---

## 目标

最后处理 v2 中低风险、高可用性的控制类功能：

```text
0281-0290
0304-0313
0314
0315
```

这些在 v2 中被定义为配置控制、Anarchy 特性以及最终规则/启动报告。

---

## W7-01：Stability Controls

### Patch

```text
0281-0290
```

### 包括

```text
Entity Stack
Hopper Rate
Chunk Send Limit
Entity Activation
Explosion
XP
Mob Spawn
Autosave
Chunk Unload
TPS Overload Protection
```

### 接入

```text
Leviathan Config
Feature Flags
/leviathan rules
```

---

## W7-02：Anarchy Controls

### Patch

```text
0304-0313
```

### 包括

```text
TNT Duplication
Carpet Duplication
Rail Duplication
Bedrock Breaking
Void Damage
Sculk Propagation
End Reset
Keep XP
Keep Inventory
Custom Ghast Size
```

所有功能默认值必须明确。

---

## W7-03：Final Runtime Report

### Patch

```text
0314
0315
```

### 最终

```text
/leviathan rules list
```

以及：

```text
Leviathan Startup Report
```

### 报告内容

```text
JVM
Storage
Network
Entity
Async
Configuration
Feature Status
Benchmark Status
Hardware Capability
```
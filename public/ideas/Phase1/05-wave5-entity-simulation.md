# Phase 1-E：Wave 5 — Entity / Simulation

> 对应总览：[00-overview.md](00-overview.md)

---

## 目标

对应：

```text
0161-0175
0191-0199
0201-0203
```

v2 明确把 DAB 作为核心差异化能力，并优先加入 Hopper / Item / XP / Arrow 等低成本优化。

---

## W5-01：DAB

### Patch

```text
0161-0175
```

这是 Phase 1 最值得进行性能 Benchmark 的模块之一。

### 核心

```text
Distance
 ↓
Frequency
 ↓
Entity Tick
```

并保留：

```text
Boss Exemption
Combat Exemption
Mount Exemption
Pathfinding Control
Animation Control
Entity Sleep
Wakeup
```

---

## W5-01.1：DAB 不应该直接激进启用

采用：

```text
observe
 ↓
shadow mode
 ↓
limited entities
 ↓
production
```

### Shadow Mode

服务器仍然按原逻辑 Tick，但记录：

```text
哪些实体理论上可以降频
节省多少 Tick
哪些实体会被豁免
```

等数据证明 DAB 有价值后再真正改变执行频率。

---

## W5-02：Hopper / Item / XP / Arrow

### Patch

```text
0191-0199
```

这部分属于"低风险性能优化组"。

### 重点

```text
Hopper Sleep
Item Merge
Old Item Aging
Entity Count Limit
XP Merge
Arrow Tick Reduction
```

所有会删除/改变游戏实体行为的内容必须增加：

```text
Vanilla Comparison Test
```

---

## W5-03：Collision Circuit Breaker

### Patch

```text
0201-0203
```

### 建立

```text
Entity Density
 ↓
Collision Threshold
 ↓
PVP Override
```

### 重点验证

```text
PVP
Mob Farm
Villager Farm
Entity Stack
Projectile
Knockback
```

---

## Wave 5 Gate

必须拥有至少：

```text
Normal Survival
Mob Farm
Villager Area
High Entity Density
PVP
Redstone Area
```

五种测试世界/场景。

比较：

```text
Entity Tick Cost
MSPT
Entity Count
Collision Cost
AI Cost
Player Experience
```
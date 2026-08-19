# Phase 0-B：运行环境基线

> 对应总览：[00-overview.md](00-overview.md)

---

## P0-004 Java Runtime 检测

建立启动时 Runtime Detector。

输出：

```text
Java Version
VM Name
VM Version
OS
Architecture
CPU Count
Memory
Kernel
Native Access
Preview Support
SIMD Capability
```

注意：

Phase 0 只负责检测。

真正的 JVM 参数修改放到后续 Patch。

---

## P0-005 硬件能力探测

建立：

```text
CPU
├── logical processors
├── physical processors
├── SIMD
├── AVX2
└── AVX-512

Memory
├── physical memory
├── max heap
└── direct memory

OS
├── Linux
├── Kernel
└── Filesystem
```

结果统一进入 Runtime Capability。

例如：

```java
Capabilities.cpu().avx2()
Capabilities.cpu().avx512()
Capabilities.runtime().jdk25()
Capabilities.os().linux()
```

后续 Patch 不应该自己重复探测硬件。
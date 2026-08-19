# Phase 0-I：CI/CD

> 对应总览：[00-overview.md](00-overview.md)

建立最基本的自动化检查：

```text
Push
↓
Compile
↓
Unit Test
↓
Integration Test
↓
Static Analysis
↓
Patch Validation
```

Pull Request 必须通过基础测试后才能合并。
# 📈 迁移性能验收 (OAT) 报告 (Migration Performance OAT Report)

## 🌍 元数据 (Metadata)

- **环境 (Environment)**: Production/Staging (本地测试 Local Test)
- **运行时间 (Run Time)**: 2025-12-01 16:20:19
- **测试标的 (Test Target)**: 600519.SH
- **MySQL 版本 (Version)**: 8.0.42
- **MySQL 主机 (Host)**: hli
---
## 📌 静态执行计划验证 (Execution Plan Validation)

| SQL 场景 | 分区 (Partitions) | 索引 (Index) | 类型 (Type) | 结果 (Result) |
| --- | --- | --- | --- | --- |
| 单日K线 (New) | p202101 | uniq_windcode_tradedate | range | ✅ PASS |
| 单月聚合 (New) | p202101 | uniq_windcode_tradedate | range | ✅ PASS |
| 跨月(Q1)聚合 (New) | p202101, p202102, p202103 | uniq_windcode_tradedate | range | ✅ PASS |

---
## ⚡ 热读 (内存命中) 结果 (Hot Read (RAM) Results)

> 模拟数据已在 Buffer Pool 缓存中的高频访问，主要考验 **CPU 解压性能**。

没有可用的测试数据 (No test data available)。

---
## ❄️ 冷读 (磁盘命中) 结果 (Cold Read (Disk) Results)

> 模拟数据不在缓存中、必须从磁盘读取的“温数据”访问，主要考验 **I/O 性能**。

| 场景 (Scenario) | 旧表耗时 (ms) | 新表耗时 (ms) | 性能变化 (%) |
| --- | --- | --- | --- |
| 单日K线 | 2.000 | 1.000 | -50.00% (提升) |
| 单月聚合 | 1.000 | 7.000 | +600.00% (变慢) |
| 跨月(Q1)聚合 | 6.000 | 26.000 | +333.33% (变慢) |


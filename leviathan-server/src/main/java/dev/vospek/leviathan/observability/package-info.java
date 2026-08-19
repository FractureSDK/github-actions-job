/**
 * Leviathan Observability 模块
 * <p>
 * 提供统一的指标收集、日志记录和诊断系统：
 * <ul>
 *   <li>{@link LeviathanLogger} - 统一日志命名空间 (P0-009)</li>
 *   <li>{@link DiagnosticsLogger} - 结构化诊断日志 (P0-010)</li>
 *   <li>{@link MetricRegistry} - 指标注册中心 (P0-011)</li>
 *   <li>{@link TickMetrics} - Tick 性能指标 (P0-012)</li>
 *   <li>{@link CpuMetrics} - CPU 指标 (P0-013)</li>
 *   <li>{@link MemoryMetrics} - 内存指标 (P0-014)</li>
 *   <li>{@link ThreadMetrics} - 线程指标 (P0-015)</li>
 * </ul>
 * <p>
 * 所有指标通过 {@link MetricRegistry} 统一注册和访问，支持 Counter、Gauge、Histogram、Timer、Rate 五种类型。
 *
 * @since Leviathan 26.2
 */
package dev.vospek.leviathan.observability;
/**
 * Leviathan Bootstrap 模块
 * <p>
 * 负责服务器启动时的基础设施初始化：
 * <ul>
 *   <li>{@link RuntimeDetector} - Java 运行时基线检测 (P0-004)</li>
 *   <li>{@link HardwareCapabilities} - 硬件能力探测 (P0-005)</li>
 *   <li>{@link LeviathanBootstrap} - 统一启动入口</li>
 * </ul>
 * <p>
 * 所有后续 Patch 通过 {@link HardwareCapabilities.Capabilities} 统一访问硬件能力，
 * 避免重复探测。
 *
 * @since Leviathan 26.2
 */
package dev.vospek.leviathan.bootstrap;
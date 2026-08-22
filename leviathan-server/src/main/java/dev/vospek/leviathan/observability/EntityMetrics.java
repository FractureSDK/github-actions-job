package dev.vospek.leviathan.observability;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 实体 / 模拟指标收集器（骨架）
 * <p>
 * 注册 Wave 5 实体层指标。数据来源为 NMS 实体 tick 与碰撞检测路径
 * （当前工作树无应用化 NMS 源码），故本类仅暴露 setter 与直方图，
 * 由后续源码应用后在实体 tick / 碰撞路径打点。
 * <p>
 * 对应 Phase 1-E (W5-01 DAB / W5-02 Hopper-Item-XP / W5-03 Collision) 可观测面
 */
public final class EntityMetrics {

    private static final MetricRegistry REGISTRY = MetricRegistry.get();

    // 实体计数 / 密度（由 NMS region tick 路径设置）
    private final AtomicLong activeEntityCount = new AtomicLong(0);
    private final AtomicLong tickingEntityCount = new AtomicLong(0);
    private final AtomicLong collisionChecks = new AtomicLong(0);

    // DAB 影子模式计数（W5-01.1：记录理论上可降频的实体数与节省 tick 数）
    private final AtomicLong dabShadowCandidates = new AtomicLong(0);
    private final AtomicLong dabShadowReducedTicks = new AtomicLong(0);
    private volatile long dabEnabled = 0;

    // Hopper / Item / XP / Arrow 计数（W5-02 骨架）
    private final AtomicLong sleepingHoppers = new AtomicLong(0);
    private final AtomicLong mergedItems = new AtomicLong(0);
    private final AtomicLong mergedXpOrbs = new AtomicLong(0);
    private final AtomicLong reducedTickArrows = new AtomicLong(0);

    // 耗时直方图骨架：由 NMS 实体 tick / 碰撞路径打点
    private final MetricRegistry.Histogram entityTickCostNs;
    private final MetricRegistry.Histogram collisionCostNs;

    private EntityMetrics() {
        REGISTRY.gaugeLong("entity.active_count", this::getActiveEntityCount);
        REGISTRY.gaugeLong("entity.ticking_count", this::getTickingEntityCount);
        REGISTRY.gaugeLong("entity.collision.checks", this::getCollisionChecks);
        REGISTRY.gaugeLong("entity.dab.enabled", () -> dabEnabled);
        REGISTRY.gaugeLong("entity.dab.shadow_candidates", this::getDabShadowCandidates);
        REGISTRY.gaugeLong("entity.dab.shadow_reduced_ticks", this::getDabShadowReducedTicks);
        REGISTRY.gaugeLong("entity.hopper.sleeping", this::getSleepingHoppers);
        REGISTRY.gaugeLong("entity.item.merged", this::getMergedItems);
        REGISTRY.gaugeLong("entity.xp.merged", this::getMergedXpOrbs);
        REGISTRY.gaugeLong("entity.arrow.reduced_ticks", this::getReducedTickArrows);
        this.entityTickCostNs = REGISTRY.histogram("entity.tick.cost_ns");
        this.collisionCostNs = REGISTRY.histogram("entity.collision.cost_ns");
    }

    private static final class Holder {
        static final EntityMetrics INSTANCE = new EntityMetrics();
    }

    public static EntityMetrics get() {
        return Holder.INSTANCE;
    }

    // ==================== NMS hook setter ====================

    public void setActiveEntityCount(long count) {
        activeEntityCount.set(count);
    }

    public void setTickingEntityCount(long count) {
        tickingEntityCount.set(count);
    }

    public void addCollisionChecks(long delta) {
        collisionChecks.addAndGet(delta);
    }

    public void setDabEnabled(boolean enabled) {
        dabEnabled = enabled ? 1L : 0L;
    }

    public void addDabShadowCandidates(long delta) {
        dabShadowCandidates.addAndGet(delta);
    }

    public void addDabShadowReducedTicks(long delta) {
        dabShadowReducedTicks.addAndGet(delta);
    }

    public void setSleepingHoppers(long count) {
        sleepingHoppers.set(count);
    }

    public void addMergedItems(long delta) {
        mergedItems.addAndGet(delta);
    }

    public void addMergedXpOrbs(long delta) {
        mergedXpOrbs.addAndGet(delta);
    }

    public void addReducedTickArrows(long delta) {
        reducedTickArrows.addAndGet(delta);
    }

    public void recordEntityTickCostNanos(long nanos) {
        entityTickCostNs.recordNanos(nanos);
    }

    public void recordCollisionCostNanos(long nanos) {
        collisionCostNs.recordNanos(nanos);
    }

    // ==================== 便捷查询 ====================

    public long getActiveEntityCount() {
        return activeEntityCount.get();
    }

    public long getTickingEntityCount() {
        return tickingEntityCount.get();
    }

    public long getCollisionChecks() {
        return collisionChecks.get();
    }

    public long getDabShadowCandidates() {
        return dabShadowCandidates.get();
    }

    public long getDabShadowReducedTicks() {
        return dabShadowReducedTicks.get();
    }

    public long getSleepingHoppers() {
        return sleepingHoppers.get();
    }

    public long getMergedItems() {
        return mergedItems.get();
    }

    public long getMergedXpOrbs() {
        return mergedXpOrbs.get();
    }

    public long getReducedTickArrows() {
        return reducedTickArrows.get();
    }

    public MetricRegistry.Histogram getEntityTickCostNs() {
        return entityTickCostNs;
    }

    public MetricRegistry.Histogram getCollisionCostNs() {
        return collisionCostNs;
    }

    /**
     * 实体配置摘要（用于诊断日志）
     */
    public String describe() {
        return "Active entities: " + activeEntityCount.get()
            + ", ticking: " + tickingEntityCount.get()
            + ", DAB: " + (dabEnabled == 1L ? "on" : "off")
            + ", shadow reduced ticks: " + dabShadowReducedTicks.get()
            + ", sleeping hoppers: " + sleepingHoppers.get();
    }
}
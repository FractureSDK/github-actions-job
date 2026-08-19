package dev.vospek.leviathan.observability;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * 统一指标注册中心
 * <p>
 * 所有模块通过此类注册和访问指标，支持五种指标类型：
 * <ul>
 *   <li>{@link Counter} - 单调递增计数器 (线程安全)</li>
 *   <li>{@link Gauge} - 瞬时值观测</li>
 *   <li>{@link Histogram} - 分布统计（支持百分位数）</li>
 *   <li>{@link Timer} - 耗时统计（内部使用 Histogram）</li>
 *   <li>{@link Rate} - 速率统计（单位时间内的事件数）</li>
 * </ul>
 * <p>
 * 对应 Phase 0-E: P0-011
 */
public final class MetricRegistry {

    private static final Logger LOGGER = LogManager.getLogger(MetricRegistry.class);

    private static final MetricRegistry INSTANCE = new MetricRegistry();

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    private final Map<String, Gauge<?>> gauges = new ConcurrentHashMap<>();
    private final Map<String, Histogram> histograms = new ConcurrentHashMap<>();
    private final Map<String, Timer> timers = new ConcurrentHashMap<>();
    private final Map<String, Rate> rates = new ConcurrentHashMap<>();

    private MetricRegistry() {
    }

    public static MetricRegistry get() {
        return INSTANCE;
    }

    // ==================== Counter ====================

    /**
     * 获取或创建计数器
     */
    public Counter counter(String name) {
        return counters.computeIfAbsent(name, Counter::new);
    }

    /**
     * 获取或创建带标签的计数器
     */
    public Counter counter(String name, String... tags) {
        String key = buildKey(name, tags);
        return counters.computeIfAbsent(key, Counter::new);
    }

    // ==================== Gauge ====================

    /**
     * 注册数值型 Gauge (推荐使用具体类型方法)
     */
    public <T extends Number> Gauge<T> gauge(String name, Supplier<T> supplier) {
        String key = buildKey(name);
        @SuppressWarnings("unchecked")
        Gauge<T> gauge = (Gauge<T>) gauges.computeIfAbsent(key, k -> new Gauge<>(supplier));
        return gauge;
    }

    /**
     * 注册双精度 Gauge
     */
    public Gauge<Double> gaugeDouble(String name, DoubleSupplier supplier) {
        return gauge(name, () -> supplier.getAsDouble());
    }

    /**
     * 注册长整型 Gauge
     */
    public Gauge<Long> gaugeLong(String name, LongSupplier supplier) {
        return gauge(name, () -> supplier.getAsLong());
    }

    /**
     * 注册整型 Gauge
     */
    public Gauge<Integer> gaugeInt(String name, IntSupplier supplier) {
        return gauge(name, () -> supplier.getAsInt());
    }

    /**
     * 注册带标签的 Gauge
     */
    public <T extends Number> Gauge<T> gauge(String name, Supplier<T> supplier, String... tags) {
        String key = buildKey(name, tags);
        @SuppressWarnings("unchecked")
        Gauge<T> gauge = (Gauge<T>) gauges.computeIfAbsent(key, k -> new Gauge<>(supplier));
        return gauge;
    }

    // ==================== Histogram ====================

    /**
     * 获取或创建直方图
     */
    public Histogram histogram(String name) {
        return histograms.computeIfAbsent(name, Histogram::new);
    }

    /**
     * 获取或创建带标签的直方图
     */
    public Histogram histogram(String name, String... tags) {
        String key = buildKey(name, tags);
        return histograms.computeIfAbsent(key, Histogram::new);
    }

    // ==================== Timer ====================

    /**
     * 获取或创建计时器
     */
    public Timer timer(String name) {
        return timers.computeIfAbsent(name, Timer::new);
    }

    /**
     * 获取或创建带标签的计时器
     */
    public Timer timer(String name, String... tags) {
        String key = buildKey(name, tags);
        return timers.computeIfAbsent(key, Timer::new);
    }

    // ==================== Rate ====================

    /**
     * 获取或创建速率计
     */
    public Rate rate(String name) {
        return rates.computeIfAbsent(name, Rate::new);
    }

    /**
     * 获取或创建带标签的速率计
     */
    public Rate rate(String name, String... tags) {
        String key = buildKey(name, tags);
        return rates.computeIfAbsent(key, Rate::new);
    }

    // ==================== Query ====================

    /**
     * 获取指标快照（用于导出/上报）
     */
    public MetricSnapshot snapshot() {
        return new MetricSnapshot(
            Map.copyOf(counters),
            Map.copyOf(gauges),
            Map.copyOf(histograms),
            Map.copyOf(timers),
            Map.copyOf(rates)
        );
    }

    /**
     * 重置所有指标（仅用于测试）
     */
    public void reset() {
        counters.clear();
        gauges.clear();
        histograms.clear();
        timers.clear();
        rates.clear();
    }

    private static String buildKey(String name, String... tags) {
        if (tags == null || tags.length == 0) {
            return name;
        }
        StringBuilder sb = new StringBuilder(name);
        for (String tag : tags) {
            sb.append(',').append(tag);
        }
        return sb.toString();
    }

    // ==================== Metric Types ====================

    /**
     * 单调递增计数器 (线程安全)
     */
    public static final class Counter {
        private final String name;
        private final AtomicLong value = new AtomicLong(0);

        private Counter(String name) {
            this.name = name;
        }

        public void inc() {
            value.incrementAndGet();
        }

        public void inc(long delta) {
            if (delta < 0) {
                throw new IllegalArgumentException("Counter cannot decrease");
            }
            value.addAndGet(delta);
        }

        public long get() {
            return value.get();
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "Counter{name='" + name + "', value=" + value.get() + "}";
        }
    }

    /**
     * 瞬时值观测
     */
    public static final class Gauge<T extends Number> {
        private final String name;
        private final Supplier<T> supplier;

        private Gauge(Supplier<T> supplier) {
            this.name = "";
            this.supplier = supplier;
        }

        public T getValue() {
            return supplier != null ? supplier.get() : null;
        }

        public double getAsDouble() {
            T value = getValue();
            return value != null ? value.doubleValue() : 0;
        }

        public long getAsLong() {
            T value = getValue();
            return value != null ? value.longValue() : 0;
        }

        public int getAsInt() {
            T value = getValue();
            return value != null ? value.intValue() : 0;
        }

        @Override
        public String toString() {
            return "Gauge{name='" + name + "', value=" + getAsDouble() + "}";
        }
    }

    /**
     * 分布统计直方图
     * <p>
     * 使用 HdrHistogram 风格的定桶算法，支持高精度百分位数查询
     */
    public static final class Histogram {
        private final String name;
        // 使用简单的数组桶实现，避免外部依赖
        // 桶范围：1us 到 10s，共 64 个桶（指数增长）
        private static final int BUCKET_COUNT = 64;
        private static final double MIN_VALUE = 1.0; // 1 microsecond
        private static final double MAX_VALUE = 10_000_000.0; // 10 seconds
        private static final double MULTIPLIER = Math.pow(MAX_VALUE / MIN_VALUE, 1.0 / (BUCKET_COUNT - 1));
        // 预计算桶边界，避免热路径中的 log/pow 计算
        private static final double[] BUCKET_BOUNDARIES = new double[BUCKET_COUNT];

        static {
            for (int i = 0; i < BUCKET_COUNT; i++) {
                BUCKET_BOUNDARIES[i] = MIN_VALUE * Math.pow(MULTIPLIER, i);
            }
        }

        private final long[] buckets = new long[BUCKET_COUNT];
        private long count = 0;
        private double sum = 0;
        private double min = Double.MAX_VALUE;
        private double max = 0;

        private Histogram(String name) {
            this.name = name;
        }

        /**
         * 记录一个值（单位：微秒）
         * <p>
         * 由 tick 线程写入，而统计读取（getMean/percentile）发生在指标同步线程，
         * 因此所有读写方法必须同步。每 tick 仅记录一次，锁竞争可忽略。
         */
        public synchronized void record(double valueUs) {
            if (valueUs < 0) return;

            count++;
            sum += valueUs;
            if (valueUs < min) min = valueUs;
            if (valueUs > max) max = valueUs;

            int bucket = valueToBucket(valueUs);
            if (bucket >= 0 && bucket < BUCKET_COUNT) {
                buckets[bucket]++;
            }
        }

        /**
         * 记录纳秒值
         */
        public void recordNanos(long nanos) {
            record(nanos / 1000.0);
        }

        /**
         * 记录毫秒值
         */
        public void recordMillis(double millis) {
            record(millis * 1000.0);
        }

        private int valueToBucket(double value) {
            if (value <= MIN_VALUE) return 0;
            if (value >= MAX_VALUE) return BUCKET_COUNT - 1;
            // 二分查找预计算的边界，比 log/pow 更快
            int low = 0, high = BUCKET_COUNT - 1;
            while (low < high) {
                int mid = (low + high) >>> 1;
                if (BUCKET_BOUNDARIES[mid] < value) {
                    low = mid + 1;
                } else {
                    high = mid;
                }
            }
            return low;
        }

        public synchronized long getCount() {
            return count;
        }

        public synchronized double getSum() {
            return sum;
        }

        public synchronized double getMin() {
            return count > 0 ? min : 0;
        }

        public synchronized double getMax() {
            return max;
        }

        public synchronized double getMean() {
            return count > 0 ? sum / count : 0;
        }

        /**
         * 获取指定百分位数（单位：微秒）
         */
        public synchronized double percentile(double p) {
            if (count == 0) return 0;
            if (p <= 0) return getMin();
            if (p >= 100) return getMax();

            long target = (long) Math.ceil(count * p / 100.0);
            long accumulated = 0;

            for (int i = 0; i < BUCKET_COUNT; i++) {
                accumulated += buckets[i];
                if (accumulated >= target) {
                    return BUCKET_BOUNDARIES[i];
                }
            }
            return getMax();
        }

        public double p50() { return percentile(50); }
        public double p75() { return percentile(75); }
        public double p90() { return percentile(90); }
        public double p95() { return percentile(95); }
        public double p99() { return percentile(99); }
        public double p999() { return percentile(99.9); }

        @Override
        public String toString() {
            return String.format("Histogram{name='%s', count=%d, mean=%.2fµs, p50=%.2f, p95=%.2f, p99=%.2f, max=%.2f}",
                name, count, getMean(), p50(), p95(), p99(), getMax());
        }
    }

    /**
     * 计时器 - 专用于记录耗时
     */
    public static final class Timer {
        private final Histogram histogram;
        private final String name;

        private Timer(String name) {
            this.name = name;
            this.histogram = new Histogram(name + ".timer");
        }

        public void recordNanos(long nanos) {
            histogram.recordNanos(nanos);
        }

        public void recordMillis(double millis) {
            histogram.recordMillis(millis);
        }

        public void recordMicros(double micros) {
            histogram.record(micros);
        }

        /**
         * 执行代码块并记录耗时
         */
        public <T> T time(java.util.function.Supplier<T> supplier) {
            long start = System.nanoTime();
            try {
                return supplier.get();
            } finally {
                recordNanos(System.nanoTime() - start);
            }
        }

        /**
         * 执行代码块并记录耗时
         */
        public void time(Runnable runnable) {
            long start = System.nanoTime();
            try {
                runnable.run();
            } finally {
                recordNanos(System.nanoTime() - start);
            }
        }

        public Histogram getHistogram() {
            return histogram;
        }

        // 委托方法
        public long getCount() { return histogram.getCount(); }
        public double getMean() { return histogram.getMean(); }
        public double getMin() { return histogram.getMin(); }
        public double getMax() { return histogram.getMax(); }
        public double p50() { return histogram.p50(); }
        public double p95() { return histogram.p95(); }
        public double p99() { return histogram.p99(); }

        @Override
        public String toString() {
            return "Timer{name='" + name + "', " + histogram + "}";
        }
    }

    /**
     * 速率计 - 单位时间内的事件数 (滑动窗口)
     */
    public static final class Rate {
        private final String name;
        private final AtomicLong count = new AtomicLong(0);
        private long lastTickCount = 0;
        private long lastTickTime = System.nanoTime();

        private Rate(String name) {
            this.name = name;
        }

        public void mark() {
            count.incrementAndGet();
        }

        public void mark(long n) {
            count.addAndGet(n);
        }

        /**
         * 获取当前速率（每秒）
         */
        public synchronized double getRate() {
            long now = System.nanoTime();
            long elapsedNanos = now - lastTickTime;
            if (elapsedNanos <= 0) return 0;

            long currentCount = count.get();
            long deltaCount = currentCount - lastTickCount;
            double rate = (deltaCount * 1_000_000_000.0) / elapsedNanos;

            lastTickCount = currentCount;
            lastTickTime = now;

            return rate;
        }

        public long getCount() {
            return count.get();
        }

        @Override
        public String toString() {
            return String.format("Rate{name='%s', count=%d, rate=%.2f/s}", name, count.get(), getRate());
        }
    }

    /**
     * 指标快照 - 用于导出
     */
    public record MetricSnapshot(
        Map<String, Counter> counters,
        Map<String, Gauge<?>> gauges,
        Map<String, Histogram> histograms,
        Map<String, Timer> timers,
        Map<String, Rate> rates
    ) {
        @Override
        public String toString() {
            return String.format("MetricSnapshot{counters=%d, gauges=%d, histograms=%d, timers=%d, rates=%d}",
                counters.size(), gauges.size(), histograms.size(), timers.size(), rates.size());
        }
    }
}
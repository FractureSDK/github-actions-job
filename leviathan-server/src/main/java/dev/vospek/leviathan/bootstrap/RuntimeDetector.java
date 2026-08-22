package dev.vospek.leviathan.bootstrap;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Java Runtime 检测器 - 启动时收集运行时基线信息
 * 对应 Phase 0-B: P0-004
 */
public final class RuntimeDetector {

    private static final Logger LOGGER = LogManager.getLogger(RuntimeDetector.class);

    private RuntimeDetector() {
    }

    /**
     * 检测并记录运行时基线信息
     */
    public static RuntimeInfo detect() {
        RuntimeInfo info = new RuntimeInfo();

        // Java 版本信息
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        info.javaVersion = System.getProperty("java.version");
        info.javaVendor = System.getProperty("java.vendor");
        info.javaHome = System.getProperty("java.home");
        info.vmName = System.getProperty("java.vm.name");
        info.vmVersion = System.getProperty("java.vm.version");
        info.vmVendor = System.getProperty("java.vm.vendor");
        info.runtimeName = runtimeMXBean.getName();
        info.runtimeStartTime = runtimeMXBean.getStartTime();
        info.runtimeUptime = runtimeMXBean.getUptime();
        info.inputArguments = runtimeMXBean.getInputArguments();

        // 操作系统信息
        info.osName = System.getProperty("os.name");
        info.osVersion = System.getProperty("os.version");
        info.osArch = System.getProperty("os.arch");

        // 架构信息
        info.architecture = System.getProperty("os.arch");

        // CPU 信息
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        info.availableProcessors = Runtime.getRuntime().availableProcessors();
        info.systemLoadAverage = osBean.getSystemLoadAverage();

        // 内存信息
        info.maxMemory = Runtime.getRuntime().maxMemory();
        info.totalMemory = Runtime.getRuntime().totalMemory();
        info.freeMemory = Runtime.getRuntime().freeMemory();

        // Kernel 信息 (Linux)
        info.kernelVersion = getKernelVersion();

        // Native Access 支持
        info.nativeAccess = checkNativeAccess();

        // Preview 支持
        info.previewFeatures = checkPreviewSupport();

        // 记录检测结果
        logRuntimeInfo(info);

        return info;
    }

    private static String getKernelVersion() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"uname", "-r"});
            process.waitFor();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            String kernel = reader.readLine();
            reader.close();
            return kernel != null ? kernel.trim() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static boolean checkNativeAccess() {
        // 检查是否支持 JNI/FFM 等原生访问
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            return unsafeClass != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean checkPreviewSupport() {
        String version = System.getProperty("java.version");
        // JDK 12+ 支持 preview features
        try {
            int major = parseMajorVersion(version);
            return major >= 12;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static int parseMajorVersion(String version) {
        // 处理类似 "1.8.0_292", "11.0.12", "17.0.5", "21.0.1" 等格式
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.split("\\.")[1]);
        }
        return Integer.parseInt(version.split("\\.")[0]);
    }

    private static void logRuntimeInfo(RuntimeInfo info) {
        LOGGER.info("━━━━━━━━━━━━━ Leviathan Runtime Detection ━━━━━━━━━━━━━");
        LOGGER.info("Java Version:      {}", info.javaVersion);
        LOGGER.info("Java Vendor:       {}", info.javaVendor);
        LOGGER.info("Java Home:         {}", info.javaHome);
        LOGGER.info("VM Name:           {}", info.vmName);
        LOGGER.info("VM Version:        {}", info.vmVersion);
        LOGGER.info("VM Vendor:         {}", info.vmVendor);
        LOGGER.info("OS:                {} {} ({})", info.osName, info.osVersion, info.osArch);
        LOGGER.info("Architecture:      {}", info.architecture);
        LOGGER.info("Available CPUs:    {}", info.availableProcessors);
        LOGGER.info("System Load Avg:   {}", info.systemLoadAverage);
        LOGGER.info("Max Memory:        {} MB", info.maxMemory / 1024 / 1024);
        LOGGER.info("Kernel Version:    {}", info.kernelVersion);
        LOGGER.info("Native Access:     {}", info.nativeAccess ? "available" : "unavailable");
        LOGGER.info("Preview Features:  {}", info.previewFeatures ? "supported" : "not supported");
        LOGGER.info("JVM Arguments:     {}", String.join(" ", info.inputArguments));
        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    /**
     * 运行时基线信息容器
     */
    public static final class RuntimeInfo {
        public String javaVersion;
        public String javaVendor;
        public String javaHome;
        public String vmName;
        public String vmVersion;
        public String vmVendor;
        public String runtimeName;
        public long runtimeStartTime;
        public long runtimeUptime;
        public List<String> inputArguments;

        public String osName;
        public String osVersion;
        public String osArch;
        public String architecture;

        public int availableProcessors;
        public double systemLoadAverage;

        public long maxMemory;
        public long totalMemory;
        public long freeMemory;

        public String kernelVersion;
        public boolean nativeAccess;
        public boolean previewFeatures;

        // 硬件能力 (由 HardwareCapabilities 填充)
        public HardwareCapabilities hardware;
    }
}
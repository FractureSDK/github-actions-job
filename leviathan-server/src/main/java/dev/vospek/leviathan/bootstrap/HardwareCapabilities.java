package dev.vospek.leviathan.bootstrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 硬件能力探测器 - 检测 CPU/SIMD/内存/OS 等硬件特性
 * 对应 Phase 0-B: P0-005
 * <p>
 * 后续 Patch 通过 {@link Capabilities} 统一访问，避免重复探测
 */
public final class HardwareCapabilities {

    private static final Logger LOGGER = LogManager.getLogger(HardwareCapabilities.class);

    private HardwareCapabilities() {
    }

    // CPU 能力
    public int logicalProcessors;
    public int physicalProcessors;
    public boolean hasSIMD;
    public boolean hasAVX2;
    public boolean hasAVX512;
    public String cpuVendor;
    public String cpuModel;
    public List<String> cpuFlags;

    // 内存能力
    public long physicalMemoryBytes;
    public long maxHeapBytes;
    public long directMemoryBytes;

    // OS 能力
    public boolean isLinux;
    public String linuxDistro;
    public String kernelVersion;
    public String filesystemType;

    /**
     * 探测所有硬件能力
     */
    public static HardwareCapabilities detect() {
        HardwareCapabilities caps = new HardwareCapabilities();

        // CPU 探测
        detectCPU(caps);

        // 内存探测
        detectMemory(caps);

        // OS 探测
        detectOS(caps);

        // 记录探测结果
        logCapabilities(caps);

        return caps;
    }

    private static void detectCPU(HardwareCapabilities caps) {
        caps.logicalProcessors = Runtime.getRuntime().availableProcessors();

        // 尝试从 /proc/cpuinfo 获取详细信息
        if (isLinux()) {
            detectCPUFromProcCpuinfo(caps);
        } else {
            // 非 Linux 环境回退
            caps.physicalProcessors = caps.logicalProcessors;
            String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
            caps.cpuVendor = arch.contains("x86") ? "x86" : "unknown";
            caps.cpuModel = System.getProperty("os.arch");
            caps.cpuFlags = List.of();
            // 基础 SIMD 假设
            caps.hasSIMD = true;
            caps.hasAVX2 = checkAVX2Support();
            caps.hasAVX512 = false;
        }
    }

    private static void detectCPUFromProcCpuinfo(HardwareCapabilities caps) {
        File cpuinfo = new File("/proc/cpuinfo");
        if (!cpuinfo.exists() || !cpuinfo.canRead()) {
            caps.physicalProcessors = caps.logicalProcessors;
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(cpuinfo))) {
            String line;
            int physicalIds = 0;
            int coreIds = 0;
            String lastPhysicalId = "";
            String lastCoreId = "";

            caps.cpuFlags = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("vendor_id")) {
                    caps.cpuVendor = line.split(":")[1].trim().toLowerCase(Locale.ROOT);
                } else if (line.startsWith("model name")) {
                    if (caps.cpuModel == null) {
                        caps.cpuModel = line.split(":")[1].trim();
                    }
                } else if (line.startsWith("flags")) {
                    String flagsStr = line.split(":")[1].trim();
                    for (String flag : flagsStr.split("\\s+")) {
                        caps.cpuFlags.add(flag);
                    }
                } else if (line.startsWith("physical id")) {
                    String pid = line.split(":")[1].trim();
                    if (!pid.equals(lastPhysicalId)) {
                        physicalIds++;
                        lastPhysicalId = pid;
                    }
                } else if (line.startsWith("core id")) {
                    String cid = line.split(":")[1].trim();
                    if (!cid.equals(lastCoreId)) {
                        coreIds++;
                        lastCoreId = cid;
                    }
                }
            }

            caps.physicalProcessors = Math.max(physicalIds, 1);
            if (coreIds > 0) {
                // 每个物理 CPU 的核心数
            }

            // 解析 SIMD 标志
            caps.hasSIMD = caps.cpuFlags.stream()
                .anyMatch(f -> f.equals("sse") || f.equals("sse2"));
            caps.hasAVX2 = caps.cpuFlags.contains("avx2");
            caps.hasAVX512 = caps.cpuFlags.stream().anyMatch(f -> f.startsWith("avx512"));

        } catch (IOException e) {
            LOGGER.warn("Failed to read /proc/cpuinfo: {}", e.getMessage());
            caps.physicalProcessors = caps.logicalProcessors;
            caps.cpuVendor = "unknown";
            caps.cpuModel = "unknown";
            caps.cpuFlags = List.of();
            caps.hasSIMD = true;
            caps.hasAVX2 = checkAVX2Support();
            caps.hasAVX512 = false;
        }
    }

    private static boolean checkAVX2Support() {
        // 运行时检查 JVM 是否支持 AVX2 相关内部优化
        // 这里简单返回 true 表示现代 x86_64 CPU 通常支持
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
        return arch.contains("x86") || arch.contains("amd64");
    }

    private static void detectMemory(HardwareCapabilities caps) {
        caps.maxHeapBytes = Runtime.getRuntime().maxMemory();

        // 直接内存估算
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            java.lang.reflect.Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            java.lang.reflect.Method maxDirectMemory = unsafeClass.getMethod("maxDirectMemory");
            caps.directMemoryBytes = (long) maxDirectMemory.invoke(unsafe);
        } catch (Exception e) {
            // 回退：使用 MaxDirectMemorySize 参数或默认值
            caps.directMemoryBytes = getMaxDirectMemoryFromVM();
        }

        // 物理内存 (Linux)
        if (isLinux()) {
            detectPhysicalMemoryLinux(caps);
        } else {
            caps.physicalMemoryBytes = -1; // unknown
        }
    }

    private static long getMaxDirectMemoryFromVM() {
        // 尝试从 VM 参数获取
        List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : args) {
            if (arg.startsWith("-XX:MaxDirectMemorySize=")) {
                String value = arg.substring("-XX:MaxDirectMemorySize=".length());
                return parseMemorySize(value);
            }
        }
        // 默认等于 max heap
        return Runtime.getRuntime().maxMemory();
    }

    private static long parseMemorySize(String value) {
        value = value.trim().toUpperCase(Locale.ROOT);
        long multiplier = 1;
        if (value.endsWith("K")) {
            multiplier = 1024;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("M")) {
            multiplier = 1024 * 1024;
            value = value.substring(0, value.length() - 1);
        } else if (value.endsWith("G")) {
            multiplier = 1024L * 1024 * 1024;
            value = value.substring(0, value.length() - 1);
        }
        try {
            return Long.parseLong(value) * multiplier;
        } catch (NumberFormatException e) {
            return Runtime.getRuntime().maxMemory();
        }
    }

    private static void detectPhysicalMemoryLinux(HardwareCapabilities caps) {
        File meminfo = new File("/proc/meminfo");
        if (!meminfo.exists() || !meminfo.canRead()) {
            caps.physicalMemoryBytes = -1;
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(meminfo))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("MemTotal:")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2) {
                        long kb = Long.parseLong(parts[1]);
                        caps.physicalMemoryBytes = kb * 1024L;
                    }
                    break;
                }
            }
        } catch (IOException | NumberFormatException e) {
            LOGGER.warn("Failed to read /proc/meminfo: {}", e.getMessage());
            caps.physicalMemoryBytes = -1;
        }
    }

    private static void detectOS(HardwareCapabilities caps) {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        caps.isLinux = osName.contains("linux");

        if (caps.isLinux) {
            caps.kernelVersion = getKernelVersion();
            caps.linuxDistro = detectLinuxDistro();
            caps.filesystemType = detectFilesystemType();
        } else {
            caps.kernelVersion = osName + " " + System.getProperty("os.version");
            caps.linuxDistro = "N/A";
            caps.filesystemType = "N/A";
        }
    }

    private static String getKernelVersion() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"uname", "-r"});
            process.waitFor();
            try (BufferedReader reader = new BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
                String kernel = reader.readLine();
                return kernel != null ? kernel.trim() : "unknown";
            }
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String detectLinuxDistro() {
        File osRelease = new File("/etc/os-release");
        if (!osRelease.exists() || !osRelease.canRead()) {
            return "unknown";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(osRelease))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("PRETTY_NAME=")) {
                    return line.split("=")[1].replace("\"", "").trim();
                }
                if (line.startsWith("NAME=")) {
                    return line.split("=")[1].replace("\"", "").trim();
                }
            }
        } catch (IOException e) {
            return "unknown";
        }
        return "unknown";
    }

    private static String detectFilesystemType() {
        try {
            Path root = Path.of("/");
            return Files.getFileStore(root).type();
        } catch (IOException e) {
            return "unknown";
        }
    }

    /**
     * 检查是否为 Linux 系统
     */
    public static boolean isLinux() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        return osName.contains("linux");
    }

    private static void logCapabilities(HardwareCapabilities caps) {
        LOGGER.info("━━━━━━━━━━━━━ Leviathan Hardware Capabilities ━━━━━━━━━━━━━");
        LOGGER.info("Logical CPUs:      {}", caps.logicalProcessors);
        LOGGER.info("Physical CPUs:     {}", caps.physicalProcessors);
        LOGGER.info("CPU Vendor:        {}", caps.cpuVendor);
        LOGGER.info("CPU Model:         {}", caps.cpuModel);
        LOGGER.info("SIMD Support:      {}", caps.hasSIMD ? "yes" : "no");
        LOGGER.info("AVX2 Support:      {}", caps.hasAVX2 ? "yes" : "no");
        LOGGER.info("AVX-512 Support:   {}", caps.hasAVX512 ? "yes" : "no");
        LOGGER.info("CPU Flags:         {}",
            caps.cpuFlags != null ? caps.cpuFlags.size() + " flags" : "unavailable");
        LOGGER.info("Physical Memory:   {}",
            caps.physicalMemoryBytes > 0 ? formatBytes(caps.physicalMemoryBytes) : "unknown");
        LOGGER.info("Max Heap:          {}", formatBytes(caps.maxHeapBytes));
        LOGGER.info("Direct Memory:     {}", formatBytes(caps.directMemoryBytes));
        LOGGER.info("OS:                {}",
            caps.isLinux ? "Linux (" + caps.linuxDistro + ")" : System.getProperty("os.name"));
        LOGGER.info("Kernel:            {}", caps.kernelVersion);
        LOGGER.info("Filesystem:        {}", caps.filesystemType);
        LOGGER.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "unknown";
        }
        if (bytes >= 1024L * 1024 * 1024) {
            return String.format(Locale.ROOT, "%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
        if (bytes >= 1024 * 1024) {
            return String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024));
        }
        if (bytes >= 1024) {
            return String.format(Locale.ROOT, "%.2f KB", bytes / 1024.0);
        }
        return bytes + " B";
    }

    /**
     * 统一访问入口 - 后续 Patch 使用此 API
     * 示例: Capabilities.cpu().avx2()
     */
    public static final class Capabilities {
        private static HardwareCapabilities instance;

        public static synchronized HardwareCapabilities cpu() {
            if (instance == null) {
                instance = detect();
            }
            return instance;
        }

        public static synchronized HardwareCapabilities get() {
            return cpu();
        }

        public static synchronized void resetForTesting() {
            instance = null;
        }
    }
}
package de.lostesburger.mySqlPlayerBridge.Utils;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本检测工具类
 * 用于检测服务器版本并提供版本相关的工具方法
 */
public class VersionUtils {

    private static String nmsVersion = null;
    private static int majorVersion = 0;
    private static int minorVersion = 0;
    private static int patchVersion = 0;

    static {
        detectVersion();
    }

    /**
     * 检测服务器版本
     */
    private static void detectVersion() {
        String version = Bukkit.getVersion();

        // 解析版本号，例如: "git-Paper-123 (MC: 1.20.4)"
        Pattern pattern = Pattern.compile("\\(MC: (\\d+)\\.(\\d+)(?:\\.(\\d+))?\\)");
        Matcher matcher = pattern.matcher(version);

        if (matcher.find()) {
            majorVersion = Integer.parseInt(matcher.group(1));
            minorVersion = Integer.parseInt(matcher.group(2));
            String patch = matcher.group(3);
            patchVersion = patch != null ? Integer.parseInt(patch) : 0;
        }

        // 获取 NMS 版本
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            nmsVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
        } catch (Exception e) {
            // 1.17+ 可能没有版本化的包名
            nmsVersion = "unknown";
        }
    }

    /**
     * 获取 NMS 版本字符串（例如: v1_20_R3）
     */
    public static String getNMSVersion() {
        return nmsVersion;
    }

    /**
     * 获取主版本号（例如: 1）
     */
    public static int getMajorVersion() {
        return majorVersion;
    }

    /**
     * 获取次版本号（例如: 20）
     */
    public static int getMinorVersion() {
        return minorVersion;
    }

    /**
     * 获取补丁版本号（例如: 4）
     */
    public static int getPatchVersion() {
        return patchVersion;
    }

    /**
     * 检查是否为指定版本或更高
     * @param major 主版本号
     * @param minor 次版本号
     * @return 如果当前版本 >= 指定版本返回 true
     */
    public static boolean isVersionOrHigher(int major, int minor) {
        if (majorVersion > major) {
            return true;
        } else if (majorVersion == major) {
            return minorVersion >= minor;
        }
        return false;
    }

    /**
     * 检查是否在版本范围内
     * @param minMajor 最小主版本
     * @param minMinor 最小次版本
     * @param maxMajor 最大主版本
     * @param maxMinor 最大次版本
     * @return 如果在范围内返回 true
     */
    public static boolean isVersionInRange(int minMajor, int minMinor, int maxMajor, int maxMinor) {
        return isVersionOrHigher(minMajor, minMinor) && !isVersionOrHigher(maxMajor, maxMinor + 1);
    }

    /**
     * 获取版本字符串（例如: "1.20.4"）
     */
    public static String getVersionString() {
        return majorVersion + "." + minorVersion + (patchVersion > 0 ? "." + patchVersion : "");
    }

    /**
     * 检查是否支持 PDC (PersistentDataContainer)
     * 1.14+ 支持
     */
    public static boolean supportsPDC() {
        return isVersionOrHigher(1, 14);
    }

    /**
     * 检查是否支持副手
     * 1.9+ 支持
     */
    public static boolean supportsOffhand() {
        return isVersionOrHigher(1, 9);
    }

    /**
     * 检查是否为 Paper 服务器
     */
    public static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * 检查是否为 Folia 服务器
     */
    public static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

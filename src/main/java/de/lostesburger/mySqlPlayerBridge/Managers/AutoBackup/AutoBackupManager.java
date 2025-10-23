package de.lostesburger.mySqlPlayerBridge.Managers.AutoBackup;

import de.lostesburger.mySqlPlayerBridge.Commands.ExportCommand.ExportCommand;
import de.lostesburger.mySqlPlayerBridge.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class AutoBackupManager {
    private BukkitTask backupTask;
    private long lastBackupTime = 0;
    private final Main plugin;
    
    public AutoBackupManager(Main plugin) {
        this.plugin = plugin;
    }
    
    public void startAutoBackup() {
        FileConfiguration config = Main.config;
        boolean enabled = config.getBoolean("auto-backup.enabled", false);
        int interval = config.getInt("auto-backup.interval", 60);
        
        // 如果任务已经在运行，先取消它
        stopAutoBackup();
        
        if (enabled) {
            // 将分钟转换为ticks（1分钟 = 1200 ticks）
            long intervalTicks = interval * 1200L;
            
            backupTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                performBackup(Bukkit.getConsoleSender());
            }, intervalTicks, intervalTicks);
            
            plugin.getLogger().info("自动备份已启用，间隔: " + interval + " 分钟");
        } else {
            plugin.getLogger().info("自动备份未启用");
        }
    }
    
    public void stopAutoBackup() {
        if (backupTask != null) {
            backupTask.cancel();
            backupTask = null;
            plugin.getLogger().info("自动备份任务已停止");
        }
    }
    
    public void performBackup(CommandSender sender) {
        try {
            lastBackupTime = System.currentTimeMillis();
            plugin.getLogger().info("开始执行自动数据库备份...");
            
            // 执行备份
            ExportCommand exportCommand = new ExportCommand();
            
            // 创建备份目录
            File backupDir = new File(plugin.getDataFolder(), "backup");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // 执行备份逻辑
            exportCommand.execute(sender, new String[0]);
            
            // 清理旧备份
            cleanupOldBackups();
            
            plugin.getLogger().info("自动数据库备份完成");
        } catch (Exception e) {
            plugin.getLogger().severe("自动备份失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void cleanupOldBackups() {
        try {
            File backupDir = new File(plugin.getDataFolder(), "backup");
            if (!backupDir.exists()) {
                return;
            }
            
            int keepCount = Main.config.getInt("auto-backup.keep-count", 10);
            if (keepCount <= 0) {
                return; // 不限制备份文件数量
            }
            
            // 获取所有备份文件
            File[] backupFiles = backupDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("backup_") && name.endsWith(".sql");
                }
            });
            
            if (backupFiles == null || backupFiles.length <= keepCount) {
                return;
            }
            
            // 按修改时间排序，最旧的在前面
            Arrays.sort(backupFiles, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            
            // 删除多余的备份文件
            int toDelete = backupFiles.length - keepCount;
            for (int i = 0; i < toDelete; i++) {
                if (backupFiles[i].delete()) {
                    plugin.getLogger().info("已删除旧备份文件: " + backupFiles[i].getName());
                } else {
                    plugin.getLogger().warning("无法删除旧备份文件: " + backupFiles[i].getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("清理旧备份文件时出错: " + e.getMessage());
        }
    }
    
    public String getLastBackupTime() {
        if (lastBackupTime == 0) {
            return "从未";
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(lastBackupTime));
    }
    
    public boolean isAutoBackupEnabled() {
        return backupTask != null;
    }
}
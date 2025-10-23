package de.lostesburger.mySqlPlayerBridge.Commands.BackupCommand;

import de.lostesburger.mySqlPlayerBridge.Commands.CommandInterface;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Managers.AutoBackup.AutoBackupManager;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class BackupCommand implements CommandInterface {

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (args.length < 1) {
            sendUsage(commandSender);
            return;
        }

        Main mainInstance = (Main) Main.getInstance();
        AutoBackupManager backupManager = mainInstance.getAutoBackupManager();
        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                handleStart(commandSender, backupManager);
                break;
            case "stop":
                handleStop(commandSender, backupManager);
                break;
            case "status":
                handleStatus(commandSender, backupManager);
                break;
            case "now":
                handleBackupNow(commandSender, backupManager);
                break;
            default:
                sendUsage(commandSender);
                break;
        }
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "stop", "status", "now");
        }
        return Arrays.asList();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage("§7------- §9MySqlPlayerBridge 备份命令 §7-------");
        sender.sendMessage("§9/mpb backup start §7- 启用自动备份");
        sender.sendMessage("§9/mpb backup stop §7- 停止自动备份");
        sender.sendMessage("§9/mpb backup status §7- 查看自动备份状态");
        sender.sendMessage("§9/mpb backup now §7- 立即执行备份");
    }

    private void handleStart(CommandSender sender, AutoBackupManager backupManager) {
        Main.config.set("auto-backup.enabled", true);
        backupManager.startAutoBackup();
        sender.sendMessage(Chat.getMessage("backup-started"));
    }

    private void handleStop(CommandSender sender, AutoBackupManager backupManager) {
        Main.config.set("auto-backup.enabled", false);
        backupManager.stopAutoBackup();
        sender.sendMessage(Chat.getMessage("backup-stopped"));
    }

    private void handleStatus(CommandSender sender, AutoBackupManager backupManager) {
        boolean enabled = Main.config.getBoolean("auto-backup.enabled", false);
        int interval = Main.config.getInt("auto-backup.interval", 60);
        int keepCount = Main.config.getInt("auto-backup.keep-count", 10);
        
        sender.sendMessage("§7------- §9自动备份状态 §7-------");
        sender.sendMessage("§7状态: " + (enabled ? "§a已启用" : "§c已禁用"));
        if (enabled) {
            sender.sendMessage("§7间隔: §f" + interval + " 分钟");
            sender.sendMessage("§7保留数量: §f" + keepCount);
            sender.sendMessage("§7上次备份: §f" + backupManager.getLastBackupTime());
        }
    }

    private void handleBackupNow(CommandSender sender, AutoBackupManager backupManager) {
        sender.sendMessage(Chat.getMessage("backup-starting-now"));
        backupManager.performBackup(sender);
    }
}
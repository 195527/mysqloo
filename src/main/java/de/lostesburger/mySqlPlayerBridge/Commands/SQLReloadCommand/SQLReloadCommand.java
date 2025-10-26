package de.lostesburger.mySqlPlayerBridge.Commands.SQLReloadCommand;

import de.lostesburger.mySqlPlayerBridge.Commands.CommandInterface;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Storage.MySQLStorageManager;
import de.lostesburger.mySqlPlayerBridge.Storage.SQLiteStorageManager;
import de.lostesburger.mySqlPlayerBridge.Storage.StorageManager;
import de.lostesburger.mySqlPlayerBridge.Storage.YAMLStorageManager;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.util.List;

public class SQLReloadCommand implements CommandInterface {
    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission(Main.config.getString("settings.admin-permission"))) {
            commandSender.sendMessage(Chat.getMessage("permission-error"));
            return;
        }

        try {
            // 先保存所有在线玩家数据
            if (Main.storageManager != null) {
                Main.storageManager.closeConnection();
            }

            // 重新初始化存储管理器
            String storageType = Main.config.getString("storage-type", "mysql").toLowerCase();
            
            switch (storageType) {
                case "mysql":
                    Main.mySqlConnectionHandler = new de.lostesburger.mySqlPlayerBridge.Handlers.MySqlConnection.MySqlConnectionHandler(
                            Main.mysqlConf.getString("host"),
                            Main.mysqlConf.getInt("port"),
                            Main.mysqlConf.getString("database"),
                            Main.mysqlConf.getString("user"),
                            Main.mysqlConf.getString("password")
                    );
                    Main.storageManager = new MySQLStorageManager(Main.mySqlConnectionHandler.getManager());
                    break;
                    
                case "sqlite":
                    String dbPath = Main.getInstance().getDataFolder().getAbsolutePath() + File.separator + "database.db";
                    Main.storageManager = new SQLiteStorageManager(dbPath);
                    break;
                    
                case "yaml":
                    Main.storageManager = new YAMLStorageManager(Main.getInstance().getDataFolder());
                    break;
                    
                default:
                    commandSender.sendMessage("§c不支持的存储类型: " + storageType);
                    return;
            }
            
            commandSender.sendMessage(Chat.getMessage("sql-reload-success").replace("{type}", storageType));
            Main.getInstance().getLogger().info("存储管理器已重新加载，当前类型: " + storageType);

        } catch (Exception e) {
            commandSender.sendMessage("§c重新加载存储管理器时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] args) {
        return List.of();
    }
}
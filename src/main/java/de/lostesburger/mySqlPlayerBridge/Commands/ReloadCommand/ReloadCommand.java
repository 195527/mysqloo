package de.lostesburger.mySqlPlayerBridge.Commands.ReloadCommand;

import de.lostesburger.mySqlPlayerBridge.Commands.CommandInterface;
import de.lostesburger.mySqlPlayerBridge.Config.SimpleConfig;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Managers.Modules.ModulesManager;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class ReloadCommand implements CommandInterface {
    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission(Main.config.getString("settings.admin-permission"))) {
            commandSender.sendMessage(Chat.getMessage("permission-error"));
            return;
        }

        try {
            // 重新加载配置文件
            SimpleConfig ymlConfig = new SimpleConfig((Main) Main.getInstance(), "config.yml");
            Main.config = ymlConfig.getConfig();
            Main.prefix = Main.config.getString("prefix");

            SimpleConfig ymlConfigMySQL = new SimpleConfig((Main) Main.getInstance(), "mysql.yml");
            Main.mysqlConf = ymlConfigMySQL.getConfig();
            Main.INVENTORY_TABLE_NAME = Main.mysqlConf.getString("inventory-table-name");
            Main.ENDERCHEST_TABLE_NAME = Main.mysqlConf.getString("enderchest-table-name");
            Main.ECONOMY_TABLE_NAME = Main.mysqlConf.getString("economy-table-name");
            Main.EXPERIENCE_TABLE_NAME = Main.mysqlConf.getString("experience-table-name");
            Main.HEALTH_FOOD_AIR_TABLE_NAME = Main.mysqlConf.getString("health-food-air-table-name");
            Main.POTION_EFFECTS_TABLE_NAME = Main.mysqlConf.getString("potion-effects-table-name");

            SimpleConfig ymlConfigMessages = new SimpleConfig((Main) Main.getInstance(), "messages.yml");
            Main.messages = ymlConfigMessages.getConfig();

            // 重新初始化模块管理器
            Main.modulesManager = new ModulesManager();

            // 重新初始化存储管理器
            ((Main) Main.getInstance()).initializeStorageManager();

            // 重新保存配置
            try {
                Main.config.set("version", Main.version);
                Main.config.save(new File(((Main) Main.getInstance()).getDataFolder(), "config.yml"));
            } catch (IOException ignored) {
            }

            commandSender.sendMessage(Chat.getMessage("reload-success"));
            Main.getInstance().getLogger().log(Level.INFO, "配置文件已重新加载 by " + commandSender.getName());

        } catch (Exception e) {
            commandSender.sendMessage("§c重新加载配置时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] args) {
        return List.of();
    }
}
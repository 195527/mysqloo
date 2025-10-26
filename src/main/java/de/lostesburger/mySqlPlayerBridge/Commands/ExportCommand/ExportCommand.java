package de.lostesburger.mySqlPlayerBridge.Commands.ExportCommand;

import de.lostesburger.mySqlPlayerBridge.Commands.CommandInterface;
import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Managers.MySqlData.MySqlDataManager;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ExportCommand implements CommandInterface {

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        execute(commandSender, args, null);
    }
    
    public void execute(CommandSender commandSender, String[] args, File backupDir) {
        commandSender.sendMessage(Chat.getMessage("export-starting"));

        // 在异步线程中执行导出操作，避免阻塞主线程
        Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                exportDatabase(commandSender, backupDir);
            } catch (Exception e) {
                commandSender.sendMessage(Chat.getMessage("export-failed").replace("{error}", e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void exportDatabase(CommandSender commandSender) {
        exportDatabase(commandSender, null);
    }
    
    private void exportDatabase(CommandSender commandSender, File backupDir) {
        // 检查是否使用MySQL存储
        if (Main.mySqlConnectionHandler == null) {
            commandSender.sendMessage(Chat.getMessage("export-failed").replace("{error}", "导出功能仅支持MySQL存储模式"));
            return;
        }
        
        MySqlDataManager dataManager = Main.mySqlConnectionHandler.getMySqlDataManager();
        Connection connection = null;
        try {
            // 修复：使用正确的存储管理器获取连接
            if (Main.storageManager != null && Main.storageManager instanceof de.lostesburger.mySqlPlayerBridge.Storage.MySQLStorageManager) {
                // 如果是MySQL存储，尝试获取连接
                connection = ((de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager) 
                    ((de.lostesburger.mySqlPlayerBridge.Storage.MySQLStorageManager) Main.storageManager).getClass()
                    .getDeclaredField("databaseManager").get(Main.storageManager))
                    .getConnection();
            } else {
                // 默认方式获取连接
                java.lang.reflect.Field databaseManagerField = dataManager.getClass().getDeclaredField("storageManager");
                databaseManagerField.setAccessible(true);
                DatabaseManager databaseManager = (DatabaseManager) databaseManagerField.get(dataManager);
                connection = databaseManager.getConnection();
            }
        } catch (Exception e) {
            commandSender.sendMessage(Chat.getMessage("export-failed").replace("{error}", "数据库连接失败: " + e.getMessage()));
            e.printStackTrace();
            return;
        }
        
        try {
            // 确定备份目录
            if (backupDir == null) {
                backupDir = new File(Main.getInstance().getDataFolder(), "backup");
            }
            
            // 创建备份目录
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            
            // 创建备份文件
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            File backupFile = new File(backupDir, "backup_" + timestamp + ".sql");
            
            FileWriter writer = new FileWriter(backupFile);
            
            // 写入SQL文件头部信息
            writer.write("-- MySqlPlayerBridge 数据库备份\n");
            writer.write("-- 创建时间: " + new Date() + "\n");
            writer.write("-- 多表结构\n\n");
            
            // 写入创建表的语句
            writeTableStructure(writer, Main.INVENTORY_TABLE_NAME, 
                "  `id` int(0) UNSIGNED NOT NULL AUTO_INCREMENT,\n" +
                "  `player_uuid` char(36) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,\n" +
                "  `player_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                "  `inventory` longtext CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,\n" +
                "  `armor` text CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,\n" +
                "  `hotbar_slot` int(0) NOT NULL,\n" +
                "  `gamemode` int(0) NOT NULL,\n" +
                "  `sync_complete` varchar(5) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,\n" +
                "  `last_seen` char(13) CHARACTER SET latin1 COLLATE latin1_swedish_ci NOT NULL,\n" +
                "  PRIMARY KEY (`id`) USING BTREE,\n" +
                "  UNIQUE INDEX `player_uuid`(`player_uuid`) USING BTREE");
            
            writeTableStructure(writer, Main.ENDERCHEST_TABLE_NAME,
                "  `id` int(0) UNSIGNED NOT NULL AUTO_INCREMENT,\n" +
                "  `player_uuid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `player_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                "  `enderchest` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `sync_complete` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `last_seen` char(13) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  PRIMARY KEY (`id`) USING BTREE,\n" +
                "  UNIQUE INDEX `player_uuid`(`player_uuid`) USING BTREE");
            
            writeTableStructure(writer, Main.ECONOMY_TABLE_NAME,
                "  `id` int(0) UNSIGNED NOT NULL AUTO_INCREMENT,\n" +
                "  `player_uuid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `player_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                "  `money` double(30, 2) NOT NULL,\n" +
                "  `offline_money` double(30, 2) NOT NULL,\n" +
                "  `sync_complete` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `last_seen` char(13) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  PRIMARY KEY (`id`) USING BTREE,\n" +
                "  UNIQUE INDEX `player_uuid`(`player_uuid`) USING BTREE");
            
            writeTableStructure(writer, Main.EXPERIENCE_TABLE_NAME,
                "  `id` int(0) UNSIGNED NOT NULL AUTO_INCREMENT,\n" +
                "  `player_uuid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `player_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                "  `exp` float(60, 30) NOT NULL,\n" +
                "  `exp_to_level` int(0) NOT NULL,\n" +
                "  `total_exp` int(0) NOT NULL,\n" +
                "  `exp_lvl` int(0) NOT NULL,\n" +
                "  `sync_complete` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `last_seen` char(13) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  PRIMARY KEY (`id`) USING BTREE,\n" +
                "  UNIQUE INDEX `player_uuid`(`player_uuid`) USING BTREE");
            
            writeTableStructure(writer, Main.HEALTH_FOOD_AIR_TABLE_NAME,
                "  `id` int(0) UNSIGNED NOT NULL AUTO_INCREMENT,\n" +
                "  `player_uuid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `player_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                "  `health` double(10, 2) NOT NULL,\n" +
                "  `health_scale` double(10, 2) NOT NULL,\n" +
                "  `max_health` double(10, 2) NOT NULL,\n" +
                "  `food` int(0) NOT NULL,\n" +
                "  `saturation` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `air` int(0) NOT NULL,\n" +
                "  `max_air` int(0) NOT NULL,\n" +
                "  `sync_complete` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `last_seen` char(13) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  PRIMARY KEY (`id`) USING BTREE,\n" +
                "  UNIQUE INDEX `player_uuid`(`player_uuid`) USING BTREE");
            
            writeTableStructure(writer, Main.POTION_EFFECTS_TABLE_NAME,
                "  `id` int(0) UNSIGNED NOT NULL AUTO_INCREMENT,\n" +
                "  `player_uuid` char(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `player_name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,\n" +
                "  `potion_effects` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `sync_complete` varchar(5) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  `last_seen` char(13) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,\n" +
                "  PRIMARY KEY (`id`) USING BTREE,\n" +
                "  UNIQUE INDEX `player_uuid`(`player_uuid`) USING BTREE");
            
            // 查询并导出所有表的数据
            exportTableData(writer, connection, Main.INVENTORY_TABLE_NAME);
            exportTableData(writer, connection, Main.ENDERCHEST_TABLE_NAME);
            exportTableData(writer, connection, Main.ECONOMY_TABLE_NAME);
            exportTableData(writer, connection, Main.EXPERIENCE_TABLE_NAME);
            exportTableData(writer, connection, Main.HEALTH_FOOD_AIR_TABLE_NAME);
            exportTableData(writer, connection, Main.POTION_EFFECTS_TABLE_NAME);
            
            writer.close();
            
            commandSender.sendMessage(Chat.getMessage("export-success").replace("{file}", backupFile.getAbsolutePath()));
        } catch (Exception e) {
            commandSender.sendMessage(Chat.getMessage("export-failed").replace("{error}", e.getMessage()));
            e.printStackTrace();
        }
    }
    
    private void writeTableStructure(FileWriter writer, String tableName, String columns) throws IOException {
        writer.write("-- ----------------------------\n");
        writer.write("-- Table structure for " + tableName + "\n");
        writer.write("-- ----------------------------\n");
        writer.write("DROP TABLE IF EXISTS `" + tableName + "`;\n");
        writer.write("CREATE TABLE `" + tableName + "` (\n");
        writer.write(columns + "\n");
        writer.write(") ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;\n\n");
    }
    
    private void exportTableData(FileWriter writer, Connection connection, String tableName) throws Exception {
        writer.write("-- ----------------------------\n");
        writer.write("-- Records of " + tableName + "\n");
        writer.write("-- ----------------------------\n");
        
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT * FROM " + tableName);
        
        // 获取列数和列名
        int columnCount = resultSet.getMetaData().getColumnCount();
        String[] columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = resultSet.getMetaData().getColumnName(i + 1);
        }
        
        // 写入数据
        while (resultSet.next()) {
            writer.write("INSERT INTO `" + tableName + "` VALUES (");
            
            for (int i = 0; i < columnCount; i++) {
                Object value = resultSet.getObject(i + 1);
                if (i > 0) writer.write(", ");
                
                if (value == null) {
                    writer.write("NULL");
                } else {
                    if (value instanceof Number) {
                        writer.write(value.toString());
                    } else {
                        writer.write("'" + escapeString(value.toString()) + "'");
                    }
                }
            }
            
            writer.write(");\n");
        }
        
        writer.write("\n");
        statement.close();
        resultSet.close();
    }
    
    private String escapeString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"");
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] args) {
        return List.of();
    }
}
package de.lostesburger.mySqlPlayerBridge.Storage;

import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;
import de.lostesburger.mySqlPlayerBridge.Main;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * YAML存储管理器实现
 */
public class YAMLStorageManager extends StorageManager {
    private final File dataFolder;
    private final File yamlBackupFolder; // 专门用于存储YAML数据的文件夹
    
    public YAMLStorageManager(File dataFolder) {
        this.dataFolder = dataFolder;
        // 创建yaml_bak文件夹用于存储所有YAML数据
        this.yamlBackupFolder = new File(dataFolder, "yaml_bak");
        if (!yamlBackupFolder.exists()) {
            yamlBackupFolder.mkdirs();
        }
        Main.getInstance().getLogger().info("YAML存储目录: " + yamlBackupFolder.getAbsolutePath());
    }
    
    private File getPlayerFile(UUID playerUUID, String tableName) {
        // 所有数据都存储在yaml_bak文件夹下
        File tableFolder = new File(yamlBackupFolder, tableName);
        if (!tableFolder.exists()) {
            tableFolder.mkdirs();
        }
        return new File(tableFolder, playerUUID.toString() + ".yml");
    }
    
    @Override
    public boolean entryExists(String tableName, Map<String, Object> conditions) throws SQLException {
        try {
            Object uuidObj = conditions.get("player_uuid");
            if (uuidObj == null) {
                return false;
            }
            
            UUID playerUUID = UUID.fromString(uuidObj.toString());
            File playerFile = getPlayerFile(playerUUID, tableName);
            return playerFile.exists();
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public HashMap<String, Object> getEntry(String tableName, Map<String, Object> conditions) throws SQLException {
        try {
            Object uuidObj = conditions.get("player_uuid");
            if (uuidObj == null) {
                return null;
            }
            
            UUID playerUUID = UUID.fromString(uuidObj.toString());
            File playerFile = getPlayerFile(playerUUID, tableName);
            
            if (!playerFile.exists()) {
                return null;
            }
            
            YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
            HashMap<String, Object> result = new HashMap<>();
            
            for (String key : config.getKeys(true)) {
                if (!key.contains(".")) { // 只获取顶层键
                    result.put(key, config.get(key));
                }
            }
            
            return result;
        } catch (Exception e) {
            throw new SQLException("读取YAML文件失败", e);
        }
    }
    
    @Override
    public void setOrUpdateEntry(String tableName, Map<String, Object> conditions, HashMap<String, Object> data) throws SQLException {
        try {
            Object uuidObj = conditions.get("player_uuid");
            if (uuidObj == null) {
                throw new SQLException("缺少player_uuid条件");
            }
            
            UUID playerUUID = UUID.fromString(uuidObj.toString());
            File playerFile = getPlayerFile(playerUUID, tableName);
            
            YamlConfiguration config = new YamlConfiguration();
            
            // 如果文件已存在，先加载现有数据
            if (playerFile.exists()) {
                config = YamlConfiguration.loadConfiguration(playerFile);
            }
            
            // 更新数据
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            
            // 保存文件
            config.save(playerFile);
        } catch (IOException e) {
            throw new SQLException("保存YAML文件失败", e);
        }
    }
    
    @Override
    public void deleteEntry(String tableName, String columnName, Object value) throws SQLException {
        if (!"player_uuid".equals(columnName)) {
            throw new SQLException("YAML存储仅支持通过player_uuid删除");
        }
        
        try {
            UUID playerUUID = UUID.fromString(value.toString());
            File playerFile = getPlayerFile(playerUUID, tableName);
            
            if (playerFile.exists()) {
                playerFile.delete();
            }
        } catch (Exception e) {
            throw new SQLException("删除YAML文件失败", e);
        }
    }
    
    @Override
    public void createTable(String tableName, DatabaseManager.ColumnDefinition... columns) throws SQLException {
        // YAML存储不需要创建表，只需确保目录存在
        File tableFolder = new File(yamlBackupFolder, tableName);
        if (!tableFolder.exists()) {
            tableFolder.mkdirs();
        }
        Main.getInstance().getLogger().info("YAML表目录已创建: " + tableFolder.getAbsolutePath());
    }
    
    @Override
    public boolean isConnectionAlive() {
        return yamlBackupFolder.exists() && yamlBackupFolder.isDirectory();
    }
    
    @Override
    public void closeConnection() {
        // YAML存储不需要关闭连接
    }
}
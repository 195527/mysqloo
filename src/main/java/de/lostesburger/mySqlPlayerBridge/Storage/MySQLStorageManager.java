package de.lostesburger.mySqlPlayerBridge.Storage;

import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * MySQL存储管理器实现
 */
public class MySQLStorageManager extends StorageManager {
    private final DatabaseManager databaseManager;
    
    public MySQLStorageManager(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }
    
    @Override
    public boolean entryExists(String tableName, Map<String, Object> conditions) throws SQLException {
        return databaseManager.entryExists(tableName, conditions);
    }
    
    @Override
    public HashMap<String, Object> getEntry(String tableName, Map<String, Object> conditions) throws SQLException {
        Map<String, Object> result = databaseManager.getEntry(tableName, conditions);
        if (result == null) {
            return null;
        }
        return new HashMap<>(result);
    }
    
    @Override
    public void setOrUpdateEntry(String tableName, Map<String, Object> conditions, HashMap<String, Object> data) throws SQLException {
        databaseManager.setOrUpdateEntry(tableName, conditions, data);
    }
    
    @Override
    public void deleteEntry(String tableName, String columnName, Object value) throws SQLException {
        databaseManager.deleteEntry(tableName, columnName, (String) value);
    }
    
    @Override
    public void createTable(String tableName, DatabaseManager.ColumnDefinition... columns) throws SQLException {
        databaseManager.createTable(tableName, columns);
    }
    
    @Override
    public boolean isConnectionAlive() {
        return databaseManager.isConnectionAlive();
    }
    
    @Override
    public void closeConnection() {
        // MySQL连接由连接池管理，不需要手动关闭
    }
}
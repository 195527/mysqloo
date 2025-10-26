package de.lostesburger.mySqlPlayerBridge.Storage;

import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 存储管理器抽象类
 * 定义了所有存储方式必须实现的基本操作
 */
public abstract class StorageManager {
    
    /**
     * 检查条目是否存在
     * @param tableName 表名
     * @param conditions 查询条件
     * @return 是否存在
     * @throws SQLException SQL异常
     */
    public abstract boolean entryExists(String tableName, Map<String, Object> conditions) throws SQLException;
    
    /**
     * 获取条目
     * @param tableName 表名
     * @param conditions 查询条件
     * @return 条目数据
     * @throws SQLException SQL异常
     */
    public abstract HashMap<String, Object> getEntry(String tableName, Map<String, Object> conditions) throws SQLException;
    
    /**
     * 设置或更新条目
     * @param tableName 表名
     * @param conditions 查询条件
     * @param data 数据
     * @throws SQLException SQL异常
     */
    public abstract void setOrUpdateEntry(String tableName, Map<String, Object> conditions, HashMap<String, Object> data) throws SQLException;
    
    /**
     * 删除条目
     * @param tableName 表名
     * @param columnName 列名
     * @param value 值
     * @throws SQLException SQL异常
     */
    public abstract void deleteEntry(String tableName, String columnName, Object value) throws SQLException;
    
    /**
     * 创建表
     * @param tableName 表名
     * @param columns 列定义
     * @throws SQLException SQL异常
     */
    public abstract void createTable(String tableName, DatabaseManager.ColumnDefinition... columns) throws SQLException;
    
    /**
     * 检查连接是否存活
     * @return 连接是否存活
     */
    public abstract boolean isConnectionAlive();
    
    /**
     * 关闭连接
     */
    public abstract void closeConnection();
}
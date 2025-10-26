package de.lostesburger.mySqlPlayerBridge.Storage;

import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;
import de.lostesburger.mySqlPlayerBridge.Main;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * SQLite存储管理器实现
 */
public class SQLiteStorageManager extends StorageManager {
    private Connection connection;
    private final String databasePath;
    
    public SQLiteStorageManager(String databasePath) {
        this.databasePath = databasePath;
        connect();
        createTables();
    }
    
    private void connect() {
        try {
            // 加载SQLite JDBC驱动
            Class.forName("org.sqlite.JDBC");
            
            // 创建数据库文件路径
            File dbFile = new File(databasePath);
            if (!dbFile.getParentFile().exists()) {
                dbFile.getParentFile().mkdirs();
            }
            
            // 建立连接
            String url = "jdbc:sqlite:" + databasePath;
            this.connection = DriverManager.getConnection(url);
            Main.getInstance().getLogger().info("SQLite数据库连接成功: " + databasePath);
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("SQLite数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createTables() {
        try {
            // 创建所有需要的表
            createInventoryTable();
            createEnderChestTable();
            createEconomyTable();
            createExperienceTable();
            createHealthFoodAirTable();
            createPotionEffectsTable();
        } catch (SQLException e) {
            Main.getInstance().getLogger().severe("创建SQLite表失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createInventoryTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mpdb_inventory (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid TEXT UNIQUE NOT NULL, " +
                "player_name TEXT NOT NULL, " +
                "inventory TEXT NOT NULL, " +
                "armor TEXT NOT NULL, " +
                "hotbar_slot INTEGER NOT NULL, " +
                "gamemode INTEGER NOT NULL, " +
                "sync_complete TEXT NOT NULL, " +
                "last_seen TEXT NOT NULL" +
                ")";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }
    
    private void createEnderChestTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mpdb_enderchest (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid TEXT UNIQUE NOT NULL, " +
                "player_name TEXT NOT NULL, " +
                "enderchest TEXT NOT NULL, " +
                "sync_complete TEXT NOT NULL, " +
                "last_seen TEXT NOT NULL" +
                ")";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }
    
    private void createEconomyTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mpdb_economy (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid TEXT UNIQUE NOT NULL, " +
                "player_name TEXT NOT NULL, " +
                "money REAL NOT NULL, " +
                "offline_money REAL NOT NULL, " +
                "sync_complete TEXT NOT NULL, " +
                "last_seen TEXT NOT NULL" +
                ")";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }
    
    private void createExperienceTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mpdb_experience (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid TEXT UNIQUE NOT NULL, " +
                "player_name TEXT NOT NULL, " +
                "exp REAL NOT NULL, " +
                "exp_to_level INTEGER NOT NULL, " +
                "total_exp INTEGER NOT NULL, " +
                "exp_lvl INTEGER NOT NULL, " +
                "sync_complete TEXT NOT NULL, " +
                "last_seen TEXT NOT NULL" +
                ")";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }
    
    private void createHealthFoodAirTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mpdb_health_food_air (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid TEXT UNIQUE NOT NULL, " +
                "player_name TEXT NOT NULL, " +
                "health REAL NOT NULL, " +
                "health_scale REAL NOT NULL, " +
                "max_health REAL NOT NULL, " +
                "food INTEGER NOT NULL, " +
                "saturation TEXT NOT NULL, " +
                "air INTEGER NOT NULL, " +
                "max_air INTEGER NOT NULL, " +
                "sync_complete TEXT NOT NULL, " +
                "last_seen TEXT NOT NULL" +
                ")";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }
    
    private void createPotionEffectsTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS mpdb_potionEffects (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid TEXT UNIQUE NOT NULL, " +
                "player_name TEXT NOT NULL, " +
                "potion_effects TEXT NOT NULL, " +
                "sync_complete TEXT NOT NULL, " +
                "last_seen TEXT NOT NULL" +
                ")";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.execute();
        }
    }
    
    @Override
    public boolean entryExists(String tableName, Map<String, Object> conditions) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT 1 FROM ");
        sql.append(tableName);
        sql.append(" WHERE ");
        
        int i = 0;
        for (String key : conditions.keySet()) {
            if (i > 0) sql.append(" AND ");
            sql.append(key).append(" = ?");
            i++;
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (Object value : conditions.values()) {
                stmt.setObject(index++, value);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
    
    @Override
    public HashMap<String, Object> getEntry(String tableName, Map<String, Object> conditions) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM ");
        sql.append(tableName);
        sql.append(" WHERE ");
        
        int i = 0;
        for (String key : conditions.keySet()) {
            if (i > 0) sql.append(" AND ");
            sql.append(key).append(" = ?");
            i++;
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int index = 1;
            for (Object value : conditions.values()) {
                stmt.setObject(index++, value);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    HashMap<String, Object> result = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    
                    for (int j = 1; j <= columnCount; j++) {
                        String columnName = metaData.getColumnName(j);
                        Object value = rs.getObject(j);
                        result.put(columnName, value);
                    }
                    
                    return result;
                }
            }
        }
        
        return null;
    }
    
    @Override
    public void setOrUpdateEntry(String tableName, Map<String, Object> conditions, HashMap<String, Object> data) throws SQLException {
        // 先检查是否存在
        boolean exists = entryExists(tableName, conditions);
        
        if (exists) {
            // 更新现有条目
            StringBuilder sql = new StringBuilder("UPDATE ");
            sql.append(tableName);
            sql.append(" SET ");
            
            int i = 0;
            for (String key : data.keySet()) {
                if (i > 0) sql.append(", ");
                sql.append(key).append(" = ?");
                i++;
            }
            
            sql.append(" WHERE ");
            
            i = 0;
            for (String key : conditions.keySet()) {
                if (i > 0) sql.append(" AND ");
                sql.append(key).append(" = ?");
                i++;
            }
            
            try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
                int index = 1;
                for (Object value : data.values()) {
                    stmt.setObject(index++, value);
                }
                
                for (Object value : conditions.values()) {
                    stmt.setObject(index++, value);
                }
                
                stmt.executeUpdate();
            }
        } else {
            // 插入新条目
            StringBuilder columns = new StringBuilder();
            StringBuilder values = new StringBuilder();
            
            int i = 0;
            for (String key : data.keySet()) {
                if (i > 0) {
                    columns.append(", ");
                    values.append(", ");
                }
                columns.append(key);
                values.append("?");
                i++;
            }
            
            String sql = "INSERT INTO " + tableName + " (" + columns.toString() + ") VALUES (" + values.toString() + ")";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                int index = 1;
                for (Object value : data.values()) {
                    stmt.setObject(index++, value);
                }
                
                stmt.executeUpdate();
            }
        }
    }
    
    @Override
    public void deleteEntry(String tableName, String columnName, Object value) throws SQLException {
        String sql = "DELETE FROM " + tableName + " WHERE " + columnName + " = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setObject(1, value);
            stmt.executeUpdate();
        }
    }
    
    @Override
    public void createTable(String tableName, DatabaseManager.ColumnDefinition... columns) throws SQLException {
        // SQLite表在初始化时已经创建，这里不需要额外操作
        Main.getInstance().getLogger().info("SQLite表已存在，跳过创建: " + tableName);
    }
    
    @Override
    public boolean isConnectionAlive() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    @Override
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                Main.getInstance().getLogger().info("SQLite数据库连接已关闭");
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().severe("关闭SQLite数据库连接时出错: " + e.getMessage());
        }
    }
}
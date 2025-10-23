package de.lostesburger.mySqlPlayerBridge.Database;

import de.lostesburger.mySqlPlayerBridge.Main;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class DatabaseManager {
    private Connection connection;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public DatabaseManager(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        connect();
    }

    private void connect() {
        try {
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
            this.connection = DriverManager.getConnection(url, username, password);
            Main.getInstance().getLogger().info("数据库连接成功!");
            Main.getInstance().getLogger().info("连接信息: Host=" + host + ", Port=" + port + ", Database=" + database + ", User=" + username);
        } catch (SQLException e) {
            Main.getInstance().getLogger().severe("数据库连接失败!");
            Main.getInstance().getLogger().severe("连接参数:");
            Main.getInstance().getLogger().severe("  Host: " + host);
            Main.getInstance().getLogger().severe("  Port: " + port);
            Main.getInstance().getLogger().severe("  Database: " + database);
            Main.getInstance().getLogger().severe("  Username: " + username);
            Main.getInstance().getLogger().severe("  Error: " + e.getMessage());
            throw new RuntimeException("Failed to connect to database. Please check your database configuration in mysql.yml", e);
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }
            
            // Test connection
            if (!connection.isValid(5)) {
                connect();
            }
        } catch (SQLException e) {
            connect();
        }
        return connection;
    }

    public boolean isConnectionAlive() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    public void createTable(String tableName, ColumnDefinition... columns) throws SQLException {
        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + tableName + "` (");
        
        List<String> columnDefinitions = new ArrayList<>();
        String primaryKey = null;
        
        for (ColumnDefinition column : columns) {
            StringBuilder columnSql = new StringBuilder("`" + column.name + "` " + column.type);
            
            if (column.length > 0) {
                columnSql.append("(").append(column.length).append(")");
            }
            
            if (column.autoIncrement) {
                columnSql.append(" AUTO_INCREMENT");
            }
            
            if (column.primaryKey) {
                primaryKey = column.name;
            }
            
            columnDefinitions.add(columnSql.toString());
        }
        
        sql.append(String.join(", ", columnDefinitions));
        
        if (primaryKey != null) {
            sql.append(", PRIMARY KEY (`").append(primaryKey).append("`)");
        }
        
        sql.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            stmt.executeUpdate();
        }
    }

    public boolean entryExists(String tableName, Map<String, Object> conditions) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT 1 FROM `" + tableName + "` WHERE ");
        List<String> whereConditions = new ArrayList<>();
        for (String key : conditions.keySet()) {
            whereConditions.add("`" + key + "` = ?");
        }
        sql.append(String.join(" AND ", whereConditions));
        sql.append(" LIMIT 1");

        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            int index = 1;
            for (Object value : conditions.values()) {
                stmt.setObject(index++, value);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void setOrUpdateEntry(String tableName, Map<String, Object> conditions, Map<String, Object> data) throws SQLException {
        if (entryExists(tableName, conditions)) {
            // Update existing entry
            updateEntry(tableName, conditions, data);
        } else {
            // Insert new entry
            insertEntry(tableName, data);
        }
    }

    private void updateEntry(String tableName, Map<String, Object> conditions, Map<String, Object> data) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE `" + tableName + "` SET ");
        List<String> setParts = new ArrayList<>();
        for (String key : data.keySet()) {
            setParts.add("`" + key + "` = ?");
        }
        sql.append(String.join(", ", setParts));
        
        sql.append(" WHERE ");
        List<String> whereConditions = new ArrayList<>();
        for (String key : conditions.keySet()) {
            whereConditions.add("`" + key + "` = ?");
        }
        sql.append(String.join(" AND ", whereConditions));

        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            int index = 1;
            for (Object value : data.values()) {
                stmt.setObject(index++, value);
            }
            for (Object value : conditions.values()) {
                stmt.setObject(index++, value);
            }
            stmt.executeUpdate();
        }
    }

    private void insertEntry(String tableName, Map<String, Object> data) throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO `" + tableName + "` (");
        StringBuilder placeholders = new StringBuilder(" VALUES (");
        
        List<String> columnNames = new ArrayList<>(data.keySet());
        sql.append("`" + String.join("`, `", columnNames) + "`");
        sql.append(")");
        
        List<String> placeholderList = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            placeholderList.add("?");
        }
        placeholders.append(String.join(", ", placeholderList));
        placeholders.append(")");
        
        sql.append(placeholders);

        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            int index = 1;
            for (String key : columnNames) {
                stmt.setObject(index++, data.get(key));
            }
            stmt.executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getEntry(String tableName, Map<String, Object> conditions) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM `" + tableName + "` WHERE ");
        List<String> whereConditions = new ArrayList<>();
        for (String key : conditions.keySet()) {
            whereConditions.add("`" + key + "` = ?");
        }
        sql.append(String.join(" AND ", whereConditions));
        sql.append(" LIMIT 1");

        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            int index = 1;
            for (Object value : conditions.values()) {
                stmt.setObject(index++, value);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> result = new HashMap<>();
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        result.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    return result;
                }
                return null;
            }
        }
    }
    
    public void deleteEntry(String tableName, String columnName, String value) throws SQLException {
        String sql = "DELETE FROM `" + tableName + "` WHERE `" + columnName + "` = ?";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.executeUpdate();
        }
    }

    public static class ColumnDefinition {
        private final String name;
        private final String type;
        private final int length;
        private boolean primaryKey = false;
        private boolean autoIncrement = false;

        private ColumnDefinition(String name, String type, int length) {
            this.name = name;
            this.type = type;
            this.length = length;
        }

        public static ColumnDefinition integer(String name) {
            return new ColumnDefinition(name, "INT", 0);
        }

        public static ColumnDefinition varchar(String name, int length) {
            return new ColumnDefinition(name, "VARCHAR", length);
        }

        public static ColumnDefinition text(String name) {
            return new ColumnDefinition(name, "TEXT", 0);
        }

        public static ColumnDefinition longText(String name) {
            return new ColumnDefinition(name, "LONGTEXT", 0);
        }

        public static ColumnDefinition doubLe(String name) {
            return new ColumnDefinition(name, "DOUBLE", 0);
        }

        public static ColumnDefinition Float(String name) {
            return new ColumnDefinition(name, "FLOAT", 0);
        }

        public ColumnDefinition primaryKey(boolean isPrimaryKey) {
            this.primaryKey = isPrimaryKey;
            if (isPrimaryKey && "INT".equals(this.type)) {
                this.autoIncrement = true;
            }
            return this;
        }
    }
}
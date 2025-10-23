package de.lostesburger.mySqlPlayerBridge.Handlers.MySqlConnection;

import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;
import de.lostesburger.mySqlPlayerBridge.Handlers.Errors.MySqlErrorHandler;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Managers.MySqlData.MySqlDataManager;

import java.sql.SQLException;

public class MySqlConnectionHandler {
    private final DatabaseManager databaseManager;
    private final MySqlDataManager mySqlDataManager;

    public MySqlConnectionHandler(String host, int port, String database, String username, String password) {
        try {
            this.databaseManager = new DatabaseManager(host, port, database, username, password);
        } catch (Exception e) {
            new MySqlErrorHandler().onInitialize();
            throw new RuntimeException(e);
        }
        createTables();

        this.mySqlDataManager = new MySqlDataManager(databaseManager);
    }

    private void createTables() {
        try {
            // Create inventory table
            databaseManager.createTable(Main.INVENTORY_TABLE_NAME,
                    DatabaseManager.ColumnDefinition.integer("id").primaryKey(true),
                    DatabaseManager.ColumnDefinition.varchar("player_uuid", 36),
                    DatabaseManager.ColumnDefinition.varchar("player_name", 16),
                    DatabaseManager.ColumnDefinition.longText("inventory"),
                    DatabaseManager.ColumnDefinition.text("armor"),
                    DatabaseManager.ColumnDefinition.integer("hotbar_slot"),
                    DatabaseManager.ColumnDefinition.integer("gamemode"),
                    DatabaseManager.ColumnDefinition.varchar("sync_complete", 5),
                    DatabaseManager.ColumnDefinition.varchar("last_seen", 13)
            );
            
            //Create enderchest table
            databaseManager.createTable(Main.ENDERCHEST_TABLE_NAME,
                    DatabaseManager.ColumnDefinition.integer("id").primaryKey(true),
                    DatabaseManager.ColumnDefinition.varchar("player_uuid", 36),
                    DatabaseManager.ColumnDefinition.varchar("player_name", 16),
                    DatabaseManager.ColumnDefinition.longText("enderchest"),
                    DatabaseManager.ColumnDefinition.varchar("sync_complete", 5),
                    DatabaseManager.ColumnDefinition.varchar("last_seen", 13)
            );
            
            //Create economy table
            databaseManager.createTable(Main.ECONOMY_TABLE_NAME,
                    DatabaseManager.ColumnDefinition.integer("id").primaryKey(true),
                    DatabaseManager.ColumnDefinition.varchar("player_uuid", 36),
                    DatabaseManager.ColumnDefinition.varchar("player_name", 16),
                    DatabaseManager.ColumnDefinition.doubLe("money"),
                    DatabaseManager.ColumnDefinition.doubLe("offline_money"),
                    DatabaseManager.ColumnDefinition.varchar("sync_complete", 5),
                    DatabaseManager.ColumnDefinition.varchar("last_seen", 13)
            );
            
            //Create experience table
            databaseManager.createTable(Main.EXPERIENCE_TABLE_NAME,
                    DatabaseManager.ColumnDefinition.integer("id").primaryKey(true),
                    DatabaseManager.ColumnDefinition.varchar("player_uuid", 36),
                    DatabaseManager.ColumnDefinition.varchar("player_name", 16),
                    DatabaseManager.ColumnDefinition.Float("exp"),
                    DatabaseManager.ColumnDefinition.integer("exp_to_level"),
                    DatabaseManager.ColumnDefinition.integer("total_exp"),
                    DatabaseManager.ColumnDefinition.integer("exp_lvl"),
                    DatabaseManager.ColumnDefinition.varchar("sync_complete", 5),
                    DatabaseManager.ColumnDefinition.varchar("last_seen", 13)
            );
            
            // Create health_food_air table
            databaseManager.createTable(Main.HEALTH_FOOD_AIR_TABLE_NAME,
                    DatabaseManager.ColumnDefinition.integer("id").primaryKey(true),
                    DatabaseManager.ColumnDefinition.varchar("player_uuid", 36),
                    DatabaseManager.ColumnDefinition.varchar("player_name", 16),
                    DatabaseManager.ColumnDefinition.doubLe("health"),
                    DatabaseManager.ColumnDefinition.doubLe("health_scale"),
                    DatabaseManager.ColumnDefinition.doubLe("max_health"),
                    DatabaseManager.ColumnDefinition.integer("food"),
                    DatabaseManager.ColumnDefinition.varchar("saturation", 20),
                    DatabaseManager.ColumnDefinition.integer("air"),
                    DatabaseManager.ColumnDefinition.integer("max_air"),
                    DatabaseManager.ColumnDefinition.varchar("sync_complete", 5),
                    DatabaseManager.ColumnDefinition.varchar("last_seen", 13)
            );
            
            // Create potion effects table
            databaseManager.createTable(Main.POTION_EFFECTS_TABLE_NAME,
                    DatabaseManager.ColumnDefinition.integer("id").primaryKey(true),
                    DatabaseManager.ColumnDefinition.varchar("player_uuid", 36),
                    DatabaseManager.ColumnDefinition.varchar("player_name", 16),
                    DatabaseManager.ColumnDefinition.text("potion_effects"),
                    DatabaseManager.ColumnDefinition.varchar("sync_complete", 5),
                    DatabaseManager.ColumnDefinition.varchar("last_seen", 13)
            );
        } catch (SQLException e) {
            new MySqlErrorHandler().onTableCreate();
            throw new RuntimeException(e);
        }
    }

    public DatabaseManager getManager(){ return databaseManager; }
    public DatabaseManager getMySQL(){ return this.databaseManager; }
    public MySqlDataManager getMySqlDataManager(){ return this.mySqlDataManager; }
}
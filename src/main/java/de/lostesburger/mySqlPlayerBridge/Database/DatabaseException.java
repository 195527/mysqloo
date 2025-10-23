package de.lostesburger.mySqlPlayerBridge.Database;

import java.sql.SQLException;

public class DatabaseException extends Exception {
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, SQLException cause) {
        super(message, cause);
    }
    
    public DatabaseException(SQLException cause) {
        super(cause);
    }
}
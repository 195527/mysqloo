package de.lostesburger.mySqlPlayerBridge.Commands.ImportCommand;

import de.lostesburger.mySqlPlayerBridge.Commands.CommandInterface;
import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Managers.MySqlData.MySqlDataManager;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

public class ImportCommand implements CommandInterface {

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (args.length < 1) {
            commandSender.sendMessage(Chat.getMessage("import-usage"));
            return;
        }

        String fileName = args[0];
        File backupFile = new File(Main.getInstance().getDataFolder(), fileName);
        
        if (!backupFile.exists()) {
            commandSender.sendMessage(Chat.getMessage("import-file-not-found").replace("{file}", fileName));
            return;
        }

        commandSender.sendMessage(Chat.getMessage("import-starting").replace("{file}", fileName));

        // 在异步线程中执行导入操作，避免阻塞主线程
        Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                importDatabase(commandSender, backupFile);
            } catch (Exception e) {
                commandSender.sendMessage(Chat.getMessage("import-failed").replace("{error}", e.getMessage()));
                e.printStackTrace();
            }
        });
    }

    private void importDatabase(CommandSender commandSender, File backupFile) {
        MySqlDataManager dataManager = Main.mySqlConnectionHandler.getMySqlDataManager();
        Connection connection = null;
        try {
            connection = dataManager.databaseManager.getConnection();
        } catch (Exception e) {
            commandSender.sendMessage(Chat.getMessage("import-failed").replace("{error}", "数据库连接失败: " + e.getMessage()));
            e.printStackTrace();
            return;
        }
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader(backupFile));
            Statement statement = connection.createStatement();
            
            String line;
            int count = 0;
            
            while ((line = reader.readLine()) != null) {
                // 跳过注释行和空行
                if (line.trim().isEmpty() || line.startsWith("--") || line.startsWith("/*") || 
                    line.startsWith("CREATE DATABASE") || line.startsWith("USE ")) {
                    continue;
                }
                
                // 处理DROP TABLE语句
                if (line.startsWith("DROP TABLE")) {
                    try {
                        statement.executeUpdate(line);
                    } catch (Exception ignored) {
                        // 忽略删除表时的错误
                    }
                    continue;
                }
                
                // 处理CREATE TABLE语句
                if (line.startsWith("CREATE TABLE")) {
                    StringBuilder createTableStatement = new StringBuilder(line);
                    while ((line = reader.readLine()) != null && !line.trim().equals(";")) {
                        createTableStatement.append("\n").append(line);
                        if (line.trim().endsWith(")")) {
                            break;
                        }
                    }
                    if (line != null && line.trim().equals(";")) {
                        createTableStatement.append(";");
                    }
                    
                    try {
                        statement.executeUpdate(createTableStatement.toString());
                    } catch (Exception e) {
                        // 如果创建表失败，尝试继续
                        e.printStackTrace();
                    }
                    continue;
                }
                
                // 执行插入语句
                if (line.startsWith("INSERT INTO")) {
                    try {
                        statement.executeUpdate(line);
                        count++;
                    } catch (Exception e) {
                        // 如果插入失败，尝试继续
                        e.printStackTrace();
                    }
                }
            }
            
            statement.close();
            reader.close();
            
            commandSender.sendMessage(Chat.getMessage("import-success").replace("{count}", String.valueOf(count)));
        } catch (Exception e) {
            commandSender.sendMessage(Chat.getMessage("import-failed").replace("{error}", e.getMessage()));
            e.printStackTrace();
        }
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] args) {
        if (args.length == 1) {
            // 获取插件数据文件夹中的所有.sql文件
            File dataFolder = Main.getInstance().getDataFolder();
            File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".sql"));
            
            if (files != null) {
                List<String> fileNames = new java.util.ArrayList<>();
                for (File file : files) {
                    fileNames.add(file.getName());
                }
                return fileNames;
            }
        }
        return List.of();
    }
}
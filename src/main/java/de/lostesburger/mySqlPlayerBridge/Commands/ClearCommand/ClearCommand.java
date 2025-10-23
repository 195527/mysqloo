package de.lostesburger.mySqlPlayerBridge.Commands.ClearCommand;

import de.lostesburger.mySqlPlayerBridge.Commands.CommandInterface;
import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClearCommand implements CommandInterface {
    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if(!commandSender.hasPermission(Main.config.getString("settings.admin-permission"))){
            commandSender.sendMessage(Chat.getMessage("permission-error"));
            return;
        }

        if(strings.length < 1 || strings.length > 2){
            commandSender.sendMessage(Chat.getMessage("clear-wrong-usage"));
            return;
        }

        String target = strings[0];
        String clearType = (strings.length > 1) ? strings[1].toLowerCase() : "all";

        DatabaseManager manager = Main.mySqlConnectionHandler.getManager();

        if(target.equalsIgnoreCase("*")){
            if (!clearType.equals("all")) {
                commandSender.sendMessage("§c使用 * 通配符时不能指定特定类型");
                return;
            }
            
            try {
                // 获取所有条目然后逐个删除，避免使用不存在的方法
                // 这里我们使用一个不同的方法来删除所有数据
                try (var stmt = manager.getConnection().createStatement()) {
                    int deleted = stmt.executeUpdate("DELETE FROM " + Main.INVENTORY_TABLE_NAME);
                    Bukkit.getLogger().info("§c已删除所有玩家数据 (" + deleted + " 条记录)");
                    commandSender.sendMessage("§c已删除所有玩家数据 (" + deleted + " 条记录)");
                }
                
                // 清空所有在线玩家的背包
                for (Player player : Bukkit.getOnlinePlayers()) {
                    clearPlayerInventory(player);
                }
            } catch (SQLException e) {
                commandSender.sendMessage("§c删除所有玩家数据失败: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }else {
            // 改进获取离线玩家的方式，避免使用过时的方法
            UUID targetUUID = null;
            OfflinePlayer targetPlayer = null;
            Player onlinePlayer = null;
            
            // 首先尝试通过名称查找玩家
            for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
                if (op.getName() != null && op.getName().equals(target)) {
                    targetUUID = op.getUniqueId();
                    targetPlayer = op;
                    break;
                }
            }
            
            // 如果通过名称找不到，尝试解析为UUID
            if(targetUUID == null) {
                try {
                    targetUUID = UUID.fromString(target);
                } catch (IllegalArgumentException ignored) {
                    // 不是有效的UUID格式
                }
            }
            
            // 检查玩家是否在线
            if (targetUUID != null) {
                onlinePlayer = Bukkit.getPlayer(targetUUID);
            }
            
            if(targetUUID == null){
                commandSender.sendMessage(Chat.getMessage("clear-player-not-found"));
                return;
            }
            
            try {
                switch (clearType) {
                    case "all":
                        // 使用字符串形式的UUID进行删除操作，避免编码问题
                        manager.deleteEntry(Main.INVENTORY_TABLE_NAME, "player_uuid", targetUUID.toString());
                        String playerName = (targetPlayer != null) ? targetPlayer.getName() : targetUUID.toString();
                        Bukkit.getLogger().info("§c已删除玩家: " + playerName + " (" + targetUUID + ")");
                        commandSender.sendMessage("§c已删除玩家数据: " + playerName);
                        
                        // 如果玩家在线，清空其背包
                        if (onlinePlayer != null) {
                            clearPlayerInventory(onlinePlayer);
                        }
                        break;
                        
                    case "inv":
                    case "inventory":
                        // 只清除玩家背包数据
                        Map<String, Object> updateInventory = new HashMap<>();
                        updateInventory.put("inventory", "[]");
                        manager.setOrUpdateEntry(Main.INVENTORY_TABLE_NAME, Map.of("player_uuid", targetUUID.toString()), updateInventory);
                        commandSender.sendMessage("§c已清除玩家 " + target + " 的背包数据");
                        
                        // 如果玩家在线，清空其背包
                        if (onlinePlayer != null) {
                            onlinePlayer.getInventory().clear();
                            onlinePlayer.sendMessage("§c您的背包已被管理员清空");
                        }
                        break;
                        
                    case "end":
                    case "enderchest":
                        // 只清除玩家末影箱数据
                        Map<String, Object> updateEnderChest = new HashMap<>();
                        updateEnderChest.put("enderchest", "[]");
                        manager.setOrUpdateEntry(Main.INVENTORY_TABLE_NAME, Map.of("player_uuid", targetUUID.toString()), updateEnderChest);
                        commandSender.sendMessage("§c已清除玩家 " + target + " 的末影箱数据");
                        
                        // 如果玩家在线，清空其末影箱
                        if (onlinePlayer != null) {
                            onlinePlayer.getEnderChest().clear();
                            onlinePlayer.sendMessage("§c您的末影箱已被管理员清空");
                        }
                        break;
                        
                    default:
                        commandSender.sendMessage("§c未知的清除类型。可用类型: all, inv, end");
                        return;
                }
            } catch (SQLException e) {
                commandSender.sendMessage("§c删除玩家数据失败: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] strings) {
        List<String> options = new ArrayList<String>();
        
        if (strings.length == 1) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                options.add(player.getName());
            });
            options.add("*");
        } else if (strings.length == 2) {
            options.add("all");
            options.add("inv");
            options.add("end");
        }

        return options;
    }
    
    /**
     * 清空玩家背包并发送消息
     * @param player 要清空背包的玩家
     */
    private void clearPlayerInventory(Player player) {
        // 清空物品栏
        player.getInventory().clear();
        
        // 清空盔甲槽
        player.getInventory().setArmorContents(new ItemStack[4]);
        
        // 清空末影箱
        player.getEnderChest().clear();
        
        // 发送消息给玩家
        player.sendMessage("§c您的背包已被管理员清空。");
    }
    
    // 添加一个辅助方法来删除条目
    private void deleteEntry(DatabaseManager manager, String tableName, String columnName, String value) throws SQLException {
        String sql = "DELETE FROM `" + tableName + "` WHERE `" + columnName + "` = ?";
        try (var stmt = manager.getConnection().prepareStatement(sql)) {
            stmt.setString(1, value);
            stmt.executeUpdate();
        }
    }
}
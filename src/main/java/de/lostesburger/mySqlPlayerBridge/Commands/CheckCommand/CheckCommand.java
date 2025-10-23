package de.lostesburger.mySqlPlayerBridge.Commands.CheckCommand;

import de.lostesburger.mySqlPlayerBridge.Commands.CommandInterface;
import de.lostesburger.mySqlPlayerBridge.Database.DatabaseException;
import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Managers.MySqlData.MySqlDataManager;
import de.lostesburger.mySqlPlayerBridge.Serialization.BukkitItemSerializer;
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

public class CheckCommand implements CommandInterface {

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission(Main.config.getString("settings.admin-permission"))) {
            commandSender.sendMessage(Chat.getMessage("permission-error"));
            return;
        }

        if (args.length != 1) {
            commandSender.sendMessage(Chat.getMessage("check-wrong-usage"));
            return;
        }

        String target = args[0];

        // 查找目标玩家
        UUID targetUUID = null;
        String playerName = target;

        // 尝试通过名称查找玩家
        for (OfflinePlayer op : Bukkit.getOfflinePlayers()) {
            if (op.getName() != null && op.getName().equals(target)) {
                targetUUID = op.getUniqueId();
                playerName = op.getName();
                break;
            }
        }

        // 如果通过名称找不到，尝试解析为UUID
        if (targetUUID == null) {
            try {
                targetUUID = UUID.fromString(target);
                // 尝试获取玩家名
                OfflinePlayer player = Bukkit.getOfflinePlayer(targetUUID);
                if (player.getName() != null) {
                    playerName = player.getName();
                }
            } catch (IllegalArgumentException ignored) {
                // 不是有效的UUID格式
                commandSender.sendMessage("§c未找到玩家: " + target);
                return;
            }
        }

        if (targetUUID == null) {
            commandSender.sendMessage("§c未找到玩家: " + target);
            return;
        }

        // 检查数据库中的玩家数据
        DatabaseManager manager = Main.mySqlConnectionHandler.getManager();
        try {
            // 首先检查玩家是否在线，如果在线则先保存其当前数据
            Player onlinePlayer = Bukkit.getPlayer(targetUUID);
            if (onlinePlayer != null) {
                // 保存玩家当前数据到数据库，确保数据是最新的
                Main.mySqlConnectionHandler.getMySqlDataManager().savePlayerData(onlinePlayer);
            }
            
            if (!manager.entryExists(Main.INVENTORY_TABLE_NAME, Map.of("player_uuid", targetUUID.toString()))) {
                commandSender.sendMessage("§c数据库中没有找到玩家 " + playerName + " (" + targetUUID + ") 的数据");
                return;
            }

            // 从所有表中获取玩家数据
            HashMap<String, Object> data = new HashMap<>();
            
            // 从各个表中获取数据
            try {
                HashMap<String, Object> inventoryData = (HashMap<String, Object>) manager.getEntry(Main.INVENTORY_TABLE_NAME, Map.of("player_uuid", targetUUID.toString()));
                if (inventoryData != null) data.putAll(inventoryData);
            } catch (Exception ignored) {}
            
            try {
                HashMap<String, Object> enderchestData = (HashMap<String, Object>) manager.getEntry(Main.ENDERCHEST_TABLE_NAME, Map.of("player_uuid", targetUUID.toString()));
                if (enderchestData != null) data.putAll(enderchestData);
            } catch (Exception ignored) {}
            
            try {
                HashMap<String, Object> economyData = (HashMap<String, Object>) manager.getEntry(Main.ECONOMY_TABLE_NAME, Map.of("player_uuid", targetUUID.toString()));
                if (economyData != null) data.putAll(economyData);
            } catch (Exception ignored) {}
            
            try {
                HashMap<String, Object> experienceData = (HashMap<String, Object>) manager.getEntry(Main.EXPERIENCE_TABLE_NAME, Map.of("player_uuid", targetUUID.toString()));
                if (experienceData != null) data.putAll(experienceData);
            } catch (Exception ignored) {}
            
            try {
                HashMap<String, Object> healthData = (HashMap<String, Object>) manager.getEntry(Main.HEALTH_FOOD_AIR_TABLE_NAME, Map.of("player_uuid", targetUUID.toString()));
                if (healthData != null) data.putAll(healthData);
            } catch (Exception ignored) {}
            
            try {
                HashMap<String, Object> potionData = (HashMap<String, Object>) manager.getEntry(Main.POTION_EFFECTS_TABLE_NAME, Map.of("player_uuid", targetUUID.toString()));
                if (potionData != null) data.putAll(potionData);
            } catch (Exception ignored) {}
            
            // 显示玩家信息
            commandSender.sendMessage("§7======== §9玩家数据检查 §7========");
            commandSender.sendMessage("§7玩家名: §f" + playerName);
            commandSender.sendMessage("§7UUID: §f" + targetUUID);
            
            // 显示金币（如果启用）
            if (Main.modulesManager.syncVaultEconomy) {
                Object money = data.get("money");
                commandSender.sendMessage("§7金币: §f" + (money != null ? money.toString() : "0.0"));
            } else {
                commandSender.sendMessage("§7金币: §f未启用经济同步");
            }
            
            // 显示背包信息
            Object inventoryData = data.get("inventory");
            if (inventoryData != null) {
                try {
                    ItemStack[] inventoryContents = BukkitItemSerializer.deserialize(inventoryData.toString());
                    Map<String, Integer> itemCounts = new HashMap<>();
                    Map<String, ItemStack> itemExamples = new HashMap<>();
                    int totalItems = 0;

                    if (inventoryContents != null) {
                        for (ItemStack item : inventoryContents) {
                            if (item != null && item.getAmount() > 0) {
                                String itemKey = item.getType().toString();
                                itemCounts.put(itemKey, itemCounts.getOrDefault(itemKey, 0) + item.getAmount());
                                if (!itemExamples.containsKey(itemKey)) {
                                    itemExamples.put(itemKey, item);
                                }
                                totalItems += item.getAmount();
                            }
                        }
                    }

                    if (totalItems > 0) {
                        commandSender.sendMessage("§7背包物品: §f共 " + totalItems + " 个物品");
                        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                            String chineseName = Main.itemTranslationManager.getChineseName(entry.getKey());
                            commandSender.sendMessage("§7  - §f" + chineseName + " §7x §f" + entry.getValue());
                        }
                    } else {
                        commandSender.sendMessage("§7背包物品: §f无");
                    }
                } catch (Exception e) {
                    commandSender.sendMessage("§7背包物品: §c无法解析 (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
                    e.printStackTrace();
                }
            } else {
                commandSender.sendMessage("§7背包物品: §f无数据");
            }
            
            // 显示末影箱信息
            Object enderChestData = data.get("enderchest");
            if (enderChestData != null) {
                try {
                    ItemStack[] enderChestContents = BukkitItemSerializer.deserialize(enderChestData.toString());
                    Map<String, Integer> itemCounts = new HashMap<>();
                    Map<String, ItemStack> itemExamples = new HashMap<>();
                    int totalItems = 0;

                    if (enderChestContents != null) {
                        for (ItemStack item : enderChestContents) {
                            if (item != null && item.getAmount() > 0) {
                                String itemKey = item.getType().toString();
                                itemCounts.put(itemKey, itemCounts.getOrDefault(itemKey, 0) + item.getAmount());
                                if (!itemExamples.containsKey(itemKey)) {
                                    itemExamples.put(itemKey, item);
                                }
                                totalItems += item.getAmount();
                            }
                        }
                    }

                    if (totalItems > 0) {
                        commandSender.sendMessage("§7末影箱物品: §f共 " + totalItems + " 个物品");
                        for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                            String chineseName = Main.itemTranslationManager.getChineseName(entry.getKey());
                            commandSender.sendMessage("§7  - §f" + chineseName + " §7x §f" + entry.getValue());
                        }
                    } else {
                        commandSender.sendMessage("§7末影箱物品: §f无");
                    }
                } catch (Exception e) {
                    commandSender.sendMessage("§7末影箱物品: §c无法解析 (" + e.getClass().getSimpleName() + ": " + e.getMessage() + ")");
                    e.printStackTrace();
                }
            } else {
                commandSender.sendMessage("§7末影箱物品: §f无数据");
            }
            
            // 显示生命值和饱食度
            if (Main.modulesManager.syncHealth) {
                Object health = data.get("health");
                commandSender.sendMessage("§7生命值: §f" + (health != null ? health.toString() : "未知"));
            } else {
                commandSender.sendMessage("§7生命值: §f未启用同步");
            }
            
            if (Main.modulesManager.syncSaturation) {
                Object saturation = data.get("saturation");
                Object foodLevel = data.get("food_level");

                if (foodLevel != null) {
                    int foodValue = Integer.parseInt(foodLevel.toString());
                    commandSender.sendMessage("§7饱食度: §f" + foodValue + " (还需要: " + (20 - foodValue) + ")");
                } else if (saturation != null) {
                    double saturationValue = Double.parseDouble(saturation.toString());
                    double neededSaturation = 20.0 - saturationValue;
                    commandSender.sendMessage("§7饱食度: §f" + String.format("%.2f", saturationValue) + " (还需要: " + String.format("%.2f", neededSaturation) + ")");
                } else {
                    commandSender.sendMessage("§7饱食度: §f未知");
                }
            } else {
                commandSender.sendMessage("§7饱食度: §f未启用同步");
            }

            if (Main.modulesManager.syncExp) {
                Object exp = data.get("exp");
                Object expLevel = data.get("exp_level");

                String levelStr = expLevel != null ? expLevel.toString() : "0";
                String progressStr = exp != null ? String.format("%.2f%%", Double.parseDouble(exp.toString()) * 100) : "0%";

                commandSender.sendMessage("§7经验等级: §f" + levelStr + " §7(进度: §f" + progressStr + "§7)");
            } else {
                commandSender.sendMessage("§7经验: §f未启用同步");
            }
            
            // 显示药水效果
            if (Main.modulesManager.syncPotionEffects) {
                Object potionEffectsData = data.get("potion_effects");
                if (potionEffectsData != null && !potionEffectsData.toString().isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> effects = de.lostesburger.mySqlPlayerBridge.Utils.Utils.deserializeObject(potionEffectsData.toString());
                        if (effects != null && !effects.isEmpty()) {
                            commandSender.sendMessage("§7药水效果数量: §f" + effects.size());
                            // 显示前3个药水效果作为示例
                            int count = 0;
                            for (Map<String, Object> effectMap : effects) {
                                if (count >= 3) {
                                    commandSender.sendMessage("§7... 还有更多效果");
                                    break;
                                }
                                String type = (String) effectMap.get("type");
                                int amplifier = (Integer) effectMap.get("amplifier");
                                int duration = (Integer) effectMap.get("duration");
                                commandSender.sendMessage("§7  - " + type + " " + amplifier + " (持续时间: " + duration + ")");
                                count++;
                            }
                        } else {
                            commandSender.sendMessage("§7药水效果: §f无");
                        }
                    } catch (Exception e) {
                        commandSender.sendMessage("§7药水效果: §c无法解析");
                    }
                } else {
                    commandSender.sendMessage("§7药水效果: §f无");
                }
            } else {
                commandSender.sendMessage("§7药水效果: §f未启用同步");
            }
            
            commandSender.sendMessage("§7========================");
            
        } catch (Exception e) {
            commandSender.sendMessage("§c查询玩家数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                options.add(player.getName());
            }
            // 可以考虑添加一些最近的离线玩家
        }
        return options;
    }
}
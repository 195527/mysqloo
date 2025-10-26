package de.lostesburger.mySqlPlayerBridge.Managers.MySqlData;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Collection;

import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;
import de.lostesburger.mySqlPlayerBridge.Handlers.Errors.MySqlErrorHandler;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Managers.Modules.ModulesManager;
import de.lostesburger.mySqlPlayerBridge.Storage.StorageManager;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

import java.sql.SQLException;
import java.io.IOException;


public class MySqlDataManager {
    public final StorageManager storageManager;

    public MySqlDataManager(StorageManager storageManager){
        this.storageManager = storageManager;
    }

    public boolean hasData(Player player){
        UUID uuid = player.getUniqueId();
        try {
            if(Main.DEBUG){
                System.out.println("Checking if player has data! Player: "+player.getName());
            }
            // Check if player exists in any of the tables
            return this.storageManager.entryExists(Main.INVENTORY_TABLE_NAME, Map.of("player_uuid", uuid.toString())) ||
                    this.storageManager.entryExists(Main.ENDERCHEST_TABLE_NAME, Map.of("player_uuid", uuid.toString())) ||
                    this.storageManager.entryExists(Main.ECONOMY_TABLE_NAME, Map.of("player_uuid", uuid.toString())) ||
                    this.storageManager.entryExists(Main.EXPERIENCE_TABLE_NAME, Map.of("player_uuid", uuid.toString())) ||
                    this.storageManager.entryExists(Main.HEALTH_FOOD_AIR_TABLE_NAME, Map.of("player_uuid", uuid.toString())) ||
                    this.storageManager.entryExists(Main.POTION_EFFECTS_TABLE_NAME, Map.of("player_uuid", uuid.toString()));
        } catch (SQLException e) {
            new MySqlErrorHandler().hasPlayerData(player);
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Object> getCurrentData(Player player){
        String gamemode = String.valueOf(player.getGameMode().ordinal());
        int exp_level = player.getLevel();
        float exp = player.getExp();
        double health = player.getHealth();
        float saturation = player.getSaturation();
        double money = 0.0;
        if(Main.modulesManager.syncVaultEconomy && Main.vaultManager != null){
            money = Main.vaultManager.getBalance(player);
        }

        Location location = player.getLocation();
        String world = Objects.requireNonNull(location.getWorld()).getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();

        HashMap<String, Object> map = new HashMap<>();
        map.put("server_type", Main.serverType);
        map.put("serialization_type", Main.serializationType.toString());
        map.put("gamemode", gamemode);
        map.put("exp_level", exp_level);
        map.put("exp", exp);
        map.put("health", health);
        map.put("saturation", saturation);

        // 添加药水效果
        Collection<PotionEffect> potionEffects = player.getActivePotionEffects();
        List<Map<String, Object>> serializedEffects = new ArrayList<>();
        for (PotionEffect effect : potionEffects) {
            Map<String, Object> effectMap = new HashMap<>();
            effectMap.put("type", effect.getType().getName());
            effectMap.put("amplifier", effect.getAmplifier());
            effectMap.put("duration", effect.getDuration());
            effectMap.put("ambient", effect.isAmbient());
            effectMap.put("showParticles", effect.hasParticles());
            effectMap.put("showIcon", effect.hasIcon());
            serializedEffects.add(effectMap);
        }
        String serializedPotionEffects = de.lostesburger.mySqlPlayerBridge.Utils.Utils.serializeObject(serializedEffects);
        map.put("potion_effects", serializedPotionEffects);
        map.put("money", money);
        map.put("world", world);
        map.put("x", x);
        map.put("y", y);
        map.put("z", z);
        map.put("yaw", yaw);
        map.put("pitch", pitch);

        String serializedInventory;
        String serializedEnderChest;
        String serializedArmor;
        try {
            // 检查YAML序列化器是否可用
            if (Main.yamlSerializer == null) {
                throw new RuntimeException("YAML序列化器未初始化。插件无法正常工作。");
            }

            // 使用YAML格式序列化整个物品栏（包括盔甲槽和副手槽）
            serializedInventory = Main.yamlSerializer.serialize(player.getInventory().getContents());
            serializedEnderChest = Main.yamlSerializer.serialize(player.getEnderChest().getContents());

            // 不再单独序列化盔甲槽，因为已经包含在完整的物品栏中了
            serializedArmor = ""; // 保留此字段以保持数据库兼容性，但留空

            if(Main.DEBUG){
                System.out.println("使用YAML格式序列化物品数据");
                System.out.println("Inv前50字符: " + serializedInventory.substring(0, Math.min(50, serializedInventory.length())) + "...");
                System.out.println("EnderChest前50字符: " + serializedEnderChest.substring(0, Math.min(50, serializedEnderChest.length())) + "...");
                System.out.println("Armor前50字符: " + serializedArmor.substring(0, Math.min(50, serializedArmor.length())) + "...");
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("序列化玩家物品时出错: " + e.getMessage());
            Main.getInstance().getLogger().severe("请确保序列化器已正确初始化");
            e.printStackTrace();
            throw new RuntimeException("物品序列化失败", e);
        }

        map.put("inventory", serializedInventory);
        map.put("enderchest", serializedEnderChest);
        map.put("armor", serializedArmor);

        return map;
    }

    // New method to save player data to multiple tables
    public void savePlayerDataToMultipleTables(Player player) {
        if(!this.hasData(player) && Main.config.getBoolean("settings.no-entry-protection")){ return; }

        UUID uuid = player.getUniqueId();
        String playerName = player.getName();
        String currentTime = String.valueOf(System.currentTimeMillis());

        HashMap<String, Object> data = this.getCurrentData(player);

        try {
            // Save inventory data
            HashMap<String, Object> inventoryData = new HashMap<>();
            inventoryData.put("player_uuid", uuid.toString());
            inventoryData.put("player_name", playerName);
            inventoryData.put("inventory", data.get("inventory"));
            inventoryData.put("armor", data.get("armor"));
            inventoryData.put("hotbar_slot", 0);
            inventoryData.put("gamemode", data.get("gamemode"));
            inventoryData.put("sync_complete", "true");
            inventoryData.put("last_seen", currentTime);

            this.storageManager.setOrUpdateEntry(Main.INVENTORY_TABLE_NAME,
                    Map.of("player_uuid", uuid.toString()), inventoryData);

            // Save enderchest data
            HashMap<String, Object> enderchestData = new HashMap<>();
            enderchestData.put("player_uuid", uuid.toString());
            enderchestData.put("player_name", playerName);
            enderchestData.put("enderchest", data.get("enderchest"));
            enderchestData.put("sync_complete", "true");
            enderchestData.put("last_seen", currentTime);

            this.storageManager.setOrUpdateEntry(Main.ENDERCHEST_TABLE_NAME,
                    Map.of("player_uuid", uuid.toString()), enderchestData);

            // Save economy data
            HashMap<String, Object> economyData = new HashMap<>();
            economyData.put("player_uuid", uuid.toString());
            economyData.put("player_name", playerName);
            economyData.put("money", data.get("money"));
            economyData.put("offline_money", 0.0);
            economyData.put("sync_complete", "true");
            economyData.put("last_seen", currentTime);

            this.storageManager.setOrUpdateEntry(Main.ECONOMY_TABLE_NAME,
                    Map.of("player_uuid", uuid.toString()), economyData);

            // Save experience data
            HashMap<String, Object> experienceData = new HashMap<>();
            experienceData.put("player_uuid", uuid.toString());
            experienceData.put("player_name", playerName);
            experienceData.put("exp", data.get("exp"));
            experienceData.put("exp_to_level", 0);
            experienceData.put("total_exp", 0);
            experienceData.put("exp_lvl", data.get("exp_level"));
            experienceData.put("sync_complete", "true");
            experienceData.put("last_seen", currentTime);

            this.storageManager.setOrUpdateEntry(Main.EXPERIENCE_TABLE_NAME,
                    Map.of("player_uuid", uuid.toString()), experienceData);

            // Save health/food/air data
            HashMap<String, Object> healthData = new HashMap<>();
            healthData.put("player_uuid", uuid.toString());
            healthData.put("player_name", playerName);
            healthData.put("health", data.get("health"));
            healthData.put("health_scale", 0.0);
            healthData.put("max_health", 20.0);
            healthData.put("food", 20);
            healthData.put("saturation", data.get("saturation"));
            healthData.put("air", 300);
            healthData.put("max_air", 300);
            healthData.put("sync_complete", "true");
            healthData.put("last_seen", currentTime);

            this.storageManager.setOrUpdateEntry(Main.HEALTH_FOOD_AIR_TABLE_NAME,
                    Map.of("player_uuid", uuid.toString()), healthData);

            // Save potion effects data
            HashMap<String, Object> potionData = new HashMap<>();
            potionData.put("player_uuid", uuid.toString());
            potionData.put("player_name", playerName);
            potionData.put("potion_effects", data.get("potion_effects"));
            potionData.put("sync_complete", "true");
            potionData.put("last_seen", currentTime);

            this.storageManager.setOrUpdateEntry(Main.POTION_EFFECTS_TABLE_NAME,
                    Map.of("player_uuid", uuid.toString()), potionData);

        } catch (SQLException e) {
            new MySqlErrorHandler().savePlayerData(player, data);
            throw new RuntimeException(e);
        }
    }

    public void savePlayerData(Player player){
        savePlayerDataToMultipleTables(player);
    }

    @SuppressWarnings("unchecked")
    public HashMap<String, Object> getPlayerDataFromDB(Player player) throws RuntimeException {
        if(!hasData(player)){
            throw new RuntimeException("Player has no data: " + player.getName());
        }

        try {
            // Get data from all tables and merge them
            HashMap<String, Object> mergedData = new HashMap<>();

            // Get inventory data
            try {
                HashMap<String, Object> inventoryData = (HashMap<String, Object>) this.storageManager.getEntry(Main.INVENTORY_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (inventoryData != null) mergedData.putAll(inventoryData);
            } catch (SQLException ignored) {}

            // Get enderchest data
            try {
                HashMap<String, Object> enderchestData = (HashMap<String, Object>) this.storageManager.getEntry(Main.ENDERCHEST_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (enderchestData != null) mergedData.putAll(enderchestData);
            } catch (SQLException ignored) {}

            // Get economy data
            try {
                HashMap<String, Object> economyData = (HashMap<String, Object>) this.storageManager.getEntry(Main.ECONOMY_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (economyData != null) mergedData.putAll(economyData);
            } catch (SQLException ignored) {}

            // Get experience data
            try {
                HashMap<String, Object> experienceData = (HashMap<String, Object>) this.storageManager.getEntry(Main.EXPERIENCE_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (experienceData != null) mergedData.putAll(experienceData);
            } catch (SQLException ignored) {}

            // Get health/food/air data
            try {
                HashMap<String, Object> healthData = (HashMap<String, Object>) this.storageManager.getEntry(Main.HEALTH_FOOD_AIR_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (healthData != null) mergedData.putAll(healthData);
            } catch (SQLException ignored) {}

            // Get potion effects data
            try {
                HashMap<String, Object> potionData = (HashMap<String, Object>) this.storageManager.getEntry(Main.POTION_EFFECTS_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (potionData != null) mergedData.putAll(potionData);
            } catch (SQLException ignored) {}

            return mergedData;
        } catch (Exception e) {
            new MySqlErrorHandler().getPlayerData(player);
            throw new RuntimeException(e);
        }
    }

    public boolean checkDatabaseConnection(){ return storageManager.isConnectionAlive(); }

    public void applyDataToPlayer(Player player){
        if(Main.DEBUG){
            System.out.println("attempting to applyDataToPlayer player: "+player.getName());
        }

        HashMap<String, Object> data = this.getPlayerDataFromDB(player);

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            ModulesManager modules = Main.modulesManager;

            if(modules.syncInventory){
                try {
                    if (data.containsKey("inventory")) {
                        String inventoryData = String.valueOf(data.get("inventory"));
                        if (inventoryData != null && !inventoryData.isEmpty()) {
                            // 使用YAML反序列化
                            if (Main.yamlSerializer == null) {
                                throw new RuntimeException("YAML序列化器未初始化");
                            }
                            ItemStack[] inventoryContents = Main.yamlSerializer.deserialize(inventoryData);
                            
                            // 检查是否成功反序列化了物品
                            if (inventoryContents != null && inventoryContents.length > 0) {
                                // 检查是否包含非空物品
                                boolean hasNonNullItems = false;
                                for (ItemStack item : inventoryContents) {
                                    if (item != null) {
                                        hasNonNullItems = true;
                                        break;
                                    }
                                }
                                
                                if (hasNonNullItems) {
                                    player.getInventory().setContents(inventoryContents);
                                    Main.getInstance().getLogger().info("成功加载玩家 " + player.getName() + " 的背包数据，包含 " + inventoryContents.length + " 个槽位");
                                } else {
                                    Main.getInstance().getLogger().warning("背包数据反序列化结果全为空物品");
                                }
                            } else {
                                Main.getInstance().getLogger().warning("背包数据反序列化结果为空或长度为0");
                            }
                        } else {
                            Main.getInstance().getLogger().warning("背包数据为空，跳过加载");
                        }
                    }
                } catch (Exception e) {
                    Main.getInstance().getLogger().severe("无法反序列化玩家背包数据: " + e.getMessage());
                    e.printStackTrace();
                    player.kickPlayer(Chat.getMessage("sync-failed"));
                    throw new RuntimeException(e);
                }
            }

            if(modules.syncEnderChest){
                try {
                    if (data.containsKey("enderchest")) {
                        String enderChestData = String.valueOf(data.get("enderchest"));
                        if (enderChestData != null && !enderChestData.isEmpty()) {
                            // 使用YAML反序列化
                            if (Main.yamlSerializer == null) {
                                throw new RuntimeException("YAML序列化器未初始化");
                            }
                            ItemStack[] enderChestContents = Main.yamlSerializer.deserialize(enderChestData);
                            player.getEnderChest().setContents(enderChestContents);
                        } else {
                            Main.getInstance().getLogger().warning("末影箱数据为空，跳过加载");
                        }
                    }
                } catch (Exception e) {
                    Main.getInstance().getLogger().severe("无法反序列化玩家末影箱数据: " + e.getMessage());
                    e.printStackTrace();
                    player.kickPlayer(Chat.getMessage("sync-failed"));
                    throw new RuntimeException(e);
                }
            }

            if(modules.syncGamemode){
                if (data.containsKey("gamemode")) {
                    String gamemodeStr = String.valueOf(data.get("gamemode"));
                    try {
                        player.setGameMode(GameMode.valueOf(gamemodeStr));
                    } catch (IllegalArgumentException e) {
                        try {
                            int gamemodeOrdinal = Integer.parseInt(gamemodeStr);
                            GameMode[] values = GameMode.values();
                            if (gamemodeOrdinal >= 0 && gamemodeOrdinal < values.length) {
                                player.setGameMode(values[gamemodeOrdinal]);
                            }
                        } catch (NumberFormatException ignored) {
                            player.setGameMode(GameMode.SURVIVAL);
                        }
                    }
                }
            }

            if(modules.syncHealth){
                if (data.containsKey("health")) {
                    Object healthObj = data.get("health");
                    Double health;
                    if (healthObj instanceof String) {
                        health = Double.parseDouble((String) healthObj);
                    } else {
                        // 修复类型转换错误：确保正确处理各种数字类型
                        if (healthObj instanceof Double) {
                            health = (Double) healthObj;
                        } else if (healthObj instanceof Float) {
                            health = ((Float) healthObj).doubleValue();
                        } else if (healthObj instanceof Integer) {
                            health = ((Integer) healthObj).doubleValue();
                        } else {
                            throw new IllegalArgumentException("Cannot convert " + healthObj.getClass() + " to Double");
                        }
                    }
                    player.setHealth(health);
                }
            }

            if(modules.syncSaturation){
                if (data.containsKey("saturation")) {
                    Object saturationObj = data.get("saturation");
                    Float saturation;
                    if (saturationObj instanceof String) {
                        saturation = Float.parseFloat((String) saturationObj);
                    } else {
                        // 修复类型转换错误：使用convertToFloat方法处理各种数字类型
                        saturation = convertToFloat(saturationObj);
                    }
                    player.setSaturation(saturation);
                }
            }

            // 恢复药水效果
            if(modules.syncPotionEffects && data.containsKey("potion_effects")) {
                try {
                    String serializedEffects = (String) data.get("potion_effects");
                    if (!serializedEffects.isEmpty()) {
                        List<Map<String, Object>> effects = de.lostesburger.mySqlPlayerBridge.Utils.Utils.deserializeObject(serializedEffects);
                        if (effects != null) {
                            for (Map<String, Object> effectMap : effects) {
                                PotionEffectType type = PotionEffectType.getByName((String) effectMap.get("type"));
                                if (type != null) {
                                    int amplifier = (Integer) effectMap.get("amplifier");
                                    int duration = (Integer) effectMap.get("duration");
                                    boolean ambient = (Boolean) effectMap.get("ambient");
                                    boolean showParticles = (Boolean) effectMap.get("showParticles");
                                    boolean showIcon = (Boolean) effectMap.get("showIcon");
                                    PotionEffect effect = new PotionEffect(type, duration, amplifier, ambient, showParticles, showIcon);
                                    player.addPotionEffect(effect);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    player.kickPlayer(Chat.getMessage("sync-failed"));
                    throw new RuntimeException(e);
                }
            }

            if(modules.syncVaultEconomy){
                if (Main.vaultManager != null && data.containsKey("money")) {
                    Object moneyObj = data.get("money");
                    Double money;
                    if (moneyObj instanceof String) {
                        money = Double.parseDouble((String) moneyObj);
                    } else {
                        // 修复类型转换错误：确保正确处理各种数字类型
                        if (moneyObj instanceof Double) {
                            money = (Double) moneyObj;
                        } else if (moneyObj instanceof Float) {
                            money = ((Float) moneyObj).doubleValue();
                        } else if (moneyObj instanceof Integer) {
                            money = ((Integer) moneyObj).doubleValue();
                        } else {
                            throw new IllegalArgumentException("Cannot convert " + moneyObj.getClass() + " to Double");
                        }
                    }
                    Main.vaultManager.setBalance(player, money);
                }
            }

            if(modules.syncExp){
                if (data.containsKey("exp") && data.containsKey("exp_level")) {
                    Object expObj = data.get("exp");
                    Float exp;
                    if (expObj instanceof String) {
                        exp = Float.parseFloat((String) expObj);
                    } else {
                        // 修复类型转换错误：使用convertToFloat方法处理各种数字类型
                        exp = convertToFloat(expObj);
                    }
                    player.setExp(exp);
                    player.setLevel((Integer) data.get("exp_level"));
                }
            }

            if(modules.syncLocation){
                if (data.containsKey("world") && data.containsKey("x") && data.containsKey("y") && data.containsKey("z")) {
                    World world = Bukkit.getWorld((String) data.get("world"));

                    Object xObj = data.get("x");
                    Object yObj = data.get("y");
                    Object zObj = data.get("z");
                    Double x, y, z;

                    if (xObj instanceof String) {
                        x = Double.parseDouble((String) xObj);
                    } else {
                        // 修复类型转换错误：确保正确处理各种数字类型
                        if (xObj instanceof Double) {
                            x = (Double) xObj;
                        } else if (xObj instanceof Float) {
                            x = ((Float) xObj).doubleValue();
                        } else if (xObj instanceof Integer) {
                            x = ((Integer) xObj).doubleValue();
                        } else {
                            throw new IllegalArgumentException("Cannot convert " + xObj.getClass() + " to Double");
                        }
                    }

                    if (yObj instanceof String) {
                        y = Double.parseDouble((String) yObj);
                    } else {
                        // 修复类型转换错误：确保正确处理各种数字类型
                        if (yObj instanceof Double) {
                            y = (Double) yObj;
                        } else if (yObj instanceof Float) {
                            y = ((Float) yObj).doubleValue();
                        } else if (yObj instanceof Integer) {
                            y = ((Integer) yObj).doubleValue();
                        } else {
                            throw new IllegalArgumentException("Cannot convert " + yObj.getClass() + " to Double");
                        }
                    }

                    if (zObj instanceof String) {
                        z = Double.parseDouble((String) zObj);
                    } else {
                        // 修复类型转换错误：确保正确处理各种数字类型
                        if (zObj instanceof Double) {
                            z = (Double) zObj;
                        } else if (zObj instanceof Float) {
                            z = ((Float) zObj).doubleValue();
                        } else if (zObj instanceof Integer) {
                            z = ((Integer) zObj).doubleValue();
                        } else {
                            throw new IllegalArgumentException("Cannot convert " + zObj.getClass() + " to Double");
                        }
                    }

                    Location location = new Location(world, x, y, z,
                            data.containsKey("yaw") ? convertToFloat(data.get("yaw")) : 0.0f,
                            data.containsKey("pitch") ? convertToFloat(data.get("pitch")) : 0.0f);

                    boolean isFolia = false;
                    try {
                        Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                        isFolia = true;
                    } catch (ClassNotFoundException ignored) {}

                    if (isFolia){
                        try {
                            Method teleportAsync = player.getClass().getMethod("teleportAsync", Location.class);
                            CompletableFuture<Boolean> future = (CompletableFuture<Boolean>) teleportAsync.invoke(player, location);

                            future.thenAccept(success -> {
                                if (!success) {
                                    Bukkit.getLogger().warning("Failed to teleport player! Player: " + player.getName());
                                }
                            });
                        } catch (NoSuchMethodException e) {} catch (Exception e) {
                            e.printStackTrace();
                        }
                    }else {
                        player.teleport(location);
                    }
                }
            }
        });
    }

    public void saveAllOnlinePlayers(){
        for (Player player : Bukkit.getOnlinePlayers()){
            this.savePlayerData(player);
        }
    }

    public void saveAllOnlinePlayersAsync(){
        for (Player player : Bukkit.getOnlinePlayers()){
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> { this.savePlayerData(player);});
        }
    }

    private Float convertToFloat(Object obj) {
        if (obj instanceof String) {
            return Float.parseFloat((String) obj);
        } else if (obj instanceof Float) {
            return (Float) obj;
        } else if (obj instanceof Double) {
            return ((Double) obj).floatValue();
        } else if (obj instanceof Integer) {
            return ((Integer) obj).floatValue();
        } else {
            throw new IllegalArgumentException("Cannot convert " + obj.getClass() + " to Float");
        }
    }
}
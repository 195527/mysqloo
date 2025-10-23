package de.lostesburger.mySqlPlayerBridge.Managers.MySqlData;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Collection;

import de.lostesburger.mySqlPlayerBridge.Database.DatabaseManager;
import de.lostesburger.mySqlPlayerBridge.Handlers.Errors.MySqlErrorHandler;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Managers.Modules.ModulesManager;
import de.lostesburger.mySqlPlayerBridge.Serialization.BukkitItemSerializer;
import de.lostesburger.mySqlPlayerBridge.Utils.ItemSerializationUtils;
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


public class MySqlDataManager {
    public final DatabaseManager databaseManager;

    public MySqlDataManager(DatabaseManager manager){
        databaseManager = manager;
    }

    public boolean hasData(Player player){
        UUID uuid = player.getUniqueId();
        try {
            if(Main.DEBUG){
                System.out.println("Checking if player has data! Player: "+player.getName());
            }
            // Check if player exists in any of the tables
            return this.databaseManager.entryExists(Main.INVENTORY_TABLE_NAME, Map.of("player_uuid", uuid.toString())) ||
                   this.databaseManager.entryExists(Main.ENDERCHEST_TABLE_NAME, Map.of("player_uuid", uuid.toString())) ||
                   this.databaseManager.entryExists(Main.ECONOMY_TABLE_NAME, Map.of("player_uuid", uuid.toString())) ||
                   this.databaseManager.entryExists(Main.EXPERIENCE_TABLE_NAME, Map.of("player_uuid", uuid.toString())) ||
                   this.databaseManager.entryExists(Main.HEALTH_FOOD_AIR_TABLE_NAME, Map.of("player_uuid", uuid.toString())) ||
                   this.databaseManager.entryExists(Main.POTION_EFFECTS_TABLE_NAME, Map.of("player_uuid", uuid.toString()));
        } catch (SQLException e) {
            new MySqlErrorHandler().hasPlayerData(player);
            throw new RuntimeException(e);
        }
    }

    public HashMap<String, Object> getCurrentData(Player player){
        // Vielleicht Async machen ?? (wird wahrscheinlich schon async called)

        String gamemode = String.valueOf(player.getGameMode().ordinal()); // 修改为使用ordinal
        int exp_level = player.getLevel();
        float exp = player.getExp();
        double health = player.getHealth();
        float saturation = player.getSaturation();
        double money = 0.0;
        if(Main.modulesManager.syncVaultEconomy){
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
        // 序列化药水效果
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
            // 使用Bukkit标准序列化方法确保与其他插件兼容
            serializedInventory = BukkitItemSerializer.serialize(player.getInventory().getContents());
            serializedEnderChest = BukkitItemSerializer.serialize(player.getEnderChest().getContents());
            ItemStack[] armorContents = new ItemStack[]{
                    player.getInventory().getBoots(),
                    player.getInventory().getLeggings(),
                    player.getInventory().getChestplate(),
                    player.getInventory().getHelmet()
            };
            serializedArmor = BukkitItemSerializer.serialize(armorContents);

            if(Main.DEBUG){
                System.out.println("Inv: "+serializedInventory);
                System.out.println("EnderChest"+serializedEnderChest);
                System.out.println("Armor: "+serializedArmor);
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().severe("序列化玩家物品时出错: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
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
            inventoryData.put("hotbar_slot", 0); // Default value
            inventoryData.put("gamemode", data.get("gamemode")); // 修复：直接使用已序列化的游戏模式
            inventoryData.put("sync_complete", "true");
            inventoryData.put("last_seen", currentTime);
            
            this.databaseManager.setOrUpdateEntry(Main.INVENTORY_TABLE_NAME, 
                Map.of("player_uuid", uuid.toString()), inventoryData);
            
            // Save enderchest data
            HashMap<String, Object> enderchestData = new HashMap<>();
            enderchestData.put("player_uuid", uuid.toString());
            enderchestData.put("player_name", playerName);
            enderchestData.put("enderchest", data.get("enderchest"));
            enderchestData.put("sync_complete", "true");
            enderchestData.put("last_seen", currentTime);
            
            this.databaseManager.setOrUpdateEntry(Main.ENDERCHEST_TABLE_NAME, 
                Map.of("player_uuid", uuid.toString()), enderchestData);
            
            // Save economy data
            HashMap<String, Object> economyData = new HashMap<>();
            economyData.put("player_uuid", uuid.toString());
            economyData.put("player_name", playerName);
            economyData.put("money", data.get("money"));
            economyData.put("offline_money", 0.0);
            economyData.put("sync_complete", "true");
            economyData.put("last_seen", currentTime);
            
            this.databaseManager.setOrUpdateEntry(Main.ECONOMY_TABLE_NAME, 
                Map.of("player_uuid", uuid.toString()), economyData);
            
            // Save experience data
            HashMap<String, Object> experienceData = new HashMap<>();
            experienceData.put("player_uuid", uuid.toString());
            experienceData.put("player_name", playerName);
            experienceData.put("exp", data.get("exp"));
            experienceData.put("exp_to_level", 0); // Default value
            experienceData.put("total_exp", 0); // Default value
            experienceData.put("exp_lvl", data.get("exp_level"));
            experienceData.put("sync_complete", "true");
            experienceData.put("last_seen", currentTime);
            
            this.databaseManager.setOrUpdateEntry(Main.EXPERIENCE_TABLE_NAME, 
                Map.of("player_uuid", uuid.toString()), experienceData);
            
            // Save health/food/air data
            HashMap<String, Object> healthData = new HashMap<>();
            healthData.put("player_uuid", uuid.toString());
            healthData.put("player_name", playerName);
            healthData.put("health", data.get("health"));
            healthData.put("health_scale", 0.0);
            healthData.put("max_health", 20.0);
            healthData.put("food", 20); // Default value
            healthData.put("saturation", data.get("saturation"));
            healthData.put("air", 300); // Default value
            healthData.put("max_air", 300); // Default value
            healthData.put("sync_complete", "true");
            healthData.put("last_seen", currentTime);
            
            this.databaseManager.setOrUpdateEntry(Main.HEALTH_FOOD_AIR_TABLE_NAME, 
                Map.of("player_uuid", uuid.toString()), healthData);
            
            // Save potion effects data
            HashMap<String, Object> potionData = new HashMap<>();
            potionData.put("player_uuid", uuid.toString());
            potionData.put("player_name", playerName);
            potionData.put("potion_effects", data.get("potion_effects"));
            potionData.put("sync_complete", "true");
            potionData.put("last_seen", currentTime);
            
            this.databaseManager.setOrUpdateEntry(Main.POTION_EFFECTS_TABLE_NAME, 
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
                HashMap<String, Object> inventoryData = (HashMap<String, Object>) this.databaseManager.getEntry(Main.INVENTORY_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (inventoryData != null) mergedData.putAll(inventoryData);
            } catch (SQLException ignored) {}
            
            // Get enderchest data
            try {
                HashMap<String, Object> enderchestData = (HashMap<String, Object>) this.databaseManager.getEntry(Main.ENDERCHEST_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (enderchestData != null) mergedData.putAll(enderchestData);
            } catch (SQLException ignored) {}
            
            // Get economy data
            try {
                HashMap<String, Object> economyData = (HashMap<String, Object>) this.databaseManager.getEntry(Main.ECONOMY_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (economyData != null) mergedData.putAll(economyData);
            } catch (SQLException ignored) {}
            
            // Get experience data
            try {
                HashMap<String, Object> experienceData = (HashMap<String, Object>) this.databaseManager.getEntry(Main.EXPERIENCE_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (experienceData != null) mergedData.putAll(experienceData);
            } catch (SQLException ignored) {}
            
            // Get health/food/air data
            try {
                HashMap<String, Object> healthData = (HashMap<String, Object>) this.databaseManager.getEntry(Main.HEALTH_FOOD_AIR_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (healthData != null) mergedData.putAll(healthData);
            } catch (SQLException ignored) {}
            
            // Get potion effects data
            try {
                HashMap<String, Object> potionData = (HashMap<String, Object>) this.databaseManager.getEntry(Main.POTION_EFFECTS_TABLE_NAME, Map.of("player_uuid", player.getUniqueId().toString()));
                if (potionData != null) mergedData.putAll(potionData);
            } catch (SQLException ignored) {}
            
            return mergedData;
        } catch (Exception e) {
            new MySqlErrorHandler().getPlayerData(player);
            throw new RuntimeException(e);
        }
    }

    public boolean checkDatabaseConnection(){ return databaseManager.isConnectionAlive(); }

    public void applyDataToPlayer(Player player){
        if(Main.DEBUG){
            System.out.println("attempting to applyDataToPlayer player: "+player.getName());
        }

        HashMap<String, Object> data = this.getPlayerDataFromDB(player);

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            ModulesManager modules = Main.modulesManager;

            if(modules.syncInventory){
                try {
                    // Check if inventory data exists
                    if (data.containsKey("inventory")) {
                        String inventoryData = String.valueOf(data.get("inventory"));
                        if (ItemSerializationUtils.isValidItemData(inventoryData)) {
                            ItemStack[] inventoryContents = ItemSerializationUtils.detectAndDeserialize(inventoryData);
                            player.getInventory().setContents(inventoryContents);
                        } else {
                            Main.getInstance().getLogger().warning("无效的背包数据，跳过加载");
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
                    // Check if enderchest data exists
                    if (data.containsKey("enderchest")) {
                        String enderChestData = String.valueOf(data.get("enderchest"));
                        if (ItemSerializationUtils.isValidItemData(enderChestData)) {
                            ItemStack[] enderChestContents = ItemSerializationUtils.detectAndDeserialize(enderChestData);
                            player.getEnderChest().setContents(enderChestContents);
                        } else {
                            Main.getInstance().getLogger().warning("无效的末影箱数据，跳过加载");
                        }
                    }
                } catch (Exception e) {
                    Main.getInstance().getLogger().severe("无法反序列化玩家末影箱数据: " + e.getMessage());
                    e.printStackTrace();
                    player.kickPlayer(Chat.getMessage("sync-failed"));
                    throw new RuntimeException(e);
                }
            }
            if(modules.syncArmorSlots){
                try {
                    // Check if armor data exists
                    if (data.containsKey("armor")) {
                        String armorData = String.valueOf(data.get("armor"));
                        if (ItemSerializationUtils.isValidItemData(armorData)) {
                            ItemStack[] armorContents = ItemSerializationUtils.detectAndDeserialize(armorData);
                            player.getInventory().setArmorContents(armorContents);
                        } else {
                            Main.getInstance().getLogger().warning("无效的装备数据，跳过加载");
                        }
                    }
                } catch (Exception e) {
                    Main.getInstance().getLogger().severe("无法反序列化玩家装备数据: " + e.getMessage());
                    e.printStackTrace();
                    player.kickPlayer(Chat.getMessage("sync-failed"));
                    throw new RuntimeException(e);
                }
            }
            if(modules.syncGamemode){
                // Check if gamemode data exists
                if (data.containsKey("gamemode")) {
                    String gamemodeStr = String.valueOf(data.get("gamemode"));
                    try {
                        // 尝试直接通过枚举名称获取
                        player.setGameMode(GameMode.valueOf(gamemodeStr));
                    } catch (IllegalArgumentException e) {
                        // 如果失败，尝试通过序号获取
                        try {
                            int gamemodeOrdinal = Integer.parseInt(gamemodeStr);
                            GameMode[] values = GameMode.values();
                            if (gamemodeOrdinal >= 0 && gamemodeOrdinal < values.length) {
                                player.setGameMode(values[gamemodeOrdinal]);
                            }
                        } catch (NumberFormatException ignored) {
                            // 如果两种方式都失败，默认设置为生存模式
                            player.setGameMode(GameMode.SURVIVAL);
                        }
                    }
                }
            }
            if(modules.syncHealth){
                // Check if health data exists
                if (data.containsKey("health")) {
                    Object healthObj = data.get("health");
                    Double health;
                    if (healthObj instanceof String) {
                        health = Double.parseDouble((String) healthObj);
                    } else {
                        health = (Double) healthObj;
                    }
                    player.setHealth(health);
                }
            }
            if(modules.syncSaturation){
                // Check if saturation data exists
                if (data.containsKey("saturation")) {
                    Object saturationObj = data.get("saturation");
                    Float saturation;
                    if (saturationObj instanceof String) {
                        saturation = Float.parseFloat((String) saturationObj);
                    } else {
                        saturation = (Float) saturationObj;
                    }
                    player.setSaturation(saturation);
                }
            }
            // 恢复药水效果
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
                // Check if money data exists
                if (data.containsKey("money")) {
                    Object moneyObj = data.get("money");
                    Double money;
                    if (moneyObj instanceof String) {
                        money = Double.parseDouble((String) moneyObj);
                    } else {
                        money = (Double) moneyObj;
                    }
                    Main.vaultManager.setBalance(player, money);
                }
            }
            if(modules.syncExp){
                // Check if experience data exists
                if (data.containsKey("exp") && data.containsKey("exp_level")) {
                    Object expObj = data.get("exp");
                    Float exp;
                    if (expObj instanceof String) {
                        exp = Float.parseFloat((String) expObj);
                    } else {
                        exp = (Float) expObj;
                    }
                    player.setExp(exp);
                    player.setLevel((Integer) data.get("exp_level"));
                }
            }
            if(modules.syncLocation){
                // Check if location data exists
                if (data.containsKey("world") && data.containsKey("x") && data.containsKey("y") && data.containsKey("z")) {
                    World world = Bukkit.getWorld((String) data.get("world"));
                    
                    // Handle coordinate type conversions
                    Object xObj = data.get("x");
                    Object yObj = data.get("y");
                    Object zObj = data.get("z");
                    Double x, y, z;
                    
                    if (xObj instanceof String) {
                        x = Double.parseDouble((String) xObj);
                    } else {
                        x = (Double) xObj;
                    }
                    
                    if (yObj instanceof String) {
                        y = Double.parseDouble((String) yObj);
                    } else {
                        y = (Double) yObj;
                    }
                    
                    if (zObj instanceof String) {
                        z = Double.parseDouble((String) zObj);
                    } else {
                        z = (Double) zObj;
                    }
                    
                    Location location = new Location(world, x, y, z, 
                        data.containsKey("yaw") ? convertToFloat(data.get("yaw")) : 0.0f, 
                        data.containsKey("pitch") ? convertToFloat(data.get("pitch")) : 0.0f);

                    // Check if running Folia
                    boolean isFolia = false;
                    try {
                        Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                        isFolia = true;
                    } catch (ClassNotFoundException ignored) {}
                    
                    if (isFolia){
                        try {
                            // For Folia, we need to use the regional scheduler
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
    
    /**
     * 将对象转换为Float类型，处理字符串和数字类型
     * @param obj 要转换的对象
     * @return 转换后的Float值
     */
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
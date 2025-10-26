package de.lostesburger.mySqlPlayerBridge.Managers.PlayerBridge;

import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Managers.MySqlData.MySqlDataManager;
import de.lostesburger.mySqlPlayerBridge.NoEntryProtection.NoEntryProtection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public class PlayerBridgeManager implements Listener {
    private final MySqlDataManager mySqlDataManager;
    private BukkitTask autoSyncTask;

    public PlayerBridgeManager(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        this.startAutoSyncTask();
        // 修复：检查mySqlConnectionHandler是否为null，如果不为null则使用它，否则使用storageManager创建DataManager
        if (Main.mySqlConnectionHandler != null) {
            this.mySqlDataManager = Main.mySqlConnectionHandler.getMySqlDataManager();
        } else {
            this.mySqlDataManager = new MySqlDataManager(Main.storageManager);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            if(this.mySqlDataManager.hasData(player)){
                this.mySqlDataManager.applyDataToPlayer(player);
                Main.playerManager.sendDataLoadedMessage(player);
            }else {
                if(!NoEntryProtection.isTriggered(player)){
                    this.mySqlDataManager.savePlayerData(player);
                    Main.playerManager.sendCreatedDataMessage(player);
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLeave(PlayerQuitEvent event){
        Player player = event.getPlayer();
        this.mySqlDataManager.savePlayerData(player);
    }

    private void startAutoSyncTask(){
        assert this.mySqlDataManager != null;
        autoSyncTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Main.getInstance(), () -> {
            // 修复：检查mySqlConnectionHandler是否为null
            if (Main.mySqlConnectionHandler != null) {
                Main.mySqlConnectionHandler.getMySqlDataManager().saveAllOnlinePlayersAsync();
            } else {
                // 对于非MySQL存储，直接使用mySqlDataManager
                this.mySqlDataManager.saveAllOnlinePlayersAsync();
            }
        }, 20, Main.modulesManager.syncTaskDelay); // 使用配置文件中的同步延迟值，而不是硬编码
        
        Main.schedulers.add(() -> autoSyncTask.cancel());
    }
}
package de.lostesburger.mySqlPlayerBridge.Utils.Checks;

import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;

public class DatabaseConfigCheck implements Listener {
    private final FileConfiguration mysql;
    private final boolean isSetup;
    private BukkitTask task;

    public DatabaseConfigCheck(FileConfiguration mysqlConf){
        this.mysql = mysqlConf;
        this.isSetup = this.check();

        if(!this.isSetup){
            Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
            task = Bukkit.getScheduler().runTaskTimer(Main.getInstance(), () -> {
                Bukkit.broadcastMessage(Chat.getMessage("no-database-config-error"));
            }, 40, 40);
            Main.schedulers.add(() -> task.cancel());
        }
    }

    private boolean check() {
        if(Objects.requireNonNull(this.mysql.getString("host")).isEmpty() || this.mysql.getString("host") == null){
            return false;
        }

        if(Objects.requireNonNull(this.mysql.getString("database")).isEmpty() || this.mysql.getString("database") == null){
            return false;
        }

        if(Objects.requireNonNull(this.mysql.getString("user")).isEmpty() || this.mysql.getString("user") == null){
            return false;
        }

        return true;
    }

    public boolean isSetup(){return this.isSetup; }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        player.sendTitle("§cMySqlPlayerBridge 错误", Chat.getMessageWithoutPrefix("no-database-config-error"), 0, 120*20, 0);
    }

}
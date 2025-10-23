package de.lostesburger.mySqlPlayerBridge.NoEntryProtection;


import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class NoEntryProtection {
    private static void triggerProtection(Player player){
        String message = Chat.getMessage("no-entry-protection-kick");

        if(player == null) return;
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            player.sendMessage(Chat.msg(message));
            player.kickPlayer(message);
        });


    }
    public static boolean isTriggered(Player player) {
        boolean triggered = Main.config.getBoolean("settings.no-entry-protection");
        if(triggered){
            triggerProtection(player);
            Main.getInstance().getLogger().log(Level.WARNING, "No entry protection triggert! A player tried to join without existing data. Player: "+player.getName());
        }
        return triggered;
    }
}
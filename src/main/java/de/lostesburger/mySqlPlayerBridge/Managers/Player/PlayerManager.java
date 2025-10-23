package de.lostesburger.mySqlPlayerBridge.Managers.Player;

import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.entity.Player;

public class PlayerManager {


    public static void sendCreatedDataMessage(Player player){
        player.sendMessage(Chat.getMessage("created-data"));
    }

    public static void sendDataLoadedMessage(Player player){
        player.sendMessage(Chat.getMessage("sync-success"));
    }

    public static void syncFailedKick(Player player){
        Main.getInstance().getServer().getScheduler().runTask(Main.getInstance(), () -> {
            String msg = Chat.getMessage("sync-failed");
            player.sendMessage(msg);
            player.kickPlayer(msg);
        });

    }
}
package de.lostesburger.mySqlPlayerBridge.Utils;

import de.lostesburger.mySqlPlayerBridge.Main;
import org.bukkit.ChatColor;

public class Chat {
    public static String msg(String message){
        return ChatColor.translateAlternateColorCodes('&', Main.prefix + message);
    }
    public static String getMessage(String messageKey){
        String message = Main.messages.getString(messageKey);
        if (message == null) {
            // 如果消息键不存在，返回默认值
            return "§c消息键不存在: " + messageKey;
        }
        return msg(message);
    }
    public static String getMessageWithoutPrefix(String messageKey){
        String message = Main.messages.getString(messageKey);
        if (message == null) {
            // 如果消息键不存在，返回默认值
            return "§c消息键不存在: " + messageKey;
        }
        return message;
    }
}
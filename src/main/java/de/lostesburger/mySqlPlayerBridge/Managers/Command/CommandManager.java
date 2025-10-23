package de.lostesburger.mySqlPlayerBridge.Managers.Command;

import de.lostesburger.mySqlPlayerBridge.Commands.MPBCommand.MPBCommand;
import de.lostesburger.mySqlPlayerBridge.Main;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public class CommandManager {
    public CommandManager(){
        String rootCommand = Main.config.getString("settings.command-prefix");
        
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            
            commandMap.register(rootCommand, new BukkitCommandWrapper(rootCommand, new MPBCommand()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
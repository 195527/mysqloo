package de.lostesburger.mySqlPlayerBridge.Commands;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface CommandInterface {
    void execute(CommandSender commandSender, String[] args);
    List<String> tabComplete(CommandSender commandSender, String[] args);
}
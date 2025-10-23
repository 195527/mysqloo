package de.lostesburger.mySqlPlayerBridge.Managers.Command;

import de.lostesburger.mySqlPlayerBridge.Commands.CommandInterface;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public class BukkitCommandWrapper extends Command {
    private final CommandInterface command;

    public BukkitCommandWrapper(String name, CommandInterface command) {
        super(name);
        this.command = command;
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        command.execute(commandSender, strings);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return command.tabComplete(sender, args);
    }
}
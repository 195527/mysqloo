package de.lostesburger.mySqlPlayerBridge.Commands;

import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubCommandManager {
    private final Map<String, CommandInterface> subCommands = new HashMap<>();

    public void addSubCommand(String name, CommandInterface command) {
        subCommands.put(name.toLowerCase(), command);
    }

    public void executeIntern(CommandSender sender, String[] args) {
        if (args.length == 0) {
            throw new SubCommandException(SubCommandException.Type.NO_ARGS_ERROR, String.join(", ", subCommands.keySet()));
        }

        String subCommandName = args[0].toLowerCase();
        CommandInterface subCommand = subCommands.get(subCommandName);
        
        if (subCommand == null) {
            throw new SubCommandException(SubCommandException.Type.UNKNOWN_SUBCOMMAND, String.join(", ", subCommands.keySet()));
        }

        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);
        
        subCommand.execute(sender, subArgs);
    }

    public List<String> tabCompleteIntern(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return subCommands.keySet().stream()
                    .filter(name -> args.length == 0 || name.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        String subCommandName = args[0].toLowerCase();
        CommandInterface subCommand = subCommands.get(subCommandName);
        
        if (subCommand != null) {
            String[] subArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subArgs, 0, subArgs.length);
            return subCommand.tabComplete(sender, subArgs);
        }
        
        return List.of();
    }
}
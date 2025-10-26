package de.lostesburger.mySqlPlayerBridge.Commands.SQLReloadCommand;

import de.lostesburger.mySqlPlayerBridge.Commands.CommandInterface;
import de.lostesburger.mySqlPlayerBridge.Commands.SubCommandException;
import de.lostesburger.mySqlPlayerBridge.Commands.SubCommandManager;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SQLCommand implements CommandInterface {
    private SubCommandManager subCommandManager;

    public SQLCommand() {
        subCommandManager = new SubCommandManager();
        subCommandManager.addSubCommand("reload", new SQLReloadCommand());
    }

    @Override
    public void execute(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission(Main.config.getString("settings.admin-permission"))) {
            commandSender.sendMessage(Chat.getMessage("permission-error"));
            return;
        }

        try {
            subCommandManager.executeIntern(commandSender, args);
        } catch (SubCommandException e) {
            switch (e.getErrorType()){
                case UNKNOWN_SUBCOMMAND -> {
                    commandSender.sendMessage("§c未知子命令。可用命令: §7reload");
                    break;
                }
                case NO_ARGS_ERROR -> {
                    commandSender.sendMessage("§c请提供一个子命令。可用命令: §7reload");
                    break;
                }
                default -> {
                    commandSender.sendMessage(Chat.getMessage("unknown-error"));
                    break;
                }
            }
        }
    }

    @Override
    public List<String> tabComplete(CommandSender commandSender, String[] args) {
        if (!commandSender.hasPermission(Main.config.getString("settings.admin-permission"))) {
            return List.of();
        }
        
        if (args.length <= 1) {
            return List.of("reload");
        }
        
        return List.of();
    }
}
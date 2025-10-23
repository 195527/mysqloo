package de.lostesburger.mySqlPlayerBridge.Commands.MPBCommand;


import de.lostesburger.mySqlPlayerBridge.Commands.CommandInterface;
import de.lostesburger.mySqlPlayerBridge.Commands.SubCommandManager;
import de.lostesburger.mySqlPlayerBridge.Commands.SubCommandException;
import de.lostesburger.mySqlPlayerBridge.Commands.ClearCommand.ClearCommand;
import de.lostesburger.mySqlPlayerBridge.Commands.CheckCommand.CheckCommand;
import de.lostesburger.mySqlPlayerBridge.Commands.ReloadCommand.ReloadCommand;
import de.lostesburger.mySqlPlayerBridge.Commands.ExportCommand.ExportCommand;
import de.lostesburger.mySqlPlayerBridge.Commands.ImportCommand.ImportCommand;
import de.lostesburger.mySqlPlayerBridge.Commands.BackupCommand.BackupCommand;
import de.lostesburger.mySqlPlayerBridge.Main;
import de.lostesburger.mySqlPlayerBridge.Utils.Chat;
import org.bukkit.command.CommandSender;

import java.util.List;

public class MPBCommand implements CommandInterface {
    private SubCommandManager subCommandManager;
    private String adminPerm;

    public MPBCommand() {
        subCommandManager = new SubCommandManager();
        adminPerm = Main.config.getString("settings.admin-permission");

        subCommandManager.addSubCommand("clear", new ClearCommand());
        subCommandManager.addSubCommand("check", new CheckCommand());
        subCommandManager.addSubCommand("reload", new ReloadCommand());
        subCommandManager.addSubCommand("export", new ExportCommand());
        subCommandManager.addSubCommand("import", new ImportCommand());
        subCommandManager.addSubCommand("backup", new BackupCommand());
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if(this.adminPerm.isEmpty() || !commandSender.hasPermission(this.adminPerm)){
            commandSender.sendMessage(Chat.getMessage("permission-error"));
            return;
        }

        try {
            subCommandManager.executeIntern(commandSender, strings);
        } catch (SubCommandException e) {
            switch (e.getErrorType()){
                case UNKNOWN_SUBCOMMAND -> {
                    commandSender.sendMessage(Chat.getMessage("unknown-subcommand-error").replace("{subcommands}", e.getDetails()));
                    break;
                }
                case NO_ARGS_ERROR -> {
                    commandSender.sendMessage(Chat.getMessage("no-subcommand-error").replace("{subcommands}", e.getDetails()));
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
    public List<String> tabComplete(CommandSender commandSender, String[] strings) {
        if(this.adminPerm.isEmpty() || !commandSender.hasPermission(this.adminPerm)){ return List.of(); }
        return subCommandManager.tabCompleteIntern(commandSender, strings);
    }
}
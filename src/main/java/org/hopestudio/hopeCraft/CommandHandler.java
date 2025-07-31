package org.hopestudio.hopeCraft;


import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandHandler implements CommandExecutor {
    HopeCraft plugin;

    public CommandHandler(HopeCraft plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String cmd, String[] args) {
        if (args.length == 1) {
            if (args[0].equals("on")) {
                plugin.listenOn(commandSender);
            } else if (args[0].equals("off")) {
                plugin.listenOff(commandSender);
            } else if (args[0].equals("reload")) {
                plugin.reload(commandSender);
            } else if (args[0].equals("help")) {
                commandSender.sendMessage("§7[§bShiftAndF§7] §9命令帮助");
                commandSender.sendMessage("§9/saf reload 重载插件");
                commandSender.sendMessage("§9/saf on 开始监听");
                commandSender.sendMessage("§9/saf off 停止监听");
            } else {
                commandSender.sendMessage("§7[§bShiftAndF§7] §e错误的用法");
            }
        } else {
            commandSender.sendMessage("§7[§bShiftAndF§7] §e错误的用法");
        }
        return true;
    }


}
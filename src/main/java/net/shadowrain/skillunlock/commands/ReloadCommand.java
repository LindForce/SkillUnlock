package net.shadowrain.skillunlock.commands;

import net.shadowrain.skillunlock.SkillUnlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand implements CommandExecutor {

    SkillUnlock plugin;

    public ReloadCommand(SkillUnlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String reloadMessage = plugin.getConfig().getString("reload-message");
        String noPermMessage = plugin.getConfig().getString("no-perm");

        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (p.hasPermission("su.reload")) {

                plugin.reloadConfig();
                p.sendMessage(plugin.color(plugin.COLOR_PREFIX + " " + "&7" + reloadMessage));

            } else {
                p.sendMessage(plugin.color(plugin.COLOR_PREFIX + " " + noPermMessage));
            }

        } else if (sender instanceof ConsoleCommandSender) {
            plugin.reloadConfig();
            System.out.println(plugin.PREFIX + " " + reloadMessage);
        }

        return true;
    }
}

package net.shadowrain.skillunlock.commands;

import net.shadowrain.skillunlock.SkillUnlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SUCommand implements CommandExecutor {

    SkillUnlock plugin;

    public SUCommand(SkillUnlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            plugin.openMainMenu(p);
        } else {
            System.out.println(plugin.PREFIX + " You need to be in-game to use this command");
        }


        return true;
    }
}

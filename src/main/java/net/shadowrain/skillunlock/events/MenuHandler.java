package net.shadowrain.skillunlock.events;


import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.query.QueryOptions;
import net.milkbowl.vault.economy.Economy;
import net.shadowrain.skillunlock.SkillUnlock;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Map;
import java.util.Objects;

public class MenuHandler implements Listener {

    LuckPerms luckPerms = LuckPermsProvider.get();

    SkillUnlock plugin;

    public MenuHandler(SkillUnlock plugin) {
        this.plugin = plugin;
    }

    /**
     * toggleTitle() - Toggle LuckPerms prefix for a player.
     * @param p Player to set prefix to.
     * @param title Prefix to add to player (format '&8[Title]').
     * @param equipMessage Equip success message.
     * @param removeMessage Prefix remove success message.
     */
    public void toggleTitle(Player p, String title, String equipMessage, String removeMessage) {
        luckPerms.getUserManager().modifyUser(p.getUniqueId(), (User user) -> {

            int prePriority = user.getNodes().stream()
                    .filter(NodeType.PREFIX::matches)
                    .map(NodeType.PREFIX::cast)
                    .mapToInt(ChatMetaNode::getPriority)
                    .max()
                    .orElse(0);

            // Check if user has prefix
            if (p.hasPermission("prefix." + prePriority + "." + title)) {
                // Create a node to add to the player.
                Node prefixNode = PrefixNode.builder(title, prePriority).build();

                // Add the node to the user.
                user.data().remove(prefixNode);

                // Tell the sender.
                p.sendMessage(plugin.color(plugin.COLOR_PREFIX + " " + removeMessage + " " + title));
            } else {

                user.data().clear(NodeType.PREFIX::matches);

                // Find the highest priority of their other prefixes
                // We need to do this because they might inherit a prefix from a parent group,
                // and we want the prefix we set to override that!
                Map<Integer, String> inheritedPrefixes = user.getCachedData().getMetaData(QueryOptions.nonContextual()).getPrefixes();
                int priority = inheritedPrefixes.keySet().stream().mapToInt(i -> i + 1).max().orElse(10);

                // Create a node to add to the player.
                Node prefixNode = PrefixNode.builder(title, priority).build();

                // Add the node to the user.
                user.data().add(prefixNode);

                // Tell the sender.
                p.sendMessage(plugin.color(plugin.COLOR_PREFIX + " " + equipMessage + " " + title));
            }
        });
    }

    /**
     * setPermission() - Give LuckPerms permission (skill) to user.
     * @param p Player to give perm to.
     * @param perm Perm node to give to player.
     * @param res Success message response.
     */
    public void setPermission(Player p, String perm, String res) {
        // Build title permission node and give to player.

        Node permNode = Node.builder(perm).build();

        luckPerms.getUserManager().modifyUser(p.getUniqueId(), (User user) -> {
            DataMutateResult result = user.data().add(permNode);
            if (!result.wasSuccessful()) {
                p.sendMessage(ChatColor.RED + "An error occurred when setting permission, please report this to an admin.");
            } else {
                p.sendMessage(res);
            }
        });
    }


    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) {
            return;
        }

        // Defining player and getEconomy method.
        Player p = (Player) e.getWhoClicked();
        Economy economy = SkillUnlock.getEconomy();

        // Defining config constants.
        final String MAIN_MENU = plugin.getConfig().getString("main-menu");
        final String SKILL_MENU = plugin.getConfig().getString("skill-menu");
        final String TITLE_MENU = plugin.getConfig().getString("title-menu");

        final String SKILL_SUCCESS = plugin.getConfig().getString("skill-success");
        final String SKILL_DENY = plugin.getConfig().getString("skill-deny");

        final String TITLE_SUCCESS = plugin.getConfig().getString("title-success");
        final String TITLE_EQUIP = plugin.getConfig().getString("title-equip");
        final String TITLE_UNEQUIP = plugin.getConfig().getString("title-unequip");

        final String DEPOSIT_MESSAGE = plugin.getConfig().getString("deposit-message");
        final String NO_MONEY = plugin.getConfig().getString("no-money");


        if (e.getView().getTitle().equalsIgnoreCase(MAIN_MENU)) {
            p.closeInventory();

            switch(e.getCurrentItem().getType()) {
                case ENCHANTED_BOOK:

                    plugin.openSkillMenu(p);
                    break;
                case NAME_TAG:

                    plugin.openTitleMenu(p);
                    break;
            }
        }

        if (e.getView().getTitle().equalsIgnoreCase(SKILL_MENU)) {

            // Check if clicked item is back button.

            if (e.getSlot() == 53) {
                plugin.openMainMenu(p);
            } else {
                p.closeInventory();

                // Use amount to decide what permission was clicked.
                int number = Objects.requireNonNull(e.getCurrentItem()).getAmount();
                String strNumber = String.valueOf(number);

                // Get command name, cost and perm node.
                String currentCommand = plugin.getConfig().getString("permissions." + strNumber + ".command");
                double cost = Double.parseDouble(Objects.requireNonNull(plugin.getConfig().getString("permissions." + strNumber + ".cost")));
                String perm = plugin.getConfig().getString("permissions." + strNumber + ".node");

                if (p.hasPermission(perm)) {
                    // Check if player already has the permission.
                    p.sendMessage(plugin.color(plugin.COLOR_PREFIX + " " + SKILL_DENY));
                    return;
                }

                if (economy.getBalance(p) < cost) {
                    // Check if player has enough money.
                    p.sendMessage(plugin.color(plugin.COLOR_PREFIX + " " + NO_MONEY));
                    return;
                }

                // Format response and run set permission method.
                String res = plugin.color(plugin.COLOR_PREFIX + " " + SKILL_SUCCESS + currentCommand);
                setPermission(p, perm, res);

                // Withdraw money.
                economy.withdrawPlayer(p, cost);
                p.sendMessage(plugin.color(plugin.COLOR_PREFIX + " &e$" + cost + " " + DEPOSIT_MESSAGE));
            }
        }

        if (e.getView().getTitle().equalsIgnoreCase(TITLE_MENU)) {

            // Check if clicked item is back button.
            if (e.getSlot() == 53) {
                plugin.openMainMenu(p);
            } else {
                // Use amount to decide what title was clicked.
                int number = Objects.requireNonNull(e.getCurrentItem()).getAmount();
                String strNumber = String.valueOf(number);

                // Get title name, cost and perm node.
                String title = plugin.getConfig().getString("titles." + strNumber + ".title");
                double cost = Double.parseDouble(Objects.requireNonNull(plugin.getConfig().getString("titles." + strNumber + ".cost")));
                String titlePerm = "su.prefix." + Objects.requireNonNull(plugin.getConfig().getString("titles." + strNumber + ".name")).toLowerCase();


                if (p.hasPermission(titlePerm)) {

                    p.closeInventory();
                    toggleTitle(p, title, TITLE_EQUIP, TITLE_UNEQUIP);

                } else {
                    // Player does not have title perm, has to buy.
                    if (economy.getBalance(p) < cost) {
                        p.closeInventory();
                        // Check if player has enough money.
                        p.sendMessage(plugin.color(plugin.COLOR_PREFIX + " " + NO_MONEY));
                        return;
                    }

                    // Build title permission node and give to player.

                    String res = plugin.color(plugin.COLOR_PREFIX + " " + TITLE_SUCCESS + " " + title);
                    setPermission(p, titlePerm, res);

                    // Withdraw money.
                    economy.withdrawPlayer(p, cost);
                    p.sendMessage(plugin.color(plugin.COLOR_PREFIX + " &e$" + cost + " " + DEPOSIT_MESSAGE));

                    // Refresh inventory.
                    p.closeInventory();
                    plugin.openTitleMenu(p);

                }
            }
        }
    }
}

package net.shadowrain.skillunlock;

import net.milkbowl.vault.economy.Economy;
import net.shadowrain.skillunlock.commands.ReloadCommand;
import net.shadowrain.skillunlock.commands.SUCommand;
import net.shadowrain.skillunlock.events.MenuHandler;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Objects;

import static java.lang.Integer.parseInt;

public final class SkillUnlock extends JavaPlugin {

    private static Economy econ = null;

    public final String COLOR_PREFIX = "&8[&dSkill Unlock&8]";
    public final String PREFIX = "[Skill Unlock]";


    /**
     * Capitalize the first letter of a string. Originally used to capitalize first letter of command when hovering.
     * @param str String to capitalize.
     * @return The capitalized string.
     */
    public String capitalize(String str) {
        String s1 = str.substring(0, 1).toUpperCase();

        return s1 + str.substring(1);
    }

    /**
     * Shortened function to use Minecraft color translation on text.
     * @param text Text to use color translation on.
     * @return String including the color translation function.
     */
    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }


    // Setup Vault API
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    // Get Economy function to implement economy in other classes.
    public static Economy getEconomy() {
        return econ;
    }

    /**
     * Count through config category and determine how many inventory slots will be needed to display.
     * @param category What category length to calculate.
     * @return Amount of inventory slots needed.
     */
    public int calculateSlots(String category) {
        int total = 0;

        // Try to count keys in config to be used when creating GUI menu.
        try {
            for (String ignored : Objects.requireNonNull(getConfig().getConfigurationSection(category)).getKeys(false)) {
                total += 1;
            }
        } catch (NullPointerException err) {
            System.out.println(PREFIX + " NullPointerException when calculating slots! Please report this to an admin.");
        }

        // Count the total slots needed in multiples of 9. (Add 9 instead of 8 to make room for back options)
        total = (total + 9) / 9 * 9;

        return total;
    }

    /**
     * Create and open Main Menu.
     * @param p Player to open inventory for.
     */
    public void openMainMenu(Player p) {
        final String MAIN_MENU = getConfig().getString("main-menu");
        final String SKILLS_TITLE = getConfig().getString("skills-title");
        final String SKILLS_DESCRIPTION = getConfig().getString("skills-description");
        final String TITLES_TITLE = getConfig().getString("titles-title");
        final String TITLES_DESCRIPTION = getConfig().getString("titles-description");

        Inventory menu = Bukkit.createInventory(p, 27, color(MAIN_MENU));

        // Skills menu button
        ItemStack skills = new ItemStack(Material.ENCHANTED_BOOK);

        ItemMeta skillsMeta = skills.getItemMeta();
        skillsMeta.setDisplayName(color(SKILLS_TITLE));

        ArrayList<String> skillsLore = new ArrayList<>();
        skillsLore.add(color(SKILLS_DESCRIPTION));
        skillsMeta.setLore(skillsLore);
        skills.setItemMeta(skillsMeta);

        // Titles menu button
        ItemStack titles = new ItemStack(Material.NAME_TAG);

        ItemMeta titlesMeta = titles.getItemMeta();
        titlesMeta.setDisplayName(color(TITLES_TITLE));

        ArrayList<String> titlesLore = new ArrayList<>();
        titlesLore.add(color(TITLES_DESCRIPTION));
        titlesMeta.setLore(titlesLore);
        titles.setItemMeta(titlesMeta);


        menu.setItem(12, skills);
        menu.setItem(14, titles);

        p.openInventory(menu);

    }

    /**
     * Create and open Skills Menu.
     * @param p Player to open inventory for.
     */
    public void openSkillMenu(Player p) {
        final String SKILL_MENU = getConfig().getString("skill-menu");

        // Try to create GUI menu and fill it with items by iterating through the permissions keys.
        try {
            int slots = calculateSlots("permissions");
            if (slots <= 54) {
                slots = 54;
                Inventory menu = Bukkit.createInventory(p, slots, color(SKILL_MENU));

                int i = 0;

                for (String key : Objects.requireNonNull(getConfig().getConfigurationSection("permissions")).getKeys(false)) {
                    String command = getConfig().getString("permissions." + key + ".command");

                    // Check if command string has a / before to see if it needs to be removed for the display of the command.
                    try {
                        if (command.charAt(0) == '/') {
                            command = command.substring(1);
                        }
                    } catch (NullPointerException err) {
                        System.out.println(PREFIX + " NullPointerException when reading command string from config.");
                    }


                    String description = getConfig().getString("permissions." + key + ".description");
                    String permission = getConfig().getString("permissions." + key + ".node");
                    String cost = getConfig().getString("permissions." + key + ".cost");
                    int amount = parseInt(key); // Use key as amount to read current key when handling click event.

                    ItemStack perm = new ItemStack(Material.ENCHANTED_BOOK);
                    ItemMeta permMeta = perm.getItemMeta();
                    permMeta.setDisplayName(capitalize(command));

                    ArrayList<String> lore = new ArrayList<>();

                    // Change title depending on if the player has the permission.
                    if (p.hasPermission(permission)) {
                        lore.add(color("&aOwned"));
                    } else {
                        lore.add(color(description));
                        lore.add(ChatColor.GREEN + "$" + cost);
                    }

                    permMeta.setLore(lore);
                    perm.setItemMeta(permMeta);
                    perm.setAmount(amount);

                    menu.setItem(i, perm);

                    // Add back button.
                    ItemStack back = new ItemStack(Material.BARRIER);
                    ItemMeta backMeta = back.getItemMeta();
                    backMeta.setDisplayName(color("&eGo Back"));
                    back.setItemMeta(backMeta);
                    menu.setItem(slots - 1, back);

                    i += 1;
                }

                p.openInventory(menu);
            } else {
                System.out.println(PREFIX + " Exceeded max amount of items (54) in menu!");
            }

        } catch (NullPointerException err) {
            System.out.println(PREFIX + " NullPointerException when creating menu! Config not loaded correctly.");
        } catch (IllegalArgumentException err) {
            System.out.println(PREFIX + " IllegalArgumentException when creating menu! (More than 54 perms?)");
        }
    }

    /**
     * Create and open Main Menu.
     * @param p Player to open inventory for.
     */
    public void openTitleMenu(Player p) {
        final String TITLE_MENU = getConfig().getString("title-menu");

        // Try to create GUI menu and fill it with items by iterating through the permissions keys.
        try {
            int slots = calculateSlots("titles");
            if (slots <= 54) {
                slots = 54;
                Inventory menu = Bukkit.createInventory(p, slots, color(TITLE_MENU));

                int i = 0;

                for (String key : Objects.requireNonNull(getConfig().getConfigurationSection("titles")).getKeys(false)) {
                    String title = getConfig().getString("titles." + key + ".title");
                    String description = getConfig().getString("titles." + key + ".name");
                    String cost = getConfig().getString("titles." + key + ".cost");
                    int amount = parseInt(key); // Use key as amount to read current key when handling click event.

                    // Set item stack.
                    ItemStack prefix = new ItemStack(Material.NAME_TAG);
                    ItemMeta prefixMeta = prefix.getItemMeta();
                    prefixMeta.setDisplayName(color(title));

                    ArrayList<String> lore = new ArrayList<>();

                    // Change title depending on if the player owns the prefix.
                    if (p.hasPermission("su.prefix." + getConfig().getString("titles." + key + ".name"))) {
                        lore.add(color("&aToggle title: &f" + description));
                    } else {
                        lore.add(color("&eUnlock title: &f" + description));
                        lore.add(ChatColor.GREEN + "$" + cost);
                    }

                    prefixMeta.setLore(lore);
                    prefix.setItemMeta(prefixMeta);
                    prefix.setAmount(amount);

                    menu.setItem(i, prefix);

                    i += 1;
                }

                ItemStack back = new ItemStack(Material.BARRIER);
                ItemMeta backMeta = back.getItemMeta();
                backMeta.setDisplayName(color("&eGo Back"));
                back.setItemMeta(backMeta);
                menu.setItem(slots - 1, back);


                p.openInventory(menu);
            } else {
                System.out.println(PREFIX + " Exceeded max amount of items (54) in menu!");
            }
        } catch (NullPointerException err) {
            System.out.println(PREFIX + " NullPointerException when creating menu! Config not loaded correctly.");
        }
    }


    @Override
    public void onEnable() {
        saveDefaultConfig();

        getCommand("skills").setExecutor(new SUCommand(this));
        getCommand("sureload").setExecutor(new ReloadCommand(this));
        getServer().getPluginManager().registerEvents(new MenuHandler(this), this);

        if (!setupEconomy() ) {
            System.out.println(PREFIX + " No economy plugin found, disabling Vault.");
            getServer().getPluginManager().disablePlugin(this);
        }



    }
}

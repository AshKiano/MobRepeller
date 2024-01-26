package com.ashkiano.mobrepeller;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class MobRepeller extends JavaPlugin implements Listener {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.saveDefaultConfig();
        config = this.getConfig();

        Metrics metrics = new Metrics(this, 19206);

        this.getLogger().info("Thank you for using the MobRepeller plugin! If you enjoy using this plugin, please consider making a donation to support the development. You can donate at: https://paypal.me/josefvyskocil");

        checkForUpdates();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("getarmor")) {
            String permission = config.getString("getarmor_permission");
            if (player.hasPermission(permission)) {
                giveArmor(player, Material.LEATHER_HELMET);
                giveArmor(player, Material.LEATHER_CHESTPLATE);
                giveArmor(player, Material.LEATHER_LEGGINGS);
                giveArmor(player, Material.LEATHER_BOOTS);
                player.sendMessage(ChatColor.GREEN + "You received mob repelling armor!");
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
        }

        return false;
    }

    private void giveArmor(Player player, Material material) {
        ItemStack armor = new ItemStack(material, 1);
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
        meta.setLore(Arrays.asList(ChatColor.GREEN + "Repels mobs!"));
        armor.setItemMeta(meta);
        player.getInventory().addItem(armor);
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player)) return;

        Player player = (Player) event.getTarget();
        EntityType mobType = event.getEntityType();

        boolean hasFullSet = true;
        for (ItemStack piece : player.getInventory().getArmorContents()) {
            if (piece == null || !(piece.getItemMeta() instanceof LeatherArmorMeta) || !((LeatherArmorMeta) piece.getItemMeta()).getLore().contains(ChatColor.GREEN + "Repels mobs!")) {
                hasFullSet = false;
            }
        }

        if (hasFullSet && config.getBoolean("enable_mob_pushing")) {
            event.setCancelled(true);
            event.setTarget(null);

            Location playerLocation = player.getLocation();
            Location mobLocation = event.getEntity().getLocation();
            Vector direction = mobLocation.toVector().subtract(playerLocation.toVector()).normalize();
            event.getEntity().setVelocity(direction.multiply(new Vector(1, 0, 1)));
        } else {
            for (ItemStack piece : player.getInventory().getArmorContents()) {
                if (piece != null) {
                    LeatherArmorMeta meta = (LeatherArmorMeta) piece.getItemMeta();
                    if (meta.hasLore() && meta.getLore().contains(ChatColor.GREEN + "Repels mobs!")) {
                        String armorType = piece.getType().name().toLowerCase().replace("leather_", "");
                        List<String> mobList = config.getStringList(armorType);
                        if (mobList != null && mobList.contains(mobType.name())) {
                            event.setCancelled(true);
                            event.setTarget(null);
                            player.sendMessage(ChatColor.GREEN + "Your " + armorType + " repelled a " + mobType.name() + "!");
                            return;
                        }
                    }
                }
            }
        }
    }

    private void checkForUpdates() {
        try {
            String pluginName = this.getDescription().getName();
            URL url = new URL("https://www.ashkiano.com/version_check.php?plugin=" + pluginName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                String jsonResponse = response.toString();
                JSONObject jsonObject = new JSONObject(jsonResponse);
                if (jsonObject.has("error")) {
                    this.getLogger().warning("Error when checking for updates: " + jsonObject.getString("error"));
                } else {
                    String latestVersion = jsonObject.getString("latest_version");

                    String currentVersion = this.getDescription().getVersion();
                    if (currentVersion.equals(latestVersion)) {
                        this.getLogger().info("This plugin is up to date!");
                    } else {
                        this.getLogger().warning("There is a newer version (" + latestVersion + ") available! Please update!");
                    }
                }
            } else {
                this.getLogger().warning("Failed to check for updates. Response code: " + responseCode);
            }
        } catch (Exception e) {
            this.getLogger().warning("Failed to check for updates. Error: " + e.getMessage());
        }
    }

}

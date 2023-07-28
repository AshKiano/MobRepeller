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

        System.out.println("Thank you for using the MobRepeller plugin! If you enjoy using this plugin, please consider making a donation to support the development. You can donate at: https://paypal.me/josefvyskocil");
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

}

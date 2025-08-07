package org.hopestudio.hopeCraft;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;


public class PlayerListener implements Listener {
    private final HopeCraft plugin;

    PlayerListener(HopeCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        plugin.hasTrigger(event);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 设置加入消息为null，取消默认消息
        event.joinMessage(null);
        // 向全服玩家发送自定义消息
        plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + event.getPlayer().getName() + ChatColor.GREEN + "上了！");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 设置退出消息为null，取消默认消息
        event.quitMessage(null);
        // 向全服玩家发送自定义消息
        plugin.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + event.getPlayer().getName() + ChatColor.GREEN + "下惹。");
    }

    @EventHandler
    public void onPlayerMilk(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.COW && event.getPlayer().getInventory().getItemInMainHand().getType() == Material.BUCKET) {
            Location location = event.getRightClicked().getLocation();
            location.getWorld().dropItemNaturally(location, new ItemStack(Material.NETHERITE_INGOT));
            location.getWorld().createExplosion(location, 0);
        }
    }
}
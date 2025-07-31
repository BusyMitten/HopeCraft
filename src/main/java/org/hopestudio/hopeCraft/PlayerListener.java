package org.hopestudio.hopeCraft;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;


public class PlayerListener implements Listener {
    private HopeCraft plugin;

    PlayerListener(HopeCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        plugin.hasTrigger(event);
    }
}

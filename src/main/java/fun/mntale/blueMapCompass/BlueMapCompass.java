package fun.mntale.blueMapCompass;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Set;
import java.util.List;
import java.util.UUID;

public final class BlueMapCompass extends JavaPlugin implements Listener {

    public static BlueMapCompass instance;
    public BannerMarkerStorage bannerMarkerStorage;
    public BannerMarkerListener bannerMarkerListener;
    public MarkerGUI markerGUI;
    public static boolean debug = false;

    @Override
    public void onEnable() {
        instance = this;
        BlueMapIntegration.initialize(instance);
        bannerMarkerStorage = new BannerMarkerStorage(this);
        bannerMarkerListener = new BannerMarkerListener();
        bannerMarkerListener.restoreAllMarkers();
        markerGUI = new MarkerGUI();
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(markerGUI, this);
        getServer().getPluginManager().registerEvents(bannerMarkerListener, this);
        // Start global waypoint update task
        WaypointManager.startGlobalWaypointTask(this);
    }

    @Override
    public void onDisable() {
        // Stop global waypoint update task
        WaypointManager.stopGlobalWaypointTask();
    }

    @EventHandler
    public void onCompassRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (!player.hasPermission("bluemapcompass.use")) {
            return;
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (item != null && item.getType() == Material.COMPASS) {
                event.setCancelled(true);
                MarkerGUI.openFor(player);
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        JavaPlugin plugin = BlueMapCompass.instance;
        Set<String> tracked = fun.mntale.blueMapCompass.WaypointManager.getTrackedMarkers(player);
        List<BannerMarkerStorage.BannerMarkerData> allBannerMarkers = fun.mntale.blueMapCompass.BlueMapCompass.instance.bannerMarkerStorage.getAllMarkers();
        Set<String> validMarkerIds = new java.util.HashSet<>();
        for (BannerMarkerStorage.BannerMarkerData marker : allBannerMarkers) validMarkerIds.add(marker.markerId);

        Set<String> newTracked = new java.util.HashSet<>();
        for (String markerId : tracked) {
            if (validMarkerIds.contains(markerId)) {
                newTracked.add(markerId);
            } else {
                fun.mntale.blueMapCompass.WaypointManager.removeWaypointDisplayOnly(player, markerId);
            }
        }
        fun.mntale.blueMapCompass.WaypointManager.setTrackedMarkers(player, newTracked);
        // No display logic here; global task will handle display restoration
        // Per-player display cleanup after join (delayed, changed to standard Bukkit)
        Bukkit.getScheduler().runTaskLater(this, () -> {
            UUID uuid = player.getUniqueId();
            for (World world : getServer().getWorlds()) {
                for (org.bukkit.entity.BlockDisplay entity : world.getEntitiesByClass(org.bukkit.entity.BlockDisplay.class)) {
                    String playerId = entity.getPersistentDataContainer().get(fun.mntale.blueMapCompass.WaypointManager.DISPLAY_PLAYER_KEY, org.bukkit.persistence.PersistentDataType.STRING);
                    if (playerId != null && playerId.equals(uuid.toString())) {
                        entity.remove();
                    }
                }
                for (org.bukkit.entity.TextDisplay entity : world.getEntitiesByClass(org.bukkit.entity.TextDisplay.class)) {
                    String playerId = entity.getPersistentDataContainer().get(fun.mntale.blueMapCompass.WaypointManager.DISPLAY_PLAYER_KEY, org.bukkit.persistence.PersistentDataType.STRING);
                    if (playerId != null && playerId.equals(uuid.toString())) {
                        entity.remove();
                    }
                }
            }
        }, 60L);
    }
}

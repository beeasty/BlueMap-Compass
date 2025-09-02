package fun.mntale.blueMapCompass;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.awt.Graphics2D;

public class BlueMapIntegration {
    private static Plugin plugin;
    private static final Map<String, String> BANNER_IMAGE_URLS = new HashMap<>();
    static {
        BANNER_IMAGE_URLS.put("white", "https://minecraft.wiki/images/White_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("light_gray", "https://minecraft.wiki/images/Light_Gray_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("gray", "https://minecraft.wiki/images/Gray_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("black", "https://minecraft.wiki/images/Black_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("brown", "https://minecraft.wiki/images/Brown_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("red", "https://minecraft.wiki/images/Red_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("orange", "https://minecraft.wiki/images/Orange_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("yellow", "https://minecraft.wiki/images/Yellow_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("lime", "https://minecraft.wiki/images/Lime_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("green", "https://minecraft.wiki/images/Green_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("cyan", "https://minecraft.wiki/images/Cyan_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("light_blue", "https://minecraft.wiki/images/Light_Blue_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("blue", "https://minecraft.wiki/images/Blue_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("purple", "https://minecraft.wiki/images/Purple_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("magenta", "https://minecraft.wiki/images/Magenta_Banner_JE2_BE2.gif");
        BANNER_IMAGE_URLS.put("pink", "https://minecraft.wiki/images/Pink_Banner_JE2_BE2.gif");
    }

    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();
        return resizedImage;
    }

    public static void initialize(Plugin mainPlugin) {
        plugin = mainPlugin;
        BlueMapAPI.onEnable(api -> {
            java.nio.file.Path webRoot = api.getWebApp().getWebRoot();
            File bannerDir = webRoot.resolve("assets/bluemap/web/images/banners").toFile();
            if (!bannerDir.exists()) bannerDir.mkdirs();
            for (Map.Entry<String, String> entry : BANNER_IMAGE_URLS.entrySet()) {
                String color = entry.getKey();
                String url = entry.getValue();
                File outFile = new File(bannerDir, color + ".png");
                if (!outFile.exists()) {
                    Bukkit.getLogger().info("[BlueMapCompass] Downloading banner image for " + color + " from " + url);
                    try (InputStream in = new URL(url).openStream()) {
                        BufferedImage image = ImageIO.read(in); // Reads first frame of GIF
                        if (image != null) {
                            BufferedImage resized = resizeImage(image, 24, 48);
                            ImageIO.write(resized, "png", outFile);
                            Bukkit.getLogger().info("[BlueMapCompass] Saved banner image for " + color + " to " + outFile.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        Bukkit.getLogger().warning("[BlueMapCompass] Failed to download/convert banner image for " + color + ": " + e.getMessage());
                    }
                }
            }
        });
        BlueMapAPI.onDisable(api -> {});
    }

    public static List<MarkerData> getMarkers() {
        List<MarkerData> markers = new ArrayList<>();
        Optional<BlueMapAPI> optionalApi = BlueMapAPI.getInstance();
        if (optionalApi.isEmpty()) return List.of();
        BlueMapAPI api = optionalApi.get();
        BannerMarkerStorage bannerStorage = new BannerMarkerStorage(plugin);
        List<BannerMarkerStorage.BannerMarkerData> bannerDataList = bannerStorage.getAllMarkers();

        // Add banner markers from storage (no duplicates)
        for (BannerMarkerStorage.BannerMarkerData banner : bannerDataList) {
            markers.add(new MarkerData(
                banner.markerId,
                banner.name,
                "Banner",
                banner.world,
                banner.x,
                banner.y,
                banner.z,
                "banner-markers",
                banner.placerName != null && !banner.placerName.isEmpty() ? banner.placerName : banner.placerUuid,
                banner.color
            ));
        }

        // Add other markers from BlueMap (skip banner-markers set)
        for (BlueMapWorld world : api.getWorlds()) {
            for (BlueMapMap map : world.getMaps()) {
                for (Map.Entry<String, MarkerSet> entry : map.getMarkerSets().entrySet()) {
                    String markerSetId = entry.getKey();
                    if (markerSetId.equalsIgnoreCase("banner-markers")) continue;
                    MarkerSet markerSet = entry.getValue();
                    for (Map.Entry<String, Marker> markerEntry : markerSet.getMarkers().entrySet()) {
                        String markerId = markerEntry.getKey();
                        Marker marker = markerEntry.getValue();
                        String label = marker.getLabel();
                        int x = 0, y = 0, z = 0;
                        try {
                            var posMethod = marker.getClass().getMethod("getPosition");
                            var pos = posMethod.invoke(marker);
                            x = (int) ((com.flowpowered.math.vector.Vector3d) pos).getX();
                            y = (int) ((com.flowpowered.math.vector.Vector3d) pos).getY();
                            z = (int) ((com.flowpowered.math.vector.Vector3d) pos).getZ();
                        } catch (Exception ignored) {}
                        markers.add(new MarkerData(
                            markerId,
                            label != null ? label : markerId,
                            marker.getClass().getSimpleName(),
                            world.getId(),
                            x, y, z,
                            markerSetId,
                            null,
                            null
                        ));
                    }
                }
            }
        }
        return markers;
    }

    public static boolean teleportToMarker(Player player, MarkerData marker) {
        try {
            World world = Bukkit.getWorld(marker.world());
            if (world == null) {
                player.sendMessage("§cWorld '" + marker.world() + "' not found!");
                return false;
            }
            Location location = new Location(world, marker.x(), marker.y(), marker.z());
            player.teleportAsync(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
            player.sendMessage("§aTeleported to " + marker.name() + "!");
            return true;
        } catch (Exception e) {
            player.sendMessage("§cFailed to teleport to " + marker.name() + "!");
            return false;
        }
    }
} 
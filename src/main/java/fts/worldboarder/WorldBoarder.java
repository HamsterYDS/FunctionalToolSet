package fts.worldboarder;

import fts.FunctionalToolSet;
import fts.ResourceUtils;
import fts.api.BlockApi;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class WorldBoarder implements Listener {
    private static HashMap<String, BoarderType> types = new HashMap<>();
    private static HashMap<String, Boarder> boarders = new HashMap<>();
    private static ArrayList<String> particles = new ArrayList<>();
    private static ArrayList<UUID> players = new ArrayList<>();

    public static void initialize(FunctionalToolSet plugin) {
        ResourceUtils.autoUpdateConfigs("boarder.yml");
        File file = new File(plugin.getDataFolder(), "boarder.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.getBoolean("enable")) {
            return;
        }

        for (String path : yaml.getKeys(false)) {
            if (path.equalsIgnoreCase("enable")) {
                continue;
            }
            BoarderType type = BoarderType.valueOf(yaml.getString(path + ".type"));
            boolean isTransparent = yaml.getBoolean(path + ".isTransparent");
            switch (type) {
            case RECTANGLE:
                boarders.put(path, new RectangleBoarder(isTransparent,
                        yaml.getInt(path + ".x1"), yaml.getInt(path + ".z1"),
                        yaml.getInt(path + ".x2"), yaml.getInt(path + ".z2")));
                break;
            case ROUND:
                boarders.put(path, new RoundBoarder(isTransparent, yaml.getInt(path + ".radius")));
                break;
            }
            types.put(path, type);
        }

        Bukkit.getPluginManager().registerEvents(new WorldBoarder(), plugin);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (boarders.containsKey(world.getName())) {
            if (player.hasPermission("fts.boarder.ignore")) {
                return;
            }
            Location loc = event.getTo().getBlock().getLocation();
            Boarder boarder = boarders.get(world.getName());
            BoarderType type = types.get(world.getName());

            boolean flag = false;
            switch (type) {
            case RECTANGLE:
                flag = ((RectangleBoarder) boarder).isOutOfWorld(loc);
                break;
            case ROUND:
                flag = ((RoundBoarder) boarder).isOutOfWorld(loc);
                break;
            }

            if (flag) {
                if (boarder.isTransparent && (!players.contains(player.getUniqueId()))) {
                    Location otherSide = boarder.getTheOtherSide(loc);
                    player.teleport(otherSide);
                    player.sendMessage("你刚刚超出了地图的边界，现已经穿梭到了地图遥远的另一边");
                    player.sendMessage("如果想要穿梭回去，请等待数秒~");
                    players.add(player.getUniqueId());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            players.remove(player.getUniqueId());
                            player.sendMessage("你穿梭地图的冷却结束了，现在可以“原路”返回");
                        }
                    }.runTaskLater(FunctionalToolSet.getInstance(), 100L);
                } else {
                    event.setCancelled(true);
                    if (particles.contains(BlockApi.locToStr(loc))) {
                        return;
                    }
                    world.spawnParticle(Particle.BARRIER, loc, 1);
                    particles.add(BlockApi.locToStr(loc));
                    player.sendMessage("你已经到地图的边界，无法继续前进");
                }
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Player player = event.getPlayer();
        World world = player.getWorld();
        if (boarders.containsKey(world.getName())) {
            if (player.hasPermission("fts.boarder.ignore")) {
                return;
            }
            Location loc = event.getTo().getBlock().getLocation();
            Boarder boarder = boarders.get(world.getName());
            BoarderType type = types.get(world.getName());

            boolean flag = false;
            switch (type) {
            case RECTANGLE:
                flag = ((RectangleBoarder) boarder).isOutOfWorld(loc);
                break;
            case ROUND:
                flag = ((RoundBoarder) boarder).isOutOfWorld(loc);
                break;
            }

            if (flag) {
                event.setCancelled(true);
                player.sendMessage("传送失败，目标点超出地图边界");
            }
        }
    }

    private enum BoarderType {
        RECTANGLE, ROUND
    }

    private static class Boarder {
        boolean isTransparent;

        Boarder(boolean isTransparent) {
            this.isTransparent = isTransparent;
        }

        Location getTheOtherSide(Location loc) {
            Location centre = new Location(loc.getWorld(), 0, loc.getY(), 0);
            Vector vector = new Vector(loc.getBlockX(), 0, loc.getBlockZ());
            vector.multiply(-1);
            Location result = centre.add(vector).clone();
            vector.multiply(-1).normalize();
            result.add(vector);
            result.add(vector);
            result.add(vector);

            result.setY(256);
            while (result.getBlock().getType() == Material.AIR) {
                result.add(0, -1, 0);
            }
            return result.add(0, 1, 0);
        }
    }

    private static class RectangleBoarder extends Boarder {
        int x1, z1, x2, z2;

        public RectangleBoarder(boolean isTransparent, int x1, int z1, int x2, int z2) {
            super(isTransparent);
            this.x1 = Math.min(x1, x2);
            this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2);
            this.z2 = Math.max(z1, z2);
        }

        boolean isOutOfWorld(Location loc) {
            int x = loc.getBlockX();
            int z = loc.getBlockZ();
            if (x1 <= x && x <= x2) {
                return z1 > z || z > z2;
            }
            return true;
        }
    }

    private static class RoundBoarder extends Boarder {
        int radius;

        public RoundBoarder(boolean isTransparent, int radius) {
            super(isTransparent);
            this.radius = radius;
        }

        boolean isOutOfWorld(Location loc) {
            return loc.distance(new Location(loc.getWorld(), 0, loc.getY(), 0)) <= radius;
        }
    }
}
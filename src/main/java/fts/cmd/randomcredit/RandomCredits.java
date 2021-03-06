package fts.cmd.randomcredit;

import fts.FunctionalToolSet;
import fts.spi.ResourceUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;

public class RandomCredits {
    private static final HashMap<String, String> permissionGroups = new HashMap<>();
    private static final HashMap<String, String> commandGroups = new HashMap<>();
    private static final HashMap<String, Double> percents = new HashMap<>();
    private static double totalPercents = 0.0;

    public static void initialize(FunctionalToolSet plugin) {
        ResourceUtils.autoUpdateConfigs("randomcredits.yml");
        File file = new File(plugin.getDataFolder(), "randomcredits.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

        for (String path : yaml.getKeys(false)) {
            String message = yaml.getString(path + ".message");
            double percent = yaml.getDouble(path + ".percent");
            if (path.startsWith("permission")) {
                String permission = yaml.getString(path + ".permission");
                initPermission(permission, message, percent);
            }
            if (path.startsWith("command")) {
                String command = yaml.getString(path + ".command");
                initCommand(command, message, percent);
            }
            totalPercents += percent;
        }
    }

    public static void initPermission(String permission, String message, double percent) {
        permissionGroups.put(permission, message);
        percents.put(permission, percent);
    }

    public static void initCommand(String command, String message, double percent) {
        commandGroups.put(command, message);
        percents.put(command, percent);
    }

    public static void goRandomPermission(CommandSender sender, Player player) {
        if (!sender.hasPermission("fts.cmd.randomcredit")) {
            ResourceUtils.sendMessage(sender, "no-permission-random-credit");
            return;
        }
        if (player == null) {
            ResourceUtils.sendMessage(sender, "no-such-a-player");
            return;
        }
        String permission = "";
        while (permission.equalsIgnoreCase("")) {
            int random = (int) (Math.random() * (permissionGroups.size() - 1));
            String index = (String) permissionGroups.keySet().toArray()[random];

            double percent = percents.get(index);
            if (Math.random() <= percent / totalPercents) {
                permission = index;
            }
        }
        FunctionalToolSet.vaultPermission.playerAdd(player, permission);
        String message = permissionGroups.get(permission);
        Bukkit.broadcastMessage(message.replace("%player%", player.getName()).replace("%permission%", permission));
    }

    public static void goRandomCommand(CommandSender sender, Player player) {
        if (!sender.hasPermission("fts.cmd.randomcredit")) {
            ResourceUtils.sendMessage(sender, "no-permission-random-credit");
            return;
        }
        if (player == null) {
            ResourceUtils.sendMessage(sender, "no-such-a-player");
            return;
        }
        String cmd = "";
        while (cmd.equalsIgnoreCase("")) {
            int random = (int) (Math.random() * (commandGroups.size() - 1));
            String index = (String) commandGroups.keySet().toArray()[random];

            double percent = percents.get(index);
            if (Math.random() <= percent / totalPercents) {
                cmd = index;
            }
        }
        String message = commandGroups.get(cmd);
        Bukkit.broadcastMessage(message.replace("%player%", player.getName()));
        if (player.isOp()) {
            player.performCommand(cmd.replace("[空格]", " ").replace("{player}", player.getName()));
        } else {
            player.setOp(true);
            player.performCommand(cmd.replace("[空格]", " ").replace("{player}", player.getName()));
            player.setOp(false);
        }
    }
}

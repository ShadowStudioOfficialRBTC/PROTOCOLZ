package com.shadowstudios.protocolz;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ProtocolZ extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static final Set<Material> BLOCKED_MATERIALS = EnumSet.of(
            Material.FIRE,
            Material.SOUL_FIRE,
            Material.TNT,
            Material.PISTON,
            Material.STICKY_PISTON,
            Material.MOVING_PISTON,
            Material.PISTON_HEAD,
            Material.REDSTONE_WIRE,
            Material.REDSTONE_BLOCK,
            Material.REDSTONE_TORCH,
            Material.REDSTONE_WALL_TORCH,
            Material.REPEATER,
            Material.COMPARATOR,
            Material.OBSERVER,
            Material.DISPENSER,
            Material.DROPPER,
            Material.HOPPER,
            Material.LEVER,
            Material.DAYLIGHT_DETECTOR
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (getCommand("protocolz") != null) {
            getCommand("protocolz").setExecutor(this);
            getCommand("protocolz").setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("protocolz.use")) {
            sender.sendMessage("You do not have permission to run ProtocolZ.");
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage("Usage: /protocolz ^<cpt^|cptr^|relese^|reload^>");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "cpt":
                executeCpt(sender);
                return true;
            case "cptr":
                executeCptr(sender);
                return true;
            case "relese":
            case "release":
                executeRelease(sender);
                return true;
            case "reload":
                reloadConfig();
                sender.sendMessage("[ProtocolZ] Reloaded config.yml.");
                return true;
            default:
                sender.sendMessage("Usage: /protocolz ^<cpt^|cptr^|relese^|reload^>");
                return true;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
    private void executeCpt(CommandSender sender) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick freeze");

        BanList nameBans = Bukkit.getBanList(BanList.Type.NAME);

        for (Player player : Bukkit.getOnlinePlayers()) {
            nameBans.addBan(player.getName(), "ProtocolZ Containment Activated", (Date) null, "ProtocolZ");
            player.kickPlayer("Server locked down by ProtocolZ.");
        }

        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline.getName() != null) {
                nameBans.addBan(offline.getName(), "ProtocolZ Containment Activated", (Date) null, "ProtocolZ");
            }
        }

        nameBans.addBan("DEVLOPER__", "ProtocolZ Containment Activated", (Date) null, "ProtocolZ");

        int removedEntities = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Player)) {
                    entity.remove();
                    removedEntities++;
                }
            }
        }

        int removedBlocks = 0;
        for (World world : Bukkit.getWorlds()) {
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();

            for (Chunk chunk : world.getLoadedChunks()) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = minY; y < maxY; y++) {
                            Block block = chunk.getBlock(x, y, z);
                            if (BLOCKED_MATERIALS.contains(block.getType())) {
                                block.setType(Material.AIR, false);
                                removedBlocks++;
                            }
                        }
                    }
                }
            }
        }

        sender.sendMessage("[ProtocolZ] CPT executed: Froze ticks, banned all players, removed "
                + removedEntities + " entities, and cleared " + removedBlocks + " blocks.");
    }

    @SuppressWarnings({"rawtypes", "deprecation"})
    private void executeCptr(CommandSender sender) {
        BanList nameBans = Bukkit.getBanList(BanList.Type.NAME);
        int unbannedCount = 0;

        for (Object obj : nameBans.getBanEntries()) {
            if (obj instanceof BanEntry) {
                BanEntry entry = (BanEntry) obj;
                String target = entry.getTarget();
                nameBans.pardon(target);
                unbannedCount++;
            }
        }

        sender.sendMessage("[ProtocolZ] CPTR executed: Unbanned all players (" + unbannedCount + " ban(s) cleared^).");
    }

    @SuppressWarnings({"rawtypes", "deprecation"})
    private void executeRelease(CommandSender sender) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tick unfreeze");

        reloadConfig();
        List<String> targets = getConfig().getStringList("unban-on-release");
        BanList nameBans = Bukkit.getBanList(BanList.Type.NAME);

        int unbannedCount = 0;
        for (String name : targets) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            nameBans.pardon(trimmed);
            unbannedCount++;
        }

        sender.sendMessage("[ProtocolZ] Release executed: Unfroze ticks and unbanned configured player(s) ("
                + unbannedCount + "^).");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("cpt", "cptr", "relese", "release", "reload");
        }
        return Collections.emptyList();
    }
}

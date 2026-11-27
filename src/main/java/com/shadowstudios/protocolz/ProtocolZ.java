package com.shadowstudios.protocolz;

import io.papermc.paper.event.world.WorldGameRuleChangeEvent;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class ProtocolZ extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private static final Set<Material> COMMAND_BLOCK_MATERIALS = EnumSet.of(
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK
    );

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
            Material.DAYLIGHT_DETECTOR,
            Material.COMMAND_BLOCK,
            Material.CHAIN_COMMAND_BLOCK,
            Material.REPEATING_COMMAND_BLOCK
    );

    private boolean containmentActive;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        containmentActive = getConfig().getBoolean("containment-active", false);
        getServer().getPluginManager().registerEvents(this, this);
        if (containmentActive) {
            for (World world : Bukkit.getWorlds()) {
                setContainmentGameRules(world, false);
                removeCommandBlocks(world);
            }
            dispatchTickCommand("tick freeze");
        }
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
            sender.sendMessage("Usage: /protocolz <cpt|cptr|releaseud|releseud|release|relese|reload>");
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
            case "releaseud":
            case "releseud":
            case "relese":
            case "release":
                executeRelease(sender);
                return true;
            case "reload":
                reloadConfig();
                sender.sendMessage("[ProtocolZ] Reloaded config.yml.");
                return true;
            default:
                sender.sendMessage("Usage: /protocolz <cpt|cptr|releaseud|releseud|release|relese|reload>");
                return true;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
    private void executeCpt(CommandSender sender) {
        containmentActive = true;
        getConfig().set("containment-active", true);
        saveConfig();

        boolean ticksFrozen = dispatchTickCommand("tick freeze");
        boolean gameRulesDisabled = setContainmentGameRules(false);

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

        for (String name : getConfig().getStringList("unban-on-release")) {
            if (name != null && !name.isBlank()) {
                nameBans.addBan(name.trim(), "ProtocolZ Containment Activated", (Date) null, "ProtocolZ");
            }
        }

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
        int removedCommandBlocks = 0;
        for (World world : Bukkit.getWorlds()) {
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();

            for (Chunk chunk : world.getLoadedChunks()) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = minY; y < maxY; y++) {
                            Block block = chunk.getBlock(x, y, z);
                            if (BLOCKED_MATERIALS.contains(block.getType())) {
                                if (COMMAND_BLOCK_MATERIALS.contains(block.getType())) {
                                    removedCommandBlocks++;
                                }
                                block.setType(Material.AIR, false);
                                removedBlocks++;
                            }
                        }
                    }
                }
            }
        }

        sender.sendMessage("[ProtocolZ] CPT executed: banned players, removed " + removedEntities
                + " entities, cleared " + removedBlocks + " blocks (" + removedCommandBlocks
                + " command blocks), and disabled command blocks and TNT.");
        if (!ticksFrozen) {
            sender.sendMessage("[ProtocolZ] WARNING: Could not freeze server ticks; check the server console.");
        }
        if (!gameRulesDisabled) {
            sender.sendMessage("[ProtocolZ] WARNING: Could not disable all containment gamerules; check the server console.");
        }
    }

    @SuppressWarnings({"rawtypes", "deprecation"})
    private void executeCptr(CommandSender sender) {
        containmentActive = false;
        getConfig().set("containment-active", false);
        saveConfig();

        boolean gameRulesEnabled = setContainmentGameRules(true);
        boolean ticksUnfrozen = dispatchTickCommand("tick unfreeze");

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

        sender.sendMessage("[ProtocolZ] CPTR executed: unbanned all players (" + unbannedCount
                + " ban(s) cleared), unfroze ticks, and enabled command blocks and TNT.");
        if (!ticksUnfrozen) {
            sender.sendMessage("[ProtocolZ] WARNING: Could not unfreeze server ticks; check the server console.");
        }
        if (!gameRulesEnabled) {
            sender.sendMessage("[ProtocolZ] WARNING: Could not enable all containment gamerules; check the server console.");
        }
    }

    @SuppressWarnings({"rawtypes", "deprecation"})
    private void executeRelease(CommandSender sender) {
        boolean ticksUnfrozen = dispatchTickCommand("tick unfreeze");

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

        sender.sendMessage("[ProtocolZ] Temporary release executed: unfroze ticks and unbanned configured player(s) ("
                + unbannedCount + "). Command-block and TNT gamerules are unchanged; use /protocolz cptr to enable them.");
        if (!ticksUnfrozen) {
            sender.sendMessage("[ProtocolZ] WARNING: Could not unfreeze server ticks; check the server console.");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTntPrime(TNTPrimeEvent event) {
        if (containmentActive) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTntExplosionPrime(ExplosionPrimeEvent event) {
        if (containmentActive && event.getEntity() instanceof TNTPrimed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTntExplode(EntityExplodeEvent event) {
        if (containmentActive && event.getEntity() instanceof TNTPrimed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldLoad(WorldLoadEvent event) {
        if (containmentActive) {
            setContainmentGameRules(event.getWorld(), false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (containmentActive) {
            int removed = removeCommandBlocks(event.getChunk());
            if (removed > 0) {
                getLogger().warning("Removed " + removed + " command block(s) from a newly loaded chunk "
                        + "while containment is active.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandBlockCommand(ServerCommandEvent event) {
        if (containmentActive && event.getSender() instanceof BlockCommandSender) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onContainmentGameRuleChange(WorldGameRuleChangeEvent event) {
        GameRule<?> gameRule = event.getGameRule();
        boolean protectedRule = gameRule.equals(GameRule.COMMAND_BLOCKS_ENABLED)
                || gameRule.equals(GameRule.TNT_EXPLODES);
        if (containmentActive && protectedRule && "true".equalsIgnoreCase(event.getValue())) {
            event.setCancelled(true);
        }
    }

    private boolean dispatchTickCommand(String command) {
        boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (!dispatched) {
            getLogger().warning("Could not dispatch console command: " + command);
        }
        return dispatched;
    }

    private boolean setContainmentGameRules(boolean enabled) {
        boolean success = true;
        for (World world : Bukkit.getWorlds()) {
            if (!setContainmentGameRules(world, enabled)) {
                success = false;
            }
        }
        return success;
    }

    private boolean setContainmentGameRules(World world, boolean enabled) {
        boolean commandBlocksSet = world.setGameRule(GameRule.COMMAND_BLOCKS_ENABLED, enabled);
        boolean tntSet = world.setGameRule(GameRule.TNT_EXPLODES, enabled);
        if (!commandBlocksSet || !tntSet) {
            getLogger().warning("Could not set command-block/TNT gamerules for world " + world.getName() + ".");
            return false;
        }
        return true;
    }

    private void removeCommandBlocks(World world) {
        for (Chunk chunk : world.getLoadedChunks()) {
            removeCommandBlocks(chunk);
        }
    }

    private int removeCommandBlocks(Chunk chunk) {
        int removed = 0;
        for (BlockState state : chunk.getTileEntities()) {
            if (COMMAND_BLOCK_MATERIALS.contains(state.getType())) {
                state.getBlock().setType(Material.AIR, false);
                removed++;
            }
        }
        return removed;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("cpt", "cptr", "releseud", "releaseud", "relese", "release", "reload");
        }
        return Collections.emptyList();
    }
}

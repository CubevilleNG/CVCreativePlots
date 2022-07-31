package org.cubeville.cvcreativeplots;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import org.cubeville.commons.commands.CommandParser;

import org.cubeville.cvtools.commands.*;

import java.util.*;

public class CVCreativePlots extends JavaPlugin implements Listener {

    private CommandParser commandParser;
    private List<String> worldNames;

    public void onEnable() {
        worldNames = new ArrayList<>();
        updateConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    public void updateConfig() {
        commandParser = new CommandParser();
        ConfigurationSection plotdata = getConfig().getConfigurationSection("plotdata");
        if(plotdata == null) return;
        Map<String, Integer> teleportYs = new HashMap<>();
        for(String worldname: plotdata.getKeys(false)) {
            worldNames.add(worldname.toLowerCase());
            ConfigurationSection p = plotdata.getConfigurationSection(worldname);
            commandParser.addCommand
                (new CreatePlot
                 (worldname,
                  p.getInt("regionSize"),
                  p.getInt("plotDistance"),
                  p.getInt("pasteY"),
                  p.getInt("wgRegionMinY"),
                  p.getInt("wgRegionMaxY"),
                  p.getString("templateRegionWorld"),
                  p.getString("templateRegion"),
                  p.getBoolean("syncCopy")));

            teleportYs.put(worldname, p.getInt("teleportY"));
        }
        commandParser.addCommand(new Home(teleportYs));
        commandParser.addCommand(new Subzone());
        commandParser.addCommand(new AdminRename(this));
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equals("cvcreativeplots")) {
            if(args.length == 1 && args[0].equals("reload")) {
                updateConfig();
                sender.sendMessage("§aConfiguration reloaded.");
                return true;
            }
            else {
                return commandParser.execute(sender, args);
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws ProtectedRegion.CircularInheritanceException {
        Player player = event.getPlayer();
        if(!worldNames.contains(player.getWorld().getName().toLowerCase())) return;
        BukkitPlayer bPlayer = BukkitAdapter.adapt(player);
        for(String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if(world != null) {
                RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(world));
                List<ProtectedRegion> ownedRegions = new ArrayList<>();
                assert manager != null;
                manager.getRegions().forEach((id, region) -> {
                    if (region.getOwners().contains(bPlayer.getUniqueId()) && region.getParent() == null && !region.getId().equalsIgnoreCase("__global__") && !region.getId().equalsIgnoreCase("creativespawn")) {
                        ownedRegions.add(region);
                    }
                });
                if(ownedRegions.size() == 1) {
                    if(!ownedRegions.get(0).getId().equalsIgnoreCase(bPlayer.getName())) {
                        updatePlotName(manager, ownedRegions.get(0), player.getName().toLowerCase());
                    }
                }
            }
        }
    }

    public void updatePlotName(RegionManager manager, ProtectedRegion oldRegion, String pName) throws ProtectedRegion.CircularInheritanceException {
        BlockVector3 min = oldRegion.getMinimumPoint();
        BlockVector3 max = oldRegion.getMaximumPoint();
        DefaultDomain owners = oldRegion.getOwners();
        DefaultDomain members = oldRegion.getMembers();
        Map<Flag<?>, Object> flags = oldRegion.getFlags();
        Set<ProtectedRegion> children = new HashSet<>();
        for(ProtectedRegion r : manager.getRegions().values()) {
            if(r.getParent() != null && r.getParent().equals(oldRegion)) {
                children.add(r);
            }
        }

        ProtectedRegion newRegion = new ProtectedCuboidRegion(pName, min, max);
        newRegion.setOwners(owners);
        newRegion.setMembers(members);
        newRegion.setFlags(flags);
        manager.addRegion(newRegion);
        for(ProtectedRegion r: children) {
            r.setParent(newRegion);
        }
        manager.removeRegion(oldRegion.getId());
    }


}

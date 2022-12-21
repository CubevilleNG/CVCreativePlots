package org.cubeville.cvcreativeplots;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.registry.state.BooleanProperty;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import org.cubeville.commons.commands.CommandParser;


import java.util.*;

public class CVCreativePlots extends JavaPlugin implements Listener {

    private CommandParser commandParserHome;
    private CommandParser commandParserSubzone;
    private HashMap<String, HashMap<String, Object>> config;
    private Home homeCommand;

    public void onEnable() {
        config = new HashMap<>();
        updateConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    public void updateConfig() {
        commandParserHome = new CommandParser();
        commandParserSubzone = new CommandParser();
        ConfigurationSection plotdata = getConfig().getConfigurationSection("plotdata");
        if(plotdata == null) return;
        for(String worldname: plotdata.getKeys(false)) {
            HashMap<String, Object> worldConfig = new HashMap<>();
            ConfigurationSection p = plotdata.getConfigurationSection(worldname);
            worldConfig.put("regionSize", p.getInt("regionSize"));
            worldConfig.put("plotDistance", p.getInt("plotDistance"));
            worldConfig.put("pasteY", p.getInt("pasteY"));
            worldConfig.put("wgRegionMinY", p.getInt("wgRegionMinY"));
            worldConfig.put("wgRegionMaxY", p.getInt("wgRegionMaxY"));
            worldConfig.put("templateRegionWorld", p.getString("templateRegionWorld"));
            worldConfig.put("templateRegion", p.getString("templateRegion"));
            worldConfig.put("syncCopy", p.getBoolean("syncCopy"));
            worldConfig.put("teleportY", p.getInt("teleportY"));
            worldConfig.put("disablePlotCreation", p.getBoolean("disablePlotCreation"));
            config.put(worldname.toLowerCase(), worldConfig);
        }
        homeCommand = new Home(config);
        commandParserHome.addCommand(homeCommand);
        commandParserSubzone.addCommand(new Subzone());
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(command.getName().equalsIgnoreCase("cvcreativeplots")) {
            if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                updateConfig();
                sender.sendMessage("§aConfiguration reloaded.");
                return true;
            } else if(args.length > 0 && args[0].equalsIgnoreCase("disableplotcreation")) {
                if(!(sender instanceof Player)) {
                    sender.sendMessage("You must be a player to run this command!");
                    return true;
                }
                if(args.length != 2 || (!args[1].equalsIgnoreCase("true") && !args[1].equalsIgnoreCase("false"))) {
                    sender.sendMessage("Invalid command! Try /cvcreativeplotsdisableplotcreation <true | false>");
                    return true;
                }
                ConfigurationSection plotdata = getConfig().getConfigurationSection("plotdata");
                if(plotdata == null) return true;
                ConfigurationSection p = plotdata.getConfigurationSection(((Player)sender).getWorld().getName().toLowerCase());
                p.set("disablePlotCreation", Boolean.valueOf(args[1]));
                saveConfig();
                HashMap<String, HashMap<String, Object>> config = homeCommand.getConfig();
                HashMap<String, Object> worldConfig = config.get(((Player)sender).getWorld().getName());
                worldConfig.put("disablePlotCreation", Boolean.valueOf(args[1]));
                config.put(((Player)sender).getWorld().getName(), worldConfig);
                homeCommand.setConfig(config);
                sender.sendMessage("DisablePlotCreation set to " + args[1] + " in world: " + ((Player)sender).getWorld().getName());
                return true;
            }
            return false;
        } else if(command.getName().equalsIgnoreCase("home")) {
            return commandParserHome.execute(sender, args);
        } else if(command.getName().equalsIgnoreCase("subzone")) {
            return commandParserSubzone.execute(sender, args);
        }
        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) throws ProtectedRegion.CircularInheritanceException {
        Player player = event.getPlayer();
        if(!config.containsKey(player.getWorld().getName().toLowerCase())) return;
        BukkitPlayer bPlayer = BukkitAdapter.adapt(player);
        for(String worldName : config.keySet()) {
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

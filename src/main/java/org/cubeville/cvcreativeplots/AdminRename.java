package org.cubeville.cvcreativeplots;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cubeville.commons.commands.*;

import java.util.*;

public class AdminRename extends BaseCommand {

    private final CVCreativePlots plugin;

    public AdminRename(CVCreativePlots plugin) {
        super("adminrename");
        addOptionalBaseParameter(new CommandParameterString());
        setPermission("cvcreativeplots.admin");

        this.plugin = plugin;
    }

    @Override
    public CommandResponse execute(CommandSender sender, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters) throws CommandExecutionException {

        Player player = (Player) sender;
        BukkitPlayer bPlayer = BukkitAdapter.adapt(player);
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(bPlayer.getWorld());

        if(baseParameters.size() > 0) {
            ProtectedRegion oldRegion = manager.getRegion((String) baseParameters.get(0));
            if(oldRegion == null || oldRegion.getId().equalsIgnoreCase("__global__") || oldRegion.getId().equalsIgnoreCase("creativespawn")) return new CommandResponse("BAD REGION!");

            Set<UUID> owners = oldRegion.getOwners().getUniqueIds();
            List<UUID> o = new ArrayList<>(owners);

            UUID uuid = o.get(0);
            String currentName = Bukkit.getOfflinePlayer(uuid).getName();

            if(!oldRegion.getId().equalsIgnoreCase(currentName)) {
                try {
                    plugin.updatePlotName(manager, oldRegion, currentName);
                } catch (ProtectedRegion.CircularInheritanceException e) {
                    e.printStackTrace();
                }
                return new CommandResponse(ChatColor.LIGHT_PURPLE + "Region: " +  ChatColor.GOLD + oldRegion.getId() + ChatColor.LIGHT_PURPLE + " renamed to: " + ChatColor.GOLD + currentName);
            }
            return new CommandResponse(ChatColor.LIGHT_PURPLE + "Region: " + ChatColor.GOLD + oldRegion.getId() + ChatColor.LIGHT_PURPLE + " not renamed!");
        } else {
            Map<ProtectedRegion, String> updateList = new HashMap<>();
            Map<UUID, String> oPlayers = new HashMap<>();
            for(OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                if((!oPlayers.containsKey(p.getUniqueId()) || oPlayers.get(p.getUniqueId()) == null) && p.getName() != null) {
                    oPlayers.put(p.getUniqueId(), p.getName());
                }
            }

            manager.getRegions().forEach((id, region) -> {
                Set<UUID> owners = region.getOwners().getUniqueIds();
                List<UUID> o = new ArrayList<>(owners);
                String currentName;
                if(o.size() > 0) {
                    UUID uuid = o.get(0);
                    currentName = oPlayers.get(uuid);
                } else {
                    currentName = null;
                }
                if(currentName != null && !region.getId().equalsIgnoreCase(currentName) && !region.getId().equalsIgnoreCase("__global__") && !region.getId().equalsIgnoreCase("creativespawn")) {
                    updateList.put(region, currentName);
                }
            });

            int i = 1;
            for(ProtectedRegion r : updateList.keySet()) {
                Bukkit.getScheduler().runTaskLater(plugin, ()-> {
                    try {
                        plugin.updatePlotName(manager, r, updateList.get(r));
                    } catch (ProtectedRegion.CircularInheritanceException e) {
                        e.printStackTrace();
                    }
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "Region: " +  ChatColor.GOLD + r.getId() + ChatColor.LIGHT_PURPLE + " renamed to: " + ChatColor.GOLD + updateList.get(r));
                }, 20L * i);
                i++;
            }
            return new CommandResponse(ChatColor.LIGHT_PURPLE + "Estimated completion time in seconds: " + ChatColor.GOLD + (i));
        }
    }
}

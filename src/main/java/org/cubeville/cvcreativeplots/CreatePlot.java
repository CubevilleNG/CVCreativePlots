package org.cubeville.cvcreativeplots;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.cubeville.commons.commands.BaseCommand;
import org.cubeville.commons.commands.CommandExecutionException;
import org.cubeville.commons.commands.CommandParameterOnlinePlayer;
import org.cubeville.commons.commands.CommandResponse;

public class CreatePlot extends BaseCommand {
    private final int MAX_PLOTS = 1000;

    String worldname;
    int regionSize;
    int plotDistance;
    int pasteY;
    int wgRegionMinY;
    int wgRegionMaxY;
    String templateRegionWorld;
    String templateRegion;
    boolean syncCopy;
    
    public CreatePlot(String worldname, int regionSize, int plotDistance, int pasteY, int wgRegionMinY, int wgRegionMaxY, String templateRegionWorld, String templateRegion, boolean syncCopy) {
        super("createplot " + worldname);
        addBaseParameter(new CommandParameterOnlinePlayer());
        setPermission("cvcreativeplots.createplot");
        this.worldname = worldname;
        this.regionSize = regionSize;
        this.plotDistance = plotDistance;
        this.pasteY = pasteY;
        this.wgRegionMinY = wgRegionMinY;
        this.wgRegionMaxY = wgRegionMaxY;
        this.templateRegionWorld = templateRegionWorld;
        this.templateRegion = templateRegion;
        this.syncCopy = syncCopy;
    }

    private void copyPlotToLocation(BlockVector3 loc) {
        String cmd = String.format("cvblocks copytocoord %s %s %s %d,%d,%d",
                                   templateRegionWorld, // source world
                                   templateRegion, // source region
                                   worldname, // target world
                                   loc.getX(), pasteY, loc.getZ() // (x, y, z)
                                   );
        if(syncCopy) cmd += " sync";
        System.out.println("Running cvblocks command: " + cmd);
        if(syncCopy) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd); }
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private void createPlotRegion(BlockVector3 min, Player player) {
        min = min.add(plotDistance - 1, 0, plotDistance - 1);
        BlockVector3 max = BlockVector3.at(min.getX() + regionSize - 1, wgRegionMaxY, min.getZ() + regionSize - 1);
        ProtectedRegion region = new ProtectedCuboidRegion(player.getName(), min, max);
        region.getOwners().addPlayer(player.getUniqueId());
        region.setFlag(Flags.GREET_MESSAGE, String.format("&bEntering the plot of &3%s&b!", player.getName()));
        region.setFlag(Flags.FAREWELL_MESSAGE, String.format("&bLeaving the plot of &3%s&b!", player.getName()));

        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager allRegions = container.get(BukkitAdapter.adapt(Bukkit.getServer().getWorld(worldname)));

        allRegions.addRegion(region);
    }

    private BlockVector3 findPlotLocation() throws CommandExecutionException {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager allRegions = container.get(BukkitAdapter.adapt(Bukkit.getServer().getWorld(worldname)));

        // Set initial value of grid location to (0, 1)
        BlockVector2 gridLocation = BlockVector2.at(0, 1);

        // Start by moving in the negative direction on the X axis.
        BlockVector2 moveDirection = BlockVector2.at(-1, 0);

        for (int i = 0; i < MAX_PLOTS; i++) {
            BlockVector3 plotLocation = BlockVector3.at(
                                                        gridLocation.getX() * (regionSize + plotDistance),
                                                        wgRegionMinY,
                                                        gridLocation.getZ()  * (regionSize + plotDistance)
                                                        );
            BlockVector3 checkLocation = plotLocation.add(10, 0, 10);
            ApplicableRegionSet pointRegionSet = allRegions.getApplicableRegions(checkLocation);

            // If no region exists, return the point the new plot should be
            if (pointRegionSet.size() == 0) {
                return plotLocation;
            }

            // If the last region checked was the highest corner (eg. (1, 1), (2, 2), (40, 40)) then move on to the next layer.
            if (gridLocation.getX() + gridLocation.getZ() == 2 * Math.max(gridLocation.abs().getX(), gridLocation.abs().getZ())) {
                gridLocation = gridLocation.add(0, 1);
            } else {
                gridLocation = gridLocation.add(moveDirection);
                // If it has hit a corner change the direction it is traveling
                if (gridLocation.abs().getX() == gridLocation.abs().getZ()) {
                    moveDirection = BlockVector2.at(
                                                    -1 * moveDirection.getZ(),
                                                    moveDirection.getX()
                                                    );

                }
            }
        }
        throw new CommandExecutionException("Could not find a plot to use.");
    }

    @Override
    public CommandResponse execute(CommandSender sender, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters)
        throws CommandExecutionException {

        CommandResponse cr = new CommandResponse();
        cr.setBaseMessage("&c&lCreating Plot...");

        BlockVector3 plotLocation = findPlotLocation();
        copyPlotToLocation(plotLocation);
        createPlotRegion(plotLocation, (Player) baseParameters.get(0));
        cr.addMessage(String.format("&bPlot has been created for %s at &3&lX: %d&b,&3&l Z: %d &b!", baseParameters.get(0), plotLocation.getX(), plotLocation.getZ()));

        return cr;
    }
}

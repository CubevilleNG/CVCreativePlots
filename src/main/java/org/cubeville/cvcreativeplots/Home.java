package org.cubeville.cvcreativeplots;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.cubeville.commons.commands.BaseCommand;
import org.cubeville.commons.commands.CommandExecutionException;
import org.cubeville.commons.commands.CommandParameterString;
import org.cubeville.commons.commands.CommandResponse;

import java.util.*;

public class Home extends BaseCommand {

	private final int MAX_PLOTS = 5000;
	private HashMap<String, HashMap<String, Object>> config;

	public Home(HashMap<String, HashMap<String, Object>> config) {
		super("");
		// parameter is string instead of online player because you can tp to plot of offline player
		addOptionalBaseParameter(new CommandParameterString());
		setPermission("cvcreativeplots.home");
		this.config = config;
	}

	@Override
	public CommandResponse execute(CommandSender sender, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters) throws CommandExecutionException {
		if (!(sender instanceof Player)) {
			throw new CommandExecutionException("Cannot run command from console.");
		}

		Player p = (Player) sender;
		World world = p.getWorld();

		if(!config.containsKey(world.getName().toLowerCase())) {
			throw new CommandExecutionException("This world is not setup for /home! Check config!");
		}

		// If player is specified in the command
		if (baseParameters.size() >= 1) {
			ProtectedRegion region = getRegionByName(world, (String) baseParameters.get(0));
			if(region == null || region.getParent() != null || !isRegionValidPlot(world, region)) {
				throw new CommandExecutionException(baseParameters.get(0) + " does not have a plot!");
			}
			tpPlayerToPlot(p, region);
			return new CommandResponse("&bTeleported to the plot of " + baseParameters.get(0));
		}

		ProtectedRegion region = getRegionByName(world, p.getName());
		if(region == null) {
			return createPlot(world, p);
		} else {
			if(!isRegionValidPlot(world, region)) {
				throw new CommandExecutionException("You already have a plot created and it is configured wrong! Contact an administrator!");
			}
			tpPlayerToPlot(p, region);
			return new CommandResponse("&bTeleported to your plot");
		}
	}

	public void tpPlayerToPlot(Player player, ProtectedRegion plot) {
		BlockVector3 corner = plot.getMaximumPoint();
		World world = player.getWorld();
		Location tpLocation = new Location(world, corner.getX() - 1, (int) config.get(world.getName().toLowerCase()).get("teleportY"), corner.getZ() - 1, 135, 0);
		player.teleport(tpLocation);
	}

	public ProtectedRegion getRegionByName(World world, String name) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager allRegions = container.get(BukkitAdapter.adapt(world));
		for(ProtectedRegion region : allRegions.getRegions().values()) {
			if(region.getId().equalsIgnoreCase(name)) {
				return region;
			}
		}
		return null;
	}

	public boolean isRegionValidPlot(World world, ProtectedRegion region) {
		int minY = (int) config.get(world.getName().toLowerCase()).get("wgRegionMinY");
		int maxY = (int) config.get(world.getName().toLowerCase()).get("wgRegionMaxY");
		int regionSize = (int) config.get(world.getName().toLowerCase()).get("regionSize");
		int volume = ((maxY - minY) * regionSize * regionSize);
		System.out.println(region.volume());
		System.out.println(volume);
		return region.volume() == volume;
	}

	public CommandResponse createPlot(World world, Player p) throws CommandExecutionException {
		Bukkit.getConsoleSender().sendMessage("&c&lCreating Plot...");
		BlockVector3 plotLocation = findPlotLocation(world);
		copyPlotToLocation(world, plotLocation);
		createPlotRegion(world, plotLocation, p);
		Bukkit.getConsoleSender().sendMessage(String.format("&bPlot has been created for %s at &3&lX: %d&b,&3&l Z: %d &b!", p.getName(), plotLocation.getX(), plotLocation.getZ()));
		return new CommandResponse("&bA plot has been created for you! Please read over the rules at the creative world spawn if you have not already. Use &a/home&b whilst on the creative server to get to your plot, &a/help creative&b lists more commands. Have fun!");
	}

	public BlockVector3 findPlotLocation(World world) throws CommandExecutionException {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager allRegions = container.get(BukkitAdapter.adapt(Bukkit.getServer().getWorld(world.getName())));

		// Set initial value of grid location to (0, 1)
		BlockVector2 gridLocation = BlockVector2.at(0, 1);

		// Start by moving in the negative direction on the X axis.
		BlockVector2 moveDirection = BlockVector2.at(-1, 0);

		int regionSize = (int) config.get(world.getName().toLowerCase()).get("regionSize");
		int plotDistance = (int) config.get(world.getName().toLowerCase()).get("plotDistance");
		int wgRegionMinY = (int) config.get(world.getName().toLowerCase()).get("wgRegionMinY");
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

	public void copyPlotToLocation(World world, BlockVector3 loc) {
		String cmd = String.format("cvblocks copytocoord %s %s %s %d,%d,%d",
				config.get(world.getName().toLowerCase()).get("templateRegionWorld"), // source world
				config.get(world.getName().toLowerCase()).get("templateRegion"), // source region
				world.getName(), // target world
				loc.getX(), (int) config.get(world.getName().toLowerCase()).get("pasteY"), loc.getZ() // (x, y, z)
		);
		if((boolean) config.get(world.getName().toLowerCase()).get("syncCopy")) cmd += " sync";
		Bukkit.getConsoleSender().sendMessage(("Running cvblocks command: " + cmd));
		if((boolean) config.get(world.getName().toLowerCase()).get("syncCopy")) { Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd); }
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
	}

	private void createPlotRegion(World world, BlockVector3 min, Player player) {
		int plotDistance = (int) config.get(world.getName().toLowerCase()).get("plotDistance");
		int regionSize = (int) config.get(world.getName().toLowerCase()).get("regionSize");
		min = min.add(plotDistance - 1, 0, plotDistance - 1);
		BlockVector3 max = BlockVector3.at(min.getX() + regionSize - 1, (int) config.get(world.getName().toLowerCase()).get("wgRegionMaxY"), min.getZ() + regionSize - 1);
		ProtectedRegion region = new ProtectedCuboidRegion(player.getName(), min, max);
		region.getOwners().addPlayer(player.getUniqueId());
		region.setFlag(Flags.GREET_MESSAGE, String.format("&bEntering the plot of &3%s&b!", player.getName()));
		region.setFlag(Flags.FAREWELL_MESSAGE, String.format("&bLeaving the plot of &3%s&b!", player.getName()));

		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager allRegions = container.get(BukkitAdapter.adapt(Bukkit.getServer().getWorld(world.getName())));

		allRegions.addRegion(region);
	}
}

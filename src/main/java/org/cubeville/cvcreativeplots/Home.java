package org.cubeville.cvcreativeplots;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cubeville.commons.commands.BaseCommand;
import org.cubeville.commons.commands.CommandExecutionException;
import org.cubeville.commons.commands.CommandParameterString;
import org.cubeville.commons.commands.CommandResponse;

import java.util.*;

public class Home extends BaseCommand {

	Map<String, Integer> teleportYs;

	public Home(Map<String, Integer> teleportYs) {
		super("home");
		// parameter is string instead of online player because you can tp to plot of offline player
		addOptionalBaseParameter(new CommandParameterString());
		setPermission("cvcreativeplots.home");
		this.teleportYs = teleportYs;
	}

	public void tpPlayerToPlot(Player player, ProtectedRegion plot) {
		BlockVector3 corner = plot.getMaximumPoint();
		World world = player.getWorld();
		Location tpLocation = new Location(world, corner.getX() - 1, this.teleportYs.get(world.getName()), corner.getZ() - 1, 135, 0);
		player.teleport(tpLocation);
	}

	// the reason we split this up is if a person wants to
	public List<ProtectedRegion> findRegionsWithOwner(World world, String player) throws CommandExecutionException {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager allRegions = container.get(BukkitAdapter.adapt(world));
		List<ProtectedRegion> ownedRegions = new ArrayList<>();
		WorldGuardPlugin plugin = WorldGuardPlugin.inst();

		allRegions.getRegions().forEach((id, region) -> {
			if(region.getId().equalsIgnoreCase(player) && region.getParent() == null) {
				for(UUID uuid : region.getOwners().getUniqueIds()) {
					String oName = Bukkit.getOfflinePlayer(uuid).getName();
					if(oName != null && oName.equalsIgnoreCase(player)) {
						ownedRegions.add(region);
						break;
					}
				}
			}
		});

		return ownedRegions;
	}

	public boolean doesIdenticalRegionNameExist(World world, String name) {
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionManager allRegions = container.get(BukkitAdapter.adapt(world));

		for(ProtectedRegion region : allRegions.getRegions().values()) {
			if(region.getId().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public CommandResponse execute(CommandSender sender, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters) throws CommandExecutionException {
		if (!(sender instanceof Player)) {
			throw new CommandExecutionException("Cannot run command from console.");
		}

		Player p = (Player) sender;
		World world = p.getWorld();

		// If player is specified in the command
		if (baseParameters.size() >= 1) {
			List<ProtectedRegion> ownedRegions = findRegionsWithOwner(world, (String) baseParameters.get(0));

			// Throw error if there is more than 1 region owned by a player
			if (ownedRegions.size() > 1) {
				throw new CommandExecutionException(String.format("Player %s is owner of %d regions, expected 1", baseParameters.get(0), ownedRegions.size()));
			}
                        else if (ownedRegions.size() == 0) {
                            throw new CommandExecutionException("No region found");
                        }
			ProtectedRegion plot = ownedRegions.get(0);
			this.tpPlayerToPlot(p, plot);
			return new CommandResponse("&bTeleported to the plot of " + baseParameters.get(0));

		}

		List<ProtectedRegion> ownedRegions = findRegionsWithOwner(world, p.getName());

		// Throw error if there is more than 1 region owned by a player
		if (ownedRegions.size() > 1) {
			throw new CommandExecutionException(String.format("Player %s is owner of %d regions, expected 1", p.getName(), ownedRegions.size()));
		}

		if (ownedRegions.size() == 1) {
			ProtectedRegion plot = ownedRegions.get(0);
			this.tpPlayerToPlot(p, plot);
			return new CommandResponse("&bTeleported to your plot");
		}

		if(doesIdenticalRegionNameExist(world, p.getName())) {
			throw new CommandExecutionException("A region named " + p.getName() + " already exists but is not a plot!");
		}

		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cvcreativeplots createplot " + world.getName() + " " + p.getName());
		return new CommandResponse("&bA plot has been created for you! Please read over the rules at the creative world spawn if you have not already. Use &a/home&b whilst on the creative server to get to your plot, &a/help creative&b lists more commands. Have fun!");
	}
}

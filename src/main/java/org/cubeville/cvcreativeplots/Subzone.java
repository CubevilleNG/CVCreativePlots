package org.cubeville.cvcreativeplots;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.cubeville.commons.commands.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Subzone extends BaseCommand {

    public Subzone() {
        super("subzone");
        addBaseParameter(new CommandParameterString());
        addBaseParameter(new CommandParameterString(CommandParameterString.NO_SPECIAL_CHARACTERS));
        setPermission("cvcreativeplots.subzone");
    }

    @Override
    public CommandResponse execute(CommandSender sender, Set<String> flags, Map<String, Object> parameters, List<Object> baseParameters) {

        Player player = (Player) sender;
        BukkitPlayer bPlayer = BukkitAdapter.adapt(player);
        RegionManager manager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(bPlayer.getWorld());
        assert manager != null;
        ProtectedRegion parent = manager.getRegion((String) baseParameters.get(0));
        ProtectedRegion child = manager.getRegion((String) baseParameters.get(1));
        LocalSession localSession = WorldEdit.getInstance().getSessionManager().get(bPlayer);
        Region playerSelection;

        if(parent == null) return new CommandResponse(ChatColor.RED + "Unable to find region: " + baseParameters.get(0));
        if(child != null) return new CommandResponse(ChatColor.RED + "The subzone: " + baseParameters.get(1) + " already exists! Try a different name!");
        if(!parent.getOwners().contains(player.getUniqueId())) return new CommandResponse(ChatColor.RED + "You are not owner of the region: " + baseParameters.get(0));
        try {
            playerSelection = localSession.getSelection(bPlayer.getWorld());
        } catch (IncompleteRegionException e) {
            return new CommandResponse(ChatColor.RED + "You must make a selection first!");
        }

        BlockVector3 min = playerSelection.getMinimumPoint();
        BlockVector3 max = playerSelection.getMaximumPoint();
        if(!parent.contains(min) || !parent.contains(max)) return new CommandResponse(ChatColor.RED + "Your selection is outside your plot!");

        ProtectedRegion subzone = new ProtectedCuboidRegion((String) baseParameters.get(1), min, max);
        manager.addRegion(subzone);
        try {
            subzone.setParent(parent);
        } catch (ProtectedRegion.CircularInheritanceException e) {
            throw new RuntimeException(e);
        }
        return new CommandResponse(ChatColor.LIGHT_PURPLE + "Subzone: " + baseParameters.get(1) + " created!");
    }
}

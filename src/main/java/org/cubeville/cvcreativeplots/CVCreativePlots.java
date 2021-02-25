package org.cubeville.cvcreativeplots;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import org.bukkit.plugin.java.JavaPlugin;

import org.cubeville.commons.commands.CommandParser;

import org.cubeville.cvtools.commands.*;

import java.util.HashMap;
import java.util.Map;

public class CVCreativePlots extends JavaPlugin {

    private CommandParser commandParser;

    public void onEnable() {
        updateConfig();
    }

    public void updateConfig() {
        commandParser = new CommandParser();
        ConfigurationSection plotdata = getConfig().getConfigurationSection("plotdata");
        if(plotdata == null) return;
        Map<String, Integer> teleportYs = new HashMap<>();
        for(String worldname: plotdata.getKeys(false)) {
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
                  p.getString("templateRegion")));

            teleportYs.put(worldname, p.getInt("teleportY"));
        }
        commandParser.addCommand(new Home(teleportYs));
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

}

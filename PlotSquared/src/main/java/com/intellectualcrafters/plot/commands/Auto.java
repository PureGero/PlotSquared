////////////////////////////////////////////////////////////////////////////////////////////////////
// PlotSquared - A plot manager and world generator for the Bukkit API                             /
// Copyright (c) 2014 IntellectualSites/IntellectualCrafters                                       /
//                                                                                                 /
// This program is free software; you can redistribute it and/or modify                            /
// it under the terms of the GNU General Public License as published by                            /
// the Free Software Foundation; either version 3 of the License, or                               /
// (at your option) any later version.                                                             /
//                                                                                                 /
// This program is distributed in the hope that it will be useful,                                 /
// but WITHOUT ANY WARRANTY; without even the implied warranty of                                  /
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                                   /
// GNU General Public License for more details.                                                    /
//                                                                                                 /
// You should have received a copy of the GNU General Public License                               /
// along with this program; if not, write to the Free Software Foundation,                         /
// Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA                               /
//                                                                                                 /
// You can contact us via: support@intellectualsites.com                                           /
////////////////////////////////////////////////////////////////////////////////////////////////////
package com.intellectualcrafters.plot.commands;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.intellectualcrafters.plot.BukkitMain;
import com.intellectualcrafters.plot.PlotSquared;
import com.intellectualcrafters.plot.config.C;
import com.intellectualcrafters.plot.config.Settings;
import com.intellectualcrafters.plot.object.Plot;
import com.intellectualcrafters.plot.object.PlotCluster;
import com.intellectualcrafters.plot.object.PlotId;
import com.intellectualcrafters.plot.object.PlotWorld;
import com.intellectualcrafters.plot.util.ClusterManager;
import com.intellectualcrafters.plot.util.PlotHelper;
import com.intellectualcrafters.plot.util.bukkit.BukkitUtil;
import com.intellectualcrafters.plot.util.bukkit.PlayerFunctions;

public class Auto extends SubCommand {
    public Auto() {
        super("auto", "plots.auto", "Claim the nearest plot", "auto", "a", CommandCategory.CLAIMING, true);
    }
    
    public static PlotId getNextPlot(final PlotId id, final int step) {
        final int absX = Math.abs(id.x);
        final int absY = Math.abs(id.y);
        if (absX > absY) {
            if (id.x > 0) {
                return new PlotId(id.x, id.y + 1);
            } else {
                return new PlotId(id.x, id.y - 1);
            }
        } else if (absY > absX) {
            if (id.y > 0) {
                return new PlotId(id.x - 1, id.y);
            } else {
                return new PlotId(id.x + 1, id.y);
            }
        } else {
            if (id.x.equals(id.y) && (id.x > 0)) {
                return new PlotId(id.x, id.y + step);
            }
            if (id.x == absX) {
                return new PlotId(id.x, id.y + 1);
            }
            if (id.y == absY) {
                return new PlotId(id.x, id.y - 1);
            }
            return new PlotId(id.x + 1, id.y);
        }
    }
    
    // TODO auto claim a mega plot with schematic
    @Override
    public boolean execute(final Player plr, final String... args) {
        World world;
        int size_x = 1;
        int size_z = 1;
        String schematic = "";
        if (PlotSquared.getPlotWorlds().size() == 1) {
            world = Bukkit.getWorld(PlotSquared.getPlotWorlds().iterator().next());
        } else {
            if (PlotSquared.isPlotWorld(plr.getWorld().getName())) {
                world = plr.getWorld();
            } else {
                PlayerFunctions.sendMessage(plr, C.NOT_IN_PLOT_WORLD);
                return false;
            }
        }
        if (args.length > 0) {
            if (BukkitMain.hasPermission(plr, "plots.auto.mega")) {
                try {
                    final String[] split = args[0].split(",");
                    size_x = Integer.parseInt(split[0]);
                    size_z = Integer.parseInt(split[1]);
                    if ((size_x < 1) || (size_z < 1)) {
                        PlayerFunctions.sendMessage(plr, "&cError: size<=0");
                    }
                    if ((size_x > 4) || (size_z > 4)) {
                        PlayerFunctions.sendMessage(plr, "&cError: size>4");
                    }
                    if (args.length > 1) {
                        schematic = args[1];
                    }
                } catch (final Exception e) {
                    size_x = 1;
                    size_z = 1;
                    schematic = args[0];
                    // PlayerFunctions.sendMessage(plr,
                    // "&cError: Invalid size (X,Y)");
                    // return false;
                }
            } else {
                schematic = args[0];
                // PlayerFunctions.sendMessage(plr, C.NO_PERMISSION);
                // return false;
            }
        }
        if ((size_x * size_z) > Settings.MAX_AUTO_SIZE) {
            PlayerFunctions.sendMessage(plr, C.CANT_CLAIM_MORE_PLOTS_NUM, Settings.MAX_AUTO_SIZE + "");
            return false;
        }
        final int diff = PlayerFunctions.getPlayerPlotCount(world, plr) - PlayerFunctions.getAllowedPlots(plr);
        if ((diff + (size_x * size_z)) > 0) {
            if (diff < 0) {
                PlayerFunctions.sendMessage(plr, C.CANT_CLAIM_MORE_PLOTS_NUM, (-diff) + "");
            } else {
                PlayerFunctions.sendMessage(plr, C.CANT_CLAIM_MORE_PLOTS);
            }
            return false;
        }
        final PlotWorld pWorld = PlotSquared.getWorldSettings(world.getName());
        if ((PlotSquared.economy != null) && pWorld.USE_ECONOMY) {
            double cost = pWorld.PLOT_PRICE;
            cost = (size_x * size_z) * cost;
            if (cost > 0d) {
                final Economy economy = PlotSquared.economy;
                if (economy.getBalance(plr) < cost) {
                    sendMessage(plr, C.CANNOT_AFFORD_PLOT, "" + cost);
                    return true;
                }
                economy.withdrawPlayer(plr, cost);
                sendMessage(plr, C.REMOVED_BALANCE, cost + "");
            }
        }
        if (!schematic.equals("")) {
            // if (pWorld.SCHEMATIC_CLAIM_SPECIFY) {
            if (!pWorld.SCHEMATICS.contains(schematic.toLowerCase())) {
                sendMessage(plr, C.SCHEMATIC_INVALID, "non-existent: " + schematic);
                return true;
            }
            if (!BukkitMain.hasPermission(plr, "plots.claim." + schematic) && !plr.hasPermission("plots.admin.command.schematic")) {
                PlayerFunctions.sendMessage(plr, C.NO_SCHEMATIC_PERMISSION, schematic);
                return true;
            }
            // }
        }
        final String worldname = world.getName();
        final PlotWorld plotworld = PlotSquared.getWorldSettings(worldname);
        if (plotworld.TYPE == 2) {
            final Location loc = BukkitUtil.getLocation(plr);
            final Plot plot = PlotHelper.getCurrentPlot(new com.intellectualcrafters.plot.object.Location(worldname, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            if (plot == null) {
                return sendMessage(plr, C.NOT_IN_PLOT);
            }
            final PlotCluster cluster = ClusterManager.getCluster(loc);
            // Must be standing in a cluster
            if (cluster == null) {
                PlayerFunctions.sendMessage(plr, C.NOT_IN_CLUSTER);
                return false;
            }
            final PlotId bot = cluster.getP1();
            final PlotId top = cluster.getP2();
            final PlotId origin = new PlotId((bot.x + top.x) / 2, (bot.y + top.y) / 2);
            PlotId id = new PlotId(0, 0);
            final int width = Math.max((top.x - bot.x) + 1, (top.y - bot.y) + 1);
            final int max = width * width;
            //
            for (int i = 0; i <= max; i++) {
                final PlotId currentId = new PlotId(origin.x + id.x, origin.y + id.y);
                final Plot current = PlotHelper.getPlot(worldname, currentId);
                if ((current != null) && (current.hasOwner() == false) && (current.settings.isMerged() == false) && cluster.equals(ClusterManager.getCluster(current))) {
                    Claim.claimPlot(plr, current, true, true);
                    return true;
                }
                id = getNextPlot(id, 1);
            }
            // no free plots
            PlayerFunctions.sendMessage(plr, C.NO_FREE_PLOTS);
            return false;
        }
        boolean br = false;
        if ((size_x == 1) && (size_z == 1)) {
            while (!br) {
                final Plot plot = PlotHelper.getPlot(worldname, getLastPlot(worldname));
                if ((plot.owner == null)) {
                    Claim.claimPlot(plr, plot, true, true);
                    br = true;
                }
                PlotHelper.lastPlot.put(worldname, getNextPlot(getLastPlot(worldname), 1));
            }
        } else {
            boolean lastPlot = true;
            while (!br) {
                final PlotId start = getNextPlot(getLastPlot(worldname), 1);
                // Checking if the current set of plots is a viable option.
                PlotHelper.lastPlot.put(worldname, start);
                if (lastPlot) {
                }
                if ((PlotSquared.getPlots(worldname).get(start) != null) && (PlotSquared.getPlots(worldname).get(start).owner != null)) {
                    continue;
                } else {
                    lastPlot = false;
                }
                final PlotId end = new PlotId((start.x + size_x) - 1, (start.y + size_z) - 1);
                if (PlotHelper.isUnowned(worldname, start, end)) {
                    for (int i = start.x; i <= end.x; i++) {
                        for (int j = start.y; j <= end.y; j++) {
                            final Plot plot = PlotHelper.getPlot(worldname, new PlotId(i, j));
                            final boolean teleport = ((i == end.x) && (j == end.y));
                            Claim.claimPlot(plr, plot, teleport, true);
                        }
                    }
                    if (!PlotHelper.mergePlots(plr, worldname, PlayerFunctions.getPlotSelectionIds(start, end))) {
                        return false;
                    }
                    br = true;
                }
            }
        }
        PlotHelper.lastPlot.put(worldname, new PlotId(0, 0));
        return true;
    }
    
    public PlotId getLastPlot(final String world) {
        if ((PlotHelper.lastPlot == null) || !PlotHelper.lastPlot.containsKey(world)) {
            PlotHelper.lastPlot.put(world, new PlotId(0, 0));
        }
        return PlotHelper.lastPlot.get(world);
    }
}

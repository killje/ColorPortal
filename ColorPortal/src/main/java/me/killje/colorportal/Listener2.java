package me.killje.colorportal;

import com.avaje.ebean.validation.Future;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Button;
import org.bukkit.metadata.MetadataValue;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class Listener2 implements Listener {

    private final ColorPortal2 plugin;
    private final String channelTable;
    private final String portalTable;
    private final boolean shutdownOnError;
    private final Map<Integer, Channel> channels = new HashMap<>();
    private final Map<Player, SignPlayerHelper> sphs = new HashMap<>();

    public Listener2(ColorPortal2 plugin) {
        this.plugin = plugin;
        this.channelTable = plugin.getConfig().getString("channelTabel");
        this.portalTable = plugin.getConfig().getString("portalTabel");
        this.shutdownOnError = plugin.getConfig().getBoolean("shutdownOnError");
        loadPortals();
    }

    private void loadPortals() {
        try {
            Query channelQuery = new Query(channelTable).select();
            ResultSet rs = channelQuery.sqlConnect();

            while (!rs.isClosed() && rs.next()) {
                int channel = rs.getInt("channel");
                String restriction = rs.getString("restriction");
                UUID owner = UUID.fromString(rs.getString("owner"));
                channels.put(channel, new Channel(owner, channel, restriction));
            }

            Query portalQuery = new Query(portalTable).select();
            rs = portalQuery.sqlConnect();

            while (!rs.isClosed() && rs.next()) {
                int channel = rs.getInt("channel");
                String name = rs.getString("name");
                UUID world = UUID.fromString(rs.getString("owner"));
                Sign sign = (Sign) Query.queryToBlock(world, rs.getString("locationSign"));
                Block button = Query.queryToBlock(world, rs.getString("locationButton"));
                Block signBlock = Query.queryToBlock(world, rs.getString("locationSignBlock"));
                Block buttonBlock = Query.queryToBlock(world, rs.getString("locationButtonBlock"));
                int color = rs.getInt("color");
                Material material;
                if (rs.getInt("material") == 0) {
                    material = Material.WOOL;
                } else {
                    material = Material.STAINED_CLAY;
                }
                Portal2 portal = new Portal2(name, button, sign, signBlock, buttonBlock, material, channel, color);
                channels.get(channel).addPortal(portal);
            }
        } catch (SQLTimeoutException ex) {
            plugin.getLogger().log(Level.SEVERE, "Connection to database Timed out during loading of portals.", ex);
            if (shutdownOnError) {
                plugin.getServer().shutdown();
            }
        } catch (SQLException ex) {
            plugin.getLogger().log(Level.SEVERE, "A error occured during loading of portals", ex);
            if (shutdownOnError) {
                plugin.getServer().shutdown();
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!isPortalBlock(event.getClickedBlock())) {
            return;
        }
        if (event.getClickedBlock().getType().equals(Material.WALL_SIGN)) {
            Portal2 portal = (Portal2) event.getClickedBlock().getMetadata("colorPortal").get(0).value();
            event.setCancelled(true);
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                rightClickSign(portal, event.getPlayer());
            } else if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                leftClickSign(portal, event.getPlayer());
            }
        } else if (isButton(event.getClickedBlock())) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
        }
    }

    private void rightClickSign(Portal2 portal, Player player) {
        boolean isOwner = channels.get(portal.getChannel()).getOwner().equals(player.getUniqueId());
        if (!portal.hasChangePermission(player, isOwner)) {
            player.sendMessage(ChatColor.RED + "You do not have the permission to change this sign destination.");
            return;
        }
        SignPlayerHelper sph;
        if (!sphs.containsKey(player)) {
            sph = new SignPlayerHelper(player, portal.getSign());
            sphs.put(player, sph);
            sph.init();
            return;
        } else {
            sph = sphs.get(player);
            if (!sph.isSameSign(portal.getSign().getLocation())) {
                sph.newSign(portal.getSign());
                sph.init();
                return;
            }
        }
        sph.next();
    }

    private void leftClickSign(Portal2 portal, Player player) {
        boolean isOwner = channels.get(portal.getChannel()).getOwner().equals(player.getUniqueId());
        if (!portal.hasChangePermission(player, isOwner)) {
            player.sendMessage(ChatColor.RED + "You do not have the permission to change this sign destination.");
            return;
        }
        if (!sphs.containsKey(player)) {
            return;
        }
        SignPlayerHelper sph = sphs.get(player);
        if (!sph.isSameSign(portal.getSign().getLocation())) {
            return;
        }
        String currentName = sph.getCurrentName();
        portal.setDestination(currentName);
        throw new UnsupportedOperationException("not yet implemented");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreakMonitor(BlockBreakEvent event) {
        throw new UnsupportedOperationException("not yet implemented");
        /*event.getBlock().removeMetadata("isColorPortalBlock", plugin);
         event.getBlock().removeMetadata("colorPortal", plugin);*/
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreakLow(BlockBreakEvent event) {
        if (!ColorPortal.usePerms) {
            return;
        }
        if (!isPortalBlock(event.getBlock())) {
            return;
        }
        List<MetadataValue> colorPortal = event.getBlock().getMetadata("colorPortal");
        Portal2 portal = (Portal2) colorPortal.get(0).value();
        boolean isOwner = channels.get(portal.getChannel()).getOwner().equals(event.getPlayer().getUniqueId());
        if (!portal.hasDestroyPermission(event.getPlayer(), isOwner)) {
            event.getPlayer().sendMessage(ChatColor.RED + "You do not have the permission to destroy a color portal");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onExplosion(EntityExplodeEvent event) {
        for (Iterator<Block> it = event.blockList().iterator(); it.hasNext();) {
            Block block = it.next();
            if (isPortalBlock(block)) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onSignWrite(SignChangeEvent event) {
        if (event.getBlock().getType() != Material.WALL_SIGN) {
            return;
        }
        Sign sign = (Sign) event.getBlock();
        Block signBlock = event.getBlock().getRelative(((org.bukkit.material.Sign) event.getBlock().getState().getData()).getAttachedFace());
        if (!isPortalColorBlock(signBlock)) {
            return;
        }
        Block button = signBlock.getLocation().add(0.0D, -1.0D, 0.0D).getBlock();
        while (button.getType() == Material.AIR) {
            button = button.getLocation().add(0.0D, -1.0D, 0.0D).getBlock();
        }
        if (!isButton(button)) {
            return;
        }
        if (ColorPortal.usePerms && !event.getPlayer().hasPermission("colorportals.create")) {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not authorized to create portals");
            return;
        }
        Button b = (Button) button.getState().getData();
        Block buttonBlock = button.getRelative(b.getAttachedFace());

        DyeColor buttonColor = DyeColor.getByDyeData(buttonBlock.getData());
        DyeColor signColor = DyeColor.getByDyeData(signBlock.getData());
        if (!buttonColor.equals(signColor)) {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "the block where the sign is placed on needs to be the same");
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "color and Material as where the button is placed on");
            return;
        }
        if (!buttonBlock.getLocation().add(0.0D, -1.0D, 0.0D).getBlock().getType().equals(Material.AIR)) {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "Block below button must be air!");
            return;
        }
        String name = event.getLine(0);
        if (name.length() == 0) {
            event.getPlayer().sendMessage(ChatColor.RED + "The name of the portal cannot be left blank");
            return;
        }
        String chan = event.getLine(1);
        int channel;
        if (NumberUtils.isNumber(chan)) {
            channel = Integer.parseInt(chan);
        } else {
            event.getPlayer().sendMessage(ChatColor.RED + "The channel must be an integer in order to create a portal");
            return;
        }
        if ((channel < 0) || (channel > 99999)) {
            event.getPlayer().sendMessage(ChatColor.RED + "The channel must be between 0 and 100,000.");
            return;
        }
        UUID owner = event.getPlayer().getUniqueId();
        if (channels.containsKey(channel)) {
            if (!channels.get(channel).getOwner().equals(owner)) {
                event.getPlayer().sendMessage(ChatColor.RED + "This channel is in use. Use a diffrent number");
                return;
            }
        } else {
            channels.put(channel, new Channel(owner, channel));
        }
        Portal2 newPortal = new Portal2(name, button, sign,
                signBlock, buttonBlock, signBlock.getType(), channel, signColor.getDyeData());
        channels.get(channel).addPortal(newPortal);
    }

    private boolean isPortalBlock(Block block) {
        if (!block.hasMetadata("isColorPortalBlock")) {
            return false;
        }
        return block.getMetadata("isColorPortalBlock").get(0).asBoolean();
    }

    private boolean isPortalColorBlock(Block block) {
        return block.getType().equals(Material.WOOL)
                || block.getType().equals(Material.STAINED_CLAY);
    }

    private boolean isButton(Block block) {
        return block.getType().equals(Material.WOOD_BUTTON)
                || block.getType().equals(Material.STONE_BUTTON);
    }

    private boolean isWallSign(Block block) {
        return block.getType().equals(Material.WALL_SIGN);
    }
}

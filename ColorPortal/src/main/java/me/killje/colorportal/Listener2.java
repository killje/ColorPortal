package me.killje.colorportal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.HashMap;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.Button;

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
                Block sign = Query.queryToBlock(world, rs.getString("locationSign"));
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
                Portal2 portal = new Portal2(name, button, sign, signBlock, buttonBlock, material, color);
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
        if (event.getClickedBlock().getType().equals(Material.WALL_SIGN)) {
            Sign sign = (Sign) event.getClickedBlock();
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                if (rightClickSign(sign, event.getPlayer())) {
                    event.setCancelled(true);
                }
            } else if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                if (leftClickSign(sign, event.getPlayer())) {
                    event.setCancelled(true);
                }
            }
        } else if (isButton(event.getClickedBlock())) {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

        }
    }

    private boolean rightClickSign(Sign sign, Player player) {
        String channelLine = sign.getLine(1);
        if (!channelLine.matches("[0-9]{1,6}:([0-9]|[0-9][0-9])")) {
            return false;
        }
        String[] channelNode = channelLine.split(":");
        int channelId = Integer.parseInt(channelNode[0]);
        int node = Integer.parseInt(channelNode[1]);
        if (!channels.containsKey(channelId)) {
            return false;
        }
        Channel channel = channels.get(channelId);
        if (!channel.hasPortal(node)) {
            return false;
        }
        Portal2 portal = null;
        for (Portal2 portalByNode : channel.getPortals(node)) {
            if (portalByNode.getSign().getLocation().equals(sign.getLocation())) {
                portal = portalByNode;
                break;
            }
        }
        if (portal == null) {
            return false;
        }
        SignPlayerHelper sph;
        if (!sphs.containsKey(player)) {
            sph = new SignPlayerHelper(player, sign);
            sphs.put(player, sph);
        }else{
            sph = sphs.get(player);
            if (!sph.isSameSign(sign.getLocation())) {
                sph.newSign(sign);
            }
        }
        
        return false;
    }

    private boolean leftClickSign(Sign sign, Player player) {
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreakMonitor(BlockBreakEvent event) {

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreakLow(BlockBreakEvent event) {

    }

    @EventHandler
    public void onSignWrite(SignChangeEvent event) {
        if (event.getBlock().getType() != Material.WALL_SIGN) {
            return;
        }
        org.bukkit.material.Sign sign = (org.bukkit.material.Sign) event.getBlock().getState().getData();
        Block signBlock = event.getBlock().getRelative(sign.getAttachedFace());
        if (!isPortalBlock(signBlock)) {
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
        Portal2 newPortal = new Portal2(name, button, event.getBlock(),
                signBlock, buttonBlock, signBlock.getType(), signColor.getDyeData());
        channels.get(channel).addPortal(newPortal);
    }

    private boolean isPortalBlock(Block block) {
        return block.getType().equals(Material.WOOL)
                || block.getType().equals(Material.STAINED_CLAY);
    }

    private boolean isButton(Block block) {
        return block.getType().equals(Material.WOOD_BUTTON)
                || block.getType().equals(Material.STONE_BUTTON);
    }
}

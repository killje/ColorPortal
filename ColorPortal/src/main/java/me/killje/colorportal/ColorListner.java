package me.killje.colorportal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
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

/**
 *
 * @author Patrick Beuks (killje)
 */
public class ColorListner implements Listener {
    //listen to events

    private PortalList portals = new PortalList();
    private Connection connection;
    private final ColorPortal cportals;

    public ColorListner(ColorPortal cportals) {
        this.cportals = cportals;
    }

    @EventHandler
    public void onSignWrite(SignChangeEvent event) {
        if (event.getBlock().getType() != Material.WALL_SIGN) {
            return;
        }
        org.bukkit.material.Sign s = (org.bukkit.material.Sign) event.getBlock().getState().getData();
        Block attachedBlock = event.getBlock().getRelative(s.getAttachedFace());
        if (attachedBlock.getType() != Material.WOOL && attachedBlock.getType() != Material.STAINED_CLAY) {
            return;
        }
        Block blockBelow = attachedBlock.getLocation().add(0.0D, -1.0D, 0.0D).getBlock();
        while (blockBelow.getType() == Material.AIR) {
            blockBelow = blockBelow.getLocation().add(0.0D, -1.0D, 0.0D).getBlock();
        }
        if (blockBelow.getType() != Material.WOOD_BUTTON && blockBelow.getType() != Material.STONE_BUTTON) {
            return;
        }
        if (ColorPortal.usePerms && !event.getPlayer().hasPermission("colorportals.create")) {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "You are not authorized to create portals");
            event.setCancelled(true);
            return;
        }
        Button b = (Button) blockBelow.getState().getData();
        Block attachedButtonBlock = blockBelow.getRelative(b.getAttachedFace());
        DyeColor buttonColor = DyeColor.getByDyeData(attachedButtonBlock.getData());
        DyeColor signColor = DyeColor.getByDyeData(attachedBlock.getData());
        if (attachedBlock.getType() != attachedButtonBlock.getType() || buttonColor.getColor() != signColor.getColor()) {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "the block where the sign is placed on needs to be the same");
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "color and Material as where the button is placed on");
            event.setCancelled(true);
            return;
        }
        if (blockBelow.getLocation().add(0.0D, -1.0D, 0.0D).getBlock().getType() != Material.AIR) {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "Block below button must be air!");
            event.setCancelled(true);
            return;
        }
        String name = event.getLine(0);
        if (name.length() == 0) {
            event.getPlayer().sendMessage(ChatColor.RED + "The name of the portal cannot be left blank");
            event.setCancelled(true);
            return;
        }
        String chan = event.getLine(1);
        int channel = -1;
        if (isInteger(chan)) {
            channel = Integer.parseInt(chan);
        } else {
            event.getPlayer().sendMessage(ChatColor.RED + "The channel must be an integer in order to create a portal");
            event.setCancelled(true);
            return;
        }
        if ((channel < 0) || (channel > 99999)) {
            event.getPlayer().sendMessage(ChatColor.RED + "The channel must be between 0 and 100,000.");
            event.setCancelled(true);
            return;
        }
        for (Portal portal : portals) {
            switch (portal.isAvailable(channel, signColor, attachedBlock.getType())) {
                case Portal.CHANNEL_IS_IN_USE:
                    event.getPlayer().sendMessage(ChatColor.RED + "there is already a " + ChatColor.WHITE + "" + signColor.toString() + ChatColor.RED + "in use on this channel.");
                    event.getPlayer().sendMessage(ChatColor.RED + "please use a diffrent channel, color or material");
                    event.setCancelled(true);
                    return;
                case Portal.CHANNEL_NOT_IN_USE:
                    break;
                case Portal.CHANNEL_HAS_OPEN_PORTAL:
                    int node = firstAvialableNode(channel);
                    Portal newPortal = new Portal(name, channel, signColor,
                            attachedBlock.getType(), event.getBlock().getLocation(), attachedBlock.getLocation(),
                            blockBelow.getLocation(), attachedButtonBlock.getLocation(),
                            event.getPlayer().getUniqueId());
                    newPortal.setLink(portal, node);
                    portal.setLink(newPortal, node);
                    event.setLine(1, channel + ":" + node);
                    event.setLine(2, ChatColor.GREEN + "Warps To:");
                    event.setLine(3, portal.getName());

                    Sign linkPortalSign = (Sign) newPortal.getLink().getLocations()[0].getBlock().getState();

                    linkPortalSign.setLine(1, newPortal.getLink().getChannel() + ":" + node);
                    linkPortalSign.setLine(2, ChatColor.GREEN + "Warps To:");
                    linkPortalSign.setLine(3, newPortal.getName());
                    linkPortalSign.update(true);
                    event.getPlayer().sendMessage(ChatColor.GREEN + "Portal has been created on:" + channel + ":" + node);
                    addPortal(newPortal);
                    return;
            }
        }
        Portal newPortal = new Portal(name, channel, signColor,
                attachedBlock.getType(), event.getBlock().getLocation(), attachedBlock.getLocation(),
                blockBelow.getLocation(), attachedButtonBlock.getLocation(),
                event.getPlayer().getUniqueId());
        event.setLine(2, "");
        event.setLine(3, ChatColor.GRAY + "INACTIVE");
        addPortal(newPortal);
    }

    public boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private int firstAvialableNode(int channel) {
        ArrayList<Portal> channelPortals = new ArrayList<>();
        for (Portal portal : portals) {
            if (portal.getChannel() == channel) {
                channelPortals.add(portal);
            }
        }
        int node = findLowestNode(channelPortals);
        return node;
    }

    private int findLowestNode(ArrayList<Portal> channelPortals) {
        int node = 1;
        if (!channelPortals.isEmpty()) {
            boolean available = true;
            while (true) {
                for (Portal portal : channelPortals) {
                    if (portal.getNode() != 0 && portal.getNode() == node) {
                        available = false;
                        break;
                    }
                }
                if (!available) {
                    node++;
                    available = true;
                } else {
                    break;
                }
            }
        }
        return node;
    }

    private void addPortal(Portal newPortal) {
        portals.add(newPortal);
        try {
            connect();
            ResultSet rs;
            try (PreparedStatement preparedStatement = connection.prepareStatement(newPortal.addPortalQuery().getQuery(), Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.execute();
                rs = preparedStatement.getGeneratedKeys();
                if (rs.next()) {
                    newPortal.setId(rs.getInt(1));
                }
            }
        } catch (SQLException ex) {
            cportals.getLogger().log(Level.SEVERE, "Could not connect with database while adding portal", ex);
        }
        if (newPortal.getLink() != null) {
            Query query = new Query("colorPortal").update(
                    "link_id=" + newPortal.getId(),
                    "node=" + newPortal.getLink().getNode()).
                    where("id=" + newPortal.getLink().getId());
            try {
                sqlConection(query.getQuery());
            } catch (SQLException ex) {
                cportals.getLogger().log(Level.SEVERE, "Could not connect with database while adding portal", ex);
            }
        }
    }

    private void removePortal(Portal deletingPortal) {
        portals.remove(deletingPortal);
        try {
            Query query = new Query("colorPortal");
            if (deletingPortal.getLink() != null) {
                query.update("link_id = null", " node=0")
                        .where("id = " + deletingPortal.getLink().getId());
                sqlConection(query.getQuery());
            }
            query.delete().where("id=" + deletingPortal.getId());
            sqlConection(query.getQuery());
        } catch (SQLException ex) {
            cportals.getLogger().log(Level.SEVERE, "Could not connect with database while removing portal", ex);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreakCansel(BlockBreakEvent event) {
        if (!ColorPortal.usePerms) {
            return;
        }
        if (isPortalSign(event.getBlock()) || isPortalBlock(event.getBlock()) || isPortalButton(event.getBlock())) {
            if (!hasPermisionToDestroy(event.getBlock(), event.getPlayer())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean hasPermisionToDestroy(Block block, Player player) {
        for (Portal portal : portals) {
            for (int i = 0; i < 4; i++) {
                if (block.getLocation().equals(portal.getLocations()[i])) {
                    if (!player.hasPermission("colorportals.destroy")) {
                        player.sendMessage(ChatColor.DARK_RED + "You are not authorized to destroy portals");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreakMonitor(BlockBreakEvent event) {
        if (isPortalSign(event.getBlock())) {
            destroyPortal(event.getBlock().getLocation(), true, 0);
        } else if (isPortalBlock(event.getBlock())) {
            destroyPortal(event.getBlock().getLocation(), false, 1, 3);
        } else if (isPortalButton(event.getBlock())) {
            destroyPortal(event.getBlock().getLocation(), false, 2);
        }
    }

    private boolean isPortalSign(Block block) {
        if (block.getType() == Material.WALL_SIGN) {
            org.bukkit.material.Sign s = (org.bukkit.material.Sign) block.getState().getData();
            Block attachedBlock = block.getRelative(s.getAttachedFace());
            return attachedBlock.getType() == Material.WOOL || attachedBlock.getType() == Material.STAINED_CLAY;
        }
        return false;
    }

    private boolean isPortalBlock(Block block) {
        return block.getType() == Material.WOOL || block.getType() == Material.STAINED_CLAY;
    }

    private boolean isPortalButton(Block block) {
        if (block.getType() == Material.WOOD_BUTTON || block.getType() == Material.STONE_BUTTON) {
            org.bukkit.material.Button b = (org.bukkit.material.Button) block.getState().getData();
            Block attachedBlock = block.getRelative(b.getAttachedFace());
            return attachedBlock.getType() == Material.WOOL || attachedBlock.getType() == Material.STAINED_CLAY;
        }
        return false;
    }

    private void destroyPortal(Location location, boolean isSign, int... block) {
        for (Portal portal : portals) {
            for (int i : block) {
                if (location.equals(portal.getLocations()[i])) {
                    if (portal.getLink() != null) {
                        Sign linkPortalSign = (Sign) portal.getLink().getLocations()[0].getBlock().getState();
                        setSignDestroyed(linkPortalSign, portal.getLink().getChannel());
                        portal.getLink().setLink(null, 0);
                    }
                    if (!isSign) {
                        Sign PortalSign = (Sign) portal.getLocations()[0].getBlock().getState();
                        setSignDestroyed(PortalSign, portal.getChannel());
                    }
                    removePortal(portal);
                    return;
                }
            }
        }
    }

    private void setSignDestroyed(Sign sign, int channel) {
        sign.setLine(1, channel + "");
        sign.setLine(2, "");
        sign.setLine(3, ChatColor.RED + "DESTROYED");
        sign.update(true);
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        for (int i = 0; i < event.blockList().size(); i++) {
            if (event.blockList().get(i).getType() == Material.WALL_SIGN
                    || event.blockList().get(i).getType() == Material.STAINED_CLAY
                    || event.blockList().get(i).getType() == Material.WOOL
                    || event.blockList().get(i).getType() == Material.STONE_BUTTON
                    || event.blockList().get(i).getType() == Material.WOOD_BUTTON) {
                for (Portal portal : portals) {
                    if (event.blockList().get(i).getType() == Material.WALL_SIGN) {
                        if (event.blockList().get(i).getLocation().equals(portal.getLocations()[0])) {
                            event.blockList().remove(i);
                        }
                    } else if (event.blockList().get(i).getType() == Material.WOOL || event.blockList().get(i).getType() == Material.STAINED_CLAY) {
                        if (event.blockList().get(i).getLocation().equals(portal.getLocations()[1]) || event.blockList().get(i).getLocation().equals(portal.getLocations()[3])) {
                            event.blockList().remove(i);
                        }
                    } else if (event.blockList().get(i).getType() == Material.STONE_BUTTON || event.blockList().get(i).getType() == Material.WOOD_BUTTON) {
                        if (event.blockList().get(i).getLocation().equals(portal.getLocations()[2])) {
                            event.blockList().remove(i);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!isPortalButton(event.getClickedBlock())) {
            return;
        }
        Portal toPortal = null;
        for (Portal portal : portals) {
            if (event.getClickedBlock().getLocation().equals(portal.getLocations()[2])) {
                toPortal = portal.getLink();
                break;
            }
        }
        if (toPortal == null) {
            return;
        }
        if (toPortal.getLink() == null) {
            return;
        }
        String restriction = toPortal.getRestriction();
        if (!restriction.equals("") && !event.getPlayer().hasPermission("colorportal.use." + restriction)) {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "you dont have the permision to use this portal.");
            return;
        }
        if (ColorPortal.usePerms && !event.getPlayer().hasPermission("colorportals.use")) {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "You don't have premision to use portals.");
            return;
        }
        Location buttonLocation = toPortal.getLocations()[2].clone();
        if (buttonLocation.add(0.0D, -1.0D, 0.0D).getBlock().getType() != Material.AIR) {
            event.getPlayer().sendMessage(ChatColor.DARK_RED + "The linked portal is blocked!");
            return;
        }
        Location teleBlock = buttonLocation.add(0.0D, -1.0D, 0.0D);
        while (teleBlock.getBlock().getType() == Material.AIR) {
            teleBlock = teleBlock.add(0.0D, -1.0D, 0.0D);
        }
        Location adjustedLocation = teleBlock.add(0.5D, 1.0D, 0.5D);
        event.getPlayer().teleport(adjustedLocation);
    }

    private void connect() throws SQLTimeoutException, SQLException {
        if (connection != null) {
            try {
                connection.prepareStatement("SELECT 1;").execute();
            } catch (SQLException ex) {
                if ("08S01".equals(ex.getSQLState())) {
                    connection.close();
                } else {
                    throw ex;
                }
            }
        }
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:mysql://" + ColorPortal.host + "/" + ColorPortal.database, ColorPortal.username, ColorPortal.password);
        }
        try {
            connection.prepareStatement("SELECT 1;").execute();
        } catch (SQLException ex) {
            if (ex.getSQLState().equals("08S01")) {
                connection.close();
            } else {
                throw ex;
            }
        }
    }

    private ResultSet sqlConection(String querry) throws SQLTimeoutException, SQLException {
        connect();
        ResultSet rs;
        try (PreparedStatement preparedStatement = connection.prepareStatement(querry)) {
            preparedStatement.execute();
            rs = preparedStatement.getResultSet();
        }
        return rs;
    }

    public void loadPortals() {
        try {
            Query query = new Query("colorPortal").select();
            ResultSet rs = sqlConection(query.getQuery());
            PortalList loadedPortals = new PortalList();
            ArrayList<Entry<Integer, Integer>> notLinked = new ArrayList<>();
            
            while (!rs.isClosed() && rs.next()) {
                String name = rs.getString("name");
                String restriction = rs.getString("restriction");
                int channel = rs.getInt("channel");
                int node = rs.getInt("node");
                DyeColor color = DyeColor.getByColor(Color.fromRGB(rs.getInt("color")));
                int id = rs.getInt("id");
                Material material;
                if (rs.getInt("material") == 0) {
                    material = Material.WOOL;
                } else {
                    material = Material.STAINED_CLAY;
                }
                Location signLocation = getLocationFromQuery(rs.getString("locationSign"), rs.getString("world"));
                Location signBlockLocation = getLocationFromQuery(rs.getString("locationSignBlock"), rs.getString("world"));
                Location buttonLocation = getLocationFromQuery(rs.getString("locationButton"), rs.getString("world"));
                Location buttonBlockLocation = getLocationFromQuery(rs.getString("locationButtonBlock"), rs.getString("world"));
                UUID owner = UUID.fromString(rs.getString("owner"));
                int linkId = rs.getInt("link_id");
                Portal newPortal = new Portal(
                        name.replaceAll("#single quote!!#", "'"),
                        channel, color, material,
                        signLocation, signBlockLocation,
                        buttonLocation, buttonBlockLocation,
                        owner, restriction);
                newPortal.setId(id);
                if (!rs.wasNull()) {
                    boolean hasFoundLink = false;
                    for (Entry<Integer, Integer> pair : notLinked) {
                        if (pair.getValue() == id) {
                            hasFoundLink = true;
                            for (Portal portal : loadedPortals) {
                                if (portal.getId() == pair.getKey()) {
                                    newPortal.setLink(portal, node);
                                    portal.setLink(newPortal, node);
                                    break;
                                }
                            }
                        }
                    }
                    if (!hasFoundLink) {
                        Entry<Integer, Integer> pair = new SimpleEntry<>(id, linkId);
                        notLinked.add(pair);
                    }
                }
                loadedPortals.add(newPortal);
            }
            rs.close();
            portals = loadedPortals;
        } catch (SQLException ex) {
            cportals.getLogger().log(Level.SEVERE, "An error occuerd in the database when loading portals", ex);
        }
    }
    
    private Location getLocationFromQuery(String query, String world){
        StringTokenizer st = new StringTokenizer(query, ":");
        Location returnLocation = new Location(
                cportals.getServer().getWorld(world),
                Integer.parseInt(st.nextToken()),
                Integer.parseInt(st.nextToken()),
                Integer.parseInt(st.nextToken()));
        return returnLocation;
    }
}

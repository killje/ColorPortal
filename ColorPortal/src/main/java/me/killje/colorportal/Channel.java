package me.killje.colorportal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class Channel {

    private final UUID owner;
    private String restriction;
    private final int channel;
    private final ArrayList<Portal2> portals = new ArrayList<>();

    public Channel(UUID owner, int channel) {
        this(owner, channel, "");
    }

    public Channel(UUID owner, int channel, String restriction) {
        this.owner = owner;
        this.channel = channel;
        this.restriction = restriction;
    }

    public boolean addPortal(Portal2 portal) {
        if (portals.size() == 1) {
            Portal2 oldPortal = portals.get(0);
            try {
                setActiveSign(oldPortal.getSign(), oldPortal.getSignBlock());
            } catch (ClassCastException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Sign does not exsists anymore", ex);
                portals.remove(0);
            }
        }
        if (portals.isEmpty()) {
            try {
                setInactiveSign(portal.getSign());
            } catch (ClassCastException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Sign does not exsists anymore", ex);
            }
        } else {
            try {
                setActiveSign(portal.getSign(), portal.getSignBlock());
                return portals.add(portal);
            } catch (ClassCastException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Sign does not exsists anymore", ex);
            }
        }
        return false;
    }

    public boolean deletePortal(Portal2 portal) {
        boolean returnValue = portals.remove(portal);
        if (portals.size() == 1) {
            try {
                setInactiveSign(portals.get(0).getSign());
            } catch (ClassCastException ex) {
                Bukkit.getLogger().log(Level.SEVERE, "Sign does not exsists anymore", ex);
            }
        }
        return returnValue;
    }

    public UUID getOwner() {
        return owner;
    }

    private void setInactiveSign(Sign signBlock) throws ClassCastException {
        if (!signBlock.getType().equals(Material.WALL_SIGN)) {
            throw new ClassCastException();
        }
        Sign sign = signBlock;
        sign.setLine(2, "");
        sign.setLine(3, ChatColor.GRAY + "INACTIVE");
    }

    private void setActiveSign(Sign signBlock, Block signAttachedBlock) throws ClassCastException {
        if (!signBlock.getType().equals(Material.WALL_SIGN)) {
            throw new ClassCastException();
        }
        Sign sign = signBlock;
        sign.setLine(1, channel + ":" + signAttachedBlock.getData());
        sign.setLine(2, ChatColor.GREEN + "Warps To:");
        sign.setLine(3, portals.get(0).getName());
    }

    public Collection<Portal2> getPortals(int node) {
        ArrayList<Portal2> portalList = new ArrayList<>();
        for (Portal2 portal : portalList) {
            if (portal.getNode() == node) {
                portalList.add(portal);
            }
        }
        return portalList;
    }
}

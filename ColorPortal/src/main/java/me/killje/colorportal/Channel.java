package me.killje.colorportal;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

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

    private void setInactiveSign(Block signBlock) throws ClassCastException {
        if (!signBlock.getType().equals(Material.WALL_SIGN)) {
            throw new ClassCastException();
        }
        Sign sign = (Sign) signBlock;
        sign.setLine(2, "");
        sign.setLine(3, ChatColor.GRAY + "INACTIVE");
    }

    private void setActiveSign(Block signBlock, Block signAttachedBlock) throws ClassCastException {
        if (!signBlock.getType().equals(Material.WALL_SIGN)) {
            throw new ClassCastException();
        }
        Sign sign = (Sign) signBlock;
        sign.setLine(1, channel + ":" + signAttachedBlock.getData());
        sign.setLine(2, ChatColor.GREEN + "Warps To:");
        sign.setLine(3, portals.get(0).getName());
    }
    
    public boolean containsBlock(Block block){
        for (Portal2 portal2 : portals) {
            if (portal2.containsBlock(block)) {
                return true;
            }
        }
        return false;
    }

    boolean hasPermission(Block block, Player player) {
        boolean returnValue = true;
        for (Portal2 portal2 : portals) {
            if(!portal2.hasPermission(block, player, player.getUniqueId().equals(owner))){
                return false;
            }
        }
        return true;
    }
}

package me.killje.colorportal;

import java.util.ArrayList;
import java.util.UUID;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class Portal {

    private String name = "";
    private int channel = -1;
    private int node = 0;
    private final DyeColor color;
    private final String restricion;
    private final Material material;
    private Portal linkedPortal = null;
    private final Location signLocation;
    private final Location signBlockLocation;
    private final Location buttonLocation;
    private final Location buttonBlockLocation;
    private UUID owner;
    private int id;
    
    public static final int CHANNEL_NOT_IN_USE = 0;
    public static final int CHANNEL_IS_IN_USE = -1;
    public static final int CHANNEL_HAS_OPEN_PORTAL = 1;

    public Portal(String name, int channel, DyeColor color, Material material, Location signLocation, Location signBlockLocation, Location buttonLocation, Location buttonBlockLocation, UUID owner, String restriction) {
        this.name = name;
        this.channel = channel;
        this.color = color;
        this.material = material;
        this.signLocation = signLocation;
        this.signBlockLocation = signBlockLocation;
        this.buttonLocation = buttonLocation;
        this.buttonBlockLocation = buttonBlockLocation;
        this.owner = owner;
        this.restricion = restriction;
    }
    
    public Portal(String name, int channel, DyeColor color, Material material, Location signLocation, Location signBlockLocation, Location buttonLocation, Location buttonBlockLocation, UUID owner) {
        this(name,channel,color,material,signLocation,signBlockLocation,buttonLocation,buttonBlockLocation,owner,"");
    }

    public void setLink(Portal portal, int node) {
        linkedPortal = portal;
        this.node = node;
    }

    public Portal getLink() {
        return linkedPortal;
    }

    public DyeColor getColor() {
        return color;
    }
    
    public String getRestriction() {
        return restricion;
    }

    public Material getMaterial() {
        return material;
    }

    public String getName() {
        return name;
    }

    public int getChannel() {
        return channel;
    }

    public int getNode() {
        return node;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public int isAvailable(int channel, DyeColor color, Material material) {
        if (this.channel == channel && this.material == material && this.color == color) {
            if (linkedPortal == null) {
                return CHANNEL_HAS_OPEN_PORTAL;
            } else {
                return CHANNEL_IS_IN_USE;
            }
        }
        return CHANNEL_NOT_IN_USE;
    }

    public Location[] getLocations() {
        Location[] locations = new Location[4];
        locations[0] = signLocation;
        locations[1] = signBlockLocation;
        locations[2] = buttonLocation;
        locations[3] = buttonBlockLocation;
        return locations;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public Query addPortalQuery(){
        Query query;
        ArrayList<String> values = new ArrayList<>();
        values.add(getName().replaceAll("'", "#single quote!!#"));
        values.add(getChannel() + "");
        values.add(getNode() + "");
        values.add(getColor().getColor().asRGB() + "");
        if (getLink() != null) {
            values.add(getLink().getId() + "");
        }
        if (getMaterial() == Material.WOOL) {
            values.add("0");
        } else {
            values.add("1");
        }
        values.add(getLocations()[0].getWorld().getName());
        values.add(getLocations()[0].getBlockX() + ":" + getLocations()[0].getBlockY() + ":" + getLocations()[0].getBlockZ());
        values.add(getLocations()[1].getBlockX() + ":" + getLocations()[1].getBlockY() + ":" + getLocations()[1].getBlockZ());
        values.add(getLocations()[2].getBlockX() + ":" + getLocations()[2].getBlockY() + ":" + getLocations()[2].getBlockZ());
        values.add(getLocations()[3].getBlockX() + ":" + getLocations()[3].getBlockY() + ":" + getLocations()[3].getBlockZ());
        values.add(getOwner().toString());
        if (getLink() == null) {
            query = new Query("colorPortal").insert(
                    new String[]{"name", "channel", "node", "color", "material",
                        "world", "locationSign", "locationSignBlock",
                        "locationButton", "locationButtonBlock", "owner"},
                    values.toArray());
        } else {
            query = new Query("colorPortal").insert(
                    new String[]{"name", "channel", "node", "color", "link_id", "material",
                        "world", "locationSign", "locationSignBlock",
                        "locationButton", "locationButtonBlock", "owner"},
                    values.toArray());
        }
        return query;
    }
}

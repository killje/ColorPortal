package me.killje.colorportal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValueAdapter;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class Portal2 {

    private final String name;
    private final Block button;
    private final Sign sign;
    private final Block signBlock;
    private final Block buttonBlock;
    private final Material material;
    private final int channel;
    private final int node;

    public Portal2(String name, Block button, Sign sign, Block signBlock, Block buttonBlock, Material material, int channel, int node) {
        this.name = name;
        MetadataValueAdapter mvaIsColorBlock = new MetadataValueAdapter(Bukkit.getPluginManager().getPlugin("ColorPortal2")) {
            private boolean isColorBlock = true;

            @Override
            public Object value() {
                return isColorBlock;
            }

            @Override
            public void invalidate() {
                isColorBlock = false;
            }
        };
        MetadataValueAdapter mvaPortal = new MetadataValueAdapter(Bukkit.getPluginManager().getPlugin("ColorPortal2")) {
            private Portal2 portal = Portal2.this;

            @Override
            public Object value() {
                return portal;
            }

            @Override
            public void invalidate() {
                portal = null;
            }
        };
        button.setMetadata("isColorPortalBlock", mvaIsColorBlock);
        sign.setMetadata("isColorPortalBlock", mvaIsColorBlock);
        signBlock.setMetadata("isColorPortalBlock", mvaIsColorBlock);
        buttonBlock.setMetadata("isColorPortalBlock", mvaIsColorBlock);
        button.setMetadata("colorPortal", mvaPortal);
        sign.setMetadata("colorPortal", mvaPortal);
        signBlock.setMetadata("colorPortal", mvaPortal);
        buttonBlock.setMetadata("colorPortal", mvaPortal);
        this.button = button;
        this.sign = sign;
        this.signBlock = signBlock;
        this.buttonBlock = buttonBlock;
        this.material = material;
        this.channel = channel;
        this.node = node;
    }

    public Sign getSign() {
        return sign;
    }

    public String getName() {
        return name;
    }

    public Block getSignBlock() {
        return signBlock;
    }

    public int getNode() {
        return node;
    }

    public boolean hasDestroyPermission(Player player, boolean owner) {
        return owner || player.hasPermission("colorportal.destroy.other");
    }
    
    public boolean hasChangePermission(Player player, boolean owner) {
        return owner || player.hasPermission("colorportal.change.other");
    }

    public int getChannel() {
        return channel;
    }
    
    public void setDestination(String name){
        sign.setLine(3, name);
    }

    public Block getButton() {
        return button;
    }
}

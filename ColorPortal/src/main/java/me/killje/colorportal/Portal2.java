package me.killje.colorportal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.metadata.LazyMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.MetadataValueAdapter;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class Portal2 {

    private final String name;
    private final Block button;
    private final Block sign;
    private final Block signBlock;
    private final Block buttonBlock;
    private final Material material;
    private final int node;

    public Portal2(String name, Block button, Block sign, Block signBlock, Block buttonBlock, Material material, int node) {
        this.name = name;
        MetadataValueAdapter mva = new MetadataValueAdapter(Bukkit.getPluginManager().getPlugin("ColorPortal2")) {
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
        button.setMetadata("isColorPortalBlock", mva);
        sign.setMetadata("isColorPortalBlock", mva);
        signBlock.setMetadata("isColorPortalBlock", mva);
        buttonBlock.setMetadata("isColorPortalBlock", mva);
        this.button = button;
        this.sign = sign;
        this.signBlock = signBlock;
        this.buttonBlock = buttonBlock;
        this.material = material;
        this.node = node;
    }

    public Block getSign() {
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

    boolean containsBlock(Block block) {
        return button.equals(block) || sign.equals(block) || signBlock.equals(block) || buttonBlock.equals(block);
    }

    public boolean hasPermission(Block block, Player player, boolean owner) {
        if (!containsBlock(block)) {
            return true;
        } else {
            if (owner || player.hasPermission("colorportal.destroy.other")) {
                return true;
            }
        }
        return false;
    }

}

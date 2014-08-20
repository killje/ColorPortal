package me.killje.colorportal;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

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

    public Portal2(String name, Block button, Block sign, Block signBlock, Block buttonBlock, Material material) {
        this.name = name;
        this.button = button;
        this.sign = sign;
        this.signBlock = signBlock;
        this.buttonBlock = buttonBlock;
        this.material = material;
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

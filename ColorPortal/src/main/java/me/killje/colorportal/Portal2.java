package me.killje.colorportal;

import org.bukkit.Material;
import org.bukkit.block.Block;

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
    
    public int getNode(){
        return node;
    }
}

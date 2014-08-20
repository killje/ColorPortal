package me.killje.colorportal;

import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class SignPlayerHelper {
    
    private final Player player;
    private Sign sign;
    private String[] oldMessage;

    public SignPlayerHelper(Player player, Sign sign) {
        this.player = player;
        this.sign = sign;
        oldMessage = sign.getLines();
    }
    
    public boolean isSameSign(Location location){
        return sign.getLocation().equals(location);
    }
    
    public void newSign(Sign sign){
        player.sendSignChange(this.sign.getLocation(), oldMessage);
        this.sign = sign;
        oldMessage = sign.getLines();
    }
}

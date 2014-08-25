package me.killje.colorportal;

import java.util.ArrayList;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class SignPlayerHelper {

    private final Player player;
    private Portal2 portal;
    private String[] oldMessage;
    private int currentDestination;
    private Channel channel;
    private final ArrayList<String> destinations = new ArrayList<>();

    public SignPlayerHelper(Player player, Portal2 portal, Channel channel) {
        this.player = player;
        this.portal = portal;
        oldMessage = portal.getSign().getLines();
        this.channel = channel;
        for (Portal2 portalIt : channel.getPortals(portal.getNode())) {
            if (!portal.equals(portalIt)) {
                destinations.add(portalIt.getName());
            }
        }
        if (channel.getPortals(portal.getNode()).size() > 1) {
            String destinationOnSign = portal.getSign().getLine(3);
            currentDestination = destinations.indexOf(destinationOnSign);
            if (currentDestination == -1) {
                currentDestination = 0;
            }
        }
    }

    public boolean isSameSign(Sign sign) {
        return portal.getSign().equals(sign);
    }

    public void newSign(Channel channel, Portal2 portal) {
        oldMessage[3] = destinations.get(currentDestination);
        player.sendSignChange(portal.getSign().getLocation(), oldMessage);
        this.channel = channel;
        this.portal = portal;
        oldMessage = portal.getSign().getLines();
        destinations.clear();
        for (Portal2 portalIt : channel.getPortals(portal.getNode())) {
            if (!portal.equals(portalIt)) {
                destinations.add(portalIt.getName());
            }
        }
        if (channel.getPortals(portal.getNode()).size() > 1) {
            String destinationOnSign = portal.getSign().getLine(3);
            currentDestination = destinations.indexOf(destinationOnSign);
            if (currentDestination == -1) {
                currentDestination = 0;
            }
        }
    }

    public String getCurrentDestination() {
        return destinations.get(currentDestination);
    }

    public void next() {
        currentDestination++;
        if (destinations.size() == currentDestination) {
            currentDestination = 0;
        }
        player.sendSignChange(portal.getSign().getLocation(), getDestinations());
    }

    public void init() {
        player.sendSignChange(portal.getSign().getLocation(), getDestinations());
    }

    public void setCurrentName() {
        portal.getSign().setLine(3, destinations.get(currentDestination));
        portal.getSign().update();
        oldMessage[3] = destinations.get(currentDestination);
        player.sendSignChange(portal.getSign().getLocation(), oldMessage);
    }

    private String destinationString(String destination) {
        String returnString = "[" + destination + "]";
        if (destination.length() > 13) {
            returnString = "[" + destination.substring(0, 13) + "]";
        }
        return returnString;
    }

    private boolean hasNext() {
        return destinations.size() - 1 == currentDestination;
    }

    private String[] getDestinations() {
        String[] returnArray = new String[4];
        if (destinations.size() <= 4) {
            return select(destinations.toArray(returnArray), currentDestination);
        }
        int index, selected;
        if (currentDestination - 2 < 0) {
            index = 0;
            selected = currentDestination;
        } else {
            selected = 2;
            index = currentDestination - 2;
        }
        if (!hasNext()) {
            index--;
            selected = 3;
        }
        returnArray[0] = destinations.get(index++);
        returnArray[1] = destinations.get(index++);
        returnArray[2] = destinations.get(index++);
        returnArray[3] = destinations.get(index);
        return select(returnArray, selected);
    }

    private String[] select(String[] items, int index) {
        items[index] = destinationString(items[index]);
        return items;
    }
}

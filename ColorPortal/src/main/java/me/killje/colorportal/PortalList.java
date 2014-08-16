package me.killje.colorportal;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class PortalList extends AbstractList<Portal>{

    private final Map<Integer, Portal> portals = new HashMap<>();

    @Override
    public void add(int id, Portal element) {
        portals.put(id, element);
    }

    @Override
    public boolean add(Portal e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Portal remove(int id) {
        return portals.remove(id);
    }
    
    
    
    @Override
    public Portal get(int index) {
        return portals.get(index);
    }

    @Override
    public int size() {
        return portals.size();
    }
}

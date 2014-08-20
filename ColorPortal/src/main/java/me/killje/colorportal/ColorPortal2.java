package me.killje.colorportal;

import com.evilmidget38.UUIDFetcher;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import me.killje.colorportal.Query.QuaryCreateTable;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class ColorPortal2 extends JavaPlugin {

    public static String host;
    public static String database;
    public static String username;
    public static String password;

    @Override
    public void onEnable() {
        host = getConfig().getString("host");
        database = getConfig().getString("database");
        username = getConfig().getString("username");
        password = getConfig().getString("password");
        saveDefaultConfig();
        Query channelQuery = new Query(getConfig().getString("channelTabel")).
                createTabelIfNotExists(new QuaryCreateTable().
                        addColum("channel", "INT(6)", null, false, false, true, false, false).
                        addColum("owner", "char(36)", "00000000-0000-0000-0000-000000000000", true, false, true).
                        addColum("restriction", "varchar(255)")
                );
        Query portalQuery = new Query(getConfig().getString("portalTabel")).
                createTabelIfNotExists(new QuaryCreateTable().
                        addColum("id", "INT(10)", null, false, false, true, true, true).
                        addColum("channelID", "INT(6)", null, false, false, false).
                        addColum("color", "INT(2)").
                        addColum("material", "INT(1)").
                        addColum("world", "char(36)").
                        addColum("locationSign", "varchar(30)").
                        addColum("locationSignBlock", "varchar(30)").
                        addColum("locationButton", "varchar(30)").
                        addColum("locationButtonBlock", "varchar(30)")
                );
        try {
            channelQuery.sqlConnect();
            portalQuery.sqlConnect();
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "Error while connection to dataBase", ex);
            if (getConfig().getBoolean("shutdownOnError")) {
                getServer().shutdown();
            }
        }
        if (new Query("colorPortal").exsists()) {
            update();
        }
    }

    private void update() {
        try {
            Query query = new Query("colorPortal").select();
            ResultSet rs;
            ArrayList<Map<String, String>> oldPortals = new ArrayList<>();
            query.connect();
            Bukkit.getLogger().info(query.getQuery());
            try (PreparedStatement preparedStatement = query.getConnection().prepareStatement(query.getQuery())) {
                preparedStatement.execute();
                rs = preparedStatement.getResultSet();
                while (rs.next()) {
                    getLogger().info("adding portal");
                    Map<String, String> portal = new HashMap<>();
                    portal.put("name", rs.getString("name"));
                    portal.put("channel", rs.getString("channel"));
                    portal.put("color", rs.getString("color"));
                    portal.put("material", rs.getString("material"));
                    String worldName = rs.getString("world");
                    World world = getServer().getWorld(worldName);
                    portal.put("world", world.getUID().toString());
                    portal.put("locationSign", rs.getString("locationSign"));
                    portal.put("locationSignBlock", rs.getString("locationSignBlock"));
                    portal.put("locationButton", rs.getString("locationButton"));
                    portal.put("locationButtonBlock", rs.getString("locationButtonBlock"));
                    portal.put("owner", rs.getString("owner"));
                    if (rs.getString("restriction").equals("")) {
                        portal.put("restriction", null);
                    } else {
                        portal.put("restriction", rs.getString("restriction"));
                    }
                    oldPortals.add(portal);
                }
            }
            UUIDFetcher fetcher = new UUIDFetcher(getAllNames(oldPortals));
            Map<String, UUID> namesToUUID = fetcher.call();
            Map<Integer, Map<String, Object>> channels = new HashMap<>();
            Query portalQuery = new Query(getConfig().getString("portalTabel"));
            Statement statement = portalQuery.getConnection().createStatement();
            int asdfasdfasfdasfafsddfasdfasdfasdfasdfasdfasdfasfadsdfasdfasfdasdfaadfsdfasdfasdfasdfasdfsdfasdfasdfasdfasdfas = 0;
            for (Map<String, String> portal : oldPortals) {
                int channelNo = Integer.parseInt(portal.get("channel"));
                String owner = portal.get("owner");
                String restriction = portal.get("restriction");
                if (!channels.containsKey(channelNo)) {
                    Map<String, Object> newChannel = new HashMap<>();
                    newChannel.put("owner", namesToUUID.get(owner).toString());
                    newChannel.put("channel", channelNo);
                    newChannel.put("restriction", restriction);
                    channels.put(channelNo, newChannel);
                }
                String[] keys = new String[8];
                Object[] values = new Object[8];
                keys[0] = "channelID";
                values[0] = channelNo;
                keys[1] = "color";
                values[1] = Integer.parseInt(DyeColor.getByColor(Color.fromRGB(Integer.parseInt(portal.get("color")))).getDyeData() + "");
                keys[2] = "material";
                values[2] = Integer.parseInt(portal.get("material"));
                keys[3] = "world";
                values[3] = portal.get("world");
                keys[4] = "locationSign";
                values[4] = portal.get("locationSign");
                keys[5] = "locationSignBlock";
                values[5] = portal.get("locationSignBlock");
                keys[6] = "locationButton";
                values[6] = portal.get("locationButton");
                keys[7] = "locationButtonBlock";
                values[7] = portal.get("locationButtonBlock");
                StringTokenizer st = new StringTokenizer((String) values[4], ":");
                if (signUpdater(new Location(
                        getServer().getWorld((String) values[3]),
                        Integer.parseInt(st.nextToken()),
                        Integer.parseInt(st.nextToken()),
                        Integer.parseInt(st.nextToken())), channelNo + ":" + (Integer) values[1])) {
                    if (!channels.containsKey(channelNo)) {
                        Map<String, Object> newChannel = new HashMap<>();
                        newChannel.put("owner", namesToUUID.get(owner).toString());
                        newChannel.put("channel", channelNo);
                        newChannel.put("restriction", restriction);
                        channels.put(channelNo, newChannel);
                    }
                    statement.addBatch(portalQuery.insert(keys, values).getQuery());
                }
                asdfasdfasfdasfafsddfasdfasdfasdfasdfasdfasdfasfadsdfasdfasfdasdfaadfsdfasdfasdfasdfasdfsdfasdfasdfasdfasdfas++;
                if (asdfasdfasfdasfafsddfasdfasdfasdfasdfasdfasdfasfadsdfasdfasfdasdfaadfsdfasdfasdfasdfasdfsdfasdfasdfasdfasdfas > 60) {
                    break;
                }
            }
            statement.executeBatch();
            Query channelQuery = new Query(getConfig().getString("channelTabel"));
            statement = channelQuery.getConnection().createStatement();
            for (Map<String, Object> map : channels.values()) {
                Set<Entry<String, Object>> set = map.entrySet();
                String[] keys = new String[3];
                Object[] values = new Object[3];
                int i = 0;
                for (Entry<String, Object> entry : set) {
                    keys[i] = entry.getKey();
                    values[i] = entry.getValue();
                    i++;
                }
                getLogger().info(i + "");
                channelQuery.insert(keys, values);
                statement.addBatch(channelQuery.getQuery());
            }
            statement.executeBatch();
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "Error while connection to dataBase", ex);
            if (getConfig().getBoolean("shutdownOnError")) {
                getServer().shutdown();
            }
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Could not parse fetcher result", ex);
            if (getConfig().getBoolean("shutdownOnError")) {
                getServer().shutdown();
            }
        }
    }

    private ArrayList<String> getAllNames(ArrayList<Map<String, String>> totalList) {
        Set<String> returnList = new HashSet<>();
        for (Map<String, String> map : totalList) {
            getLogger().info(map.get("owner"));
        }
        return new ArrayList<>(returnList);
    }

    private boolean signUpdater(Location location, String channel) {
        if (location == null || location.getBlock() == null || !location.getBlock().getType().equals(Material.WALL_SIGN)) {
            return false;
        }
        Sign sign = (Sign) location.getBlock();
        sign.setLine(1, channel);
        return true;
    }
}

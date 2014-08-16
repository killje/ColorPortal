package me.killje.colorportal;

import com.evilmidget38.UUIDFetcher;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class ColorPortal extends JavaPlugin {

    public static boolean usePerms = false;
    protected File portalFile = null;
    public final ColorListner alisten = new ColorListner(this);
    public static String host;
    public static String database;
    public static String username;
    public static String password;
    private FileConfiguration config;
    private Connection connection;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this.alisten, this);
        config = getConfig();
        saveConfig();

        host = config.getString("host");
        database = config.getString("databasename");
        username = config.getString("username");
        password = config.getString("password");

        Query query = new Query("colorPortal").
                createTabelIfNotExists("id INT(10)", "name varchar(15)",
                        "channel INT(6)", "node INT(2)", "color INT(10)",
                        "link_id INT(10)", "material INT(1)",
                        "world varchar(30)", "locationSign varchar(17)",
                        "locationSignBlock varchar(17)",
                        "locationButton varchar(17)",
                        "locationButtonBlock varchar(17)",
                        "owner char(36) default '00000000-0000-0000-0000-000000000000'",
                        "PRIMARY KEY (id)");
        try {
            sqlConection(query.getQuery());
            getLogger().info("Connecting to database success.");
        } catch (SQLTimeoutException e) {
            getLogger().log(Level.SEVERE, "Database did not respond.", e);
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Connect to database failed.", e);
        }
        alisten.loadPortals();
    }

    @Override
    public void onDisable() {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException ex) {
                getLogger().log(Level.SEVERE, "disconnecting from database failed", ex);
            }
        }
    }

    private void connect() throws SQLTimeoutException, SQLException {
        if (connection != null) {
            try {
                connection.prepareStatement("SELECT 1;").execute();
            } catch (SQLException ex) {
                if (ex.getSQLState().equals("08S01")) {
                    connection.close();
                } else {
                    throw ex;
                }
            }
        }
        if ((connection == null) || (connection.isClosed())) {
            connection = DriverManager.getConnection("jdbc:mysql://" + host + "/" + database, username, password);
        }
        try {
            connection.prepareStatement("SELECT 1;").execute();
        } catch (SQLException ex) {
            if (ex.getSQLState().equals("08S01")) {
                connection.close();
            } else {
                throw ex;
            }
        }
    }

    public ResultSet sqlConection(String querry) throws SQLTimeoutException, SQLException {
        connect();
        ResultSet rs;
        try (PreparedStatement preparedStatement = connection.prepareStatement(querry)) {
            preparedStatement.execute();
            rs = preparedStatement.getResultSet();
        }
        return rs;
    }

    private void updater() {
        Query query = new Query("colorPortal").showFields().where("Field = 'owner'");
        try {
            ResultSet result = sqlConection(query.getQuery());
            if (result.isClosed()) {
                return;
            }
            if (!result.next()) {
                return;
            }
            String type = result.getString("Type");
            if (type.equals("char(36)")) {
                return;
            }
            query = new Query("colorPortal").select("id", "owner");
            result = sqlConection(query.getQuery());
            Map<Integer, String> map = new HashMap<>();
            while (!result.isClosed() && result.next()) {
                map.put(result.getInt("id"), result.getString("owner"));
            }
            query = new Query("colorPortal").dropColum("owner");
            sqlConection(query.getQuery());
            query = new Query("colorPortal").addColum("owner", "char(36) DEFAULT '00000000-0000-0000-0000-000000000000'");
            sqlConection(query.getQuery());
            Set<String> test = new HashSet<>(map.values());
            UUIDFetcher fetcher = new UUIDFetcher(new ArrayList<>(test));
            Map<String, UUID> uuids = fetcher.call();
            for (Map.Entry<Integer, String> entry : map.entrySet()) {
                Integer id = entry.getKey();
                UUID uuid = uuids.get(entry.getValue());
                query.update("owner=" + uuid.toString()).where("id=" + id);
                sqlConection(query.getQuery());
            }
        } catch (SQLException ex) {
            getLogger().log(Level.SEVERE, "SQL exception", ex);
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Could not transform UUID", ex);
        }
    }
}

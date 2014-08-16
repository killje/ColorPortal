package me.killje.colorportal;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.logging.Level;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

/**
 *
 * @author Patrick Beuks (killje)
 */
public class Query {

    private String sql = "";
    private final String table;
    private Connection connection;

    public Query(String table) {
        this.table = table;
    }

    public Query select() {
        return select("*");
    }

    public Query select(String... tabels) {
        sql = "SELECT ";
        sql += StringUtils.join(tabels, ", ") + " FROM " + table;
        return this;
    }

    public Query insert(Object[] values) {
        sql = "INSERT INTO " + table + " VALUES (";
        for (Object object : values) {
            if (object instanceof Integer) {
                sql += (Integer) object + ", ";
            } else if (object instanceof String) {
                sql += "'" + (String) object + "', ";
            } else {
                throw new ClassCastException("Only integer values or text values allowed");
            }
        }
        if (values.length == 0) {
            sql += ") ";
        } else {
            sql = sql.substring(0, sql.lastIndexOf(", ")) + ") ";
        }
        return this;
    }

    public Query insert(String[] colums, Object[] values) {
        sql = "INSERT INTO " + table + " (" + StringUtils.join(colums, ", ")
                + ") VALUES (";
        for (Object object : values) {
            if (object instanceof Integer) {
                sql += (Integer) object + ", ";
            } else if (object instanceof String) {
                sql += "'" + (String) object + "', ";
            } else if (object ==null) {
                sql += "NULL, ";
            } else {
                throw new ClassCastException("Only integer values or text values allowed");
            }
        }
        if (values.length == 0) {
            sql += ") ";
        } else {
            sql = sql.substring(0, sql.lastIndexOf(", ")) + ") ";
        }
        return this;
    }

    public Query update(String... updateSets) {
        sql = "UPDATE " + table + " SET " + StringUtils.join(updateSets, ", ") + " ";
        return this;
    }

    public Query delete() {
        sql = "DELETE FROM " + table + " ";
        return this;
    }

    public Query deleteAll() {
        sql = "DELETE  * FROM " + table + " ";
        return this;
    }

    public Query selectDistinct() {
        return selectDistinct("*");
    }

    public Query selectDistinct(String... tabels) {
        sql = "SELECT DISTINCT ";
        sql = StringUtils.join(tabels, ", ") + " FROM " + table + " ";
        return this;
    }

    public Query where(String whereClause) {
        sql += whereClause + " ";
        return this;
    }

    public Query orderBy(boolean desc, String... colums) {
        sql += StringUtils.join(colums, ", ") + " ";
        if (desc) {
            sql += "DESC ";
        }
        return this;
    }

    public Query createTabel(String... columsAndType) {
        sql = "CREATE TABLE " + table + " (" + StringUtils.join(columsAndType, ", ") + ") ";
        return this;
    }

    public Query createTabelIfNotExists(String... columsAndType) {
        sql = "CREATE TABLE IF NOT EXISTS " + table + " (" + StringUtils.join(columsAndType, ", ") + ") ";
        return this;
    }

    public Query createTabelIfNotExists(QuaryCreateTable columsAndType) {
        sql = "CREATE TABLE IF NOT EXISTS " + table + " (" + columsAndType.getQuery() + ") ";
        return this;
    }

    public Query dropColum(String column) {
        sql = "ALTER TABLE " + table + " DROP COLUMN " + column + " ";
        return this;
    }

    public Query addColum(String column, String type) {
        sql = "ALTER TABLE " + table + " ADD " + column + " " + type + " ";
        return this;
    }

    public boolean exsists() {
        sql = "SHOW TABLES LIKE '" + table + "'";
        try {
            connection = DriverManager.getConnection("jdbc:mysql://" + ColorPortal2.host + "/" + ColorPortal2.database, ColorPortal2.username, ColorPortal2.password);
            DatabaseMetaData metadata = connection.getMetaData();
            ResultSet resultSet;
            resultSet = metadata.getTables(null, null, "colorPortal", null);
            return resultSet != null;
        } catch (SQLException ex) {
            Bukkit.getLogger().log(Level.SEVERE, null, ex);
            return false;
        }

    }

    public String getQuery() {
        return sql + ";";
    }

    public Query showFields() {
        sql = "SHOW FIELDS FROM " + table + " ";
        return this;
    }

    public static Block queryToBlock(UUID world, String query) {
        StringTokenizer st = new StringTokenizer(query, ":");
        Location location = new Location(Bukkit.getWorld(world),
                Integer.parseInt(st.nextToken()),
                Integer.parseInt(st.nextToken()),
                Integer.parseInt(st.nextToken()));
        return location.getBlock();
    }

    public ResultSet sqlConnect() throws SQLTimeoutException, SQLException {
        connect();
        ResultSet rs;
        Bukkit.getLogger().info(getQuery());
        try (PreparedStatement preparedStatement = connection.prepareStatement(getQuery())) {
            preparedStatement.execute();
            rs = preparedStatement.getResultSet();
        }
        return rs;
    }

    public ResultSet sqlConnectWithKeys() throws SQLTimeoutException, SQLException {
        connect();
        ResultSet rs;
        Bukkit.getLogger().info(getQuery());
        try (PreparedStatement preparedStatement = connection.prepareStatement(getQuery(), Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.execute();
            rs = preparedStatement.getGeneratedKeys();
            return rs;
        }
    }

    public void connect() throws SQLTimeoutException, SQLException {
        if (connection != null) {
            try {
                connection.prepareStatement("SELECT 1;").execute();
            } catch (SQLException ex) {
                if ("08S01".equals(ex.getSQLState())) {
                    Bukkit.getLogger().info("error");
                    connection.close();
                } else {
                    throw ex;
                }
            }
        }
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection("jdbc:mysql://" + ColorPortal2.host + "/" + ColorPortal2.database, ColorPortal2.username, ColorPortal2.password);
        }
        try {
            connection.prepareStatement("SELECT 1;").execute();
        } catch (SQLException ex) {
            if (ex.getSQLState().equals("08S01")) {
                Bukkit.getLogger().info("error");
                connection.close();
            } else {
                throw ex;
            }
        }
    }

    public Connection getConnection() throws SQLException {
        connect();
        return connection;
    }

    public static class QuaryCreateTable {

        private final ArrayList<String> query = new ArrayList<>();
        private String primair;

        public QuaryCreateTable addColum(String name, String type, String defaultValue, boolean isTextType, boolean CanBeNull, boolean isPrimear, boolean incrementPrimear, boolean hasDefault) {
            if (isPrimear) {
                primair = name;
            }
            String queryEntry = "`" + name + "` " + type;
            if (!CanBeNull) {
                if (defaultValue == null && !incrementPrimear && hasDefault) {
                    throw new IllegalArgumentException("default value can't be null if CanBeNull is false");
                }
                queryEntry += " NOT NULL";
            }
            if (CanBeNull && defaultValue == null && hasDefault) {
                queryEntry += " DEFAULT NULL";
            } else if (defaultValue != null) {
                if (isTextType) {
                    queryEntry += " DEFAULT '" + defaultValue + "'";
                } else {
                    queryEntry += " DEFAULT " + defaultValue;
                }
            }
            if (isPrimear) {
                queryEntry += " AUTO_INCREMENT";
            }
            query.add(queryEntry);
            return this;
        }

        public QuaryCreateTable addColum(String name, String type, String defaultValue, boolean isTextType, boolean CanBeNull, boolean hasDefault) {
            return this.addColum(name, type, defaultValue, isTextType, CanBeNull, false, false, hasDefault);
        }

        public QuaryCreateTable addColum(String name, String type) {
            return this.addColum(name, type, null, false, true, false);
        }

        private String getQuery() {
            if (primair == null) {
                throw new NullPointerException("No primairy key entered.");
            }
            String returnString = "";
            for (String string : query) {
                returnString += string + ", ";
            }
            returnString += "PRIMARY KEY (`" + primair + "`)";
            return returnString;
        }
    }
}

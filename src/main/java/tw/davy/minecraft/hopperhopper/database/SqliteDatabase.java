package tw.davy.minecraft.hopperhopper.database;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import tw.davy.minecraft.hopperhopper.HopperHopperPlugin;

import static tw.davy.minecraft.hopperhopper.utils.PredicateUtils.not;

/**
 * @author Davy
 */
public class SqliteDatabase implements Database {
    static private final List<ItemStack> sDummyItems = new ArrayList<>();

    private final HopperHopperPlugin mPlugin;
    private Connection mConnection;
    private PreparedStatement mPreparedUpdateStatement;
    private PreparedStatement mPreparedQueryStatement;
    private PreparedStatement mPreparedDeleteStatement;

    public SqliteDatabase(final HopperHopperPlugin plugin) {
        mPlugin = plugin;

        try {
            mConnection = connect();
            initTable();
            initPreparedStatements();
        } catch (ClassNotFoundException | SQLException e) {
            mPlugin.getLogger().warning("Cannot connect to database.");
            e.printStackTrace();
        }
    }

    private Connection connect() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");

        return DriverManager.getConnection("jdbc:sqlite:" + mPlugin.getDataFolder().getAbsolutePath() + "/storage.sqlite3");
    }

    private void initTable() throws SQLException {
        final Statement statement = mConnection.createStatement();
        statement.executeUpdate("CREATE TABLE IF NOT EXISTS records (" +
                "world STRING NOT NULL, " +
                "x INTEGER NOT NULL, " +
                "y INTEGER NOT NULL, " +
                "z INTEGER NOT NULL, " +
                "filter STRING NOT NULL" +
                ")");
        statement.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS unique_record ON records (world, x, y, z)");
        statement.close();
    }

    private void initPreparedStatements() throws SQLException {
        mPreparedUpdateStatement = mConnection.prepareStatement("REPLACE INTO records " +
                "(world, x, y, z, filter) VALUES (?, ?, ?, ?, ?)");
        mPreparedQueryStatement = mConnection.prepareStatement("SELECT * FROM records " +
                "WHERE `world` = ? AND `x` = ? AND `y` = ? AND `z` = ?");
        mPreparedDeleteStatement = mConnection.prepareStatement("DELETE FROM records " +
                "WHERE `world` = ? AND `x` = ? AND `y` = ? AND `z` = ?");
    }

    @Override
    public void saveFilter(final Block hopperBlock, final List<ItemStack> items) {
        if (items.isEmpty()) {
            clearFilter(hopperBlock);
            return;
        }

        try {
            mPreparedUpdateStatement.setString(1, hopperBlock.getWorld().getUID().toString());
            mPreparedUpdateStatement.setInt(2, hopperBlock.getX());
            mPreparedUpdateStatement.setInt(3, hopperBlock.getY());
            mPreparedUpdateStatement.setInt(4, hopperBlock.getZ());
            mPreparedUpdateStatement.setString(5, items.stream()
                    .map(item -> item.getType().name() + ":" + item.getDurability())
                    .distinct()
                    .reduce((a, b) -> a + "," + b)
                    .orElse("")
            );
            mPreparedUpdateStatement.executeUpdate();
            mPreparedUpdateStatement.clearParameters();
        } catch (SQLException ignored) {
            ignored.printStackTrace();
        }
    }

    @Override
    public List<ItemStack> loadFilter(final Block hopperBlock) {
        try {
            mPreparedQueryStatement.setString(1, hopperBlock.getWorld().getUID().toString());
            mPreparedQueryStatement.setInt(2, hopperBlock.getX());
            mPreparedQueryStatement.setInt(3, hopperBlock.getY());
            mPreparedQueryStatement.setInt(4, hopperBlock.getZ());
            final ResultSet results = mPreparedQueryStatement.executeQuery();
            mPreparedQueryStatement.clearParameters();

            if (!results.next())
                return sDummyItems;

            final String filter = results.getString("filter");
            if (filter == null)
                return sDummyItems;

            return Arrays.stream(filter.split(","))
                    .filter(not(String::isEmpty))
                    .map(item -> item.split(":"))
                    .map(data -> new ItemStack(Material.getMaterial(data[0]), 1, Short.parseShort(data[1])))
                    .collect(Collectors.toList());
        } catch (SQLException ignored) {
            ignored.printStackTrace();
        }

        return sDummyItems;
    }

    @Override
    public void clearFilter(final Block hopperBlock) {
        try {
            mPreparedDeleteStatement.setString(1, hopperBlock.getWorld().getUID().toString());
            mPreparedDeleteStatement.setInt(2, hopperBlock.getX());
            mPreparedDeleteStatement.setInt(3, hopperBlock.getY());
            mPreparedDeleteStatement.setInt(4, hopperBlock.getZ());
            mPreparedDeleteStatement.executeUpdate();
            mPreparedDeleteStatement.clearParameters();
        } catch (SQLException ignored) {
            ignored.printStackTrace();
        }
    }
}

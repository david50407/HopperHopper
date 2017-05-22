package tw.davy.minecraft.hopperhopper;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

import tw.davy.minecraft.hopperhopper.database.Database;
import tw.davy.minecraft.hopperhopper.database.MetadataDatabase;
import tw.davy.minecraft.hopperhopper.database.SqliteDatabase;
import tw.davy.minecraft.hopperhopper.listener.HopperEventListener;
import tw.davy.minecraft.hopperhopper.listener.ItemFrameEventListener;
import tw.davy.minecraft.hopperhopper.listener.PlayerInteractListener;

/**
 * @author Davy
 */
public class HopperHopperPlugin extends JavaPlugin {
    static private HopperHopperPlugin sInstance;

    private final HashMap<Material, Integer> mMaterialLevels = new HashMap<>();
    private Database mDatabase;

    static public HopperHopperPlugin getInstance() {
        return sInstance;
    }

    @Override
    public void onEnable() {
        sInstance = this;
        final PluginManager pluginManager = getServer().getPluginManager();

        saveDefaultConfig();
        final ConfigurationSection configPoweredItemsSection = getConfig().getConfigurationSection("powered-items");
        if (configPoweredItemsSection != null) {
            for (final String key : configPoweredItemsSection.getKeys(false)) {
                final Material material = Material.matchMaterial(key);
                final int level = configPoweredItemsSection.getInt(key);
                if (material != null && level >= 1 && level <= 6)
                    mMaterialLevels.put(material, level);
            }
        }

        mDatabase = new MetadataDatabase<>(this, new SqliteDatabase(this));

        pluginManager.registerEvents(new HopperEventListener(this), this);
        pluginManager.registerEvents(new ItemFrameEventListener(this), this);
        pluginManager.registerEvents(new PlayerInteractListener(), this);
    }

    @Override
    public void onDisable() {
        if (mDatabase != null)
            mDatabase.dispose();
    }

    public int getLevel(final Material material) {
        return mMaterialLevels.getOrDefault(material, 0);
    }

    public Database getFilterDatabase() {
        return mDatabase;
    }
}

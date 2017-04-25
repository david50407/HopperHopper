package tw.davy.minecraft.hopperhopper.database;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tw.davy.minecraft.hopperhopper.HopperHopperPlugin;

import static tw.davy.minecraft.hopperhopper.utils.PredicateUtils.not;

/**
 * @author Davy
 */
public class MetadataDatabase<IMPL extends Database> implements Database {
    static private final String FILTER_METADATA = "allowed-materials";
    static private final List<ItemStack> sDummyItems = new ArrayList<>();

    private final HopperHopperPlugin mPlugin;
    private final IMPL mInternalDatabase;

    public MetadataDatabase(final HopperHopperPlugin plugin, final IMPL internalDatabase) {
        mPlugin = plugin;
        mInternalDatabase = internalDatabase;
    }

    @Override
    public void saveFilter(final Block hopperBlock, final List<ItemStack> items) {
        mInternalDatabase.saveFilter(hopperBlock, items);
        saveMetadata(hopperBlock, items);
    }

    @Override
    public List<ItemStack> loadFilter(final Block hopperBlock) {
        if (!hasMetadata(hopperBlock)) {
            final List<ItemStack> items = mInternalDatabase.loadFilter(hopperBlock);
            saveMetadata(hopperBlock, items);
        }

        return loadMetadata(hopperBlock);
    }

    @Override
    public void clearFilter(final Block hopperBlock) {
        mInternalDatabase.clearFilter(hopperBlock);
        clearMetadata(hopperBlock);
    }

    private void saveMetadata(final Block hopperBlock, final List<ItemStack> items) {
        clearMetadata(hopperBlock);

        try {
            hopperBlock.setMetadata(FILTER_METADATA,
                    new FixedMetadataValue(mPlugin, items.stream()
                            .map(itemStack -> "" + itemStack.getType().name() + ":" + itemStack.getDurability())
                            .distinct()
                            .reduce((a, b) -> a + ',' + b)
                            .orElse("")
                    )
            );
        } catch (NoSuchElementException ignored) {
        }
    }

    private List<ItemStack> loadMetadata(final Block hopperBlock) {
        try {
            final String materialNames = hopperBlock.getMetadata(FILTER_METADATA).stream()
                    .filter(metadataValue -> metadataValue.getOwningPlugin() instanceof HopperHopperPlugin)
                    .findFirst()
                    .get()
                    .asString();

            return Stream.of(materialNames.split(","))
                    .filter(not(String::isEmpty))
                    .map(name -> name.split(":"))
                    .map(raw -> new ItemStack(Material.getMaterial(raw[0]), 1, Short.parseShort(raw[1])))
                    .collect(Collectors.toList());
        } catch (NoSuchElementException ignored) {
            return sDummyItems;
        }
    }

    private void clearMetadata(final Block hopperBlock) {
        hopperBlock.removeMetadata(FILTER_METADATA, mPlugin);
    }

    private boolean hasMetadata(final Block hopperBlock) {
        return hopperBlock.hasMetadata(FILTER_METADATA) &&
                hopperBlock.getMetadata(FILTER_METADATA).stream()
                        .anyMatch(metadataValue -> metadataValue.getOwningPlugin() instanceof HopperHopperPlugin);

    }
}

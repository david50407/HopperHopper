package tw.davy.minecraft.hopperhopper.inventory;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * @author Davy
 */
public class FilterInventoryHolder implements InventoryHolder {
    private final Inventory mInventory;
    private final Block mHopperBlock;

    public FilterInventoryHolder(final Block hopperBlock, final int level) {
        mInventory = Bukkit.createInventory(this, level * 9, "Filter");
        mHopperBlock = hopperBlock;
    }

    @Override
    public Inventory getInventory() {
        return mInventory;
    }

    public Block getHopperBlock() {
        return mHopperBlock;
    }
}

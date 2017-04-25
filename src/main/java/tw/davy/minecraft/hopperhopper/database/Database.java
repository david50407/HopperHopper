package tw.davy.minecraft.hopperhopper.database;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * @author Davy
 */
public interface Database {
    void saveFilter(Block hopperBlock, List<ItemStack> items);
    List<ItemStack> loadFilter(Block hopperBlock);
    void clearFilter(Block hopperBlock);
}

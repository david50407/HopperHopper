package tw.davy.minecraft.hopperhopper.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import tw.davy.minecraft.hopperhopper.HopperHopperPlugin;
import tw.davy.minecraft.hopperhopper.database.Database;

/**
 * @author Davy
 */
public class HopperEventListener implements Listener {
    private final HopperHopperPlugin mPlugin;

    public HopperEventListener(final HopperHopperPlugin plugin) {
        mPlugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryPickupItem(final InventoryPickupItemEvent ev) {
        if (ev.getInventory().getType() != InventoryType.HOPPER ||
                !(ev.getInventory().getHolder() instanceof Hopper))
            return;

        final Item itemEntity = ev.getItem();
        final ItemStack itemStack = itemEntity.getItemStack();
        final Block hopperBlock = ((Hopper) ev.getInventory().getHolder()).getBlock();
        final Database filterDatabase = mPlugin.getFilterDatabase();
        final List<ItemStack> allowedItems = filterDatabase.loadFilter(hopperBlock);

        if (!allowedItems.isEmpty() && allowedItems.parallelStream()
                .noneMatch(allowed -> allowed.getType() == itemStack.getType() &&
                        allowed.getDurability() == itemStack.getDurability()))
            ev.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(final InventoryMoveItemEvent ev) {
        if (ev.getDestination().getType() != InventoryType.HOPPER)
            return;

        final Inventory source = ev.getSource();
        final ItemStack itemStack = ev.getItem();
        if (itemStack.getType() != Material.STONE) {
            switch (source.getType()) {
                case FURNACE:
                    ev.setCancelled(true);
                    break;
                default: {
                    Optional<ItemStack> selectedItemStackOptional = Stream.of(source.getStorageContents())
                            .filter(Objects::nonNull)
                            .filter(item -> item.getType() == Material.STONE)
                            .findFirst();
                    if (selectedItemStackOptional.isPresent()) {
                        final ItemStack selectedItem = selectedItemStackOptional.get();
                        final ItemStack target = selectedItem.clone();
                        selectedItem.setAmount(selectedItem.getAmount() - 1);
                        target.setAmount(1);
                        ev.setItem(target);
                    } else {
                        ev.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperBreak(final BlockBreakEvent ev) {
        if (ev.getBlock().getType() != Material.HOPPER)
            return;

        mPlugin.getFilterDatabase().clearFilter(ev.getBlock());
    }
}

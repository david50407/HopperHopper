package tw.davy.minecraft.hopperhopper.listener;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import tw.davy.minecraft.hopperhopper.HopperHopperPlugin;
import tw.davy.minecraft.hopperhopper.database.Database;
import tw.davy.minecraft.hopperhopper.inventory.FilterInventoryHolder;
import tw.davy.minecraft.hopperhopper.utils.InventoryEventHelper;

/**
 * @author Davy
 */
public class ItemFrameEventListener implements Listener {
    private final HopperHopperPlugin mPlugin;

    public ItemFrameEventListener(final HopperHopperPlugin plugin) {
        mPlugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(final PlayerInteractEntityEvent ev) {
        if (ev.getRightClicked().getType() != EntityType.ITEM_FRAME)
            return;

        final ItemFrame itemFrameEntity = (ItemFrame) ev.getRightClicked();
        final ItemStack itemStack = itemFrameEntity.getItem();

        if (itemStack == null || itemStack.getType() == Material.AIR)
            return;

        final int level = mPlugin.getLevel(itemStack.getType());
        final Block hopperBlock = getRelativedHopperBlock(itemFrameEntity);
        if (level == 0 || hopperBlock == null)
            return;

        ev.setCancelled(true);

        final Database filterDatabase = mPlugin.getFilterDatabase();
        final List<ItemStack> filteredMaterials = filterDatabase.loadFilter(hopperBlock);
        final FilterInventoryHolder inventoryHolder = new FilterInventoryHolder(hopperBlock, level);
        filteredMaterials.forEach(data -> inventoryHolder.getInventory().addItem(data));
        ev.getPlayer().openInventory(inventoryHolder.getInventory());
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemFrameBreak(final HangingBreakEvent ev) {
        if (!(ev.getEntity() instanceof ItemFrame))
            return;

        final Block hopperBlock = getRelativedHopperBlock((ItemFrame) ev.getEntity());
        if (hopperBlock == null)
            return;

        mPlugin.getFilterDatabase().clearFilter(hopperBlock);
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemFrameItemRemove(final EntityDamageByEntityEvent ev) {
        if (!(ev.getEntity() instanceof ItemFrame))
            return;

        final ItemFrame itemFrameEntity = (ItemFrame) ev.getEntity();
        if (itemFrameEntity.getItem() == null ||
                itemFrameEntity.getItem().getType() == Material.AIR)
            return;

        final Block hopperBlock = getRelativedHopperBlock(itemFrameEntity);
        if (hopperBlock == null)
            return;

        mPlugin.getFilterDatabase().clearFilter(hopperBlock);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(final InventoryClickEvent ev) {
        final Inventory inventory = InventoryEventHelper.getClickedInventory(ev);
        if (inventory == null)
            return;

        if (inventory.getHolder() instanceof FilterInventoryHolder) {
            ev.setCancelled(true);

            final ItemStack clonedCursorItem = ev.getCursor() != null ?
                    ev.getCursor().clone() : null;

            Bukkit.getScheduler().runTask(mPlugin, () -> {
                ev.getWhoClicked().setItemOnCursor(clonedCursorItem);

                ev.setCurrentItem(new ItemStack(Material.AIR));
                if (clonedCursorItem != null) {
                    final ItemStack cloned = clonedCursorItem.clone();
                    cloned.setAmount(1);
                    ev.setCurrentItem(cloned);
                }
            });
        } else if (ev.getInventory().getHolder() instanceof FilterInventoryHolder) {
            if (ev.getClick() == ClickType.DOUBLE_CLICK)
                ev.setCancelled(true);
            else if (ev.isShiftClick()) {
                ev.setCancelled(true);

                final Inventory filterInvnetory = ev.getInventory();
                final ItemStack clonedTargetItem = ev.getCurrentItem() != null ?
                        ev.getCurrentItem().clone() : null;

                if (clonedTargetItem != null && clonedTargetItem.getType() != Material.AIR &&
                        !filterInvnetory.containsAtLeast(clonedTargetItem, 1)) {
                    clonedTargetItem.setAmount(1);
                    Bukkit.getScheduler().runTask(mPlugin, () -> {
                            filterInvnetory.addItem(clonedTargetItem);
                    });
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(final InventoryDragEvent ev) {
        final List<Inventory> affectedInventories = InventoryEventHelper.getDragedInventories(ev);
        if (affectedInventories == null)
            return;

        if (ev.getRawSlots().size() == 1) {
            final ItemStack clonedCursorItem = ev.getOldCursor().clone();
            final Inventory inventory = affectedInventories.get(0);
            final int slotId = ev.getRawSlots().iterator().next();
            if (clonedCursorItem != null &&
                    inventory.getHolder() instanceof FilterInventoryHolder) {
                ev.setCancelled(true);

                Bukkit.getScheduler().runTask(mPlugin, () -> {
                    ev.getWhoClicked().setItemOnCursor(clonedCursorItem);

                    final ItemStack cloned = clonedCursorItem.clone();
                    cloned.setAmount(1);
                    ev.getView().setItem(slotId, cloned);
                });
            }
            return;
        }

        for (final Inventory inventory : affectedInventories) {
            if (inventory.getHolder() instanceof FilterInventoryHolder)
                ev.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent ev) {
        final Inventory inventory = ev.getInventory();
        if (!(inventory.getHolder() instanceof FilterInventoryHolder))
            return;

        final FilterInventoryHolder holder = (FilterInventoryHolder) inventory.getHolder();
        final Database filterDatabase = mPlugin.getFilterDatabase();
        filterDatabase.saveFilter(holder.getHopperBlock(),
                Stream.of(inventory.getStorageContents())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
    }

    @Nullable
    private Block getRelativedHopperBlock(final ItemFrame itemFrame) {
        final Block attechedBlock = itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
        final BlockState blockState = attechedBlock.getState();

        return blockState instanceof Hopper ? attechedBlock : null;
    }
}

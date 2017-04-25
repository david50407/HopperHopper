package tw.davy.minecraft.hopperhopper.utils;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Davy
 */
public class InventoryEventHelper {
    @Nullable
    static public Inventory getClickedInventory(final InventoryClickEvent ev) {
        final InventoryView view = ev.getView();

        if (ev.getRawSlot() < 0)
            return null;
        if (view.getTopInventory() != null && ev.getRawSlot() < view.getTopInventory().getSize())
            return view.getTopInventory();
        return view.getBottomInventory();
    }

    @Nullable
    static public List<Inventory> getDragedInventories(final InventoryDragEvent ev) {
        final InventoryView view = ev.getView();

        return ev.getRawSlots().stream()
                .map(rawSlot -> {
                    if (rawSlot < 0)
                        return null;
                    if (view.getTopInventory() != null && rawSlot < view.getTopInventory().getSize())
                        return view.getTopInventory();
                    return view.getBottomInventory();
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}

package tw.davy.minecraft.hopperhopper.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * @author Davy
 */
public class PlayerInteractListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlaceItemFrame(final PlayerInteractEvent ev) {
        if (ev.getAction() != Action.RIGHT_CLICK_BLOCK ||
                !ev.hasBlock() || ev.getClickedBlock().getType() != Material.HOPPER ||
                !ev.hasItem() || ev.getItem().getType() != Material.ITEM_FRAME ||
                ev.getBlockFace() == BlockFace.UP || ev.getBlockFace() == BlockFace.DOWN)
            return;

        ev.setCancelled(true);

        final Block hopperBlock = ev.getClickedBlock();
        final Block asideBlock = hopperBlock.getRelative(ev.getBlockFace());
        if (asideBlock.getType() != Material.AIR)
            return;

        final boolean alreadyHasFilter = hopperBlock.getWorld()
                .getNearbyEntities(hopperBlock.getLocation(), 2, 1, 2)
                .parallelStream()
                .filter(entity -> entity.getType() == EntityType.ITEM_FRAME)
                .anyMatch(itemFrame ->
                        itemFrame.getLocation().getBlockY() == hopperBlock.getY() && (
                        (itemFrame.getLocation().getBlockX() == hopperBlock.getX() &&
                                Math.abs(itemFrame.getLocation().getBlockZ() - hopperBlock.getZ()) == 1) ||
                        (itemFrame.getLocation().getBlockZ() == hopperBlock.getZ() &&
                                Math.abs(itemFrame.getLocation().getBlockX() - hopperBlock.getX()) == 1)));

        if (!alreadyHasFilter) {
            ev.setCancelled(false);
            ev.setUseItemInHand(Event.Result.ALLOW);
            ev.setUseInteractedBlock(Event.Result.DENY);
        }
    }
}

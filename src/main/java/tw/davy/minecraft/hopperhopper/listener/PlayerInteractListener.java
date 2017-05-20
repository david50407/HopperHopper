package tw.davy.minecraft.hopperhopper.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

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
                .map(entity -> getRelativedHopperBlock((ItemFrame) entity))
                .filter(Objects::nonNull)
                .anyMatch(relativedHopperBlock -> hopperBlock.getLocation().equals(relativedHopperBlock.getLocation()));

        if (!alreadyHasFilter) {
            ev.setCancelled(false);
            ev.setUseItemInHand(Event.Result.ALLOW);
            ev.setUseInteractedBlock(Event.Result.DENY);
        }
    }

    @Nullable
    private Block getRelativedHopperBlock(final ItemFrame itemFrame) {
        final Block attechedBlock = itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
        final BlockState blockState = attechedBlock.getState();

        return blockState instanceof Hopper ? attechedBlock : null;
    }
}

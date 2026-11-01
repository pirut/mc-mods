package net.neoforged.neoforge.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public abstract class GrindstoneEvent extends Event {
   private final ItemStack top;
   private final ItemStack bottom;
   private int xp;

   protected GrindstoneEvent(ItemStack top, ItemStack bottom, int xp) {
      this.top = top;
      this.bottom = bottom;
      this.xp = xp;
   }

   public ItemStack getTopItem() {
      return this.top;
   }

   public ItemStack getBottomItem() {
      return this.bottom;
   }

   public int getXp() {
      return this.xp;
   }

   public void setXp(int xp) {
      this.xp = xp;
   }

   public static class OnPlaceItem extends GrindstoneEvent implements ICancellableEvent {
      private ItemStack output = ItemStack.EMPTY;

      public OnPlaceItem(ItemStack top, ItemStack bottom, int xp) {
         super(top, bottom, xp);
      }

      public ItemStack getOutput() {
         return this.output;
      }

      public void setOutput(ItemStack output) {
         this.output = output;
      }
   }

   public static class OnTakeItem extends GrindstoneEvent implements ICancellableEvent {
      private ItemStack newTop = ItemStack.EMPTY;
      private ItemStack newBottom = ItemStack.EMPTY;
      private final ContainerLevelAccess access;
      @Nullable
      private final Player player;

      public OnTakeItem(ContainerLevelAccess access, @Nullable Player player, ItemStack top, ItemStack bottom, int xp) {
         super(top, bottom, xp);
         this.access = access;
         this.player = player;
      }

      @Deprecated(
         forRemoval = true,
         since = "1.21.8"
      )
      public OnTakeItem(ItemStack top, ItemStack bottom, int xp) {
         this(ContainerLevelAccess.NULL, null, top, bottom, xp);
      }

      public ItemStack getNewTopItem() {
         return this.newTop;
      }

      public ItemStack getNewBottomItem() {
         return this.newBottom;
      }

      public void setNewTopItem(ItemStack newTop) {
         this.newTop = newTop;
      }

      public void setNewBottomItem(ItemStack newBottom) {
         this.newBottom = newBottom;
      }

      @Override
      public int getXp() {
         return super.getXp();
      }

      public ContainerLevelAccess getContainerAccess() {
         return this.access;
      }

      @Nullable
      public Player getPlayer() {
         return this.player;
      }
   }
}

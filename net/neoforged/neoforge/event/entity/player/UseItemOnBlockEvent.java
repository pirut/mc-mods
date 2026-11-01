package net.neoforged.neoforge.event.entity.player;

import com.google.common.base.Preconditions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.fml.LogicalSide;
import org.jspecify.annotations.Nullable;

public class UseItemOnBlockEvent extends Event implements ICancellableEvent {
   private final Level level;
   @Nullable
   private final Player player;
   private final InteractionHand hand;
   private final ItemStack heldItem;
   private final BlockPos pos;
   @Nullable
   private final Direction face;
   private final UseOnContext context;
   private final UseItemOnBlockEvent.UsePhase usePhase;
   private InteractionResult cancellationResult = InteractionResult.TRY_WITH_EMPTY_HAND;

   public UseItemOnBlockEvent(UseOnContext context, UseItemOnBlockEvent.UsePhase usePhase) {
      this.level = (Level)Preconditions.checkNotNull(context.getLevel(), "Null level in UseItemOnBlockEvent!");
      this.player = context.getPlayer();
      this.heldItem = (ItemStack)Preconditions.checkNotNull(context.getItemInHand(), "Null heldItem in UseItemOnBlockEvent!");
      this.hand = (InteractionHand)Preconditions.checkNotNull(context.getHand(), "Null hand in UseItemOnBlockEvent!");
      this.pos = (BlockPos)Preconditions.checkNotNull(context.getClickedPos(), "Null position in UseItemOnBlockEvent!");
      this.face = context.getClickedFace();
      this.context = context;
      this.usePhase = usePhase;
   }

   @Nullable
   public Player getPlayer() {
      return this.player;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public ItemStack getItemStack() {
      return this.heldItem;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   @Nullable
   public Direction getFace() {
      return this.face;
   }

   public Level getLevel() {
      return this.level;
   }

   public UseOnContext getUseOnContext() {
      return this.context;
   }

   public UseItemOnBlockEvent.UsePhase getUsePhase() {
      return this.usePhase;
   }

   public LogicalSide getSide() {
      return this.getLevel().isClientSide() ? LogicalSide.CLIENT : LogicalSide.SERVER;
   }

   public void cancelWithResult(InteractionResult result) {
      this.setCancellationResult(result);
      this.setCanceled(true);
   }

   public InteractionResult getCancellationResult() {
      return this.cancellationResult;
   }

   public void setCancellationResult(InteractionResult result) {
      this.cancellationResult = result;
   }

   public static enum UsePhase {
      ITEM_BEFORE_BLOCK,
      BLOCK,
      ITEM_AFTER_BLOCK;
   }
}

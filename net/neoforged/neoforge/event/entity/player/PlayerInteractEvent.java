package net.neoforged.neoforge.event.entity.player;

import com.google.common.base.Preconditions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class PlayerInteractEvent extends PlayerEvent {
   private final InteractionHand hand;
   private final BlockPos pos;
   @Nullable
   private final Direction face;

   protected PlayerInteractEvent(Player player, InteractionHand hand, BlockPos pos, @Nullable Direction face) {
      super((Player)Preconditions.checkNotNull(player, "Null player in PlayerInteractEvent!"));
      this.hand = (InteractionHand)Preconditions.checkNotNull(hand, "Null hand in PlayerInteractEvent!");
      this.pos = (BlockPos)Preconditions.checkNotNull(pos, "Null position in PlayerInteractEvent!");
      this.face = face;
   }

   public InteractionHand getHand() {
      return this.hand;
   }

   public ItemStack getItemStack() {
      return this.getEntity().getItemInHand(this.hand);
   }

   public BlockPos getPos() {
      return this.pos;
   }

   @Nullable
   public Direction getFace() {
      return this.face;
   }

   public Level getLevel() {
      return this.getEntity().level();
   }

   public LogicalSide getSide() {
      return this.getLevel().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER;
   }

   public static class EntityInteract extends PlayerInteractEvent implements ICancellableEvent {
      private InteractionResult cancellationResult = InteractionResult.PASS;
      private final Entity target;

      public EntityInteract(Player player, InteractionHand hand, Entity target) {
         super(player, hand, target.blockPosition(), null);
         this.target = target;
      }

      public Entity getTarget() {
         return this.target;
      }

      public InteractionResult getCancellationResult() {
         return this.cancellationResult;
      }

      public void setCancellationResult(InteractionResult result) {
         this.cancellationResult = result;
      }
   }

   public static class EntityInteractSpecific extends PlayerInteractEvent implements ICancellableEvent {
      private InteractionResult cancellationResult = InteractionResult.PASS;
      private final Vec3 localPos;
      private final Entity target;

      public EntityInteractSpecific(Player player, InteractionHand hand, Entity target, Vec3 localPos) {
         super(player, hand, target.blockPosition(), null);
         this.localPos = localPos;
         this.target = target;
      }

      public Vec3 getLocalPos() {
         return this.localPos;
      }

      public Entity getTarget() {
         return this.target;
      }

      public InteractionResult getCancellationResult() {
         return this.cancellationResult;
      }

      public void setCancellationResult(InteractionResult result) {
         this.cancellationResult = result;
      }
   }

   public static class LeftClickBlock extends PlayerInteractEvent implements ICancellableEvent {
      private TriState useBlock = TriState.DEFAULT;
      private TriState useItem = TriState.DEFAULT;
      private final PlayerInteractEvent.LeftClickBlock.Action action;

      @Internal
      public LeftClickBlock(Player player, BlockPos pos, Direction face, PlayerInteractEvent.LeftClickBlock.Action action) {
         super(player, InteractionHand.MAIN_HAND, pos, face);
         this.action = action;
      }

      public TriState getUseBlock() {
         return this.useBlock;
      }

      public TriState getUseItem() {
         return this.useItem;
      }

      public PlayerInteractEvent.LeftClickBlock.Action getAction() {
         return this.action;
      }

      public void setUseBlock(TriState triggerBlock) {
         this.useBlock = triggerBlock;
      }

      public void setUseItem(TriState triggerItem) {
         this.useItem = triggerItem;
      }

      public void setCanceled(boolean canceled) {
         super.setCanceled(canceled);
         if (canceled) {
            this.useBlock = TriState.FALSE;
            this.useItem = TriState.FALSE;
         }
      }

      public static enum Action {
         START,
         STOP,
         ABORT,
         CLIENT_HOLD;

         public static PlayerInteractEvent.LeftClickBlock.Action convert(ServerboundPlayerActionPacket.Action action) {
            return switch (action) {
               case START_DESTROY_BLOCK -> START;
               case STOP_DESTROY_BLOCK -> STOP;
               case ABORT_DESTROY_BLOCK -> ABORT;
               default -> START;
            };
         }
      }
   }

   public static class LeftClickEmpty extends PlayerInteractEvent {
      public LeftClickEmpty(Player player) {
         super(player, InteractionHand.MAIN_HAND, player.blockPosition(), null);
      }
   }

   public static class RightClickBlock extends PlayerInteractEvent implements ICancellableEvent {
      private InteractionResult cancellationResult = InteractionResult.PASS;
      private TriState useBlock = TriState.DEFAULT;
      private TriState useItem = TriState.DEFAULT;
      private BlockHitResult hitVec;

      public RightClickBlock(Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitVec) {
         super(player, hand, pos, hitVec.getDirection());
         this.hitVec = hitVec;
      }

      public TriState getUseBlock() {
         return this.useBlock;
      }

      public TriState getUseItem() {
         return this.useItem;
      }

      public BlockHitResult getHitVec() {
         return this.hitVec;
      }

      public void setUseBlock(TriState triggerBlock) {
         this.useBlock = triggerBlock;
      }

      public void setUseItem(TriState triggerItem) {
         this.useItem = triggerItem;
      }

      public void setCanceled(boolean canceled) {
         super.setCanceled(canceled);
         if (canceled) {
            this.useBlock = TriState.FALSE;
            this.useItem = TriState.FALSE;
         }
      }

      public InteractionResult getCancellationResult() {
         return this.cancellationResult;
      }

      public void setCancellationResult(InteractionResult result) {
         this.cancellationResult = result;
      }
   }

   public static class RightClickEmpty extends PlayerInteractEvent {
      public RightClickEmpty(Player player, InteractionHand hand) {
         super(player, hand, player.blockPosition(), null);
      }
   }

   public static class RightClickItem extends PlayerInteractEvent implements ICancellableEvent {
      private InteractionResult cancellationResult = InteractionResult.PASS;

      public RightClickItem(Player player, InteractionHand hand) {
         super(player, hand, player.blockPosition(), null);
      }

      public InteractionResult getCancellationResult() {
         return this.cancellationResult;
      }

      public void setCancellationResult(InteractionResult result) {
         this.cancellationResult = result;
      }
   }
}

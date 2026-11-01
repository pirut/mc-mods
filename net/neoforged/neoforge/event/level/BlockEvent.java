package net.neoforged.neoforge.event.level;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.PortalShape;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import org.jetbrains.annotations.Nullable;

public abstract class BlockEvent extends Event {
   private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("neoforge.debugBlockEvent", "false"));
   private final LevelAccessor level;
   private final BlockPos pos;
   private final BlockState state;

   public BlockEvent(LevelAccessor level, BlockPos pos, BlockState state) {
      this.pos = pos;
      this.level = level;
      this.state = state;
   }

   public LevelAccessor getLevel() {
      return this.level;
   }

   public BlockPos getPos() {
      return this.pos;
   }

   public BlockState getState() {
      return this.state;
   }

   public static class BlockToolModificationEvent extends BlockEvent implements ICancellableEvent {
      private final UseOnContext context;
      private final ItemAbility itemAbility;
      private final boolean simulate;
      private BlockState state;

      public BlockToolModificationEvent(BlockState originalState, UseOnContext context, ItemAbility itemAbility, boolean simulate) {
         super(context.getLevel(), context.getClickedPos(), originalState);
         this.context = context;
         this.state = originalState;
         this.itemAbility = itemAbility;
         this.simulate = simulate;
      }

      @Nullable
      public Player getPlayer() {
         return this.context.getPlayer();
      }

      public ItemStack getHeldItemStack() {
         return this.context.getItemInHand();
      }

      public ItemAbility getItemAbility() {
         return this.itemAbility;
      }

      public boolean isSimulated() {
         return this.simulate;
      }

      public UseOnContext getContext() {
         return this.context;
      }

      public void setFinalState(@Nullable BlockState finalState) {
         this.state = finalState;
      }

      public BlockState getFinalState() {
         return this.state;
      }
   }

   public static class BreakEvent extends BlockEvent implements ICancellableEvent {
      private final Player player;

      public BreakEvent(Level level, BlockPos pos, BlockState state, Player player) {
         super(level, pos, state);
         this.player = player;
      }

      public Player getPlayer() {
         return this.player;
      }

      public void setCanceled(boolean canceled) {
         super.setCanceled(canceled);
      }
   }

   public static class EntityMultiPlaceEvent extends BlockEvent.EntityPlaceEvent implements ICancellableEvent {
      private final List<BlockSnapshot> blockSnapshots;

      public EntityMultiPlaceEvent(List<BlockSnapshot> blockSnapshots, BlockState placedAgainst, @Nullable Entity entity) {
         super(blockSnapshots.get(0), placedAgainst, entity);
         this.blockSnapshots = ImmutableList.copyOf(blockSnapshots);
         if (BlockEvent.DEBUG) {
            System.out.printf("Created EntityMultiPlaceEvent - [PlacedAgainst: %s ][Entity: %s ]\n", placedAgainst, entity);
         }
      }

      public List<BlockSnapshot> getReplacedBlockSnapshots() {
         return this.blockSnapshots;
      }
   }

   public static class EntityPlaceEvent extends BlockEvent implements ICancellableEvent {
      private final Entity entity;
      private final BlockSnapshot blockSnapshot;
      private final BlockState placedBlock;
      private final BlockState placedAgainst;

      public EntityPlaceEvent(BlockSnapshot blockSnapshot, BlockState placedAgainst, @Nullable Entity entity) {
         super(blockSnapshot.getLevel(), blockSnapshot.getPos(), !(entity instanceof Player) ? blockSnapshot.getState() : blockSnapshot.getCurrentState());
         this.entity = entity;
         this.blockSnapshot = blockSnapshot;
         this.placedBlock = !(entity instanceof Player) ? blockSnapshot.getState() : blockSnapshot.getCurrentState();
         this.placedAgainst = placedAgainst;
         if (BlockEvent.DEBUG) {
            System.out.printf("Created EntityPlaceEvent - [PlacedBlock: %s ][PlacedAgainst: %s ][Entity: %s ]\n", this.getPlacedBlock(), placedAgainst, entity);
         }
      }

      @Nullable
      public Entity getEntity() {
         return this.entity;
      }

      public BlockSnapshot getBlockSnapshot() {
         return this.blockSnapshot;
      }

      public BlockState getPlacedBlock() {
         return this.placedBlock;
      }

      public BlockState getPlacedAgainst() {
         return this.placedAgainst;
      }
   }

   public static class FarmlandTrampleEvent extends BlockEvent implements ICancellableEvent {
      private final Entity entity;
      private final float fallDistance;

      public FarmlandTrampleEvent(Level level, BlockPos pos, BlockState state, float fallDistance, Entity entity) {
         super(level, pos, state);
         this.entity = entity;
         this.fallDistance = fallDistance;
      }

      public Entity getEntity() {
         return this.entity;
      }

      public float getFallDistance() {
         return this.fallDistance;
      }
   }

   public static class FluidPlaceBlockEvent extends BlockEvent implements ICancellableEvent {
      private final BlockPos liquidPos;
      private BlockState newState;
      private BlockState origState;

      public FluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
         super(level, pos, state);
         this.liquidPos = liquidPos;
         this.newState = state;
         this.origState = level.getBlockState(pos);
      }

      public BlockPos getLiquidPos() {
         return this.liquidPos;
      }

      public BlockState getNewState() {
         return this.newState;
      }

      public void setNewState(BlockState state) {
         this.newState = state;
      }

      public BlockState getOriginalState() {
         return this.origState;
      }
   }

   public static class NeighborNotifyEvent extends BlockEvent implements ICancellableEvent {
      private final EnumSet<Direction> notifiedSides;
      private final boolean forceRedstoneUpdate;

      public NeighborNotifyEvent(Level level, BlockPos pos, BlockState state, EnumSet<Direction> notifiedSides, boolean forceRedstoneUpdate) {
         super(level, pos, state);
         this.notifiedSides = notifiedSides;
         this.forceRedstoneUpdate = forceRedstoneUpdate;
      }

      public EnumSet<Direction> getNotifiedSides() {
         return this.notifiedSides;
      }

      public boolean getForceRedstoneUpdate() {
         return this.forceRedstoneUpdate;
      }
   }

   public static class PortalSpawnEvent extends BlockEvent implements ICancellableEvent {
      private final PortalShape size;

      public PortalSpawnEvent(LevelAccessor level, BlockPos pos, BlockState state, PortalShape size) {
         super(level, pos, state);
         this.size = size;
      }

      public PortalShape getPortalSize() {
         return this.size;
      }
   }
}

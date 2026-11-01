package net.minecraft.world.level.block;

import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class FlowerPotBlock extends Block {
   public static final MapCodec<FlowerPotBlock> CODEC = RecordCodecBuilder.mapCodec(
      p_344655_ -> p_344655_.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("potted").forGetter(p_304928_ -> p_304928_.potted), propertiesCodec())
         .apply(p_344655_, FlowerPotBlock::new)
   );
   private static final Map<Block, Block> POTTED_BY_CONTENT = Maps.newHashMap();
   public static final float AABB_SIZE = 3.0F;
   protected static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 6.0, 11.0);
   private final Block potted = null;
   private final Map<ResourceLocation, Supplier<? extends Block>> fullPots;
   @Nullable
   private final Supplier<FlowerPotBlock> emptyPot;
   private final Supplier<? extends Block> flowerDelegate;

   @Override
   public MapCodec<FlowerPotBlock> codec() {
      return CODEC;
   }

   @Deprecated
   public FlowerPotBlock(Block p_53528_, BlockBehaviour.Properties p_53529_) {
      this(Blocks.FLOWER_POT == null ? null : () -> (FlowerPotBlock)Blocks.FLOWER_POT, () -> p_53528_, p_53529_);
      if (Blocks.FLOWER_POT != null) {
         ((FlowerPotBlock)Blocks.FLOWER_POT).addPlant(BuiltInRegistries.BLOCK.getKey(p_53528_), () -> this);
      }
   }

   public FlowerPotBlock(@Nullable Supplier<FlowerPotBlock> emptyPot, Supplier<? extends Block> p_53528_, BlockBehaviour.Properties properties) {
      super(properties);
      this.flowerDelegate = p_53528_;
      if (emptyPot == null) {
         this.fullPots = Maps.newHashMap();
         this.emptyPot = null;
      } else {
         this.fullPots = Collections.emptyMap();
         this.emptyPot = emptyPot;
      }
   }

   @Override
   protected VoxelShape getShape(BlockState p_53556_, BlockGetter p_53557_, BlockPos p_53558_, CollisionContext p_53559_) {
      return SHAPE;
   }

   @Override
   protected ItemInteractionResult useItemOn(
      ItemStack p_316610_, BlockState p_316240_, Level p_316456_, BlockPos p_316502_, Player p_316491_, InteractionHand p_316444_, BlockHitResult p_316826_
   ) {
      BlockState blockstate = (p_316610_.getItem() instanceof BlockItem blockitem
            ? this.getEmptyPot().fullPots.getOrDefault(BuiltInRegistries.BLOCK.getKey(blockitem.getBlock()), () -> Blocks.AIR).get()
            : Blocks.AIR)
         .defaultBlockState();
      if (blockstate.isAir()) {
         return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
      } else if (!this.isEmpty()) {
         return ItemInteractionResult.CONSUME;
      } else {
         p_316456_.setBlock(p_316502_, blockstate, 3);
         p_316456_.gameEvent(p_316491_, GameEvent.BLOCK_CHANGE, p_316502_);
         p_316491_.awardStat(Stats.POT_FLOWER);
         p_316610_.consume(1, p_316491_);
         return ItemInteractionResult.sidedSuccess(p_316456_.isClientSide);
      }
   }

   @Override
   protected InteractionResult useWithoutItem(BlockState p_316363_, Level p_316655_, BlockPos p_316654_, Player p_316338_, BlockHitResult p_316518_) {
      if (this.isEmpty()) {
         return InteractionResult.CONSUME;
      } else {
         ItemStack itemstack = new ItemStack(this.potted);
         if (!p_316338_.addItem(itemstack)) {
            p_316338_.drop(itemstack, false);
         }

         p_316655_.setBlock(p_316654_, this.getEmptyPot().defaultBlockState(), 3);
         p_316655_.gameEvent(p_316338_, GameEvent.BLOCK_CHANGE, p_316654_);
         return InteractionResult.sidedSuccess(p_316655_.isClientSide);
      }
   }

   @Override
   public ItemStack getCloneItemStack(LevelReader p_304505_, BlockPos p_53532_, BlockState p_53533_) {
      return this.isEmpty() ? super.getCloneItemStack(p_304505_, p_53532_, p_53533_) : new ItemStack(this.potted);
   }

   private boolean isEmpty() {
      return this.potted == Blocks.AIR;
   }

   @Override
   protected BlockState updateShape(BlockState p_53547_, Direction p_53548_, BlockState p_53549_, LevelAccessor p_53550_, BlockPos p_53551_, BlockPos p_53552_) {
      return p_53548_ == Direction.DOWN && !p_53547_.canSurvive(p_53550_, p_53551_)
         ? Blocks.AIR.defaultBlockState()
         : super.updateShape(p_53547_, p_53548_, p_53549_, p_53550_, p_53551_, p_53552_);
   }

   public Block getPotted() {
      return this.flowerDelegate.get();
   }

   @Override
   protected boolean isPathfindable(BlockState p_53535_, PathComputationType p_53538_) {
      return false;
   }

   public FlowerPotBlock getEmptyPot() {
      return this.emptyPot == null ? this : this.emptyPot.get();
   }

   public void addPlant(ResourceLocation flower, Supplier<? extends Block> fullPot) {
      if (this.getEmptyPot() != this) {
         throw new IllegalArgumentException("Cannot add plant to non-empty pot: " + this + " (Please call addPlant on the empty pot instead)");
      } else {
         this.fullPots.put(flower, fullPot);
      }
   }

   public Map<ResourceLocation, Supplier<? extends Block>> getFullPotsView() {
      return Collections.unmodifiableMap(this.fullPots);
   }
}

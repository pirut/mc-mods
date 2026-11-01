package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Objects;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class BrushableBlockEntity extends BlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String LOOT_TABLE_TAG = "LootTable";
   private static final String LOOT_TABLE_SEED_TAG = "LootTableSeed";
   private static final String HIT_DIRECTION_TAG = "hit_direction";
   private static final String ITEM_TAG = "item";
   private static final int BRUSH_COOLDOWN_TICKS = 10;
   private static final int BRUSH_RESET_TICKS = 40;
   private static final int REQUIRED_BRUSHES_TO_BREAK = 10;
   private int brushCount;
   private long brushCountResetsAtTick;
   private long coolDownEndsAtTick;
   private ItemStack item = ItemStack.EMPTY;
   @Nullable
   private Direction hitDirection;
   @Nullable
   private ResourceKey<LootTable> lootTable;
   private long lootTableSeed;

   public BrushableBlockEntity(BlockPos pos, BlockState blockState) {
      super(BlockEntityType.BRUSHABLE_BLOCK, pos, blockState);
   }

   public boolean brush(long startTick, ServerLevel level, LivingEntity brusher, Direction hitDirection, ItemStack stack) {
      if (this.hitDirection == null) {
         this.hitDirection = hitDirection;
      }

      this.brushCountResetsAtTick = startTick + 40L;
      if (startTick < this.coolDownEndsAtTick) {
         return false;
      } else {
         this.coolDownEndsAtTick = startTick + 10L;
         this.unpackLootTable(level, brusher, stack);
         int i = this.getCompletionState();
         if (++this.brushCount >= 10) {
            this.brushingCompleted(level, brusher, stack);
            return true;
         } else {
            level.scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 2);
            int j = this.getCompletionState();
            if (i != j) {
               BlockState blockState = this.getBlockState();
               BlockState blockState2 = blockState.setValue(BlockStateProperties.DUSTED, j);
               level.setBlock(this.getBlockPos(), blockState2, 3);
            }

            return false;
         }
      }
   }

   private void unpackLootTable(ServerLevel level, LivingEntity brusher, ItemStack stack) {
      if (this.lootTable != null) {
         LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(this.lootTable);
         if (brusher instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.GENERATE_LOOT.trigger(serverPlayer, this.lootTable);
         }

         LootParams lootParams = new LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
            .withLuck(brusher.getLuck())
            .withParameter(LootContextParams.THIS_ENTITY, brusher)
            .withParameter(LootContextParams.TOOL, stack)
            .create(LootContextParamSets.ARCHAEOLOGY);
         ObjectArrayList<ItemStack> objectArrayList = lootTable.getRandomItems(lootParams, this.lootTableSeed);

         this.item = switch (objectArrayList.size()) {
            case 0 -> ItemStack.EMPTY;
            case 1 -> (ItemStack)objectArrayList.getFirst();
            default -> {
               LOGGER.warn("Expected max 1 loot from loot table {}, but got {}", this.lootTable.identifier(), objectArrayList.size());
               yield (ItemStack)objectArrayList.getFirst();
            }
         };
         this.lootTable = null;
         this.setChanged();
      }
   }

   private void brushingCompleted(ServerLevel level, LivingEntity brusher, ItemStack stack) {
      this.dropContent(level, brusher, stack);
      BlockState blockState = this.getBlockState();
      level.levelEvent(3008, this.getBlockPos(), Block.getId(blockState));
      Block block2;
      if (this.getBlockState().getBlock() instanceof BrushableBlock brushableBlock) {
         block2 = brushableBlock.getTurnsInto();
      } else {
         block2 = Blocks.AIR;
      }

      level.setBlock(this.worldPosition, block2.defaultBlockState(), 3);
   }

   private void dropContent(ServerLevel level, LivingEntity brusher, ItemStack stack) {
      this.unpackLootTable(level, brusher, stack);
      if (!this.item.isEmpty()) {
         double d = EntityType.ITEM.getWidth();
         double e = 1.0 - d;
         double f = d / 2.0;
         Direction direction = Objects.requireNonNullElse(this.hitDirection, Direction.UP);
         BlockPos blockPos = this.worldPosition.relative(direction, 1);
         double g = blockPos.getX() + 0.5 * e + f;
         double h = blockPos.getY() + 0.5 + EntityType.ITEM.getHeight() / 2.0F;
         double i = blockPos.getZ() + 0.5 * e + f;
         ItemEntity itemEntity = new ItemEntity(level, g, h, i, this.item.split(level.random.nextInt(21) + 10));
         itemEntity.setDeltaMovement(Vec3.ZERO);
         level.addFreshEntity(itemEntity);
         this.item = ItemStack.EMPTY;
      }
   }

   public void checkReset(ServerLevel level) {
      if (this.brushCount != 0 && level.getGameTime() >= this.brushCountResetsAtTick) {
         int i = this.getCompletionState();
         this.brushCount = Math.max(0, this.brushCount - 2);
         int j = this.getCompletionState();
         if (i != j) {
            level.setBlock(this.getBlockPos(), this.getBlockState().setValue(BlockStateProperties.DUSTED, j), 3);
         }

         int k = 4;
         this.brushCountResetsAtTick = level.getGameTime() + 4L;
      }

      if (this.brushCount == 0) {
         this.hitDirection = null;
         this.brushCountResetsAtTick = 0L;
         this.coolDownEndsAtTick = 0L;
      } else {
         level.scheduleTick(this.getBlockPos(), this.getBlockState().getBlock(), 2);
      }
   }

   private boolean tryLoadLootTable(ValueInput input) {
      this.lootTable = input.<ResourceKey<LootTable>>read("LootTable", LootTable.KEY_CODEC).orElse(null);
      this.lootTableSeed = input.getLongOr("LootTableSeed", 0L);
      return this.lootTable != null;
   }

   private boolean trySaveLootTable(ValueOutput output) {
      if (this.lootTable == null) {
         return false;
      } else {
         output.store("LootTable", LootTable.KEY_CODEC, this.lootTable);
         if (this.lootTableSeed != 0L) {
            output.putLong("LootTableSeed", this.lootTableSeed);
         }

         return true;
      }
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      CompoundTag compoundTag = super.getUpdateTag(registries);
      compoundTag.storeNullable("hit_direction", Direction.LEGACY_ID_CODEC, this.hitDirection);
      if (!this.item.isEmpty()) {
         RegistryOps<Tag> registryOps = registries.createSerializationContext(NbtOps.INSTANCE);
         compoundTag.store("item", ItemStack.CODEC, registryOps, this.item);
      }

      return compoundTag;
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      if (!this.tryLoadLootTable(input)) {
         this.item = input.<ItemStack>read("item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
      } else {
         this.item = ItemStack.EMPTY;
      }

      this.hitDirection = input.<Direction>read("hit_direction", Direction.LEGACY_ID_CODEC).orElse(null);
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      if (!this.trySaveLootTable(output) && !this.item.isEmpty()) {
         output.store("item", ItemStack.CODEC, this.item);
      }
   }

   public void setLootTable(ResourceKey<LootTable> lootTable, long seed) {
      this.lootTable = lootTable;
      this.lootTableSeed = seed;
   }

   private int getCompletionState() {
      if (this.brushCount == 0) {
         return 0;
      } else if (this.brushCount < 3) {
         return 1;
      } else {
         return this.brushCount < 6 ? 2 : 3;
      }
   }

   @Nullable
   public Direction getHitDirection() {
      return this.hitDirection;
   }

   public ItemStack getItem() {
      return this.item;
   }
}

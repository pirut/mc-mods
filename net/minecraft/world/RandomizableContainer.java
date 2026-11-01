package net.minecraft.world;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public interface RandomizableContainer extends Container {
   String LOOT_TABLE_TAG = "LootTable";
   String LOOT_TABLE_SEED_TAG = "LootTableSeed";

   @Nullable
   ResourceKey<LootTable> getLootTable();

   void setLootTable(@Nullable ResourceKey<LootTable> var1);

   default void setLootTable(ResourceKey<LootTable> p_335762_, long p_335967_) {
      this.setLootTable(p_335762_);
      this.setLootTableSeed(p_335967_);
   }

   long getLootTableSeed();

   void setLootTableSeed(long var1);

   BlockPos getBlockPos();

   @Nullable
   Level getLevel();

   static void setBlockEntityLootTable(BlockGetter p_309623_, RandomSource p_309643_, BlockPos p_309644_, ResourceKey<LootTable> p_335924_) {
      if (p_309623_.getBlockEntity(p_309644_) instanceof RandomizableContainer randomizablecontainer) {
         randomizablecontainer.setLootTable(p_335924_, p_309643_.nextLong());
      }
   }

   default boolean tryLoadLootTable(ValueInput p_421668_) {
      ResourceKey<LootTable> resourcekey = p_421668_.<ResourceKey<LootTable>>read("LootTable", LootTable.KEY_CODEC).orElse(null);
      this.setLootTable(resourcekey);
      this.setLootTableSeed(p_421668_.getLongOr("LootTableSeed", 0L));
      return resourcekey != null;
   }

   default boolean trySaveLootTable(ValueOutput p_422271_) {
      ResourceKey<LootTable> resourcekey = this.getLootTable();
      if (resourcekey == null) {
         return false;
      } else {
         p_422271_.store("LootTable", LootTable.KEY_CODEC, resourcekey);
         long i = this.getLootTableSeed();
         if (i != 0L) {
            p_422271_.putLong("LootTableSeed", i);
         }

         return true;
      }
   }

   default void unpackLootTable(@Nullable Player p_309628_) {
      Level level = this.getLevel();
      BlockPos blockpos = this.getBlockPos();
      ResourceKey<LootTable> resourcekey = this.getLootTable();
      if (resourcekey != null && level != null && level.getServer() != null) {
         LootTable loottable = level.getServer().reloadableRegistries().getLootTable(resourcekey);
         if (p_309628_ instanceof ServerPlayer) {
            CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer)p_309628_, resourcekey);
         }

         this.setLootTable(null);
         LootParams.Builder lootparams$builder = new LootParams.Builder((ServerLevel)level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockpos));
         if (p_309628_ != null) {
            lootparams$builder.withLuck(p_309628_.getLuck()).withParameter(LootContextParams.THIS_ENTITY, p_309628_);
         }

         loottable.fill(this, lootparams$builder.create(LootContextParamSets.CHEST), this.getLootTableSeed());
      }
   }
}

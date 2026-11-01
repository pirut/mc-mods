package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import javax.annotation.Nullable;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;
import org.slf4j.Logger;

public class AppendLoot implements RuleBlockEntityModifier {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final MapCodec<AppendLoot> CODEC = RecordCodecBuilder.mapCodec(
      p_335310_ -> p_335310_.group(ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("loot_table").forGetter(p_335311_ -> p_335311_.lootTable))
         .apply(p_335310_, AppendLoot::new)
   );
   private final ResourceKey<LootTable> lootTable;

   public AppendLoot(ResourceKey<LootTable> p_335426_) {
      this.lootTable = p_335426_;
   }

   @Override
   public CompoundTag apply(RandomSource p_277994_, @Nullable CompoundTag p_277854_) {
      CompoundTag $$2 = p_277854_ == null ? new CompoundTag() : p_277854_.copy();
      ResourceKey.codec(Registries.LOOT_TABLE)
         .encodeStart(NbtOps.INSTANCE, this.lootTable)
         .resultOrPartial(LOGGER::error)
         .ifPresent(p_277353_ -> $$2.put("LootTable", p_277353_));
      $$2.putLong("LootTableSeed", p_277994_.nextLong());
      return $$2;
   }

   @Override
   public RuleBlockEntityModifierType<?> getType() {
      return RuleBlockEntityModifierType.APPEND_LOOT;
   }
}

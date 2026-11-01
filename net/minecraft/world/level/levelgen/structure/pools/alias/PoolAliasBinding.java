package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public interface PoolAliasBinding {
   Codec<PoolAliasBinding> CODEC = BuiltInRegistries.POOL_ALIAS_BINDING_TYPE.byNameCodec().dispatch(PoolAliasBinding::codec, Function.identity());

   void forEachResolved(RandomSource var1, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> var2);

   Stream<ResourceKey<StructureTemplatePool>> allTargets();

   static DirectPoolAlias direct(String p_307227_, String p_307334_) {
      return direct(Pools.createKey(p_307227_), Pools.createKey(p_307334_));
   }

   static DirectPoolAlias direct(ResourceKey<StructureTemplatePool> p_307379_, ResourceKey<StructureTemplatePool> p_307242_) {
      return new DirectPoolAlias(p_307379_, p_307242_);
   }

   static RandomPoolAlias random(String p_405266_, WeightedList<String> p_393660_) {
      WeightedList.Builder<ResourceKey<StructureTemplatePool>> builder = WeightedList.builder();
      p_393660_.unwrap().forEach(p_393440_ -> builder.add(Pools.createKey(p_393440_.value()), p_393440_.weight()));
      return random(Pools.createKey(p_405266_), builder.build());
   }

   static RandomPoolAlias random(ResourceKey<StructureTemplatePool> p_405147_, WeightedList<ResourceKey<StructureTemplatePool>> p_393832_) {
      return new RandomPoolAlias(p_405147_, p_393832_);
   }

   static RandomGroupPoolAlias randomGroup(WeightedList<List<PoolAliasBinding>> p_393469_) {
      return new RandomGroupPoolAlias(p_393469_);
   }

   MapCodec<? extends PoolAliasBinding> codec();
}

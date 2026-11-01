package net.neoforged.neoforge.common.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.WithConditions;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public interface IGlobalLootModifier {
   Codec<IGlobalLootModifier> DIRECT_CODEC = NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS
      .byNameCodec()
      .dispatch(IGlobalLootModifier::codec, Function.identity());
   Codec<Optional<WithConditions<IGlobalLootModifier>>> CONDITIONAL_CODEC = ConditionalOps.createConditionalCodecWithConditions(DIRECT_CODEC);
   Codec<LootItemCondition[]> LOOT_CONDITIONS_CODEC = LootItemCondition.DIRECT_CODEC.listOf().xmap(list -> list.toArray(LootItemCondition[]::new), List::of);

   ObjectArrayList<ItemStack> apply(ObjectArrayList<ItemStack> var1, LootContext var2);

   MapCodec<? extends IGlobalLootModifier> codec();
}

package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class DynamicLoot extends LootPoolSingletonContainer {
   public static final MapCodec<DynamicLoot> CODEC = RecordCodecBuilder.mapCodec(
      p_466782_ -> p_466782_.group(Identifier.CODEC.fieldOf("name").forGetter(p_466783_ -> p_466783_.name))
         .and(singletonFields(p_466782_))
         .apply(p_466782_, DynamicLoot::new)
   );
   private final Identifier name;

   private DynamicLoot(Identifier p_467156_, int p_79466_, int p_79467_, List<LootItemCondition> p_299033_, List<LootItemFunction> p_298474_) {
      super(p_79466_, p_79467_, p_299033_, p_298474_);
      this.name = p_467156_;
   }

   @Override
   public LootPoolEntryType getType() {
      return LootPoolEntries.DYNAMIC;
   }

   @Override
   public void createItemStack(Consumer<ItemStack> p_79481_, LootContext p_79482_) {
      p_79482_.addDynamicDrops(this.name, p_79481_);
   }

   public static LootPoolSingletonContainer.Builder<?> dynamicEntry(Identifier p_468081_) {
      return simpleBuilder((p_466785_, p_466786_, p_466787_, p_466788_) -> new DynamicLoot(p_468081_, p_466785_, p_466786_, p_466787_, p_466788_));
   }
}

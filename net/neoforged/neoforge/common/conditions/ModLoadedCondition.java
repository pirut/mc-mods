package net.neoforged.neoforge.common.conditions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.fml.ModList;

public record ModLoadedCondition(String modid) implements ICondition {
   public static MapCodec<ModLoadedCondition> CODEC = RecordCodecBuilder.mapCodec(
      builder -> builder.group(Codec.STRING.fieldOf("modid").forGetter(ModLoadedCondition::modid)).apply(builder, ModLoadedCondition::new)
   );

   @Override
   public boolean test(ICondition.IContext context) {
      return ModList.get().isLoaded(this.modid);
   }

   @Override
   public MapCodec<? extends ICondition> codec() {
      return CODEC;
   }

   @Override
   public String toString() {
      return "mod_loaded(\"" + this.modid + "\")";
   }
}

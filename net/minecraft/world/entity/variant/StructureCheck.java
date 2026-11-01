package net.minecraft.world.entity.variant;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.Structure;

public record StructureCheck(HolderSet<Structure> requiredStructures) implements SpawnCondition {
   public static final MapCodec<StructureCheck> MAP_CODEC = RecordCodecBuilder.mapCodec(
      p_399769_ -> p_399769_.group(RegistryCodecs.homogeneousList(Registries.STRUCTURE).fieldOf("structures").forGetter(StructureCheck::requiredStructures))
         .apply(p_399769_, StructureCheck::new)
   );

   public boolean test(SpawnContext p_400042_) {
      return p_400042_.level().getLevel().structureManager().getStructureWithPieceAt(p_400042_.pos(), this.requiredStructures).isValid();
   }

   @Override
   public MapCodec<StructureCheck> codec() {
      return MAP_CODEC;
   }
}

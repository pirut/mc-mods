package net.neoforged.neoforge.common.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public interface StructureModifier {
   Codec<StructureModifier> DIRECT_CODEC = NeoForgeRegistries.STRUCTURE_MODIFIER_SERIALIZERS
      .byNameCodec()
      .dispatch(StructureModifier::codec, Function.identity());
   Codec<Holder<StructureModifier>> REFERENCE_CODEC = RegistryFileCodec.create(NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS, DIRECT_CODEC);
   Codec<HolderSet<StructureModifier>> LIST_CODEC = RegistryCodecs.homogeneousList(NeoForgeRegistries.Keys.STRUCTURE_MODIFIERS, DIRECT_CODEC);

   void modify(Holder<Structure> var1, StructureModifier.Phase var2, ModifiableStructureInfo.StructureInfo.Builder var3);

   MapCodec<? extends StructureModifier> codec();

   public static enum Phase {
      BEFORE_EVERYTHING,
      ADD,
      REMOVE,
      MODIFY,
      AFTER_EVERYTHING;
   }
}

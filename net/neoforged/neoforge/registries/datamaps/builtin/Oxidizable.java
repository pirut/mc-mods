package net.neoforged.neoforge.registries.datamaps.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public record Oxidizable(Block nextOxidationStage) {
   public static final Codec<Oxidizable> OXIDIZABLE_CODEC = BuiltInRegistries.BLOCK.byNameCodec().xmap(Oxidizable::new, Oxidizable::nextOxidationStage);
   public static final Codec<Oxidizable> CODEC = Codec.withAlternative(
      RecordCodecBuilder.create(
         in -> in.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("next_oxidation_stage").forGetter(Oxidizable::nextOxidationStage))
            .apply(in, Oxidizable::new)
      ),
      OXIDIZABLE_CODEC
   );
}

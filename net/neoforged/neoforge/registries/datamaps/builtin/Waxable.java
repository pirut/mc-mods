package net.neoforged.neoforge.registries.datamaps.builtin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

public record Waxable(Block waxed) {
   public static final Codec<Waxable> WAXABLE_CODEC = BuiltInRegistries.BLOCK.byNameCodec().xmap(Waxable::new, Waxable::waxed);
   public static final Codec<Waxable> CODEC = Codec.withAlternative(
      RecordCodecBuilder.create(in -> in.group(BuiltInRegistries.BLOCK.byNameCodec().fieldOf("waxed").forGetter(Waxable::waxed)).apply(in, Waxable::new)),
      WAXABLE_CODEC
   );
}

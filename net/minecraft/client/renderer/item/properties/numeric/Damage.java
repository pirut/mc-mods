package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record Damage(boolean normalize) implements RangeSelectItemModelProperty {
   public static final MapCodec<Damage> MAP_CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(Codec.BOOL.optionalFieldOf("normalize", true).forGetter(Damage::normalize)).apply(instance, Damage::new)
   );

   @Override
   public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      float f = stack.getDamageValue();
      float g = stack.getMaxDamage();
      return this.normalize ? Mth.clamp(f / g, 0.0F, 1.0F) : Mth.clamp(f, 0.0F, g);
   }

   @Override
   public MapCodec<Damage> type() {
      return MAP_CODEC;
   }
}

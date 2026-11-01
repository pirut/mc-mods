package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class CompassAngle implements RangeSelectItemModelProperty {
   public static final MapCodec<CompassAngle> MAP_CODEC = CompassAngleState.MAP_CODEC.xmap(CompassAngle::new, compassAngle -> compassAngle.state);
   private final CompassAngleState state;

   public CompassAngle(boolean wobble, CompassAngleState.CompassTarget compassTarget) {
      this(new CompassAngleState(wobble, compassTarget));
   }

   private CompassAngle(CompassAngleState state) {
      this.state = state;
   }

   @Override
   public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
      return this.state.get(stack, level, owner, seed);
   }

   @Override
   public MapCodec<CompassAngle> type() {
      return MAP_CODEC;
   }
}

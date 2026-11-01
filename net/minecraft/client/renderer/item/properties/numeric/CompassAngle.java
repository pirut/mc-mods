package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CompassAngle implements RangeSelectItemModelProperty {
   public static final MapCodec<CompassAngle> MAP_CODEC = CompassAngleState.MAP_CODEC.xmap(CompassAngle::new, p_388035_ -> p_388035_.state);
   private final CompassAngleState state;

   public CompassAngle(boolean p_387575_, CompassAngleState.CompassTarget p_388890_) {
      this(new CompassAngleState(p_387575_, p_388890_));
   }

   private CompassAngle(CompassAngleState p_388477_) {
      this.state = p_388477_;
   }

   @Override
   public float get(ItemStack p_387228_, @Nullable ClientLevel p_386952_, @Nullable ItemOwner p_434216_, int p_387210_) {
      return this.state.get(p_387228_, p_386952_, p_434216_, p_387210_);
   }

   @Override
   public MapCodec<CompassAngle> type() {
      return MAP_CODEC;
   }
}

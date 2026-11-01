package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.Display;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockDisplayEntityRenderState extends DisplayEntityRenderState {
   @Nullable
   public Display.BlockDisplay.BlockRenderState blockRenderState;

   @Override
   public boolean hasSubState() {
      return this.blockRenderState != null;
   }
}

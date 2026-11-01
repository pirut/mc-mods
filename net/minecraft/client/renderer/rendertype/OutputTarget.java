package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OutputTarget {
   private final String name;
   private final Supplier<RenderTarget> renderTargetSupplier;
   public static final OutputTarget MAIN_TARGET = new OutputTarget("main_target", () -> Minecraft.getInstance().getMainRenderTarget());
   public static final OutputTarget OUTLINE_TARGET = new OutputTarget("outline_target", () -> Minecraft.getInstance().levelRenderer.entityOutlineTarget());
   public static final OutputTarget WEATHER_TARGET = new OutputTarget("weather_target", () -> Minecraft.getInstance().levelRenderer.getWeatherTarget());
   public static final OutputTarget ITEM_ENTITY_TARGET = new OutputTarget(
      "item_entity_target", () -> Minecraft.getInstance().levelRenderer.getItemEntityTarget()
   );

   public OutputTarget(String p_459026_, Supplier<RenderTarget> p_459185_) {
      this.name = p_459026_;
      this.renderTargetSupplier = p_459185_;
   }

   public RenderTarget getRenderTarget() {
      RenderTarget rendertarget = this.renderTargetSupplier.get();
      return rendertarget != null ? rendertarget : Minecraft.getInstance().getMainRenderTarget();
   }

   @Override
   public String toString() {
      return "OutputTarget[" + this.name + "]";
   }
}

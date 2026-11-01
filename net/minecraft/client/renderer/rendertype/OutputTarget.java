package net.minecraft.client.renderer.rendertype;

import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class OutputTarget {
   private final String name;
   private final Supplier<RenderTarget> renderTargetSupplier;
   public static final OutputTarget MAIN_TARGET = new OutputTarget("main_target", () -> Minecraft.getInstance().getMainRenderTarget());
   public static final OutputTarget OUTLINE_TARGET = new OutputTarget("outline_target", () -> Minecraft.getInstance().levelRenderer.entityOutlineTarget());
   public static final OutputTarget WEATHER_TARGET = new OutputTarget("weather_target", () -> Minecraft.getInstance().levelRenderer.getWeatherTarget());
   public static final OutputTarget ITEM_ENTITY_TARGET = new OutputTarget(
      "item_entity_target", () -> Minecraft.getInstance().levelRenderer.getItemEntityTarget()
   );

   public OutputTarget(String name, Supplier<RenderTarget> renderTargetSupplier) {
      this.name = name;
      this.renderTargetSupplier = renderTargetSupplier;
   }

   public RenderTarget getRenderTarget() {
      RenderTarget renderTarget = this.renderTargetSupplier.get();
      return renderTarget != null ? renderTarget : Minecraft.getInstance().getMainRenderTarget();
   }

   @Override
   public String toString() {
      return "OutputTarget[" + this.name + "]";
   }
}

package net.neoforged.neoforge.client.event;

import java.util.Map;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterSpecialBlockModelRendererEvent extends Event implements IModBusEvent {
   private final Map<Block, SpecialModelRenderer.Unbaked> renderers;

   @Internal
   public RegisterSpecialBlockModelRendererEvent(Map<Block, SpecialModelRenderer.Unbaked> renderers) {
      this.renderers = renderers;
   }

   public void register(Block block, SpecialModelRenderer.Unbaked unbakedRenderer) {
      SpecialModelRenderer.Unbaked prev = this.renderers.putIfAbsent(block, unbakedRenderer);
      if (prev != null) {
         throw new IllegalStateException("Duplicate block model renderer registration for " + block);
      }
   }
}

package net.neoforged.neoforge.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class GuiLayerManager {
   public static final float Z_SEPARATION = 200.0F;
   private final List<GuiLayerManager.NamedLayer> layers = new ArrayList<>();
   private boolean initialized = false;

   public GuiLayerManager add(ResourceLocation name, LayeredDraw.Layer layer) {
      this.layers.add(new GuiLayerManager.NamedLayer(name, layer));
      return this;
   }

   public GuiLayerManager add(GuiLayerManager child, BooleanSupplier shouldRender) {
      for (GuiLayerManager.NamedLayer entry : child.layers) {
         this.add(entry.name(), (guiGraphics, partialTick) -> {
            if (shouldRender.getAsBoolean()) {
               entry.layer().render(guiGraphics, partialTick);
            }
         });
      }

      return this;
   }

   public void render(GuiGraphics guiGraphics, DeltaTracker partialTick) {
      if (!((RenderGuiEvent.Pre)NeoForge.EVENT_BUS.post(new RenderGuiEvent.Pre(guiGraphics, partialTick))).isCanceled()) {
         this.renderInner(guiGraphics, partialTick);
         NeoForge.EVENT_BUS.post(new RenderGuiEvent.Post(guiGraphics, partialTick));
      }
   }

   private void renderInner(GuiGraphics guiGraphics, DeltaTracker partialTick) {
      guiGraphics.pose().pushPose();

      for (GuiLayerManager.NamedLayer layer : this.layers) {
         if (!((RenderGuiLayerEvent.Pre)NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Pre(guiGraphics, partialTick, layer.name(), layer.layer())))
            .isCanceled()) {
            layer.layer().render(guiGraphics, partialTick);
            NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Post(guiGraphics, partialTick, layer.name(), layer.layer()));
         }

         guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
      }

      guiGraphics.pose().popPose();
   }

   public void initModdedLayers() {
      if (this.initialized) {
         throw new IllegalStateException("Duplicate initialization of NamedLayeredDraw");
      } else {
         this.initialized = true;
         ModLoader.postEvent(new RegisterGuiLayersEvent(this.layers));
      }
   }

   public int getLayerCount() {
      return this.layers.size();
   }

   public record NamedLayer(ResourceLocation name, LayeredDraw.Layer layer) {
   }
}

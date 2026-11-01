package net.neoforged.neoforge.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class GuiLayerManager {
   private final List<GuiLayerManager.NamedLayer> layers = new ArrayList<>();
   private boolean initialized = false;

   public GuiLayerManager add(Identifier name, GuiLayer layer) {
      this.layers.add(new GuiLayerManager.NamedLayer(name, layer));
      return this;
   }

   public GuiLayerManager add(Identifier name, Consumer<GuiGraphics> layer, BooleanSupplier shouldRender) {
      this.add(name, (guiGraphics, deltaTracker) -> layer.accept(guiGraphics), shouldRender);
      return this;
   }

   public GuiLayerManager add(Identifier name, GuiLayer layer, BooleanSupplier shouldRender) {
      this.layers.add(new GuiLayerManager.NamedLayer(name, (guiGraphics, deltaTracker) -> {
         if (shouldRender.getAsBoolean()) {
            layer.render(guiGraphics, deltaTracker);
         }
      }));
      return this;
   }

   public GuiLayerManager add(GuiLayerManager child, BooleanSupplier shouldRender) {
      for (GuiLayerManager.NamedLayer entry : child.layers) {
         this.add(entry.name(), entry.layer, shouldRender);
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
      for (GuiLayerManager.NamedLayer layer : this.layers) {
         if (!((RenderGuiLayerEvent.Pre)NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Pre(guiGraphics, partialTick, layer.name(), layer.layer())))
            .isCanceled()) {
            layer.layer().render(guiGraphics, partialTick);
            NeoForge.EVENT_BUS.post(new RenderGuiLayerEvent.Post(guiGraphics, partialTick, layer.name(), layer.layer()));
         }
      }
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

   public record NamedLayer(Identifier name, GuiLayer layer) {
   }
}

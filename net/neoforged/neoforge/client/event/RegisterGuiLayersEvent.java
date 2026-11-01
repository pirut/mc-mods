package net.neoforged.neoforge.client.event;

import com.google.common.base.Preconditions;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterGuiLayersEvent extends Event implements IModBusEvent {
   private final List<GuiLayerManager.NamedLayer> layers;

   @Internal
   public RegisterGuiLayersEvent(List<GuiLayerManager.NamedLayer> layers) {
      this.layers = layers;
   }

   public void registerBelowAll(ResourceLocation id, LayeredDraw.Layer layer) {
      this.register(RegisterGuiLayersEvent.Ordering.BEFORE, null, id, layer);
   }

   public void registerBelow(ResourceLocation other, ResourceLocation id, LayeredDraw.Layer layer) {
      this.register(RegisterGuiLayersEvent.Ordering.BEFORE, other, id, layer);
   }

   public void registerAbove(ResourceLocation other, ResourceLocation id, LayeredDraw.Layer layer) {
      this.register(RegisterGuiLayersEvent.Ordering.AFTER, other, id, layer);
   }

   public void registerAboveAll(ResourceLocation id, LayeredDraw.Layer layer) {
      this.register(RegisterGuiLayersEvent.Ordering.AFTER, null, id, layer);
   }

   public void replaceLayer(ResourceLocation id, LayeredDraw.Layer replacement) {
      this.wrapLayer(id, old -> replacement);
   }

   public void wrapLayer(ResourceLocation id, UnaryOperator<LayeredDraw.Layer> wrapper) {
      Objects.requireNonNull(id);

      for (int i = 0; i < this.layers.size(); i++) {
         GuiLayerManager.NamedLayer layer = this.layers.get(i);
         if (layer.name().equals(id)) {
            LayeredDraw.Layer wrapped = wrapper.apply(layer.layer());
            Objects.requireNonNull(wrapped, "wrapping layer must not be null");
            this.layers.set(i, new GuiLayerManager.NamedLayer(id, wrapped));
            return;
         }
      }

      throw new IllegalArgumentException("Attempted to wrap layer with id '" + id + "', which does not exist!");
   }

   private void register(RegisterGuiLayersEvent.Ordering ordering, @Nullable ResourceLocation other, ResourceLocation key, LayeredDraw.Layer layer) {
      Objects.requireNonNull(key);

      for (GuiLayerManager.NamedLayer namedLayer : this.layers) {
         Preconditions.checkArgument(!namedLayer.name().equals(key), "Layer already registered: " + key);
      }

      int insertPosition;
      if (other == null) {
         insertPosition = ordering == RegisterGuiLayersEvent.Ordering.BEFORE ? 0 : this.layers.size();
      } else {
         OptionalInt otherIndex = IntStream.range(0, this.layers.size()).filter(i -> this.layers.get(i).name().equals(other)).findFirst();
         if (otherIndex.isEmpty()) {
            throw new IllegalArgumentException("Attempted to order against an unregistered layer " + other + ". Only order against vanilla's and your own.");
         }

         insertPosition = otherIndex.getAsInt() + (ordering == RegisterGuiLayersEvent.Ordering.BEFORE ? 0 : 1);
      }

      this.layers.add(insertPosition, new GuiLayerManager.NamedLayer(key, layer));
   }

   private static enum Ordering {
      BEFORE,
      AFTER;
   }
}

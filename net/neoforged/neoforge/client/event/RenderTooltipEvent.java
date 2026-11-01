package net.neoforged.neoforge.client.event;

import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public abstract class RenderTooltipEvent extends Event {
   protected final ItemStack itemStack;
   protected final GuiGraphics graphics;
   protected int x;
   protected int y;
   protected Font font;
   protected final List<ClientTooltipComponent> components;

   @Internal
   protected RenderTooltipEvent(ItemStack itemStack, GuiGraphics graphics, int x, int y, Font font, List<ClientTooltipComponent> components) {
      this.itemStack = itemStack;
      this.graphics = graphics;
      this.components = Collections.unmodifiableList(components);
      this.x = x;
      this.y = y;
      this.font = font;
   }

   public ItemStack getItemStack() {
      return this.itemStack;
   }

   public GuiGraphics getGraphics() {
      return this.graphics;
   }

   public List<ClientTooltipComponent> getComponents() {
      return this.components;
   }

   public int getX() {
      return this.x;
   }

   public int getY() {
      return this.y;
   }

   public Font getFont() {
      return this.font;
   }

   public static class GatherComponents extends Event implements ICancellableEvent {
      private final ItemStack itemStack;
      private final int screenWidth;
      private final int screenHeight;
      private final List<Either<FormattedText, TooltipComponent>> tooltipElements;
      private int maxWidth;

      @Internal
      public GatherComponents(
         ItemStack itemStack, int screenWidth, int screenHeight, List<Either<FormattedText, TooltipComponent>> tooltipElements, int maxWidth
      ) {
         this.itemStack = itemStack;
         this.screenWidth = screenWidth;
         this.screenHeight = screenHeight;
         this.tooltipElements = (List<Either<FormattedText, TooltipComponent>>)(tooltipElements instanceof ArrayList
            ? tooltipElements
            : new ArrayList<>(tooltipElements));
         this.maxWidth = maxWidth;
      }

      public ItemStack getItemStack() {
         return this.itemStack;
      }

      public int getScreenWidth() {
         return this.screenWidth;
      }

      public int getScreenHeight() {
         return this.screenHeight;
      }

      public List<Either<FormattedText, TooltipComponent>> getTooltipElements() {
         return this.tooltipElements;
      }

      public int getMaxWidth() {
         return this.maxWidth;
      }

      public void setMaxWidth(int maxWidth) {
         this.maxWidth = maxWidth;
      }
   }

   public static class Pre extends RenderTooltipEvent implements ICancellableEvent {
      private final int screenWidth;
      private final int screenHeight;
      private final ClientTooltipPositioner positioner;

      @Internal
      public Pre(
         ItemStack stack,
         GuiGraphics graphics,
         int x,
         int y,
         int screenWidth,
         int screenHeight,
         Font font,
         List<ClientTooltipComponent> components,
         ClientTooltipPositioner positioner
      ) {
         super(stack, graphics, x, y, font, components);
         this.screenWidth = screenWidth;
         this.screenHeight = screenHeight;
         this.positioner = positioner;
      }

      public int getScreenWidth() {
         return this.screenWidth;
      }

      public int getScreenHeight() {
         return this.screenHeight;
      }

      public ClientTooltipPositioner getTooltipPositioner() {
         return this.positioner;
      }

      public void setFont(Font fr) {
         this.font = fr;
      }

      public void setX(int x) {
         this.x = x;
      }

      public void setY(int y) {
         this.y = y;
      }
   }

   public static class Texture extends RenderTooltipEvent {
      @Nullable
      private final Identifier originalTexture;
      @Nullable
      private Identifier texture;

      @Internal
      public Texture(ItemStack stack, GuiGraphics graphics, int x, int y, Font font, List<ClientTooltipComponent> components, @Nullable Identifier texture) {
         super(stack, graphics, x, y, font, components);
         this.originalTexture = texture;
         this.texture = texture;
      }

      @Nullable
      public Identifier getOriginalTexture() {
         return this.originalTexture;
      }

      @Nullable
      public Identifier getTexture() {
         return this.texture;
      }

      public void setTexture(@Nullable Identifier texture) {
         this.texture = texture;
      }
   }
}

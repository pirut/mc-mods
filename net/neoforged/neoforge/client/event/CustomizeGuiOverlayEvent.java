package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.platform.Window;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class CustomizeGuiOverlayEvent extends Event {
   private final Window window;
   private final GuiGraphics guiGraphics;
   private final DeltaTracker partialTick;

   @Internal
   protected CustomizeGuiOverlayEvent(Window window, GuiGraphics guiGraphics, DeltaTracker partialTick) {
      this.window = window;
      this.guiGraphics = guiGraphics;
      this.partialTick = partialTick;
   }

   public Window getWindow() {
      return this.window;
   }

   public GuiGraphics getGuiGraphics() {
      return this.guiGraphics;
   }

   public DeltaTracker getPartialTick() {
      return this.partialTick;
   }

   public static class BossEventProgress extends CustomizeGuiOverlayEvent implements ICancellableEvent {
      private final LerpingBossEvent bossEvent;
      private final int x;
      private final int y;
      private int increment;

      @Internal
      public BossEventProgress(Window window, GuiGraphics guiGraphics, DeltaTracker partialTick, LerpingBossEvent bossEvent, int x, int y, int increment) {
         super(window, guiGraphics, partialTick);
         this.bossEvent = bossEvent;
         this.x = x;
         this.y = y;
         this.increment = increment;
      }

      public LerpingBossEvent getBossEvent() {
         return this.bossEvent;
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public int getIncrement() {
         return this.increment;
      }

      public void setIncrement(int increment) {
         this.increment = increment;
      }
   }

   public static class Chat extends CustomizeGuiOverlayEvent {
      private int posX;
      private int posY;

      @Internal
      public Chat(Window window, GuiGraphics guiGraphics, DeltaTracker partialTick, int posX, int posY) {
         super(window, guiGraphics, partialTick);
         this.setPosX(posX);
         this.setPosY(posY);
      }

      public int getPosX() {
         return this.posX;
      }

      public void setPosX(int posX) {
         this.posX = posX;
      }

      public int getPosY() {
         return this.posY;
      }

      public void setPosY(int posY) {
         this.posY = posY;
      }
   }

   public static class DebugText extends CustomizeGuiOverlayEvent {
      private final List<String> left;
      private final List<String> right;

      @Internal
      public DebugText(Window window, GuiGraphics guiGraphics, DeltaTracker partialTick, List<String> left, List<String> right) {
         super(window, guiGraphics, partialTick);
         this.left = left;
         this.right = right;
      }

      public List<String> getLeft() {
         return this.left;
      }

      public List<String> getRight() {
         return this.right;
      }
   }
}

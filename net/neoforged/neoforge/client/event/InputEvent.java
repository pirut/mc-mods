package net.neoforged.neoforge.client.event;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public abstract class InputEvent extends Event {
   @Internal
   protected InputEvent() {
   }

   public static class InteractionKeyMappingTriggered extends InputEvent implements ICancellableEvent {
      private final int button;
      private final KeyMapping keyMapping;
      private final InteractionHand hand;
      private boolean handSwing = true;

      @Internal
      public InteractionKeyMappingTriggered(int button, KeyMapping keyMapping, InteractionHand hand) {
         this.button = button;
         this.keyMapping = keyMapping;
         this.hand = hand;
      }

      public void setSwingHand(boolean value) {
         this.handSwing = value;
      }

      public boolean shouldSwingHand() {
         return this.handSwing;
      }

      public InteractionHand getHand() {
         return this.hand;
      }

      public boolean isAttack() {
         return this.button == 0;
      }

      public boolean isUseItem() {
         return this.button == 1;
      }

      public boolean isPickBlock() {
         return this.button == 2;
      }

      public KeyMapping getKeyMapping() {
         return this.keyMapping;
      }
   }

   public static class Key extends InputEvent {
      private final KeyEvent keyEvent;
      private final int action;

      @Internal
      public Key(KeyEvent keyEvent, int action) {
         this.keyEvent = keyEvent;
         this.action = action;
      }

      public KeyEvent getKeyEvent() {
         return this.keyEvent;
      }

      public int getKey() {
         return this.keyEvent.key();
      }

      public int getScanCode() {
         return this.keyEvent.scancode();
      }

      public int getAction() {
         return this.action;
      }

      public int getModifiers() {
         return this.keyEvent.modifiers();
      }
   }

   public abstract static class MouseButton extends InputEvent {
      private final MouseButtonInfo mouseButtonInfo;
      private final int action;

      @Internal
      protected MouseButton(MouseButtonInfo mouseButtonInfo, int action) {
         this.mouseButtonInfo = mouseButtonInfo;
         this.action = action;
      }

      public MouseButtonInfo getMouseButtonInfo() {
         return this.mouseButtonInfo;
      }

      public int getButton() {
         return this.mouseButtonInfo.button();
      }

      public int getAction() {
         return this.action;
      }

      public int getModifiers() {
         return this.mouseButtonInfo.modifiers();
      }

      public static class Post extends InputEvent.MouseButton {
         @Internal
         public Post(MouseButtonInfo mouseButtonInfo, int action) {
            super(mouseButtonInfo, action);
         }
      }

      public static class Pre extends InputEvent.MouseButton implements ICancellableEvent {
         @Internal
         public Pre(MouseButtonInfo mouseButtonInfo, int action) {
            super(mouseButtonInfo, action);
         }
      }
   }

   public static class MouseScrollingEvent extends InputEvent implements ICancellableEvent {
      private final double scrollDeltaX;
      private final double scrollDeltaY;
      private final double mouseX;
      private final double mouseY;
      private final boolean leftDown;
      private final boolean middleDown;
      private final boolean rightDown;

      @Internal
      public MouseScrollingEvent(
         double scrollDeltaX, double scrollDeltaY, boolean leftDown, boolean middleDown, boolean rightDown, double mouseX, double mouseY
      ) {
         this.scrollDeltaX = scrollDeltaX;
         this.scrollDeltaY = scrollDeltaY;
         this.leftDown = leftDown;
         this.middleDown = middleDown;
         this.rightDown = rightDown;
         this.mouseX = mouseX;
         this.mouseY = mouseY;
      }

      public double getScrollDeltaX() {
         return this.scrollDeltaX;
      }

      public double getScrollDeltaY() {
         return this.scrollDeltaY;
      }

      public boolean isLeftDown() {
         return this.leftDown;
      }

      public boolean isRightDown() {
         return this.rightDown;
      }

      public boolean isMiddleDown() {
         return this.middleDown;
      }

      public double getMouseX() {
         return this.mouseX;
      }

      public double getMouseY() {
         return this.mouseY;
      }
   }
}

package net.neoforged.neoforge.client.event;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@OnlyIn(Dist.CLIENT)
public abstract class ScreenEvent extends Event {
   private final Screen screen;

   @Internal
   protected ScreenEvent(Screen screen) {
      this.screen = Objects.requireNonNull(screen);
   }

   public Screen getScreen() {
      return this.screen;
   }

   @Deprecated(
      forRemoval = true,
      since = "21.0"
   )
   public static class BackgroundRendered extends ScreenEvent {
      private final GuiGraphics guiGraphics;

      @Internal
      public BackgroundRendered(Screen screen, GuiGraphics guiGraphics) {
         super(screen);
         this.guiGraphics = guiGraphics;
      }

      public GuiGraphics getGuiGraphics() {
         return this.guiGraphics;
      }
   }

   public abstract static class CharacterTyped extends ScreenEvent {
      private final char codePoint;
      private final int modifiers;

      @Internal
      public CharacterTyped(Screen screen, char codePoint, int modifiers) {
         super(screen);
         this.codePoint = codePoint;
         this.modifiers = modifiers;
      }

      public char getCodePoint() {
         return this.codePoint;
      }

      public int getModifiers() {
         return this.modifiers;
      }

      public static class Post extends ScreenEvent.CharacterTyped {
         @Internal
         public Post(Screen screen, char codePoint, int modifiers) {
            super(screen, codePoint, modifiers);
         }
      }

      public static class Pre extends ScreenEvent.CharacterTyped implements ICancellableEvent {
         @Internal
         public Pre(Screen screen, char codePoint, int modifiers) {
            super(screen, codePoint, modifiers);
         }
      }
   }

   public static class Closing extends ScreenEvent {
      @Internal
      public Closing(Screen screen) {
         super(screen);
      }
   }

   public abstract static class Init extends ScreenEvent {
      private final Consumer<GuiEventListener> add;
      private final Consumer<GuiEventListener> remove;
      private final List<GuiEventListener> listenerList;

      @Internal
      protected Init(Screen screen, List<GuiEventListener> listenerList, Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove) {
         super(screen);
         this.listenerList = Collections.unmodifiableList(listenerList);
         this.add = add;
         this.remove = remove;
      }

      public List<GuiEventListener> getListenersList() {
         return this.listenerList;
      }

      public void addListener(GuiEventListener listener) {
         this.add.accept(listener);
      }

      public void removeListener(GuiEventListener listener) {
         this.remove.accept(listener);
      }

      public static class Post extends ScreenEvent.Init {
         @Internal
         public Post(Screen screen, List<GuiEventListener> list, Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove) {
            super(screen, list, add, remove);
         }
      }

      public static class Pre extends ScreenEvent.Init implements ICancellableEvent {
         @Internal
         public Pre(Screen screen, List<GuiEventListener> list, Consumer<GuiEventListener> add, Consumer<GuiEventListener> remove) {
            super(screen, list, add, remove);
         }
      }
   }

   private abstract static class KeyInput extends ScreenEvent {
      private final int keyCode;
      private final int scanCode;
      private final int modifiers;

      @Internal
      protected KeyInput(Screen screen, int keyCode, int scanCode, int modifiers) {
         super(screen);
         this.keyCode = keyCode;
         this.scanCode = scanCode;
         this.modifiers = modifiers;
      }

      public int getKeyCode() {
         return this.keyCode;
      }

      public int getScanCode() {
         return this.scanCode;
      }

      public int getModifiers() {
         return this.modifiers;
      }
   }

   public abstract static class KeyPressed extends ScreenEvent.KeyInput {
      @Internal
      public KeyPressed(Screen screen, int keyCode, int scanCode, int modifiers) {
         super(screen, keyCode, scanCode, modifiers);
      }

      public static class Post extends ScreenEvent.KeyPressed implements ICancellableEvent {
         @Internal
         public Post(Screen screen, int keyCode, int scanCode, int modifiers) {
            super(screen, keyCode, scanCode, modifiers);
         }
      }

      public static class Pre extends ScreenEvent.KeyPressed implements ICancellableEvent {
         @Internal
         public Pre(Screen screen, int keyCode, int scanCode, int modifiers) {
            super(screen, keyCode, scanCode, modifiers);
         }
      }
   }

   public abstract static class KeyReleased extends ScreenEvent.KeyInput {
      @Internal
      public KeyReleased(Screen screen, int keyCode, int scanCode, int modifiers) {
         super(screen, keyCode, scanCode, modifiers);
      }

      public static class Post extends ScreenEvent.KeyReleased implements ICancellableEvent {
         @Internal
         public Post(Screen screen, int keyCode, int scanCode, int modifiers) {
            super(screen, keyCode, scanCode, modifiers);
         }
      }

      public static class Pre extends ScreenEvent.KeyReleased implements ICancellableEvent {
         @Internal
         public Pre(Screen screen, int keyCode, int scanCode, int modifiers) {
            super(screen, keyCode, scanCode, modifiers);
         }
      }
   }

   public abstract static class MouseButtonPressed extends ScreenEvent.MouseInput {
      private final int button;

      @Internal
      public MouseButtonPressed(Screen screen, double mouseX, double mouseY, int button) {
         super(screen, mouseX, mouseY);
         this.button = button;
      }

      public int getButton() {
         return this.button;
      }

      public static class Post extends ScreenEvent.MouseButtonPressed {
         private final boolean handled;
         private ScreenEvent.MouseButtonPressed.Post.Result result = ScreenEvent.MouseButtonPressed.Post.Result.DEFAULT;

         @Internal
         public Post(Screen screen, double mouseX, double mouseY, int button, boolean handled) {
            super(screen, mouseX, mouseY, button);
            this.handled = handled;
         }

         public boolean wasClickHandled() {
            return this.handled;
         }

         public void setResult(ScreenEvent.MouseButtonPressed.Post.Result result) {
            this.result = result;
         }

         public ScreenEvent.MouseButtonPressed.Post.Result getResult() {
            return this.result;
         }

         public boolean getClickResult() {
            return this.result == ScreenEvent.MouseButtonPressed.Post.Result.FORCE_HANDLED
               ? true
               : this.result == ScreenEvent.MouseButtonPressed.Post.Result.DEFAULT && this.wasClickHandled();
         }

         public static enum Result {
            FORCE_HANDLED,
            DEFAULT,
            FORCE_UNHANDLED;
         }
      }

      public static class Pre extends ScreenEvent.MouseButtonPressed implements ICancellableEvent {
         @Internal
         public Pre(Screen screen, double mouseX, double mouseY, int button) {
            super(screen, mouseX, mouseY, button);
         }
      }
   }

   public abstract static class MouseButtonReleased extends ScreenEvent.MouseInput {
      private final int button;

      @Internal
      public MouseButtonReleased(Screen screen, double mouseX, double mouseY, int button) {
         super(screen, mouseX, mouseY);
         this.button = button;
      }

      public int getButton() {
         return this.button;
      }

      public static class Post extends ScreenEvent.MouseButtonReleased {
         private final boolean handled;
         private ScreenEvent.MouseButtonReleased.Post.Result result = ScreenEvent.MouseButtonReleased.Post.Result.DEFAULT;

         @Internal
         public Post(Screen screen, double mouseX, double mouseY, int button, boolean handled) {
            super(screen, mouseX, mouseY, button);
            this.handled = handled;
         }

         public boolean wasReleaseHandled() {
            return this.handled;
         }

         public void setResult(ScreenEvent.MouseButtonReleased.Post.Result result) {
            this.result = result;
         }

         public ScreenEvent.MouseButtonReleased.Post.Result getResult() {
            return this.result;
         }

         public boolean getReleaseResult() {
            return this.result == ScreenEvent.MouseButtonReleased.Post.Result.FORCE_HANDLED
               ? true
               : this.result == ScreenEvent.MouseButtonReleased.Post.Result.DEFAULT && this.wasReleaseHandled();
         }

         public static enum Result {
            FORCE_HANDLED,
            DEFAULT,
            FORCE_UNHANDLED;
         }
      }

      public static class Pre extends ScreenEvent.MouseButtonReleased implements ICancellableEvent {
         @Internal
         public Pre(Screen screen, double mouseX, double mouseY, int button) {
            super(screen, mouseX, mouseY, button);
         }
      }
   }

   public abstract static class MouseDragged extends ScreenEvent.MouseInput {
      private final int mouseButton;
      private final double dragX;
      private final double dragY;

      @Internal
      public MouseDragged(Screen screen, double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
         super(screen, mouseX, mouseY);
         this.mouseButton = mouseButton;
         this.dragX = dragX;
         this.dragY = dragY;
      }

      public int getMouseButton() {
         return this.mouseButton;
      }

      public double getDragX() {
         return this.dragX;
      }

      public double getDragY() {
         return this.dragY;
      }

      public static class Post extends ScreenEvent.MouseDragged {
         @Internal
         public Post(Screen screen, double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
            super(screen, mouseX, mouseY, mouseButton, dragX, dragY);
         }
      }

      public static class Pre extends ScreenEvent.MouseDragged implements ICancellableEvent {
         @Internal
         public Pre(Screen screen, double mouseX, double mouseY, int mouseButton, double dragX, double dragY) {
            super(screen, mouseX, mouseY, mouseButton, dragX, dragY);
         }
      }
   }

   private abstract static class MouseInput extends ScreenEvent {
      private final double mouseX;
      private final double mouseY;

      @Internal
      protected MouseInput(Screen screen, double mouseX, double mouseY) {
         super(screen);
         this.mouseX = mouseX;
         this.mouseY = mouseY;
      }

      public double getMouseX() {
         return this.mouseX;
      }

      public double getMouseY() {
         return this.mouseY;
      }
   }

   public abstract static class MouseScrolled extends ScreenEvent.MouseInput {
      private final double scrollDeltaX;
      private final double scrollDeltaY;

      @Internal
      public MouseScrolled(Screen screen, double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
         super(screen, mouseX, mouseY);
         this.scrollDeltaX = scrollDeltaX;
         this.scrollDeltaY = scrollDeltaY;
      }

      public double getScrollDeltaX() {
         return this.scrollDeltaX;
      }

      public double getScrollDeltaY() {
         return this.scrollDeltaY;
      }

      public static class Post extends ScreenEvent.MouseScrolled {
         @Internal
         public Post(Screen screen, double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            super(screen, mouseX, mouseY, scrollDeltaX, scrollDeltaY);
         }
      }

      public static class Pre extends ScreenEvent.MouseScrolled implements ICancellableEvent {
         @Internal
         public Pre(Screen screen, double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
            super(screen, mouseX, mouseY, scrollDeltaX, scrollDeltaY);
         }
      }
   }

   public static class Opening extends ScreenEvent implements ICancellableEvent {
      @Nullable
      private final Screen currentScreen;
      @Nullable
      private Screen newScreen;

      @Internal
      public Opening(@Nullable Screen currentScreen, Screen screen) {
         super(screen);
         this.currentScreen = currentScreen;
         this.newScreen = screen;
      }

      @Nullable
      public Screen getCurrentScreen() {
         return this.currentScreen;
      }

      @Nullable
      public Screen getNewScreen() {
         return this.newScreen;
      }

      public void setNewScreen(@Nullable Screen newScreen) {
         this.newScreen = newScreen;
      }
   }

   public abstract static class Render extends ScreenEvent {
      private final GuiGraphics guiGraphics;
      private final int mouseX;
      private final int mouseY;
      private final float partialTick;

      @Internal
      protected Render(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
         super(screen);
         this.guiGraphics = guiGraphics;
         this.mouseX = mouseX;
         this.mouseY = mouseY;
         this.partialTick = partialTick;
      }

      public GuiGraphics getGuiGraphics() {
         return this.guiGraphics;
      }

      public int getMouseX() {
         return this.mouseX;
      }

      public int getMouseY() {
         return this.mouseY;
      }

      public float getPartialTick() {
         return this.partialTick;
      }

      public static class Post extends ScreenEvent.Render {
         @Internal
         public Post(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super(screen, guiGraphics, mouseX, mouseY, partialTick);
         }
      }

      public static class Pre extends ScreenEvent.Render implements ICancellableEvent {
         @Internal
         public Pre(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super(screen, guiGraphics, mouseX, mouseY, partialTick);
         }
      }
   }

   public static class RenderInventoryMobEffects extends ScreenEvent implements ICancellableEvent {
      private final int availableSpace;
      private boolean compact;
      private int horizontalOffset;

      @Internal
      public RenderInventoryMobEffects(Screen screen, int availableSpace, boolean compact, int horizontalOffset) {
         super(screen);
         this.availableSpace = availableSpace;
         this.compact = compact;
         this.horizontalOffset = horizontalOffset;
      }

      public int getAvailableSpace() {
         return this.availableSpace;
      }

      public boolean isCompact() {
         return this.compact;
      }

      public int getHorizontalOffset() {
         return this.horizontalOffset;
      }

      public void setHorizontalOffset(int offset) {
         this.horizontalOffset = offset;
      }

      public void addHorizontalOffset(int offset) {
         this.horizontalOffset += offset;
      }

      public void setCompact(boolean compact) {
         this.compact = compact;
      }
   }
}

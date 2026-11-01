package net.minecraft.client;

import com.mojang.blaze3d.Blaze3D;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.util.Mth;
import net.minecraft.util.SmoothDouble;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import org.joml.Vector2i;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFWDropCallback;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class MouseHandler {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final long DOUBLE_CLICK_THRESHOLD_MS = 250L;
   private final Minecraft minecraft;
   private boolean isLeftPressed;
   private boolean isMiddlePressed;
   private boolean isRightPressed;
   private double xpos;
   private double ypos;
   @Nullable
   private MouseHandler.LastClick lastClick;
   @MouseButtonInfo.MouseButton
   protected int lastClickButton;
   private int fakeRightMouse;
   @Nullable
   private MouseButtonInfo activeButton = null;
   private boolean ignoreFirstMove = true;
   private int clickDepth;
   private double mousePressedTime;
   private final SmoothDouble smoothTurnX = new SmoothDouble();
   private final SmoothDouble smoothTurnY = new SmoothDouble();
   private double accumulatedDX;
   private double accumulatedDY;
   private final ScrollWheelHandler scrollWheelHandler;
   private double lastHandleMovementTime = Double.MIN_VALUE;
   private boolean mouseGrabbed;

   public MouseHandler(Minecraft minecraft) {
      this.minecraft = minecraft;
      this.scrollWheelHandler = new ScrollWheelHandler();
   }

   private void onButton(long window, MouseButtonInfo buttonInfo, @MouseButtonInfo.Action int action) {
      Window window2 = this.minecraft.getWindow();
      if (window == window2.handle()) {
         this.minecraft.getFramerateLimitTracker().onInputReceived();
         if (this.minecraft.screen != null) {
            this.minecraft.setLastInputType(InputType.MOUSE);
         }

         boolean bl = action == 1;
         MouseButtonInfo mouseButtonInfo = this.simulateRightClick(buttonInfo, bl);
         if (bl) {
            if (this.minecraft.options.touchscreen().get() && this.clickDepth++ > 0) {
               return;
            }

            this.activeButton = mouseButtonInfo;
            this.mousePressedTime = Blaze3D.getTime();
         } else if (this.activeButton != null) {
            if (this.minecraft.options.touchscreen().get() && --this.clickDepth > 0) {
               return;
            }

            this.activeButton = null;
         }

         if (this.minecraft.getOverlay() == null) {
            if (this.minecraft.screen == null) {
               if (!this.mouseGrabbed && bl) {
                  this.grabMouse();
               }
            } else {
               double d = this.getScaledXPos(window2);
               double e = this.getScaledYPos(window2);
               Screen screen = this.minecraft.screen;
               MouseButtonEvent mouseButtonEvent = new MouseButtonEvent(d, e, mouseButtonInfo);
               if (bl) {
                  screen.afterMouseAction();

                  try {
                     long l = Util.getMillis();
                     boolean bl2 = this.lastClick != null
                        && l - this.lastClick.time() < 250L
                        && this.lastClick.screen() == screen
                        && this.lastClickButton == mouseButtonEvent.button();
                     if (screen.mouseClicked(mouseButtonEvent, bl2)) {
                        this.lastClick = new MouseHandler.LastClick(l, screen);
                        this.lastClickButton = mouseButtonInfo.button();
                        return;
                     }
                  } catch (Throwable var18) {
                     CrashReport crashReport = CrashReport.forThrowable(var18, "mouseClicked event handler");
                     screen.fillCrashDetails(crashReport);
                     CrashReportCategory crashReportCategory = crashReport.addCategory("Mouse");
                     this.fillMousePositionDetails(crashReportCategory, window2);
                     crashReportCategory.setDetail("Button", mouseButtonEvent.button());
                     throw new ReportedException(crashReport);
                  }
               } else {
                  try {
                     if (screen.mouseReleased(mouseButtonEvent)) {
                        return;
                     }
                  } catch (Throwable var17) {
                     CrashReport crashReport = CrashReport.forThrowable(var17, "mouseReleased event handler");
                     screen.fillCrashDetails(crashReport);
                     CrashReportCategory crashReportCategory = crashReport.addCategory("Mouse");
                     this.fillMousePositionDetails(crashReportCategory, window2);
                     crashReportCategory.setDetail("Button", mouseButtonEvent.button());
                     throw new ReportedException(crashReport);
                  }
               }
            }
         }

         if (this.minecraft.screen == null && this.minecraft.getOverlay() == null) {
            if (mouseButtonInfo.button() == 0) {
               this.isLeftPressed = bl;
            } else if (mouseButtonInfo.button() == 2) {
               this.isMiddlePressed = bl;
            } else if (mouseButtonInfo.button() == 1) {
               this.isRightPressed = bl;
            }

            InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(mouseButtonInfo.button());
            KeyMapping.set(key, bl);
            if (bl) {
               KeyMapping.click(key);
            }
         }
      }
   }

   private MouseButtonInfo simulateRightClick(MouseButtonInfo buttonInfo, boolean isPressed) {
      if (InputQuirks.SIMULATE_RIGHT_CLICK_WITH_LONG_LEFT_CLICK && buttonInfo.button() == 0) {
         if (isPressed) {
            if ((buttonInfo.modifiers() & 2) == 2) {
               this.fakeRightMouse++;
               return new MouseButtonInfo(1, buttonInfo.modifiers());
            }
         } else if (this.fakeRightMouse > 0) {
            this.fakeRightMouse--;
            return new MouseButtonInfo(1, buttonInfo.modifiers());
         }
      }

      return buttonInfo;
   }

   public void fillMousePositionDetails(CrashReportCategory category, Window window) {
      category.setDetail(
         "Mouse location",
         () -> String.format(
            Locale.ROOT, "Scaled: (%f, %f). Absolute: (%f, %f)", getScaledXPos(window, this.xpos), getScaledYPos(window, this.ypos), this.xpos, this.ypos
         )
      );
      category.setDetail(
         "Screen size",
         () -> String.format(
            Locale.ROOT,
            "Scaled: (%d, %d). Absolute: (%d, %d). Scale factor of %d",
            window.getGuiScaledWidth(),
            window.getGuiScaledHeight(),
            window.getWidth(),
            window.getHeight(),
            window.getGuiScale()
         )
      );
   }

   private void onScroll(long windowPointer, double xOffset, double yOffset) {
      if (windowPointer == this.minecraft.getWindow().handle()) {
         this.minecraft.getFramerateLimitTracker().onInputReceived();
         boolean bl = this.minecraft.options.discreteMouseScroll().get();
         double d = this.minecraft.options.mouseWheelSensitivity().get();
         double e = (bl ? Math.signum(xOffset) : xOffset) * d;
         double f = (bl ? Math.signum(yOffset) : yOffset) * d;
         if (this.minecraft.getOverlay() == null) {
            if (this.minecraft.screen != null) {
               double g = this.getScaledXPos(this.minecraft.getWindow());
               double h = this.getScaledYPos(this.minecraft.getWindow());
               this.minecraft.screen.mouseScrolled(g, h, e, f);
               this.minecraft.screen.afterMouseAction();
            } else if (this.minecraft.player != null) {
               Vector2i vector2i = this.scrollWheelHandler.onMouseScroll(e, f);
               if (vector2i.x == 0 && vector2i.y == 0) {
                  return;
               }

               int i = vector2i.y == 0 ? -vector2i.x : vector2i.y;
               if (this.minecraft.player.isSpectator()) {
                  if (this.minecraft.gui.getSpectatorGui().isMenuActive()) {
                     this.minecraft.gui.getSpectatorGui().onMouseScrolled(-i);
                  } else {
                     float j = Mth.clamp(this.minecraft.player.getAbilities().getFlyingSpeed() + vector2i.y * 0.005F, 0.0F, 0.2F);
                     this.minecraft.player.getAbilities().setFlyingSpeed(j);
                  }
               } else {
                  Inventory inventory = this.minecraft.player.getInventory();
                  inventory.setSelectedSlot(ScrollWheelHandler.getNextScrollWheelSelection(i, inventory.getSelectedSlot(), Inventory.getSelectionSize()));
               }
            }
         }
      }
   }

   private void onDrop(long windowPointer, List<Path> files, int failedFiles) {
      this.minecraft.getFramerateLimitTracker().onInputReceived();
      if (this.minecraft.screen != null) {
         this.minecraft.screen.onFilesDrop(files);
      }

      if (failedFiles > 0) {
         SystemToast.onFileDropFailure(this.minecraft, failedFiles);
      }
   }

   public void setup(Window window) {
      InputConstants.setupMouseCallbacks(window, (l, d, e) -> this.minecraft.execute(() -> this.onMove(l, d, e)), (l, i, j, k) -> {
         MouseButtonInfo mouseButtonInfo = new MouseButtonInfo(i, k);
         this.minecraft.execute(() -> this.onButton(l, mouseButtonInfo, j));
      }, (l, d, e) -> this.minecraft.execute(() -> this.onScroll(l, d, e)), (l, i, m) -> {
         List<Path> list = new ArrayList<>(i);
         int j = 0;

         for (int k = 0; k < i; k++) {
            String string = GLFWDropCallback.getName(m, k);

            try {
               list.add(Paths.get(string));
            } catch (InvalidPathException var11) {
               j++;
               LOGGER.error("Failed to parse path '{}'", string, var11);
            }
         }

         if (!list.isEmpty()) {
            int k = j;
            this.minecraft.execute(() -> this.onDrop(l, list, k));
         }
      });
   }

   private void onMove(long windowPointer, double xpos, double ypos) {
      if (windowPointer == this.minecraft.getWindow().handle()) {
         if (this.ignoreFirstMove) {
            this.xpos = xpos;
            this.ypos = ypos;
            this.ignoreFirstMove = false;
         } else {
            if (this.minecraft.isWindowActive()) {
               this.accumulatedDX = this.accumulatedDX + (xpos - this.xpos);
               this.accumulatedDY = this.accumulatedDY + (ypos - this.ypos);
            }

            this.xpos = xpos;
            this.ypos = ypos;
         }
      }
   }

   public void handleAccumulatedMovement() {
      double d = Blaze3D.getTime();
      double e = d - this.lastHandleMovementTime;
      this.lastHandleMovementTime = d;
      if (this.minecraft.isWindowActive()) {
         Screen screen = this.minecraft.screen;
         boolean bl = this.accumulatedDX != 0.0 || this.accumulatedDY != 0.0;
         if (bl) {
            this.minecraft.getFramerateLimitTracker().onInputReceived();
         }

         if (screen != null && this.minecraft.getOverlay() == null && bl) {
            Window window = this.minecraft.getWindow();
            double f = this.getScaledXPos(window);
            double g = this.getScaledYPos(window);

            try {
               screen.mouseMoved(f, g);
            } catch (Throwable var20) {
               CrashReport crashReport = CrashReport.forThrowable(var20, "mouseMoved event handler");
               screen.fillCrashDetails(crashReport);
               CrashReportCategory crashReportCategory = crashReport.addCategory("Mouse");
               this.fillMousePositionDetails(crashReportCategory, window);
               throw new ReportedException(crashReport);
            }

            if (this.activeButton != null && this.mousePressedTime > 0.0) {
               double h = getScaledXPos(window, this.accumulatedDX);
               double i = getScaledYPos(window, this.accumulatedDY);

               try {
                  screen.mouseDragged(new MouseButtonEvent(f, g, this.activeButton), h, i);
               } catch (Throwable var19) {
                  CrashReport crashReport2 = CrashReport.forThrowable(var19, "mouseDragged event handler");
                  screen.fillCrashDetails(crashReport2);
                  CrashReportCategory crashReportCategory2 = crashReport2.addCategory("Mouse");
                  this.fillMousePositionDetails(crashReportCategory2, window);
                  throw new ReportedException(crashReport2);
               }
            }

            screen.afterMouseMove();
         }

         if (this.isMouseGrabbed() && this.minecraft.player != null) {
            this.turnPlayer(e);
         }
      }

      this.accumulatedDX = 0.0;
      this.accumulatedDY = 0.0;
   }

   public static double getScaledXPos(Window window, double xPos) {
      return xPos * window.getGuiScaledWidth() / window.getScreenWidth();
   }

   public double getScaledXPos(Window window) {
      return getScaledXPos(window, this.xpos);
   }

   public static double getScaledYPos(Window window, double yPos) {
      return yPos * window.getGuiScaledHeight() / window.getScreenHeight();
   }

   public double getScaledYPos(Window window) {
      return getScaledYPos(window, this.ypos);
   }

   private void turnPlayer(double movementTime) {
      double d = this.minecraft.options.sensitivity().get() * 0.6F + 0.2F;
      double e = d * d * d;
      double f = e * 8.0;
      double i;
      double j;
      if (this.minecraft.options.smoothCamera) {
         double g = this.smoothTurnX.getNewDeltaValue(this.accumulatedDX * f, movementTime * f);
         double h = this.smoothTurnY.getNewDeltaValue(this.accumulatedDY * f, movementTime * f);
         i = g;
         j = h;
      } else if (this.minecraft.options.getCameraType().isFirstPerson() && this.minecraft.player.isScoping()) {
         this.smoothTurnX.reset();
         this.smoothTurnY.reset();
         i = this.accumulatedDX * e;
         j = this.accumulatedDY * e;
      } else {
         this.smoothTurnX.reset();
         this.smoothTurnY.reset();
         i = this.accumulatedDX * f;
         j = this.accumulatedDY * f;
      }

      this.minecraft.getTutorial().onMouse(i, j);
      if (this.minecraft.player != null) {
         this.minecraft.player.turn(this.minecraft.options.invertMouseX().get() ? -i : i, this.minecraft.options.invertMouseY().get() ? -j : j);
      }
   }

   public boolean isLeftPressed() {
      return this.isLeftPressed;
   }

   public boolean isMiddlePressed() {
      return this.isMiddlePressed;
   }

   public boolean isRightPressed() {
      return this.isRightPressed;
   }

   public double xpos() {
      return this.xpos;
   }

   public double ypos() {
      return this.ypos;
   }

   public void setIgnoreFirstMove() {
      this.ignoreFirstMove = true;
   }

   public boolean isMouseGrabbed() {
      return this.mouseGrabbed;
   }

   public void grabMouse() {
      if (this.minecraft.isWindowActive()) {
         if (!this.mouseGrabbed) {
            if (InputQuirks.RESTORE_KEY_STATE_AFTER_MOUSE_GRAB) {
               KeyMapping.setAll();
            }

            this.mouseGrabbed = true;
            this.xpos = this.minecraft.getWindow().getScreenWidth() / 2;
            this.ypos = this.minecraft.getWindow().getScreenHeight() / 2;
            InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), 212995, this.xpos, this.ypos);
            this.minecraft.setScreen(null);
            this.minecraft.missTime = 10000;
            this.ignoreFirstMove = true;
         }
      }
   }

   public void releaseMouse() {
      if (this.mouseGrabbed) {
         this.mouseGrabbed = false;
         this.xpos = this.minecraft.getWindow().getScreenWidth() / 2;
         this.ypos = this.minecraft.getWindow().getScreenHeight() / 2;
         InputConstants.grabOrReleaseMouse(this.minecraft.getWindow(), 212993, this.xpos, this.ypos);
      }
   }

   public void cursorEntered() {
      this.ignoreFirstMove = true;
   }

   public void drawDebugMouseInfo(Font font, GuiGraphics guiGraphics) {
      Window window = this.minecraft.getWindow();
      double d = this.getScaledXPos(window);
      double e = this.getScaledYPos(window) - 8.0;
      String string = String.format(Locale.ROOT, "%.0f,%.0f", d, e);
      guiGraphics.drawString(font, string, (int)d, (int)e, -1);
   }

   @Environment(EnvType.CLIENT)
   record LastClick(long time, Screen screen) {
   }
}

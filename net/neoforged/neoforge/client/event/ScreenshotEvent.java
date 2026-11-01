package net.neoforged.neoforge.client.event;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.File;
import java.io.IOException;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class ScreenshotEvent extends Event implements ICancellableEvent {
   public static final Component DEFAULT_CANCEL_REASON = Component.literal("Screenshot canceled");
   private final NativeImage image;
   private File screenshotFile;
   @Nullable
   private Component resultMessage = null;

   @Internal
   public ScreenshotEvent(NativeImage image, File screenshotFile) {
      this.image = image;
      this.screenshotFile = screenshotFile;

      try {
         this.screenshotFile = screenshotFile.getCanonicalFile();
      } catch (IOException var4) {
      }
   }

   public NativeImage getImage() {
      return this.image;
   }

   public File getScreenshotFile() {
      return this.screenshotFile;
   }

   public void setScreenshotFile(File screenshotFile) {
      this.screenshotFile = screenshotFile;
   }

   @Nullable
   public Component getResultMessage() {
      return this.resultMessage;
   }

   public void setResultMessage(@Nullable Component resultMessage) {
      this.resultMessage = resultMessage;
   }

   public Component getCancelMessage() {
      return this.getResultMessage() != null ? this.getResultMessage() : DEFAULT_CANCEL_REASON;
   }
}

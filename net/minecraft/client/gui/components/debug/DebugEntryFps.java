package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DebugEntryFps implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
      Minecraft minecraft = Minecraft.getInstance();
      int i = minecraft.getFramerateLimitTracker().getFramerateLimit();
      Options options = minecraft.options;
      displayer.addPriorityLine(
         String.format(Locale.ROOT, "%d fps T: %s%s", minecraft.getFps(), i == 260 ? "inf" : i, options.enableVsync().get() ? " vsync" : "")
      );
   }

   @Override
   public boolean isAllowed(boolean reducedDebugInfo) {
      return true;
   }
}

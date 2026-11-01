package net.minecraft.client.gui.components.debug;

import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DebugEntrySoundMood implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.player != null) {
         displayer.addLine(
            minecraft.getSoundManager().getDebugString() + String.format(Locale.ROOT, " (Mood %d%%)", Math.round(minecraft.player.getCurrentMood() * 100.0F))
         );
      }
   }
}

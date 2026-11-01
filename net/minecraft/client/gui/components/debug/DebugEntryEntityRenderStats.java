package net.minecraft.client.gui.components.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DebugEntryEntityRenderStats implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
      String string = Minecraft.getInstance().levelRenderer.getEntityStatistics();
      if (string != null) {
         displayer.addLine(string);
      }
   }

   @Override
   public boolean isAllowed(boolean reducedDebugInfo) {
      return true;
   }
}

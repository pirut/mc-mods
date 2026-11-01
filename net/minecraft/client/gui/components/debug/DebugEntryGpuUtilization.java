package net.minecraft.client.gui.components.debug;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DebugEntryGpuUtilization implements DebugScreenEntry {
   @Override
   public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
      Minecraft minecraft = Minecraft.getInstance();
      String string = "GPU: " + (minecraft.getGpuUtilization() > 100.0 ? ChatFormatting.RED + "100%" : Math.round(minecraft.getGpuUtilization()) + "%");
      displayer.addLine(string);
   }

   @Override
   public boolean isAllowed(boolean reducedDebugInfo) {
      return true;
   }
}

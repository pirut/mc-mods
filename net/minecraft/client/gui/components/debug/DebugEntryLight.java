package net.minecraft.client.gui.components.debug;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DebugEntryLight implements DebugScreenEntry {
   public static final Identifier GROUP = Identifier.withDefaultNamespace("light");

   @Override
   public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      if (entity != null && minecraft.level != null) {
         BlockPos blockPos = entity.blockPosition();
         int i = minecraft.level.getChunkSource().getLightEngine().getRawBrightness(blockPos, 0);
         int j = minecraft.level.getBrightness(LightLayer.SKY, blockPos);
         int k = minecraft.level.getBrightness(LightLayer.BLOCK, blockPos);
         String string = "Client Light: " + i + " (" + j + " sky, " + k + " block)";
         if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES) {
            String string2;
            if (serverChunk != null) {
               LevelLightEngine levelLightEngine = serverChunk.getLevel().getLightEngine();
               string2 = "Server Light: ("
                  + levelLightEngine.getLayerListener(LightLayer.SKY).getLightValue(blockPos)
                  + " sky, "
                  + levelLightEngine.getLayerListener(LightLayer.BLOCK).getLightValue(blockPos)
                  + " block)";
            } else {
               string2 = "Server Light: (?? sky, ?? block)";
            }

            displayer.addToGroup(GROUP, List.of(string, string2));
         } else {
            displayer.addToGroup(GROUP, string);
         }
      }
   }
}

package net.minecraft.client.gui.components.debug;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class DebugEntryBiome implements DebugScreenEntry {
   private static final Identifier GROUP = Identifier.withDefaultNamespace("biome");

   @Override
   public void display(DebugScreenDisplayer displayer, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
      Minecraft minecraft = Minecraft.getInstance();
      Entity entity = minecraft.getCameraEntity();
      if (entity != null && minecraft.level != null) {
         BlockPos blockPos = entity.blockPosition();
         if (minecraft.level.isInsideBuildHeight(blockPos.getY())) {
            if (SharedConstants.DEBUG_SHOW_SERVER_DEBUG_VALUES && level instanceof ServerLevel) {
               displayer.addToGroup(
                  GROUP, List.of("Biome: " + printBiome(minecraft.level.getBiome(blockPos)), "Server Biome: " + printBiome(level.getBiome(blockPos)))
               );
            } else {
               displayer.addLine("Biome: " + printBiome(minecraft.level.getBiome(blockPos)));
            }
         }
      }
   }

   private static String printBiome(Holder<Biome> biome) {
      return (String)biome.unwrap().map(resourceKey -> resourceKey.identifier().toString(), biomex -> "[unregistered " + biomex + "]");
   }
}

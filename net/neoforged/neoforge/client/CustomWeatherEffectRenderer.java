package net.neoforged.neoforge.client;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.state.WeatherRenderState;
import net.minecraft.world.phys.Vec3;

public interface CustomWeatherEffectRenderer {
   default boolean renderSnowAndRain(LevelRenderState levelRenderState, WeatherRenderState weatherRenderState, MultiBufferSource bufferSource, Vec3 camPos) {
      return false;
   }

   default boolean tickRain(ClientLevel level, int ticks, Camera camera) {
      return false;
   }
}

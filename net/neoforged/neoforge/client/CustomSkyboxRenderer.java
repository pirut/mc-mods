package net.neoforged.neoforge.client;

import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.client.renderer.state.SkyRenderState;
import org.joml.Matrix4f;

public interface CustomSkyboxRenderer {
   default boolean renderSky(LevelRenderState levelRenderState, SkyRenderState skyRenderState, Matrix4f modelViewMatrix, Runnable setupFog) {
      return false;
   }
}

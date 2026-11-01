package net.neoforged.neoforge.client;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public interface CustomCloudsRenderer {
   default boolean renderClouds(
      LevelRenderState levelRenderState, Vec3 camPos, CloudStatus cloudStatus, int cloudColor, float cloudHeight, Matrix4f modelViewMatrix
   ) {
      return false;
   }
}

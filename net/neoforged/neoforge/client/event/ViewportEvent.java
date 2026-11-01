package net.neoforged.neoforge.client.event;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.world.level.material.FogType;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public abstract class ViewportEvent extends Event {
   private final GameRenderer renderer;
   private final Camera camera;
   private final double partialTick;

   @Internal
   public ViewportEvent(GameRenderer renderer, Camera camera, double partialTick) {
      this.renderer = renderer;
      this.camera = camera;
      this.partialTick = partialTick;
   }

   public GameRenderer getRenderer() {
      return this.renderer;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public double getPartialTick() {
      return this.partialTick;
   }

   public static class ComputeCameraAngles extends ViewportEvent {
      private float yaw;
      private float pitch;
      private float roll;

      @Internal
      public ComputeCameraAngles(Camera camera, double renderPartialTicks, float yaw, float pitch, float roll) {
         super(Minecraft.getInstance().gameRenderer, camera, renderPartialTicks);
         this.setYaw(yaw);
         this.setPitch(pitch);
         this.setRoll(roll);
      }

      public float getYaw() {
         return this.yaw;
      }

      public void setYaw(float yaw) {
         this.yaw = yaw;
      }

      public float getPitch() {
         return this.pitch;
      }

      public void setPitch(float pitch) {
         this.pitch = pitch;
      }

      public float getRoll() {
         return this.roll;
      }

      public void setRoll(float roll) {
         this.roll = roll;
      }
   }

   public static class ComputeFogColor extends ViewportEvent {
      private float red;
      private float green;
      private float blue;

      @Internal
      public ComputeFogColor(Camera camera, float partialTicks, float red, float green, float blue) {
         super(Minecraft.getInstance().gameRenderer, camera, partialTicks);
         this.setRed(red);
         this.setGreen(green);
         this.setBlue(blue);
      }

      public float getRed() {
         return this.red;
      }

      public void setRed(float red) {
         this.red = red;
      }

      public float getGreen() {
         return this.green;
      }

      public void setGreen(float green) {
         this.green = green;
      }

      public float getBlue() {
         return this.blue;
      }

      public void setBlue(float blue) {
         this.blue = blue;
      }
   }

   public static class ComputeFov extends ViewportEvent {
      private final boolean usedConfiguredFov;
      private float fov;

      @Internal
      public ComputeFov(GameRenderer renderer, Camera camera, float renderPartialTicks, float fov, boolean usedConfiguredFov) {
         super(renderer, camera, renderPartialTicks);
         this.usedConfiguredFov = usedConfiguredFov;
         this.setFOV(fov);
      }

      public float getFOV() {
         return this.fov;
      }

      public void setFOV(float fov) {
         this.fov = fov;
      }

      public boolean usedConfiguredFov() {
         return this.usedConfiguredFov;
      }
   }

   public static class RenderFog extends ViewportEvent {
      @Nullable
      private final FogEnvironment environment;
      private final FogType type;
      private final FogData fogData;

      @Internal
      public RenderFog(@Nullable FogEnvironment environment, FogType type, Camera camera, float partialTicks, FogData fogData) {
         super(Minecraft.getInstance().gameRenderer, camera, partialTicks);
         this.environment = environment;
         this.type = type;
         this.fogData = fogData;
         this.setFarPlaneDistance(fogData.environmentalEnd);
         this.setNearPlaneDistance(fogData.environmentalStart);
      }

      @Nullable
      public FogEnvironment getEnvironment() {
         return this.environment;
      }

      public FogType getType() {
         return this.type;
      }

      public float getFarPlaneDistance() {
         return this.fogData.environmentalEnd;
      }

      public float getNearPlaneDistance() {
         return this.fogData.environmentalStart;
      }

      public FogData getFogData() {
         return this.fogData;
      }

      public void setFarPlaneDistance(float distance) {
         this.fogData.environmentalEnd = distance;
      }

      public void setNearPlaneDistance(float distance) {
         this.fogData.environmentalStart = distance;
      }

      public void scaleFarPlaneDistance(float factor) {
         this.fogData.environmentalEnd *= factor;
      }

      public void scaleNearPlaneDistance(float factor) {
         this.fogData.environmentalStart *= factor;
      }
   }
}

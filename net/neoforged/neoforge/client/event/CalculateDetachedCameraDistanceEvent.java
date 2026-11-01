package net.neoforged.neoforge.client.event;

import net.minecraft.client.Camera;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class CalculateDetachedCameraDistanceEvent extends Event {
   private final Camera camera;
   private final boolean cameraFlipped;
   private final float entityScale;
   private float distance;

   @Internal
   public CalculateDetachedCameraDistanceEvent(Camera camera, boolean cameraFlipped, float entityScale, float distance) {
      this.camera = camera;
      this.cameraFlipped = cameraFlipped;
      this.entityScale = entityScale;
      this.distance = distance;
   }

   public Camera getCamera() {
      return this.camera;
   }

   public boolean isCameraFlipped() {
      return this.cameraFlipped;
   }

   public float getEntityScalingFactor() {
      return this.entityScale;
   }

   public float getDistance() {
      return this.distance;
   }

   public void setDistance(float distance) {
      this.distance = distance;
   }
}

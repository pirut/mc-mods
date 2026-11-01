package net.neoforged.neoforge.client.event;

import net.minecraft.client.Camera;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class CalculateDetachedCameraDistanceEvent extends Event {
   private final Camera camera;
   private final boolean cameraFlipped;
   private final float entityScale;
   private final float entityDistance;
   private final float vehicleEntityScale;
   private final float vehicleEntityDistance;
   private float distance;

   @Internal
   public CalculateDetachedCameraDistanceEvent(
      Camera camera, boolean cameraFlipped, float entityScale, float entityDistance, float vehicleEntityScale, float vehicleDistance
   ) {
      this.camera = camera;
      this.cameraFlipped = cameraFlipped;
      this.entityScale = entityScale;
      this.entityDistance = entityDistance;
      this.vehicleEntityScale = vehicleEntityScale;
      this.vehicleEntityDistance = vehicleDistance;
      this.distance = Math.max(entityScale * entityDistance, vehicleEntityScale * vehicleDistance);
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

   public float getEntityDistance() {
      return this.entityDistance;
   }

   public float getVehicleEntityScalingFactor() {
      return this.vehicleEntityScale;
   }

   public float getVehicleEntityDistance() {
      return this.vehicleEntityDistance;
   }

   public float getDistance() {
      return this.distance;
   }

   public void setDistance(float distance) {
      this.distance = distance;
   }
}

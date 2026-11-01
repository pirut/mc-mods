package net.neoforged.neoforge.client.event;

import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class CalculatePlayerTurnEvent extends Event {
   private double mouseSensitivity;
   private boolean cinematicCameraEnabled;

   @Internal
   public CalculatePlayerTurnEvent(double mouseSensitivity, boolean cinematicCameraEnabled) {
      this.setMouseSensitivity(mouseSensitivity);
      this.setCinematicCameraEnabled(cinematicCameraEnabled);
   }

   public double getMouseSensitivity() {
      return this.mouseSensitivity;
   }

   public void setMouseSensitivity(double mouseSensitivity) {
      this.mouseSensitivity = mouseSensitivity;
   }

   public boolean getCinematicCameraEnabled() {
      return this.cinematicCameraEnabled;
   }

   public void setCinematicCameraEnabled(boolean cinematicCameraEnabled) {
      this.cinematicCameraEnabled = cinematicCameraEnabled;
   }
}

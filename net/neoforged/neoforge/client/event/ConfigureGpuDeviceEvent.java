package net.neoforged.neoforge.client.event;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.blaze3d.GpuDeviceFeatures;
import net.neoforged.neoforge.client.blaze3d.GpuDeviceProperties;

public class ConfigureGpuDeviceEvent extends Event implements IModBusEvent, GpuDeviceFeatures {
   private final GpuDeviceProperties deviceProperties;
   private final GpuDeviceFeatures availableFeatures;
   private boolean logicOp = false;

   public ConfigureGpuDeviceEvent(GpuDeviceProperties deviceProperties, GpuDeviceFeatures availableFeatures) {
      this.deviceProperties = deviceProperties;
      this.availableFeatures = availableFeatures;
   }

   public GpuDeviceProperties getDeviceProperties() {
      return this.deviceProperties;
   }

   public GpuDeviceFeatures getAvailableFeatures() {
      return this.availableFeatures;
   }

   @Override
   public boolean logicOp() {
      return this.logicOp;
   }

   public void enableLogicOp() {
      if (!this.availableFeatures.logicOp()) {
         throw new UnsupportedOperationException("LogicOp is unavailable");
      } else {
         this.logicOp = true;
      }
   }
}

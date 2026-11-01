package net.minecraft.server.network;

import java.util.function.Consumer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public interface ConfigurationTask {
   void start(Consumer<Packet<?>> var1);

   ConfigurationTask.Type type();

   public record Type(String id) {
      public Type(ResourceLocation location) {
         this(location.toString());
      }

      @Override
      public String toString() {
         return this.id;
      }
   }
}

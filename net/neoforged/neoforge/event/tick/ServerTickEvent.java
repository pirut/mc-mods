package net.neoforged.neoforge.event.tick;

import java.util.function.BooleanSupplier;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

public abstract class ServerTickEvent extends Event {
   private final BooleanSupplier hasTime;
   private final MinecraftServer server;

   protected ServerTickEvent(BooleanSupplier hasTime, MinecraftServer server) {
      this.hasTime = hasTime;
      this.server = server;
   }

   public boolean hasTime() {
      return this.hasTime.getAsBoolean();
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   public static class Post extends ServerTickEvent {
      public Post(BooleanSupplier haveTime, MinecraftServer server) {
         super(haveTime, server);
      }
   }

   public static class Pre extends ServerTickEvent {
      public Pre(BooleanSupplier haveTime, MinecraftServer server) {
         super(haveTime, server);
      }
   }
}

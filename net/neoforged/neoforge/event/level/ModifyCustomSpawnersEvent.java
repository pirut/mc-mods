package net.neoforged.neoforge.event.level;

import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CustomSpawner;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ModifyCustomSpawnersEvent extends Event {
   private final ServerLevel serverLevel;
   private final List<CustomSpawner> customSpawners;

   @Internal
   public ModifyCustomSpawnersEvent(ServerLevel serverLevel, List<CustomSpawner> customSpawners) {
      this.serverLevel = serverLevel;
      this.customSpawners = customSpawners;
   }

   public ServerLevel getLevel() {
      return this.serverLevel;
   }

   public List<CustomSpawner> getCustomSpawners() {
      return this.customSpawners;
   }

   public void addCustomSpawner(CustomSpawner customSpawner) {
      this.customSpawners.add(customSpawner);
   }
}

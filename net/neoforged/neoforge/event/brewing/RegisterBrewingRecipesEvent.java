package net.neoforged.neoforge.event.brewing;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterBrewingRecipesEvent extends Event {
   private final PotionBrewing.Builder builder;
   private final RegistryAccess registryAccess;

   @Internal
   public RegisterBrewingRecipesEvent(PotionBrewing.Builder builder, RegistryAccess registryAccess) {
      this.builder = builder;
      this.registryAccess = registryAccess;
   }

   public PotionBrewing.Builder getBuilder() {
      return this.builder;
   }

   public RegistryAccess getRegistryAccess() {
      return this.registryAccess;
   }
}

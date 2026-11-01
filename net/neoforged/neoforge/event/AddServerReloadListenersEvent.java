package net.neoforged.neoforge.event;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.resource.VanillaServerListeners;
import org.jetbrains.annotations.ApiStatus.Internal;

public class AddServerReloadListenersEvent extends SortedReloadListenerEvent {
   private final ReloadableServerResources serverResources;
   private final RegistryAccess registryAccess;

   @Internal
   public AddServerReloadListenersEvent(ReloadableServerResources serverResources, RegistryAccess registryAccess) {
      super(serverResources.listeners(), AddServerReloadListenersEvent::lookupName);
      this.serverResources = serverResources;
      this.registryAccess = registryAccess;
   }

   public ReloadableServerResources getServerResources() {
      return this.serverResources;
   }

   public ICondition.IContext getConditionContext() {
      return this.serverResources.getConditionContext();
   }

   public RegistryAccess getRegistryAccess() {
      return this.registryAccess;
   }

   private static Identifier lookupName(PreparableReloadListener listener) {
      Identifier key = VanillaServerListeners.getNameForClass((Class<? extends PreparableReloadListener>)listener.getClass());
      if (key == null) {
         if (listener.getClass().getPackageName().startsWith("net.minecraft")) {
            throw new IllegalArgumentException("A key for the reload listener " + listener + " was not provided in VanillaServerListeners!");
         } else {
            throw new IllegalArgumentException(
               "A non-vanilla reload listener "
                  + listener
                  + " was added via mixin before the AddReloadListenerEvent! Mod-added listeners must go through the event."
            );
         }
      } else {
         return key;
      }
   }
}

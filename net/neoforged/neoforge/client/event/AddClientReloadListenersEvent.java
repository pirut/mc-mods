package net.neoforged.neoforge.client.event;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;
import net.neoforged.neoforge.event.SortedReloadListenerEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public class AddClientReloadListenersEvent extends SortedReloadListenerEvent implements IModBusEvent {
   @Internal
   public AddClientReloadListenersEvent(ReloadableResourceManager resourceManager) {
      super(resourceManager.getListeners(), AddClientReloadListenersEvent::lookupName);
   }

   private static Identifier lookupName(PreparableReloadListener listener) {
      Identifier key = VanillaClientListeners.getNameForClass((Class<? extends PreparableReloadListener>)listener.getClass());
      if (key == null) {
         if (listener.getClass().getPackageName().startsWith("net.minecraft")) {
            throw new IllegalArgumentException("A key for the reload listener " + listener + " was not provided in VanillaClientListeners!");
         } else {
            throw new IllegalArgumentException(
               "A non-vanilla reload listener "
                  + listener
                  + " was added via mixin before the AddClientReloadListenerEvent! Mod-added listeners must go through the event."
            );
         }
      } else {
         return key;
      }
   }
}

package net.neoforged.neoforge.internal;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.IBindingsProvider;
import net.neoforged.neoforge.common.NeoForge;

public class NeoForgeBindings implements IBindingsProvider {
   public IEventBus getGameBus() {
      return NeoForge.EVENT_BUS;
   }
}

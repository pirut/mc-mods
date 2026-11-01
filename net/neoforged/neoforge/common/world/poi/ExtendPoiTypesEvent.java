package net.neoforged.neoforge.common.world.poi;

import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

public final class ExtendPoiTypesEvent extends Event implements IModBusEvent {
   private final BiConsumer<ResourceKey<PoiType>, Set<BlockState>> registrar;

   ExtendPoiTypesEvent(BiConsumer<ResourceKey<PoiType>, Set<BlockState>> registrar) {
      this.registrar = registrar;
   }

   public void addBlockToPoi(ResourceKey<PoiType> typeKey, Block block) {
      this.addStatesToPoi(typeKey, Set.copyOf(block.getStateDefinition().getPossibleStates()));
   }

   public void addStatesToPoi(ResourceKey<PoiType> typeKey, Set<BlockState> states) {
      this.registrar.accept(typeKey, states);
   }
}

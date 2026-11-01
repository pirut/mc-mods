package net.neoforged.neoforge.fluids;

import java.util.Objects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.Nullable;

public class RegisterCauldronFluidContentEvent extends Event implements IModBusEvent {
   RegisterCauldronFluidContentEvent() {
   }

   public void register(Block block, Fluid fluid, int totalAmount, @Nullable IntegerProperty levelProperty) {
      Objects.requireNonNull(block, "Block may not be null");
      Objects.requireNonNull(fluid, "Fluid may not be null");
      CauldronFluidContent.register(block, fluid, totalAmount, levelProperty);
   }
}

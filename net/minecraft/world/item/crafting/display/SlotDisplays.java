package net.minecraft.world.item.crafting.display;

import net.minecraft.core.Registry;

public class SlotDisplays {
   public static SlotDisplay.Type<?> bootstrap(Registry<SlotDisplay.Type<?>> p_379401_) {
      Registry.register(p_379401_, "empty", SlotDisplay.Empty.TYPE);
      Registry.register(p_379401_, "any_fuel", SlotDisplay.AnyFuel.TYPE);
      Registry.register(p_379401_, "item", SlotDisplay.ItemSlotDisplay.TYPE);
      Registry.register(p_379401_, "item_stack", SlotDisplay.ItemStackSlotDisplay.TYPE);
      Registry.register(p_379401_, "tag", SlotDisplay.TagSlotDisplay.TYPE);
      Registry.register(p_379401_, "smithing_trim", SlotDisplay.SmithingTrimDemoSlotDisplay.TYPE);
      Registry.register(p_379401_, "with_remainder", SlotDisplay.WithRemainder.TYPE);
      return Registry.register(p_379401_, "composite", SlotDisplay.Composite.TYPE);
   }
}

package net.neoforged.neoforge.client.gui;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.function.Function;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public final class ClientTooltipComponentManager {
   private static ImmutableMap<Class<? extends TooltipComponent>, Function<TooltipComponent, ClientTooltipComponent>> FACTORIES;

   @Nullable
   public static ClientTooltipComponent createClientTooltipComponent(TooltipComponent component) {
      Function<TooltipComponent, ClientTooltipComponent> factory = (Function<TooltipComponent, ClientTooltipComponent>)FACTORIES.get(component.getClass());
      return factory != null ? factory.apply(component) : null;
   }

   @Internal
   public static void init() {
      HashMap<Class<? extends TooltipComponent>, Function<TooltipComponent, ClientTooltipComponent>> factories = new HashMap<>();
      RegisterClientTooltipComponentFactoriesEvent event = new RegisterClientTooltipComponentFactoriesEvent(factories);
      ModLoader.postEventWrapContainerInModOrder(event);
      FACTORIES = ImmutableMap.copyOf(factories);
   }

   private ClientTooltipComponentManager() {
   }
}

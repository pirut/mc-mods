package net.neoforged.neoforge.client.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.client.IItemDecorator;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterItemDecorationsEvent extends Event implements IModBusEvent {
   private final Map<Item, List<IItemDecorator>> decorators;

   @Internal
   public RegisterItemDecorationsEvent(Map<Item, List<IItemDecorator>> decorators) {
      this.decorators = decorators;
   }

   public void register(ItemLike itemLike, IItemDecorator decorator) {
      List<IItemDecorator> itemDecoratorList = this.decorators.computeIfAbsent(itemLike.asItem(), item -> new ArrayList<>());
      itemDecoratorList.add(decorator);
   }
}

package net.neoforged.neoforge.event;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class ModifyDefaultComponentsEvent extends Event implements IModBusEvent {
   public void modify(ItemLike item, Consumer<DataComponentPatch.Builder> patch) {
      DataComponentPatch.Builder builder = DataComponentPatch.builder();
      patch.accept(builder);
      DataComponentPatch compPatch = builder.build();
      if (!compPatch.isEmpty()) {
         item.asItem().modifyDefaultComponentsFrom(builder.build());
      }
   }

   public void modifyMatching(Predicate<? super Item> predicate, Consumer<DataComponentPatch.Builder> patch) {
      this.getAllItems().filter(predicate).forEach(item -> this.modify(item, patch));
   }

   public Stream<Item> getAllItems() {
      return BuiltInRegistries.ITEM.stream();
   }
}

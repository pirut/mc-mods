package net.neoforged.neoforge.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class ItemDecoratorHandler {
   private final List<IItemDecorator> itemDecorators;
   private static Map<Item, ItemDecoratorHandler> DECORATOR_LOOKUP = ImmutableMap.of();
   private static final ItemDecoratorHandler EMPTY = new ItemDecoratorHandler();

   private ItemDecoratorHandler() {
      this.itemDecorators = ImmutableList.of();
   }

   private ItemDecoratorHandler(List<IItemDecorator> itemDecorators) {
      this.itemDecorators = ImmutableList.copyOf(itemDecorators);
   }

   public static void init() {
      HashMap<Item, List<IItemDecorator>> decorators = new HashMap<>();
      RegisterItemDecorationsEvent event = new RegisterItemDecorationsEvent(decorators);
      ModLoader.postEventWrapContainerInModOrder(event);
      Builder<Item, ItemDecoratorHandler> builder = new Builder();
      decorators.forEach((item, itemDecorators) -> builder.put(item, new ItemDecoratorHandler((List<IItemDecorator>)itemDecorators)));
      DECORATOR_LOOKUP = builder.build();
   }

   public static ItemDecoratorHandler of(ItemStack stack) {
      return DECORATOR_LOOKUP.getOrDefault(stack.getItem(), EMPTY);
   }

   public void render(GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) {
      if (!this.itemDecorators.isEmpty()) {
         for (IItemDecorator itemDecorator : this.itemDecorators) {
            itemDecorator.render(guiGraphics, font, stack, xOffset, yOffset);
         }
      }
   }
}

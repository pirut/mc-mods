package net.neoforged.neoforge.capabilities;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class ItemCapability<T, C> extends BaseCapability<T, C> {
   private static final CapabilityRegistry<ItemCapability<?, ?>> registry = new CapabilityRegistry<>(ItemCapability::new);
   final Map<Item, List<ICapabilityProvider<ItemStack, C, T>>> providers = new IdentityHashMap<>();

   public static <T, C> ItemCapability<T, C> create(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
      return (ItemCapability<T, C>)registry.create(name, typeClass, contextClass);
   }

   public static <T> ItemCapability<T, Void> createVoid(ResourceLocation name, Class<T> typeClass) {
      return create(name, typeClass, void.class);
   }

   public static synchronized List<ItemCapability<?, ?>> getAll() {
      return registry.getAll();
   }

   private ItemCapability(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
      super(name, typeClass, contextClass);
   }

   @Internal
   @Nullable
   public T getCapability(ItemStack stack, C context) {
      if (stack.isEmpty()) {
         return null;
      } else {
         for (ICapabilityProvider<ItemStack, C, T> provider : this.providers.getOrDefault(stack.getItem(), List.of())) {
            T ret = provider.getCapability(stack, context);
            if (ret != null) {
               return ret;
            }
         }

         return null;
      }
   }
}

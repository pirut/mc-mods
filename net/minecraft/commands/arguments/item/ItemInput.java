package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemInput {
   private static final Dynamic2CommandExceptionType ERROR_STACK_TOO_BIG = new Dynamic2CommandExceptionType(
      (p_304131_, p_304132_) -> Component.translatableEscape("arguments.item.overstacked", p_304131_, p_304132_)
   );
   private final Holder<Item> item;
   private final DataComponentPatch components;

   public ItemInput(Holder<Item> p_235282_, DataComponentPatch p_345942_) {
      this.item = p_235282_;
      this.components = p_345942_;
   }

   public Item getItem() {
      return this.item.value();
   }

   public ItemStack createItemStack(int p_120981_, boolean p_120982_) throws CommandSyntaxException {
      ItemStack $$2 = new ItemStack(this.item, p_120981_);
      $$2.applyComponents(this.components);
      if (p_120982_ && p_120981_ > $$2.getMaxStackSize()) {
         throw ERROR_STACK_TOO_BIG.create(this.getItemName(), $$2.getMaxStackSize());
      } else {
         return $$2;
      }
   }

   public String serialize(HolderLookup.Provider p_330692_) {
      StringBuilder $$1 = new StringBuilder(this.getItemName());
      String $$2 = this.serializeComponents(p_330692_);
      if (!$$2.isEmpty()) {
         $$1.append('[');
         $$1.append($$2);
         $$1.append(']');
      }

      return $$1.toString();
   }

   private String serializeComponents(HolderLookup.Provider p_331015_) {
      DynamicOps<Tag> $$1 = p_331015_.createSerializationContext(NbtOps.INSTANCE);
      return this.components.entrySet().stream().flatMap(p_344174_ -> {
         DataComponentType<?> $$2 = p_344174_.getKey();
         ResourceLocation $$3 = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey($$2);
         if ($$3 == null) {
            return Stream.empty();
         } else {
            Optional<?> $$4 = p_344174_.getValue();
            if ($$4.isPresent()) {
               TypedDataComponent<?> $$5 = TypedDataComponent.createUnchecked($$2, $$4.get());
               return $$5.encodeValue($$1).result().stream().map(p_344172_ -> $$3.toString() + "=" + p_344172_);
            } else {
               return Stream.of("!" + $$3.toString());
            }
         }
      }).collect(Collectors.joining(String.valueOf(',')));
   }

   private String getItemName() {
      return this.item.unwrapKey().map(ResourceKey::location).orElseGet(() -> "unknown[" + this.item + "]").toString();
   }
}

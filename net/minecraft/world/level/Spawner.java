package net.minecraft.world.level;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public interface Spawner {
   void setEntityId(EntityType<?> var1, RandomSource var2);

   static void appendHoverText(ItemStack p_311863_, List<Component> p_312185_, String p_312723_) {
      Component $$3 = getSpawnEntityDisplayName(p_311863_, p_312723_);
      if ($$3 != null) {
         p_312185_.add($$3);
      } else {
         p_312185_.add(CommonComponents.EMPTY);
         p_312185_.add(Component.translatable("block.minecraft.spawner.desc1").withStyle(ChatFormatting.GRAY));
         p_312185_.add(CommonComponents.space().append(Component.translatable("block.minecraft.spawner.desc2").withStyle(ChatFormatting.BLUE)));
      }
   }

   @Nullable
   static Component getSpawnEntityDisplayName(ItemStack p_312585_, String p_312442_) {
      CompoundTag $$2 = p_312585_.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).getUnsafe();
      ResourceLocation $$3 = getEntityKey($$2, p_312442_);
      return $$3 != null
         ? BuiltInRegistries.ENTITY_TYPE
            .getOptional($$3)
            .map(p_312609_ -> Component.translatable(p_312609_.getDescriptionId()).withStyle(ChatFormatting.GRAY))
            .orElse(null)
         : null;
   }

   @Nullable
   private static ResourceLocation getEntityKey(CompoundTag p_312232_, String p_312196_) {
      if (p_312232_.contains(p_312196_, 10)) {
         String $$2 = p_312232_.getCompound(p_312196_).getCompound("entity").getString("id");
         return ResourceLocation.tryParse($$2);
      } else {
         return null;
      }
   }
}

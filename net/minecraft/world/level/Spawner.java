package net.minecraft.world.level;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jspecify.annotations.Nullable;

public interface Spawner {
   void setEntityId(EntityType<?> entityType, RandomSource random);

   static void appendHoverText(@Nullable TypedEntityData<BlockEntityType<?>> data, Consumer<Component> tooltipAdder, String key) {
      Component component = getSpawnEntityDisplayName(data, key);
      if (component != null) {
         tooltipAdder.accept(component);
      } else {
         tooltipAdder.accept(CommonComponents.EMPTY);
         tooltipAdder.accept(Component.translatable("block.minecraft.spawner.desc1").withStyle(ChatFormatting.GRAY));
         tooltipAdder.accept(CommonComponents.space().append(Component.translatable("block.minecraft.spawner.desc2").withStyle(ChatFormatting.BLUE)));
      }
   }

   @Nullable
   static Component getSpawnEntityDisplayName(@Nullable TypedEntityData<BlockEntityType<?>> data, String key) {
      return data == null
         ? null
         : data.getUnsafe()
            .getCompound(key)
            .flatMap(compoundTag -> compoundTag.getCompound("entity"))
            .flatMap(compoundTag -> compoundTag.read("id", EntityType.CODEC))
            .map(entityType -> Component.translatable(entityType.getDescriptionId()).withStyle(ChatFormatting.GRAY))
            .orElse(null);
   }
}

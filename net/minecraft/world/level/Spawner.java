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
   void setEntityId(EntityType<?> var1, RandomSource var2);

   static void appendHoverText(@Nullable TypedEntityData<BlockEntityType<?>> p_434417_, Consumer<Component> p_400266_, String p_312723_) {
      Component component = getSpawnEntityDisplayName(p_434417_, p_312723_);
      if (component != null) {
         p_400266_.accept(component);
      } else {
         p_400266_.accept(CommonComponents.EMPTY);
         p_400266_.accept(Component.translatable("block.minecraft.spawner.desc1").withStyle(ChatFormatting.GRAY));
         p_400266_.accept(CommonComponents.space().append(Component.translatable("block.minecraft.spawner.desc2").withStyle(ChatFormatting.BLUE)));
      }
   }

   @Nullable
   static Component getSpawnEntityDisplayName(@Nullable TypedEntityData<BlockEntityType<?>> p_434839_, String p_312442_) {
      return p_434839_ == null
         ? null
         : p_434839_.getUnsafe()
            .getCompound(p_312442_)
            .flatMap(p_409468_ -> p_409468_.getCompound("entity"))
            .flatMap(p_409467_ -> p_409467_.read("id", EntityType.CODEC))
            .map(p_312609_ -> Component.translatable(p_312609_.getDescriptionId()).withStyle(ChatFormatting.GRAY))
            .orElse(null);
   }
}

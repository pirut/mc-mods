package net.minecraft.world.level.block;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.ItemLike;

public interface SuspiciousEffectHolder {
   SuspiciousStewEffects getSuspiciousEffects();

   static List<SuspiciousEffectHolder> getAllEffectHolders() {
      return BuiltInRegistries.ITEM.stream().map(SuspiciousEffectHolder::tryGet).filter(Objects::nonNull).collect(Collectors.toList());
   }

   @Nullable
   static SuspiciousEffectHolder tryGet(ItemLike p_259322_) {
      if (p_259322_.asItem() instanceof BlockItem $$1 && $$1.getBlock() instanceof SuspiciousEffectHolder $$2) {
         return $$2;
      } else {
         return p_259322_.asItem() instanceof SuspiciousEffectHolder $$3 ? $$3 : null;
      }
   }
}

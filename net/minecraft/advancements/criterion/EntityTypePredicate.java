package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public record EntityTypePredicate(HolderSet<EntityType<?>> types) {
   public static final Codec<EntityTypePredicate> CODEC = RegistryCodecs.homogeneousList(Registries.ENTITY_TYPE)
      .xmap(EntityTypePredicate::new, EntityTypePredicate::types);

   public static EntityTypePredicate of(HolderGetter<EntityType<?>> p_469077_, EntityType<?> p_469862_) {
      return new EntityTypePredicate(HolderSet.direct(p_469862_.builtInRegistryHolder()));
   }

   public static EntityTypePredicate of(HolderGetter<EntityType<?>> p_467122_, TagKey<EntityType<?>> p_469192_) {
      return new EntityTypePredicate(p_467122_.getOrThrow(p_469192_));
   }

   public boolean matches(EntityType<?> p_468124_) {
      return p_468124_.is(this.types);
   }
}

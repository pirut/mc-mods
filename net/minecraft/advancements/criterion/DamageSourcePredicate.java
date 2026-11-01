package net.minecraft.advancements.criterion;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

public record DamageSourcePredicate(
   List<TagPredicate<DamageType>> tags, Optional<EntityPredicate> directEntity, Optional<EntityPredicate> sourceEntity, Optional<Boolean> isDirect
) {
   public static final Codec<DamageSourcePredicate> CODEC = RecordCodecBuilder.create(
      p_469881_ -> p_469881_.group(
            TagPredicate.codec(Registries.DAMAGE_TYPE).listOf().optionalFieldOf("tags", List.of()).forGetter(DamageSourcePredicate::tags),
            EntityPredicate.CODEC.optionalFieldOf("direct_entity").forGetter(DamageSourcePredicate::directEntity),
            EntityPredicate.CODEC.optionalFieldOf("source_entity").forGetter(DamageSourcePredicate::sourceEntity),
            Codec.BOOL.optionalFieldOf("is_direct").forGetter(DamageSourcePredicate::isDirect)
         )
         .apply(p_469881_, DamageSourcePredicate::new)
   );

   public boolean matches(ServerPlayer p_469106_, DamageSource p_468342_) {
      return this.matches(p_469106_.level(), p_469106_.position(), p_468342_);
   }

   public boolean matches(ServerLevel p_466989_, Vec3 p_467296_, DamageSource p_468930_) {
      for (TagPredicate<DamageType> tagpredicate : this.tags) {
         if (!tagpredicate.matches(p_468930_.typeHolder())) {
            return false;
         }
      }

      if (this.directEntity.isPresent() && !this.directEntity.get().matches(p_466989_, p_467296_, p_468930_.getDirectEntity())) {
         return false;
      } else {
         return this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(p_466989_, p_467296_, p_468930_.getEntity())
            ? false
            : !this.isDirect.isPresent() || this.isDirect.get() == p_468930_.isDirect();
      }
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableList.Builder<TagPredicate<DamageType>> tags = ImmutableList.builder();
      private Optional<EntityPredicate> directEntity = Optional.empty();
      private Optional<EntityPredicate> sourceEntity = Optional.empty();
      private Optional<Boolean> isDirect = Optional.empty();

      public static DamageSourcePredicate.Builder damageType() {
         return new DamageSourcePredicate.Builder();
      }

      public DamageSourcePredicate.Builder tag(TagPredicate<DamageType> p_468450_) {
         this.tags.add(p_468450_);
         return this;
      }

      public DamageSourcePredicate.Builder direct(EntityPredicate.Builder p_468764_) {
         this.directEntity = Optional.of(p_468764_.build());
         return this;
      }

      public DamageSourcePredicate.Builder source(EntityPredicate.Builder p_467241_) {
         this.sourceEntity = Optional.of(p_467241_.build());
         return this;
      }

      public DamageSourcePredicate.Builder isDirect(boolean p_467095_) {
         this.isDirect = Optional.of(p_467095_);
         return this;
      }

      public DamageSourcePredicate build() {
         return new DamageSourcePredicate(this.tags.build(), this.directEntity, this.sourceEntity, this.isDirect);
      }
   }
}

package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

public record EntityPredicate(
   Optional<EntityTypePredicate> entityType,
   Optional<DistancePredicate> distanceToPlayer,
   Optional<MovementPredicate> movement,
   EntityPredicate.LocationWrapper location,
   Optional<MobEffectsPredicate> effects,
   Optional<NbtPredicate> nbt,
   Optional<EntityFlagsPredicate> flags,
   Optional<EntityEquipmentPredicate> equipment,
   Optional<EntitySubPredicate> subPredicate,
   Optional<Integer> periodicTick,
   Optional<EntityPredicate> vehicle,
   Optional<EntityPredicate> passenger,
   Optional<EntityPredicate> targetedEntity,
   Optional<String> team,
   Optional<SlotsPredicate> slots,
   DataComponentMatchers components
) {
   public static final Codec<EntityPredicate> CODEC = Codec.recursive(
      "EntityPredicate",
      p_469121_ -> RecordCodecBuilder.create(
         p_469852_ -> p_469852_.group(
               EntityTypePredicate.CODEC.optionalFieldOf("type").forGetter(EntityPredicate::entityType),
               DistancePredicate.CODEC.optionalFieldOf("distance").forGetter(EntityPredicate::distanceToPlayer),
               MovementPredicate.CODEC.optionalFieldOf("movement").forGetter(EntityPredicate::movement),
               EntityPredicate.LocationWrapper.CODEC.forGetter(EntityPredicate::location),
               MobEffectsPredicate.CODEC.optionalFieldOf("effects").forGetter(EntityPredicate::effects),
               NbtPredicate.CODEC.optionalFieldOf("nbt").forGetter(EntityPredicate::nbt),
               EntityFlagsPredicate.CODEC.optionalFieldOf("flags").forGetter(EntityPredicate::flags),
               EntityEquipmentPredicate.CODEC.optionalFieldOf("equipment").forGetter(EntityPredicate::equipment),
               EntitySubPredicate.CODEC.optionalFieldOf("type_specific").forGetter(EntityPredicate::subPredicate),
               ExtraCodecs.POSITIVE_INT.optionalFieldOf("periodic_tick").forGetter(EntityPredicate::periodicTick),
               p_469121_.optionalFieldOf("vehicle").forGetter(EntityPredicate::vehicle),
               p_469121_.optionalFieldOf("passenger").forGetter(EntityPredicate::passenger),
               p_469121_.optionalFieldOf("targeted_entity").forGetter(EntityPredicate::targetedEntity),
               Codec.STRING.optionalFieldOf("team").forGetter(EntityPredicate::team),
               SlotsPredicate.CODEC.optionalFieldOf("slots").forGetter(EntityPredicate::slots),
               DataComponentMatchers.CODEC.forGetter(EntityPredicate::components)
            )
            .apply(p_469852_, EntityPredicate::new)
      )
   );
   public static final Codec<ContextAwarePredicate> ADVANCEMENT_CODEC = Codec.withAlternative(ContextAwarePredicate.CODEC, CODEC, EntityPredicate::wrap);

   public static ContextAwarePredicate wrap(EntityPredicate.Builder p_466868_) {
      return wrap(p_466868_.build());
   }

   public static Optional<ContextAwarePredicate> wrap(Optional<EntityPredicate> p_469648_) {
      return p_469648_.map(EntityPredicate::wrap);
   }

   public static List<ContextAwarePredicate> wrap(EntityPredicate.Builder... p_466966_) {
      return Stream.of(p_466966_).map(EntityPredicate::wrap).toList();
   }

   public static ContextAwarePredicate wrap(EntityPredicate p_469710_) {
      LootItemCondition lootitemcondition = LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, p_469710_).build();
      return new ContextAwarePredicate(List.of(lootitemcondition));
   }

   public boolean matches(ServerPlayer p_469795_, @Nullable Entity p_468632_) {
      return this.matches(p_469795_.level(), p_469795_.position(), p_468632_);
   }

   public boolean matches(ServerLevel p_468232_, @Nullable Vec3 p_468873_, @Nullable Entity p_468865_) {
      if (p_468865_ == null) {
         return false;
      } else if (this.entityType.isPresent() && !this.entityType.get().matches(p_468865_.getType())) {
         return false;
      } else {
         if (p_468873_ == null) {
            if (this.distanceToPlayer.isPresent()) {
               return false;
            }
         } else if (this.distanceToPlayer.isPresent()
            && !this.distanceToPlayer.get().matches(p_468873_.x, p_468873_.y, p_468873_.z, p_468865_.getX(), p_468865_.getY(), p_468865_.getZ())) {
            return false;
         }

         if (this.movement.isPresent()) {
            Vec3 vec3 = p_468865_.getKnownMovement();
            Vec3 vec31 = vec3.scale(20.0);
            if (!this.movement.get().matches(vec31.x, vec31.y, vec31.z, p_468865_.fallDistance)) {
               return false;
            }
         }

         if (this.location.located.isPresent() && !this.location.located.get().matches(p_468232_, p_468865_.getX(), p_468865_.getY(), p_468865_.getZ())) {
            return false;
         } else {
            if (this.location.steppingOn.isPresent()) {
               Vec3 vec32 = Vec3.atCenterOf(p_468865_.getOnPos());
               if (!p_468865_.onGround() || !this.location.steppingOn.get().matches(p_468232_, vec32.x(), vec32.y(), vec32.z())) {
                  return false;
               }
            }

            if (this.location.affectsMovement.isPresent()) {
               Vec3 vec33 = Vec3.atCenterOf(p_468865_.getBlockPosBelowThatAffectsMyMovement());
               if (!this.location.affectsMovement.get().matches(p_468232_, vec33.x(), vec33.y(), vec33.z())) {
                  return false;
               }
            }

            if (this.effects.isPresent() && !this.effects.get().matches(p_468865_)) {
               return false;
            } else if (this.flags.isPresent() && !this.flags.get().matches(p_468865_)) {
               return false;
            } else if (this.equipment.isPresent() && !this.equipment.get().matches(p_468865_)) {
               return false;
            } else if (this.subPredicate.isPresent() && !this.subPredicate.get().matches(p_468865_, p_468232_, p_468873_)) {
               return false;
            } else if (this.vehicle.isPresent() && !this.vehicle.get().matches(p_468232_, p_468873_, p_468865_.getVehicle())) {
               return false;
            } else if (this.passenger.isPresent()
               && p_468865_.getPassengers().stream().noneMatch(p_469185_ -> this.passenger.get().matches(p_468232_, p_468873_, p_469185_))) {
               return false;
            } else if (this.targetedEntity.isPresent()
               && !this.targetedEntity.get().matches(p_468232_, p_468873_, p_468865_ instanceof Mob ? ((Mob)p_468865_).getTarget() : null)) {
               return false;
            } else if (this.periodicTick.isPresent() && p_468865_.tickCount % this.periodicTick.get() != 0) {
               return false;
            } else {
               if (this.team.isPresent()) {
                  Team team = p_468865_.getTeam();
                  if (team == null || !this.team.get().equals(team.getName())) {
                     return false;
                  }
               }

               if (this.slots.isPresent() && !this.slots.get().matches(p_468865_)) {
                  return false;
               } else {
                  return !this.components.test((DataComponentGetter)p_468865_) ? false : this.nbt.isEmpty() || this.nbt.get().matches(p_468865_);
               }
            }
         }
      }
   }

   public static LootContext createContext(ServerPlayer p_468039_, Entity p_467600_) {
      LootParams lootparams = new LootParams.Builder(p_468039_.level())
         .withParameter(LootContextParams.THIS_ENTITY, p_467600_)
         .withParameter(LootContextParams.ORIGIN, p_468039_.position())
         .create(LootContextParamSets.ADVANCEMENT_ENTITY);
      return new LootContext.Builder(lootparams).create(Optional.empty());
   }

   public static class Builder {
      private Optional<EntityTypePredicate> entityType = Optional.empty();
      private Optional<DistancePredicate> distanceToPlayer = Optional.empty();
      private Optional<MovementPredicate> movement = Optional.empty();
      private Optional<LocationPredicate> located = Optional.empty();
      private Optional<LocationPredicate> steppingOnLocation = Optional.empty();
      private Optional<LocationPredicate> movementAffectedBy = Optional.empty();
      private Optional<MobEffectsPredicate> effects = Optional.empty();
      private Optional<NbtPredicate> nbt = Optional.empty();
      private Optional<EntityFlagsPredicate> flags = Optional.empty();
      private Optional<EntityEquipmentPredicate> equipment = Optional.empty();
      private Optional<EntitySubPredicate> subPredicate = Optional.empty();
      private Optional<Integer> periodicTick = Optional.empty();
      private Optional<EntityPredicate> vehicle = Optional.empty();
      private Optional<EntityPredicate> passenger = Optional.empty();
      private Optional<EntityPredicate> targetedEntity = Optional.empty();
      private Optional<String> team = Optional.empty();
      private Optional<SlotsPredicate> slots = Optional.empty();
      private DataComponentMatchers components = DataComponentMatchers.ANY;

      public static EntityPredicate.Builder entity() {
         return new EntityPredicate.Builder();
      }

      public EntityPredicate.Builder of(HolderGetter<EntityType<?>> p_468264_, EntityType<?> p_467272_) {
         this.entityType = Optional.of(EntityTypePredicate.of(p_468264_, p_467272_));
         return this;
      }

      public EntityPredicate.Builder of(HolderGetter<EntityType<?>> p_468773_, TagKey<EntityType<?>> p_467469_) {
         this.entityType = Optional.of(EntityTypePredicate.of(p_468773_, p_467469_));
         return this;
      }

      public EntityPredicate.Builder entityType(EntityTypePredicate p_468455_) {
         this.entityType = Optional.of(p_468455_);
         return this;
      }

      public EntityPredicate.Builder distance(DistancePredicate p_469376_) {
         this.distanceToPlayer = Optional.of(p_469376_);
         return this;
      }

      public EntityPredicate.Builder moving(MovementPredicate p_467198_) {
         this.movement = Optional.of(p_467198_);
         return this;
      }

      public EntityPredicate.Builder located(LocationPredicate.Builder p_468803_) {
         this.located = Optional.of(p_468803_.build());
         return this;
      }

      public EntityPredicate.Builder steppingOn(LocationPredicate.Builder p_467363_) {
         this.steppingOnLocation = Optional.of(p_467363_.build());
         return this;
      }

      public EntityPredicate.Builder movementAffectedBy(LocationPredicate.Builder p_467438_) {
         this.movementAffectedBy = Optional.of(p_467438_.build());
         return this;
      }

      public EntityPredicate.Builder effects(MobEffectsPredicate.Builder p_469239_) {
         this.effects = p_469239_.build();
         return this;
      }

      public EntityPredicate.Builder nbt(NbtPredicate p_467882_) {
         this.nbt = Optional.of(p_467882_);
         return this;
      }

      public EntityPredicate.Builder flags(EntityFlagsPredicate.Builder p_469981_) {
         this.flags = Optional.of(p_469981_.build());
         return this;
      }

      public EntityPredicate.Builder equipment(EntityEquipmentPredicate.Builder p_469889_) {
         this.equipment = Optional.of(p_469889_.build());
         return this;
      }

      public EntityPredicate.Builder equipment(EntityEquipmentPredicate p_467029_) {
         this.equipment = Optional.of(p_467029_);
         return this;
      }

      public EntityPredicate.Builder subPredicate(EntitySubPredicate p_467273_) {
         this.subPredicate = Optional.of(p_467273_);
         return this;
      }

      public EntityPredicate.Builder periodicTick(int p_467996_) {
         this.periodicTick = Optional.of(p_467996_);
         return this;
      }

      public EntityPredicate.Builder vehicle(EntityPredicate.Builder p_467419_) {
         this.vehicle = Optional.of(p_467419_.build());
         return this;
      }

      public EntityPredicate.Builder passenger(EntityPredicate.Builder p_467267_) {
         this.passenger = Optional.of(p_467267_.build());
         return this;
      }

      public EntityPredicate.Builder targetedEntity(EntityPredicate.Builder p_469827_) {
         this.targetedEntity = Optional.of(p_469827_.build());
         return this;
      }

      public EntityPredicate.Builder team(String p_468291_) {
         this.team = Optional.of(p_468291_);
         return this;
      }

      public EntityPredicate.Builder slots(SlotsPredicate p_469956_) {
         this.slots = Optional.of(p_469956_);
         return this;
      }

      public EntityPredicate.Builder components(DataComponentMatchers p_468193_) {
         this.components = p_468193_;
         return this;
      }

      public EntityPredicate build() {
         return new EntityPredicate(
            this.entityType,
            this.distanceToPlayer,
            this.movement,
            new EntityPredicate.LocationWrapper(this.located, this.steppingOnLocation, this.movementAffectedBy),
            this.effects,
            this.nbt,
            this.flags,
            this.equipment,
            this.subPredicate,
            this.periodicTick,
            this.vehicle,
            this.passenger,
            this.targetedEntity,
            this.team,
            this.slots,
            this.components
         );
      }
   }

   public record LocationWrapper(Optional<LocationPredicate> located, Optional<LocationPredicate> steppingOn, Optional<LocationPredicate> affectsMovement) {
      public static final MapCodec<EntityPredicate.LocationWrapper> CODEC = RecordCodecBuilder.mapCodec(
         p_469301_ -> p_469301_.group(
               LocationPredicate.CODEC.optionalFieldOf("location").forGetter(EntityPredicate.LocationWrapper::located),
               LocationPredicate.CODEC.optionalFieldOf("stepping_on").forGetter(EntityPredicate.LocationWrapper::steppingOn),
               LocationPredicate.CODEC.optionalFieldOf("movement_affected_by").forGetter(EntityPredicate.LocationWrapper::affectsMovement)
            )
            .apply(p_469301_, EntityPredicate.LocationWrapper::new)
      );
   }
}

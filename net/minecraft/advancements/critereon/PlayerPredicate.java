package net.minecraft.advancements.critereon;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public record PlayerPredicate(
   MinMaxBounds.Ints level,
   GameTypePredicate gameType,
   List<PlayerPredicate.StatMatcher<?>> stats,
   Object2BooleanMap<ResourceLocation> recipes,
   Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> advancements,
   Optional<EntityPredicate> lookingAt
) implements EntitySubPredicate {
   public static final int LOOKING_AT_RANGE = 100;
   public static final MapCodec<PlayerPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_297911_ -> p_297911_.group(
            MinMaxBounds.Ints.CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(PlayerPredicate::level),
            GameTypePredicate.CODEC.optionalFieldOf("gamemode", GameTypePredicate.ANY).forGetter(PlayerPredicate::gameType),
            PlayerPredicate.StatMatcher.CODEC.listOf().optionalFieldOf("stats", List.of()).forGetter(PlayerPredicate::stats),
            ExtraCodecs.object2BooleanMap(ResourceLocation.CODEC).optionalFieldOf("recipes", Object2BooleanMaps.emptyMap()).forGetter(PlayerPredicate::recipes),
            Codec.unboundedMap(ResourceLocation.CODEC, PlayerPredicate.AdvancementPredicate.CODEC)
               .optionalFieldOf("advancements", Map.of())
               .forGetter(PlayerPredicate::advancements),
            EntityPredicate.CODEC.optionalFieldOf("looking_at").forGetter(PlayerPredicate::lookingAt)
         )
         .apply(p_297911_, PlayerPredicate::new)
   );

   @Override
   public boolean matches(Entity p_222484_, ServerLevel p_222485_, @Nullable Vec3 p_222486_) {
      if (!(p_222484_ instanceof ServerPlayer $$3)) {
         return false;
      } else if (!this.level.matches($$3.experienceLevel)) {
         return false;
      } else if (!this.gameType.matches($$3.gameMode.getGameModeForPlayer())) {
         return false;
      } else {
         StatsCounter $$5 = $$3.getStats();

         for (PlayerPredicate.StatMatcher<?> $$6 : this.stats) {
            if (!$$6.matches($$5)) {
               return false;
            }
         }

         RecipeBook $$7 = $$3.getRecipeBook();
         ObjectIterator var13 = this.recipes.object2BooleanEntrySet().iterator();

         while (var13.hasNext()) {
            Entry<ResourceLocation> $$8 = (Entry<ResourceLocation>)var13.next();
            if ($$7.contains((ResourceLocation)$$8.getKey()) != $$8.getBooleanValue()) {
               return false;
            }
         }

         if (!this.advancements.isEmpty()) {
            PlayerAdvancements $$9 = $$3.getAdvancements();
            ServerAdvancementManager $$10 = $$3.getServer().getAdvancements();

            for (java.util.Map.Entry<ResourceLocation, PlayerPredicate.AdvancementPredicate> $$11 : this.advancements.entrySet()) {
               AdvancementHolder $$12 = $$10.get($$11.getKey());
               if ($$12 == null || !$$11.getValue().test($$9.getOrStartProgress($$12))) {
                  return false;
               }
            }
         }

         if (this.lookingAt.isPresent()) {
            Vec3 $$13 = $$3.getEyePosition();
            Vec3 $$14 = $$3.getViewVector(1.0F);
            Vec3 $$15 = $$13.add($$14.x * 100.0, $$14.y * 100.0, $$14.z * 100.0);
            EntityHitResult $$16 = ProjectileUtil.getEntityHitResult(
               $$3.level(), $$3, $$13, $$15, new AABB($$13, $$15).inflate(1.0), p_156765_ -> !p_156765_.isSpectator(), 0.0F
            );
            if ($$16 == null || $$16.getType() != HitResult.Type.ENTITY) {
               return false;
            }

            Entity $$17 = $$16.getEntity();
            if (!this.lookingAt.get().matches($$3, $$17) || !$$3.hasLineOfSight($$17)) {
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public MapCodec<PlayerPredicate> codec() {
      return EntitySubPredicates.PLAYER;
   }

   record AdvancementCriterionsPredicate(Object2BooleanMap<String> criterions) implements PlayerPredicate.AdvancementPredicate {
      public static final Codec<PlayerPredicate.AdvancementCriterionsPredicate> CODEC = ExtraCodecs.object2BooleanMap(Codec.STRING)
         .xmap(PlayerPredicate.AdvancementCriterionsPredicate::new, PlayerPredicate.AdvancementCriterionsPredicate::criterions);

      public boolean test(AdvancementProgress p_62296_) {
         ObjectIterator var2 = this.criterions.object2BooleanEntrySet().iterator();

         while (var2.hasNext()) {
            Entry<String> $$1 = (Entry<String>)var2.next();
            CriterionProgress $$2 = p_62296_.getCriterion((String)$$1.getKey());
            if ($$2 == null || $$2.isDone() != $$1.getBooleanValue()) {
               return false;
            }
         }

         return true;
      }
   }

   record AdvancementDonePredicate(boolean state) implements PlayerPredicate.AdvancementPredicate {
      public static final Codec<PlayerPredicate.AdvancementDonePredicate> CODEC = Codec.BOOL
         .xmap(PlayerPredicate.AdvancementDonePredicate::new, PlayerPredicate.AdvancementDonePredicate::state);

      public boolean test(AdvancementProgress p_62304_) {
         return p_62304_.isDone() == this.state;
      }
   }

   interface AdvancementPredicate extends Predicate<AdvancementProgress> {
      Codec<PlayerPredicate.AdvancementPredicate> CODEC = Codec.either(
            PlayerPredicate.AdvancementDonePredicate.CODEC, PlayerPredicate.AdvancementCriterionsPredicate.CODEC
         )
         .xmap(Either::unwrap, p_298611_ -> {
            if (p_298611_ instanceof PlayerPredicate.AdvancementDonePredicate $$1) {
               return Either.left($$1);
            } else if (p_298611_ instanceof PlayerPredicate.AdvancementCriterionsPredicate $$2) {
               return Either.right($$2);
            } else {
               throw new UnsupportedOperationException();
            }
         });
   }

   public static class Builder {
      private MinMaxBounds.Ints level = MinMaxBounds.Ints.ANY;
      private GameTypePredicate gameType = GameTypePredicate.ANY;
      private final com.google.common.collect.ImmutableList.Builder<PlayerPredicate.StatMatcher<?>> stats = ImmutableList.builder();
      private final Object2BooleanMap<ResourceLocation> recipes = new Object2BooleanOpenHashMap();
      private final Map<ResourceLocation, PlayerPredicate.AdvancementPredicate> advancements = Maps.newHashMap();
      private Optional<EntityPredicate> lookingAt = Optional.empty();

      public static PlayerPredicate.Builder player() {
         return new PlayerPredicate.Builder();
      }

      public PlayerPredicate.Builder setLevel(MinMaxBounds.Ints p_156776_) {
         this.level = p_156776_;
         return this;
      }

      public <T> PlayerPredicate.Builder addStat(StatType<T> p_299266_, Holder.Reference<T> p_299053_, MinMaxBounds.Ints p_156770_) {
         this.stats.add(new PlayerPredicate.StatMatcher<>(p_299266_, p_299053_, p_156770_));
         return this;
      }

      public PlayerPredicate.Builder addRecipe(ResourceLocation p_156781_, boolean p_156782_) {
         this.recipes.put(p_156781_, p_156782_);
         return this;
      }

      public PlayerPredicate.Builder setGameType(GameTypePredicate p_350565_) {
         this.gameType = p_350565_;
         return this;
      }

      public PlayerPredicate.Builder setLookingAt(EntityPredicate.Builder p_301131_) {
         this.lookingAt = Optional.of(p_301131_.build());
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementDone(ResourceLocation p_156784_, boolean p_156785_) {
         this.advancements.put(p_156784_, new PlayerPredicate.AdvancementDonePredicate(p_156785_));
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementCriterions(ResourceLocation p_156778_, Map<String, Boolean> p_156779_) {
         this.advancements.put(p_156778_, new PlayerPredicate.AdvancementCriterionsPredicate(new Object2BooleanOpenHashMap(p_156779_)));
         return this;
      }

      public PlayerPredicate build() {
         return new PlayerPredicate(this.level, this.gameType, this.stats.build(), this.recipes, this.advancements, this.lookingAt);
      }
   }

   record StatMatcher<T>(StatType<T> type, Holder<T> value, MinMaxBounds.Ints range, Supplier<Stat<T>> stat) {
      public static final Codec<PlayerPredicate.StatMatcher<?>> CODEC = BuiltInRegistries.STAT_TYPE
         .byNameCodec()
         .dispatch(PlayerPredicate.StatMatcher::type, PlayerPredicate.StatMatcher::createTypedCodec);

      public StatMatcher(StatType<T> p_299207_, Holder<T> p_298208_, MinMaxBounds.Ints p_299102_) {
         this(p_299207_, p_298208_, p_299102_, Suppliers.memoize(() -> p_299207_.get(p_298208_.value())));
      }

      private static <T> MapCodec<PlayerPredicate.StatMatcher<T>> createTypedCodec(StatType<T> p_299086_) {
         return RecordCodecBuilder.mapCodec(
            p_337389_ -> p_337389_.group(
                  p_299086_.getRegistry().holderByNameCodec().fieldOf("stat").forGetter(PlayerPredicate.StatMatcher::value),
                  MinMaxBounds.Ints.CODEC.optionalFieldOf("value", MinMaxBounds.Ints.ANY).forGetter(PlayerPredicate.StatMatcher::range)
               )
               .apply(p_337389_, (p_298946_, p_299298_) -> new PlayerPredicate.StatMatcher<>(p_299086_, p_298946_, p_299298_))
         );
      }

      public boolean matches(StatsCounter p_298568_) {
         return this.range.matches(p_298568_.getValue(this.stat.get()));
      }
   }
}

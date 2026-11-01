package net.minecraft.advancements.criterion;

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
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record PlayerPredicate(
   MinMaxBounds.Ints level,
   GameTypePredicate gameType,
   List<PlayerPredicate.StatMatcher<?>> stats,
   Object2BooleanMap<ResourceKey<Recipe<?>>> recipes,
   Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements,
   Optional<EntityPredicate> lookingAt,
   Optional<InputPredicate> input
) implements EntitySubPredicate {
   public static final int LOOKING_AT_RANGE = 100;
   public static final MapCodec<PlayerPredicate> CODEC = RecordCodecBuilder.mapCodec(
      p_467941_ -> p_467941_.group(
            MinMaxBounds.Ints.CODEC.optionalFieldOf("level", MinMaxBounds.Ints.ANY).forGetter(PlayerPredicate::level),
            GameTypePredicate.CODEC.optionalFieldOf("gamemode", GameTypePredicate.ANY).forGetter(PlayerPredicate::gameType),
            PlayerPredicate.StatMatcher.CODEC.listOf().optionalFieldOf("stats", List.of()).forGetter(PlayerPredicate::stats),
            ExtraCodecs.object2BooleanMap(Recipe.KEY_CODEC).optionalFieldOf("recipes", Object2BooleanMaps.emptyMap()).forGetter(PlayerPredicate::recipes),
            Codec.unboundedMap(Identifier.CODEC, PlayerPredicate.AdvancementPredicate.CODEC)
               .optionalFieldOf("advancements", Map.of())
               .forGetter(PlayerPredicate::advancements),
            EntityPredicate.CODEC.optionalFieldOf("looking_at").forGetter(PlayerPredicate::lookingAt),
            InputPredicate.CODEC.optionalFieldOf("input").forGetter(PlayerPredicate::input)
         )
         .apply(p_467941_, PlayerPredicate::new)
   );

   @Override
   public boolean matches(Entity p_468814_, ServerLevel p_469653_, @Nullable Vec3 p_468235_) {
      if (!(p_468814_ instanceof ServerPlayer serverplayer)) {
         return false;
      } else if (!this.level.matches(serverplayer.experienceLevel)) {
         return false;
      } else if (!this.gameType.matches(serverplayer.gameMode())) {
         return false;
      } else {
         StatsCounter statscounter = serverplayer.getStats();

         for (PlayerPredicate.StatMatcher<?> statmatcher : this.stats) {
            if (!statmatcher.matches(statscounter)) {
               return false;
            }
         }

         ServerRecipeBook serverrecipebook = serverplayer.getRecipeBook();
         ObjectIterator var13 = this.recipes.object2BooleanEntrySet().iterator();

         while (var13.hasNext()) {
            Entry<ResourceKey<Recipe<?>>> entry = (Entry<ResourceKey<Recipe<?>>>)var13.next();
            if (serverrecipebook.contains((ResourceKey<Recipe<?>>)entry.getKey()) != entry.getBooleanValue()) {
               return false;
            }
         }

         if (!this.advancements.isEmpty()) {
            PlayerAdvancements playeradvancements = serverplayer.getAdvancements();
            ServerAdvancementManager serveradvancementmanager = serverplayer.level().getServer().getAdvancements();

            for (java.util.Map.Entry<Identifier, PlayerPredicate.AdvancementPredicate> entry1 : this.advancements.entrySet()) {
               AdvancementHolder advancementholder = serveradvancementmanager.get(entry1.getKey());
               if (advancementholder == null || !entry1.getValue().test(playeradvancements.getOrStartProgress(advancementholder))) {
                  return false;
               }
            }
         }

         if (this.lookingAt.isPresent()) {
            Vec3 vec3 = serverplayer.getEyePosition();
            Vec3 vec31 = serverplayer.getViewVector(1.0F);
            Vec3 vec32 = vec3.add(vec31.x * 100.0, vec31.y * 100.0, vec31.z * 100.0);
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(
               serverplayer.level(), serverplayer, vec3, vec32, new AABB(vec3, vec32).inflate(1.0), p_467954_ -> !p_467954_.isSpectator(), 0.0F
            );
            if (entityhitresult == null || entityhitresult.getType() != HitResult.Type.ENTITY) {
               return false;
            }

            Entity entity = entityhitresult.getEntity();
            if (!this.lookingAt.get().matches(serverplayer, entity) || !serverplayer.hasLineOfSight(entity)) {
               return false;
            }
         }

         return !this.input.isPresent() || this.input.get().matches(serverplayer.getLastClientInput());
      }
   }

   @Override
   public MapCodec<PlayerPredicate> codec() {
      return EntitySubPredicates.PLAYER;
   }

   record AdvancementCriterionsPredicate(Object2BooleanMap<String> criterions) implements PlayerPredicate.AdvancementPredicate {
      public static final Codec<PlayerPredicate.AdvancementCriterionsPredicate> CODEC = ExtraCodecs.object2BooleanMap(Codec.STRING)
         .xmap(PlayerPredicate.AdvancementCriterionsPredicate::new, PlayerPredicate.AdvancementCriterionsPredicate::criterions);

      public boolean test(AdvancementProgress p_467555_) {
         ObjectIterator var2 = this.criterions.object2BooleanEntrySet().iterator();

         while (var2.hasNext()) {
            Entry<String> entry = (Entry<String>)var2.next();
            CriterionProgress criterionprogress = p_467555_.getCriterion((String)entry.getKey());
            if (criterionprogress == null || criterionprogress.isDone() != entry.getBooleanValue()) {
               return false;
            }
         }

         return true;
      }
   }

   record AdvancementDonePredicate(boolean state) implements PlayerPredicate.AdvancementPredicate {
      public static final Codec<PlayerPredicate.AdvancementDonePredicate> CODEC = Codec.BOOL
         .xmap(PlayerPredicate.AdvancementDonePredicate::new, PlayerPredicate.AdvancementDonePredicate::state);

      public boolean test(AdvancementProgress p_469455_) {
         return p_469455_.isDone() == this.state;
      }
   }

   interface AdvancementPredicate extends Predicate<AdvancementProgress> {
      Codec<PlayerPredicate.AdvancementPredicate> CODEC = Codec.either(
            PlayerPredicate.AdvancementDonePredicate.CODEC, PlayerPredicate.AdvancementCriterionsPredicate.CODEC
         )
         .xmap(Either::unwrap, p_469076_ -> {
            if (p_469076_ instanceof PlayerPredicate.AdvancementDonePredicate playerpredicate$advancementdonepredicate) {
               return Either.left(playerpredicate$advancementdonepredicate);
            } else if (p_469076_ instanceof PlayerPredicate.AdvancementCriterionsPredicate playerpredicate$advancementcriterionspredicate) {
               return Either.right(playerpredicate$advancementcriterionspredicate);
            } else {
               throw new UnsupportedOperationException();
            }
         });
   }

   public static class Builder {
      private MinMaxBounds.Ints level = MinMaxBounds.Ints.ANY;
      private GameTypePredicate gameType = GameTypePredicate.ANY;
      private final com.google.common.collect.ImmutableList.Builder<PlayerPredicate.StatMatcher<?>> stats = ImmutableList.builder();
      private final Object2BooleanMap<ResourceKey<Recipe<?>>> recipes = new Object2BooleanOpenHashMap();
      private final Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements = Maps.newHashMap();
      private Optional<EntityPredicate> lookingAt = Optional.empty();
      private Optional<InputPredicate> input = Optional.empty();

      public static PlayerPredicate.Builder player() {
         return new PlayerPredicate.Builder();
      }

      public PlayerPredicate.Builder setLevel(MinMaxBounds.Ints p_468389_) {
         this.level = p_468389_;
         return this;
      }

      public <T> PlayerPredicate.Builder addStat(StatType<T> p_469069_, Holder.Reference<T> p_468612_, MinMaxBounds.Ints p_467506_) {
         this.stats.add(new PlayerPredicate.StatMatcher<>(p_469069_, p_468612_, p_467506_));
         return this;
      }

      public PlayerPredicate.Builder addRecipe(ResourceKey<Recipe<?>> p_468992_, boolean p_468256_) {
         this.recipes.put(p_468992_, p_468256_);
         return this;
      }

      public PlayerPredicate.Builder setGameType(GameTypePredicate p_467149_) {
         this.gameType = p_467149_;
         return this;
      }

      public PlayerPredicate.Builder setLookingAt(EntityPredicate.Builder p_469111_) {
         this.lookingAt = Optional.of(p_469111_.build());
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementDone(Identifier p_467459_, boolean p_469573_) {
         this.advancements.put(p_467459_, new PlayerPredicate.AdvancementDonePredicate(p_469573_));
         return this;
      }

      public PlayerPredicate.Builder checkAdvancementCriterions(Identifier p_467507_, Map<String, Boolean> p_468198_) {
         this.advancements.put(p_467507_, new PlayerPredicate.AdvancementCriterionsPredicate(new Object2BooleanOpenHashMap(p_468198_)));
         return this;
      }

      public PlayerPredicate.Builder hasInput(InputPredicate p_467455_) {
         this.input = Optional.of(p_467455_);
         return this;
      }

      public PlayerPredicate build() {
         return new PlayerPredicate(this.level, this.gameType, this.stats.build(), this.recipes, this.advancements, this.lookingAt, this.input);
      }
   }

   record StatMatcher<T>(StatType<T> type, Holder<T> value, MinMaxBounds.Ints range, Supplier<Stat<T>> stat) {
      public static final Codec<PlayerPredicate.StatMatcher<?>> CODEC = BuiltInRegistries.STAT_TYPE
         .byNameCodec()
         .dispatch(PlayerPredicate.StatMatcher::type, PlayerPredicate.StatMatcher::createTypedCodec);

      public StatMatcher(StatType<T> p_467491_, Holder<T> p_468463_, MinMaxBounds.Ints p_469772_) {
         this(p_467491_, p_468463_, p_469772_, Suppliers.memoize(() -> p_467491_.get(p_468463_.value())));
      }

      private static <T> MapCodec<PlayerPredicate.StatMatcher<T>> createTypedCodec(StatType<T> p_467814_) {
         return RecordCodecBuilder.mapCodec(
            p_469532_ -> p_469532_.group(
                  p_467814_.getRegistry().holderByNameCodec().fieldOf("stat").forGetter(PlayerPredicate.StatMatcher::value),
                  MinMaxBounds.Ints.CODEC.optionalFieldOf("value", MinMaxBounds.Ints.ANY).forGetter(PlayerPredicate.StatMatcher::range)
               )
               .apply(p_469532_, (p_468267_, p_468009_) -> new PlayerPredicate.StatMatcher<>(p_467814_, p_468267_, p_468009_))
         );
      }

      public boolean matches(StatsCounter p_467586_) {
         return this.range.matches(p_467586_.getValue(this.stat.get()));
      }
   }
}

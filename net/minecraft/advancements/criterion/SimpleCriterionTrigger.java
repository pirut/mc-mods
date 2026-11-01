package net.minecraft.advancements.criterion;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.LootContext;

public abstract class SimpleCriterionTrigger<T extends SimpleCriterionTrigger.SimpleInstance> implements CriterionTrigger<T> {
   private final Map<PlayerAdvancements, Set<CriterionTrigger.Listener<T>>> players = Maps.newIdentityHashMap();

   @Override
   public final void addPlayerListener(PlayerAdvancements p_468962_, CriterionTrigger.Listener<T> p_468365_) {
      this.players.computeIfAbsent(p_468962_, p_466907_ -> Sets.newHashSet()).add(p_468365_);
   }

   @Override
   public final void removePlayerListener(PlayerAdvancements p_467024_, CriterionTrigger.Listener<T> p_467686_) {
      Set<CriterionTrigger.Listener<T>> set = this.players.get(p_467024_);
      if (set != null) {
         set.remove(p_467686_);
         if (set.isEmpty()) {
            this.players.remove(p_467024_);
         }
      }
   }

   @Override
   public final void removePlayerListeners(PlayerAdvancements p_467936_) {
      this.players.remove(p_467936_);
   }

   protected void trigger(ServerPlayer p_469999_, Predicate<T> p_467301_) {
      PlayerAdvancements playeradvancements = p_469999_.getAdvancements();
      Set<CriterionTrigger.Listener<T>> set = this.players.get(playeradvancements);
      if (set != null && !set.isEmpty()) {
         LootContext lootcontext = EntityPredicate.createContext(p_469999_, p_469999_);
         List<CriterionTrigger.Listener<T>> list = null;

         for (CriterionTrigger.Listener<T> listener : set) {
            T t = listener.trigger();
            if (p_467301_.test(t)) {
               Optional<ContextAwarePredicate> optional = t.player();
               if (optional.isEmpty() || optional.get().matches(lootcontext)) {
                  if (list == null) {
                     list = Lists.newArrayList();
                  }

                  list.add(listener);
               }
            }
         }

         if (list != null) {
            for (CriterionTrigger.Listener<T> listener1 : list) {
               listener1.run(playeradvancements);
            }
         }
      }
   }

   public interface SimpleInstance extends CriterionTriggerInstance {
      @Override
      default void validate(CriterionValidator p_469858_) {
         p_469858_.validateEntity(this.player(), "player");
      }

      Optional<ContextAwarePredicate> player();
   }
}

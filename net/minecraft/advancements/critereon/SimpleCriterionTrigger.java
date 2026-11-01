package net.minecraft.advancements.critereon;

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
   public final void addPlayerListener(PlayerAdvancements p_66243_, CriterionTrigger.Listener<T> p_66244_) {
      this.players.computeIfAbsent(p_66243_, p_66252_ -> Sets.newHashSet()).add(p_66244_);
   }

   @Override
   public final void removePlayerListener(PlayerAdvancements p_66254_, CriterionTrigger.Listener<T> p_66255_) {
      Set<CriterionTrigger.Listener<T>> $$2 = this.players.get(p_66254_);
      if ($$2 != null) {
         $$2.remove(p_66255_);
         if ($$2.isEmpty()) {
            this.players.remove(p_66254_);
         }
      }
   }

   @Override
   public final void removePlayerListeners(PlayerAdvancements p_66241_) {
      this.players.remove(p_66241_);
   }

   protected void trigger(ServerPlayer p_66235_, Predicate<T> p_66236_) {
      PlayerAdvancements $$2 = p_66235_.getAdvancements();
      Set<CriterionTrigger.Listener<T>> $$3 = this.players.get($$2);
      if ($$3 != null && !$$3.isEmpty()) {
         LootContext $$4 = EntityPredicate.createContext(p_66235_, p_66235_);
         List<CriterionTrigger.Listener<T>> $$5 = null;

         for (CriterionTrigger.Listener<T> $$6 : $$3) {
            T $$7 = $$6.trigger();
            if (p_66236_.test($$7)) {
               Optional<ContextAwarePredicate> $$8 = $$7.player();
               if ($$8.isEmpty() || $$8.get().matches($$4)) {
                  if ($$5 == null) {
                     $$5 = Lists.newArrayList();
                  }

                  $$5.add($$6);
               }
            }
         }

         if ($$5 != null) {
            for (CriterionTrigger.Listener<T> $$9 : $$5) {
               $$9.run($$2);
            }
         }
      }
   }

   public interface SimpleInstance extends CriterionTriggerInstance {
      @Override
      default void validate(CriterionValidator p_312329_) {
         p_312329_.validateEntity(this.player(), ".player");
      }

      Optional<ContextAwarePredicate> player();
   }
}

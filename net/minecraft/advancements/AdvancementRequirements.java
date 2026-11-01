package net.minecraft.advancements;

import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.network.FriendlyByteBuf;

public record AdvancementRequirements(List<List<String>> requirements) {
   public static final Codec<AdvancementRequirements> CODEC = Codec.STRING
      .listOf()
      .listOf()
      .xmap(AdvancementRequirements::new, AdvancementRequirements::requirements);
   public static final AdvancementRequirements EMPTY = new AdvancementRequirements(List.of());

   public AdvancementRequirements(FriendlyByteBuf p_301089_) {
      this(p_301089_.readList(p_319371_ -> p_319371_.readList(FriendlyByteBuf::readUtf)));
   }

   public void write(FriendlyByteBuf p_301190_) {
      p_301190_.writeCollection(this.requirements, (p_319372_, p_319373_) -> p_319372_.writeCollection(p_319373_, FriendlyByteBuf::writeUtf));
   }

   public static AdvancementRequirements allOf(Collection<String> p_301049_) {
      return new AdvancementRequirements(p_301049_.stream().map(List::of).toList());
   }

   public static AdvancementRequirements anyOf(Collection<String> p_301268_) {
      return new AdvancementRequirements(List.of(List.copyOf(p_301268_)));
   }

   public int size() {
      return this.requirements.size();
   }

   public boolean test(Predicate<String> p_301112_) {
      if (this.requirements.isEmpty()) {
         return false;
      } else {
         for (List<String> $$1 : this.requirements) {
            if (!anyMatch($$1, p_301112_)) {
               return false;
            }
         }

         return true;
      }
   }

   public int count(Predicate<String> p_301229_) {
      int $$1 = 0;

      for (List<String> $$2 : this.requirements) {
         if (anyMatch($$2, p_301229_)) {
            $$1++;
         }
      }

      return $$1;
   }

   private static boolean anyMatch(List<String> p_312163_, Predicate<String> p_301321_) {
      for (String $$2 : p_312163_) {
         if (p_301321_.test($$2)) {
            return true;
         }
      }

      return false;
   }

   public DataResult<AdvancementRequirements> validate(Set<String> p_312891_) {
      Set<String> $$1 = new ObjectOpenHashSet();

      for (List<String> $$2 : this.requirements) {
         if ($$2.isEmpty() && p_312891_.isEmpty()) {
            return DataResult.error(() -> "Requirement entry cannot be empty");
         }

         $$1.addAll($$2);
      }

      if (!p_312891_.equals($$1)) {
         Set<String> $$3 = Sets.difference(p_312891_, $$1);
         Set<String> $$4 = Sets.difference($$1, p_312891_);
         return DataResult.error(() -> "Advancement completion requirements did not exactly match specified criteria. Missing: " + $$3 + ". Unknown: " + $$4);
      } else {
         return DataResult.success(this);
      }
   }

   public boolean isEmpty() {
      return this.requirements.isEmpty();
   }

   @Override
   public String toString() {
      return this.requirements.toString();
   }

   public Set<String> names() {
      Set<String> $$0 = new ObjectOpenHashSet();

      for (List<String> $$1 : this.requirements) {
         $$0.addAll($$1);
      }

      return $$0;
   }

   public interface Strategy {
      AdvancementRequirements.Strategy AND = AdvancementRequirements::allOf;
      AdvancementRequirements.Strategy OR = AdvancementRequirements::anyOf;

      AdvancementRequirements create(Collection<String> var1);
   }
}

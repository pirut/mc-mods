package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record KeyValueCondition(Map<String, KeyValueCondition.Terms> tests) implements Condition {
   static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<KeyValueCondition> CODEC = ExtraCodecs.nonEmptyMap(Codec.unboundedMap(Codec.STRING, KeyValueCondition.Terms.CODEC))
      .xmap(KeyValueCondition::new, KeyValueCondition::tests);

   @Override
   public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> p_405000_) {
      List<Predicate<S>> list = new ArrayList<>(this.tests.size());
      this.tests.forEach((p_404083_, p_404084_) -> list.add(instantiate(p_405000_, p_404083_, p_404084_)));
      return Util.allOf(list);
   }

   private static <O, S extends StateHolder<O, S>> Predicate<S> instantiate(
      StateDefinition<O, S> p_405206_, String p_405083_, KeyValueCondition.Terms p_405754_
   ) {
      Property<?> property = p_405206_.getProperty(p_405083_);
      if (property == null) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "Unknown property '%s' on '%s'", p_405083_, p_405206_.getOwner()));
      } else {
         return p_405754_.instantiate(p_405206_.getOwner(), property);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Term(String value, boolean negated) {
      private static final String NEGATE = "!";

      public Term(String value, boolean negated) {
         if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty term");
         } else {
            this.value = value;
            this.negated = negated;
         }
      }

      public static KeyValueCondition.Term parse(String p_405246_) {
         return p_405246_.startsWith("!") ? new KeyValueCondition.Term(p_405246_.substring(1), true) : new KeyValueCondition.Term(p_405246_, false);
      }

      @Override
      public String toString() {
         return this.negated ? "!" + this.value : this.value;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record Terms(List<KeyValueCondition.Term> entries) {
      private static final char SEPARATOR = '|';
      private static final Joiner JOINER = Joiner.on('|');
      private static final Splitter SPLITTER = Splitter.on('|');
      private static final Codec<String> LEGACY_REPRESENTATION_CODEC = Codec.either(Codec.INT, Codec.BOOL)
         .flatComapMap(
            p_405478_ -> (String)p_405478_.map(String::valueOf, String::valueOf), p_405596_ -> DataResult.error(() -> "This codec can't be used for encoding")
         );
      public static final Codec<KeyValueCondition.Terms> CODEC = Codec.withAlternative(Codec.STRING, LEGACY_REPRESENTATION_CODEC)
         .comapFlatMap(KeyValueCondition.Terms::parse, KeyValueCondition.Terms::toString);

      public Terms(List<KeyValueCondition.Term> entries) {
         if (entries.isEmpty()) {
            throw new IllegalArgumentException("Empty value for property");
         } else {
            this.entries = entries;
         }
      }

      public static DataResult<KeyValueCondition.Terms> parse(String p_405101_) {
         List<KeyValueCondition.Term> list = SPLITTER.splitToStream(p_405101_).map(KeyValueCondition.Term::parse).toList();
         if (list.isEmpty()) {
            return DataResult.error(() -> "Empty value for property");
         } else {
            for (KeyValueCondition.Term keyvaluecondition$term : list) {
               if (keyvaluecondition$term.value.isEmpty()) {
                  return DataResult.error(() -> "Empty term in value '" + p_405101_ + "'");
               }
            }

            return DataResult.success(new KeyValueCondition.Terms(list));
         }
      }

      @Override
      public String toString() {
         return JOINER.join(this.entries);
      }

      public <O, S extends StateHolder<O, S>, T extends Comparable<T>> Predicate<S> instantiate(O p_404984_, Property<T> p_405617_) {
         Predicate<T> predicate = Util.anyOf(Lists.transform(this.entries, p_405722_ -> this.instantiate(p_404984_, p_405617_, p_405722_)));
         List<T> list = new ArrayList<>(p_405617_.getPossibleValues());
         int i = list.size();
         list.removeIf(predicate.negate());
         int j = list.size();
         if (j == 0) {
            KeyValueCondition.LOGGER.warn("Condition {} for property {} on {} is always false", new Object[]{this, p_405617_.getName(), p_404984_});
            return p_405630_ -> false;
         } else {
            int k = i - j;
            if (k == 0) {
               KeyValueCondition.LOGGER.warn("Condition {} for property {} on {} is always true", new Object[]{this, p_405617_.getName(), p_404984_});
               return p_404757_ -> true;
            } else {
               boolean flag;
               List<T> list1;
               if (j <= k) {
                  flag = false;
                  list1 = list;
               } else {
                  flag = true;
                  List<T> list2 = new ArrayList<>(p_405617_.getPossibleValues());
                  list2.removeIf(predicate);
                  list1 = list2;
               }

               if (list1.size() == 1) {
                  T t = (T)list1.getFirst();
                  return p_404713_ -> {
                     T t1 = p_404713_.getValue(p_405617_);
                     return t.equals(t1) ^ flag;
                  };
               } else {
                  return p_405532_ -> {
                     T t1 = p_405532_.getValue(p_405617_);
                     return list1.contains(t1) ^ flag;
                  };
               }
            }
         }
      }

      private <T extends Comparable<T>> T getValueOrThrow(Object p_405650_, Property<T> p_405129_, String p_405218_) {
         Optional<T> optional = p_405129_.getValue(p_405218_);
         if (optional.isEmpty()) {
            throw new RuntimeException(
               String.format(Locale.ROOT, "Unknown value '%s' for property '%s' on '%s' in '%s'", p_405218_, p_405129_, p_405650_, this)
            );
         } else {
            return optional.get();
         }
      }

      private <T extends Comparable<T>> Predicate<T> instantiate(Object p_405656_, Property<T> p_405057_, KeyValueCondition.Term p_405563_) {
         T t = this.getValueOrThrow(p_405656_, p_405057_, p_405563_.value);
         return p_405563_.negated ? p_405370_ -> !p_405370_.equals(t) : p_404808_ -> p_404808_.equals(t);
      }
   }
}

package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.block.model.multipart.KeyValueCondition;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConditionBuilder {
   private final Builder<String, KeyValueCondition.Terms> terms = ImmutableMap.builder();

   private <T extends Comparable<T>> void putValue(Property<T> p_405712_, KeyValueCondition.Terms p_405152_) {
      this.terms.put(p_405712_.getName(), p_405152_);
   }

   public final <T extends Comparable<T>> ConditionBuilder term(Property<T> p_405307_, T p_405097_) {
      this.putValue(p_405307_, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(p_405307_.getName(p_405097_), false))));
      return this;
   }

   @SafeVarargs
   public final <T extends Comparable<T>> ConditionBuilder term(Property<T> p_405640_, T p_405822_, T... p_405488_) {
      List<KeyValueCondition.Term> list = Stream.concat(Stream.of(p_405822_), Stream.of(p_405488_))
         .map(p_405640_::getName)
         .sorted()
         .distinct()
         .map(p_405408_ -> new KeyValueCondition.Term(p_405408_, false))
         .toList();
      this.putValue(p_405640_, new KeyValueCondition.Terms(list));
      return this;
   }

   public final <T extends Comparable<T>> ConditionBuilder negatedTerm(Property<T> p_404697_, T p_405141_) {
      this.putValue(p_404697_, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(p_404697_.getName(p_405141_), true))));
      return this;
   }

   public Condition build() {
      return new KeyValueCondition(this.terms.buildOrThrow());
   }
}

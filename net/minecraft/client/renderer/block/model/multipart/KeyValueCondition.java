package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class KeyValueCondition implements Condition {
   private static final Splitter PIPE_SPLITTER = Splitter.on('|').omitEmptyStrings();
   private final String key;
   private final String value;

   public KeyValueCondition(String p_111939_, String p_111940_) {
      this.key = p_111939_;
      this.value = p_111940_;
   }

   @Override
   public Predicate<BlockState> getPredicate(StateDefinition<Block, BlockState> p_111960_) {
      Property<?> $$1 = p_111960_.getProperty(this.key);
      if ($$1 == null) {
         throw new RuntimeException(String.format(Locale.ROOT, "Unknown property '%s' on '%s'", this.key, p_111960_.getOwner()));
      } else {
         String $$2 = this.value;
         boolean $$3 = !$$2.isEmpty() && $$2.charAt(0) == '!';
         if ($$3) {
            $$2 = $$2.substring(1);
         }

         List<String> $$4 = PIPE_SPLITTER.splitToList($$2);
         if ($$4.isEmpty()) {
            throw new RuntimeException(String.format(Locale.ROOT, "Empty value '%s' for property '%s' on '%s'", this.value, this.key, p_111960_.getOwner()));
         } else {
            Predicate<BlockState> $$5;
            if ($$4.size() == 1) {
               $$5 = this.getBlockStatePredicate(p_111960_, $$1, $$2);
            } else {
               List<Predicate<BlockState>> $$6 = $$4.stream()
                  .map(p_111958_ -> this.getBlockStatePredicate(p_111960_, $$1, p_111958_))
                  .collect(Collectors.toList());
               $$5 = p_111954_ -> $$6.stream().anyMatch(p_173509_ -> p_173509_.test(p_111954_));
            }

            return $$3 ? $$5.negate() : $$5;
         }
      }
   }

   private Predicate<BlockState> getBlockStatePredicate(StateDefinition<Block, BlockState> p_111945_, Property<?> p_111946_, String p_111947_) {
      Optional<?> $$3 = p_111946_.getValue(p_111947_);
      if ($$3.isEmpty()) {
         throw new RuntimeException(
            String.format(Locale.ROOT, "Unknown value '%s' for property '%s' on '%s' in '%s'", p_111947_, this.key, p_111945_.getOwner(), this.value)
         );
      } else {
         return p_339295_ -> p_339295_.getValue(p_111946_).equals($$3.get());
      }
   }

   @Override
   public String toString() {
      return MoreObjects.toStringHelper(this).add("key", this.key).add("value", this.value).toString();
   }
}

package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.world.entity.player.Input;

public record InputPredicate(
   Optional<Boolean> forward,
   Optional<Boolean> backward,
   Optional<Boolean> left,
   Optional<Boolean> right,
   Optional<Boolean> jump,
   Optional<Boolean> sneak,
   Optional<Boolean> sprint
) {
   public static final Codec<InputPredicate> CODEC = RecordCodecBuilder.create(
      p_469590_ -> p_469590_.group(
            Codec.BOOL.optionalFieldOf("forward").forGetter(InputPredicate::forward),
            Codec.BOOL.optionalFieldOf("backward").forGetter(InputPredicate::backward),
            Codec.BOOL.optionalFieldOf("left").forGetter(InputPredicate::left),
            Codec.BOOL.optionalFieldOf("right").forGetter(InputPredicate::right),
            Codec.BOOL.optionalFieldOf("jump").forGetter(InputPredicate::jump),
            Codec.BOOL.optionalFieldOf("sneak").forGetter(InputPredicate::sneak),
            Codec.BOOL.optionalFieldOf("sprint").forGetter(InputPredicate::sprint)
         )
         .apply(p_469590_, InputPredicate::new)
   );

   public boolean matches(Input p_467089_) {
      return this.matches(this.forward, p_467089_.forward())
         && this.matches(this.backward, p_467089_.backward())
         && this.matches(this.left, p_467089_.left())
         && this.matches(this.right, p_467089_.right())
         && this.matches(this.jump, p_467089_.jump())
         && this.matches(this.sneak, p_467089_.shift())
         && this.matches(this.sprint, p_467089_.sprint());
   }

   private boolean matches(Optional<Boolean> p_469518_, boolean p_467195_) {
      return p_469518_.<Boolean>map(p_469899_ -> p_469899_ == p_467195_).orElse(true);
   }
}

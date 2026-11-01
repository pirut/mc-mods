package net.minecraft.world.scores;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import org.jspecify.annotations.Nullable;

public class Score implements ReadOnlyScoreInfo {
   private int value;
   private boolean locked = true;
   @Nullable
   private Component display;
   @Nullable
   private NumberFormat numberFormat;

   public Score() {
   }

   public Score(Score.Packed p_457945_) {
      this.value = p_457945_.value;
      this.locked = p_457945_.locked;
      this.display = p_457945_.display.orElse(null);
      this.numberFormat = p_457945_.numberFormat.orElse(null);
   }

   public Score.Packed pack() {
      return new Score.Packed(this.value, this.locked, Optional.ofNullable(this.display), Optional.ofNullable(this.numberFormat));
   }

   @Override
   public int value() {
      return this.value;
   }

   public void value(int p_313791_) {
      this.value = p_313791_;
   }

   @Override
   public boolean isLocked() {
      return this.locked;
   }

   public void setLocked(boolean p_83399_) {
      this.locked = p_83399_;
   }

   @Nullable
   public Component display() {
      return this.display;
   }

   public void display(@Nullable Component p_313838_) {
      this.display = p_313838_;
   }

   @Nullable
   @Override
   public NumberFormat numberFormat() {
      return this.numberFormat;
   }

   public void numberFormat(@Nullable NumberFormat p_313931_) {
      this.numberFormat = p_313931_;
   }

   public record Packed(int value, boolean locked, Optional<Component> display, Optional<NumberFormat> numberFormat) {
      public static final MapCodec<Score.Packed> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_458049_ -> p_458049_.group(
               Codec.INT.optionalFieldOf("Score", 0).forGetter(Score.Packed::value),
               Codec.BOOL.optionalFieldOf("Locked", false).forGetter(Score.Packed::locked),
               ComponentSerialization.CODEC.optionalFieldOf("display").forGetter(Score.Packed::display),
               NumberFormatTypes.CODEC.optionalFieldOf("format").forGetter(Score.Packed::numberFormat)
            )
            .apply(p_458049_, Score.Packed::new)
      );
   }
}

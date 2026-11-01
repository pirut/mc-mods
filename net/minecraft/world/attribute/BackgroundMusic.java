package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvent;

public record BackgroundMusic(Optional<Music> defaultMusic, Optional<Music> creativeMusic, Optional<Music> underwaterMusic) {
   public static final BackgroundMusic EMPTY = new BackgroundMusic(Optional.empty(), Optional.empty(), Optional.empty());
   public static final BackgroundMusic OVERWORLD = new BackgroundMusic(Optional.of(Musics.GAME), Optional.of(Musics.CREATIVE), Optional.empty());
   public static final Codec<BackgroundMusic> CODEC = RecordCodecBuilder.create(
      p_458308_ -> p_458308_.group(
            Music.CODEC.optionalFieldOf("default").forGetter(BackgroundMusic::defaultMusic),
            Music.CODEC.optionalFieldOf("creative").forGetter(BackgroundMusic::creativeMusic),
            Music.CODEC.optionalFieldOf("underwater").forGetter(BackgroundMusic::underwaterMusic)
         )
         .apply(p_458308_, BackgroundMusic::new)
   );

   public BackgroundMusic(Music p_458216_) {
      this(Optional.of(p_458216_), Optional.empty(), Optional.empty());
   }

   public BackgroundMusic(Holder<SoundEvent> p_457727_) {
      this(Musics.createGameMusic(p_457727_));
   }

   public BackgroundMusic withUnderwater(Music p_457948_) {
      return new BackgroundMusic(this.defaultMusic, this.creativeMusic, Optional.of(p_457948_));
   }

   public Optional<Music> select(boolean p_457651_, boolean p_458221_) {
      if (p_458221_ && this.underwaterMusic.isPresent()) {
         return this.underwaterMusic;
      } else {
         return p_457651_ && this.creativeMusic.isPresent() ? this.creativeMusic : this.defaultMusic;
      }
   }
}

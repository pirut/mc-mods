package net.neoforged.neoforge.client.event.sound;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class PlaySoundEvent extends SoundEvent {
   private final String name;
   private final SoundInstance originalSound;
   @Nullable
   private SoundInstance sound;

   @Internal
   public PlaySoundEvent(SoundEngine manager, SoundInstance sound) {
      super(manager);
      this.originalSound = sound;
      this.name = sound.getIdentifier().getPath();
      this.setSound(sound);
   }

   public String getName() {
      return this.name;
   }

   public SoundInstance getOriginalSound() {
      return this.originalSound;
   }

   @Nullable
   public SoundInstance getSound() {
      return this.sound;
   }

   public void setSound(@Nullable SoundInstance newSound) {
      this.sound = newSound;
   }
}

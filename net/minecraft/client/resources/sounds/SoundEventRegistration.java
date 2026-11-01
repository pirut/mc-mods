package net.minecraft.client.resources.sounds;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class SoundEventRegistration {
   private final List<Sound> sounds;
   private final boolean replace;
   @Nullable
   private final String subtitle;

   public SoundEventRegistration(List<Sound> sounds, boolean replace, @Nullable String subtitle) {
      this.sounds = sounds;
      this.replace = replace;
      this.subtitle = subtitle;
   }

   public List<Sound> getSounds() {
      return this.sounds;
   }

   public boolean isReplace() {
      return this.replace;
   }

   @Nullable
   public String getSubtitle() {
      return this.subtitle;
   }
}

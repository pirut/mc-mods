package net.minecraft.client.sounds;

import com.google.common.collect.Lists;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class WeighedSoundEvents implements Weighted<Sound> {
   private final List<Weighted<Sound>> list = Lists.newArrayList();
   @Nullable
   private final Component subtitle;

   public WeighedSoundEvents(Identifier location, @Nullable String subtitleKey) {
      if (SharedConstants.DEBUG_SUBTITLES) {
         MutableComponent mutableComponent = Component.literal(location.getPath());
         if ("FOR THE DEBUG!".equals(subtitleKey)) {
            mutableComponent = mutableComponent.append(Component.literal(" missing").withStyle(ChatFormatting.RED));
         }

         this.subtitle = mutableComponent;
      } else {
         this.subtitle = subtitleKey == null ? null : Component.translatable(subtitleKey);
      }
   }

   @Override
   public int getWeight() {
      int i = 0;

      for (Weighted<Sound> weighted : this.list) {
         i += weighted.getWeight();
      }

      return i;
   }

   public Sound getSound(RandomSource randomSource) {
      int i = this.getWeight();
      if (!this.list.isEmpty() && i != 0) {
         int j = randomSource.nextInt(i);

         for (Weighted<Sound> weighted : this.list) {
            j -= weighted.getWeight();
            if (j < 0) {
               return weighted.getSound(randomSource);
            }
         }

         return SoundManager.EMPTY_SOUND;
      } else {
         return SoundManager.EMPTY_SOUND;
      }
   }

   public void addSound(Weighted<Sound> accessor) {
      this.list.add(accessor);
   }

   @Nullable
   public Component getSubtitle() {
      return this.subtitle;
   }

   @Override
   public void preloadIfRequired(SoundEngine engine) {
      for (Weighted<Sound> weighted : this.list) {
         weighted.preloadIfRequired(engine);
      }
   }
}

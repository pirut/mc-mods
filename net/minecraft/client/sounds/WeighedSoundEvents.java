package net.minecraft.client.sounds;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeighedSoundEvents implements Weighted<Sound> {
   private final List<Weighted<Sound>> list = Lists.newArrayList();
   @Nullable
   private final Component subtitle;

   public WeighedSoundEvents(ResourceLocation p_120446_, @Nullable String p_120447_) {
      this.subtitle = p_120447_ == null ? null : Component.translatable(p_120447_);
   }

   @Override
   public int getWeight() {
      int $$0 = 0;

      for (Weighted<Sound> $$1 : this.list) {
         $$0 += $$1.getWeight();
      }

      return $$0;
   }

   public Sound getSound(RandomSource p_235265_) {
      int $$1 = this.getWeight();
      if (!this.list.isEmpty() && $$1 != 0) {
         int $$2 = p_235265_.nextInt($$1);

         for (Weighted<Sound> $$3 : this.list) {
            $$2 -= $$3.getWeight();
            if ($$2 < 0) {
               return $$3.getSound(p_235265_);
            }
         }

         return SoundManager.EMPTY_SOUND;
      } else {
         return SoundManager.EMPTY_SOUND;
      }
   }

   public void addSound(Weighted<Sound> p_120452_) {
      this.list.add(p_120452_);
   }

   @Nullable
   public Component getSubtitle() {
      return this.subtitle;
   }

   @Override
   public void preloadIfRequired(SoundEngine p_120450_) {
      for (Weighted<Sound> $$1 : this.list) {
         $$1.preloadIfRequired(p_120450_);
      }
   }
}

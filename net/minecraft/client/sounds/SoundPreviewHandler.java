package net.minecraft.client.sounds;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public final class SoundPreviewHandler {
   @Nullable
   private static SoundInstance activePreview;
   @Nullable
   private static SoundSource previousCategory;

   public static void preview(SoundManager p_449449_, SoundSource p_449232_, float p_449515_) {
      stopOtherCategoryPreview(p_449449_, p_449232_);
      if (canPlaySound(p_449449_)) {
         SoundEvent soundevent = switch (p_449232_) {
            case RECORDS -> (SoundEvent)SoundEvents.NOTE_BLOCK_GUITAR.value();
            case WEATHER -> SoundEvents.LIGHTNING_BOLT_THUNDER;
            case BLOCKS -> SoundEvents.GRASS_PLACE;
            case HOSTILE -> SoundEvents.ZOMBIE_AMBIENT;
            case NEUTRAL -> SoundEvents.COW_AMBIENT;
            case PLAYERS -> (SoundEvent)SoundEvents.GENERIC_EAT.value();
            case AMBIENT -> (SoundEvent)SoundEvents.AMBIENT_CAVE.value();
            case UI -> (SoundEvent)SoundEvents.UI_BUTTON_CLICK.value();
            default -> SoundEvents.EMPTY;
         };
         if (soundevent != SoundEvents.EMPTY) {
            activePreview = SimpleSoundInstance.forUI(soundevent, 1.0F, p_449515_);
            p_449449_.play(activePreview);
         }
      }
   }

   private static void stopOtherCategoryPreview(SoundManager p_449443_, SoundSource p_449332_) {
      if (previousCategory != p_449332_) {
         previousCategory = p_449332_;
         if (activePreview != null) {
            p_449443_.stop(activePreview);
         }
      }
   }

   private static boolean canPlaySound(SoundManager p_449893_) {
      return activePreview == null || !p_449893_.isActive(activePreview);
   }
}

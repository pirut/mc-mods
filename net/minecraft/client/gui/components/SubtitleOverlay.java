package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.audio.ListenerTransform;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEventListener;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SubtitleOverlay implements SoundEventListener {
   private static final long DISPLAY_TIME = 3000L;
   private final Minecraft minecraft;
   private final List<SubtitleOverlay.Subtitle> subtitles = Lists.newArrayList();
   private boolean isListening;
   private final List<SubtitleOverlay.Subtitle> audibleSubtitles = new ArrayList<>();

   public SubtitleOverlay(Minecraft p_94641_) {
      this.minecraft = p_94641_;
   }

   public void render(GuiGraphics p_282562_) {
      SoundManager $$1 = this.minecraft.getSoundManager();
      if (!this.isListening && this.minecraft.options.showSubtitles().get()) {
         $$1.addListener(this);
         this.isListening = true;
      } else if (this.isListening && !this.minecraft.options.showSubtitles().get()) {
         $$1.removeListener(this);
         this.isListening = false;
      }

      if (this.isListening) {
         ListenerTransform $$2 = $$1.getListenerTransform();
         Vec3 $$3 = $$2.position();
         Vec3 $$4 = $$2.forward();
         Vec3 $$5 = $$2.right();
         this.audibleSubtitles.clear();

         for (SubtitleOverlay.Subtitle $$6 : this.subtitles) {
            if ($$6.isAudibleFrom($$3)) {
               this.audibleSubtitles.add($$6);
            }
         }

         if (!this.audibleSubtitles.isEmpty()) {
            int $$7 = 0;
            int $$8 = 0;
            double $$9 = this.minecraft.options.notificationDisplayTime().get();
            Iterator<SubtitleOverlay.Subtitle> $$10 = this.audibleSubtitles.iterator();

            while ($$10.hasNext()) {
               SubtitleOverlay.Subtitle $$11 = $$10.next();
               $$11.purgeOldInstances(3000.0 * $$9);
               if (!$$11.isStillActive()) {
                  $$10.remove();
               } else {
                  $$8 = Math.max($$8, this.minecraft.font.width($$11.getText()));
               }
            }

            $$8 += this.minecraft.font.width("<") + this.minecraft.font.width(" ") + this.minecraft.font.width(">") + this.minecraft.font.width(" ");

            for (SubtitleOverlay.Subtitle $$12 : this.audibleSubtitles) {
               int $$13 = 255;
               Component $$14 = $$12.getText();
               SubtitleOverlay.SoundPlayedAt $$15 = $$12.getClosest($$3);
               if ($$15 != null) {
                  Vec3 $$16 = $$15.location.subtract($$3).normalize();
                  double $$17 = $$5.dot($$16);
                  double $$18 = $$4.dot($$16);
                  boolean $$19 = $$18 > 0.5;
                  int $$20 = $$8 / 2;
                  int $$21 = 9;
                  int $$22 = $$21 / 2;
                  float $$23 = 1.0F;
                  int $$24 = this.minecraft.font.width($$14);
                  int $$25 = Mth.floor(Mth.clampedLerp(255.0F, 75.0F, (float)(Util.getMillis() - $$15.time) / (float)(3000.0 * $$9)));
                  int $$26 = $$25 << 16 | $$25 << 8 | $$25;
                  p_282562_.pose().pushPose();
                  p_282562_.pose().translate(p_282562_.guiWidth() - $$20 * 1.0F - 2.0F, p_282562_.guiHeight() - 35 - $$7 * ($$21 + 1) * 1.0F, 0.0F);
                  p_282562_.pose().scale(1.0F, 1.0F, 1.0F);
                  p_282562_.fill(-$$20 - 1, -$$22 - 1, $$20 + 1, $$22 + 1, this.minecraft.options.getBackgroundColor(0.8F));
                  int $$27 = $$26 + -16777216;
                  if (!$$19) {
                     if ($$17 > 0.0) {
                        p_282562_.drawString(this.minecraft.font, ">", $$20 - this.minecraft.font.width(">"), -$$22, $$27);
                     } else if ($$17 < 0.0) {
                        p_282562_.drawString(this.minecraft.font, "<", -$$20, -$$22, $$27);
                     }
                  }

                  p_282562_.drawString(this.minecraft.font, $$14, -$$24 / 2, -$$22, $$27);
                  p_282562_.pose().popPose();
                  $$7++;
               }
            }
         }
      }
   }

   @Override
   public void onPlaySound(SoundInstance p_94645_, WeighedSoundEvents p_94646_, float p_314721_) {
      if (p_94646_.getSubtitle() != null) {
         Component $$3 = p_94646_.getSubtitle();
         if (!this.subtitles.isEmpty()) {
            for (SubtitleOverlay.Subtitle $$4 : this.subtitles) {
               if ($$4.getText().equals($$3)) {
                  $$4.refresh(new Vec3(p_94645_.getX(), p_94645_.getY(), p_94645_.getZ()));
                  return;
               }
            }
         }

         this.subtitles.add(new SubtitleOverlay.Subtitle($$3, p_314721_, new Vec3(p_94645_.getX(), p_94645_.getY(), p_94645_.getZ())));
      }
   }

   @OnlyIn(Dist.CLIENT)
   record SoundPlayedAt(Vec3 location, long time) {
   }

   @OnlyIn(Dist.CLIENT)
   static class Subtitle {
      private final Component text;
      private final float range;
      private final List<SubtitleOverlay.SoundPlayedAt> playedAt = new ArrayList<>();

      public Subtitle(Component p_169072_, float p_314705_, Vec3 p_169073_) {
         this.text = p_169072_;
         this.range = p_314705_;
         this.playedAt.add(new SubtitleOverlay.SoundPlayedAt(p_169073_, Util.getMillis()));
      }

      public Component getText() {
         return this.text;
      }

      @Nullable
      public SubtitleOverlay.SoundPlayedAt getClosest(Vec3 p_347452_) {
         if (this.playedAt.isEmpty()) {
            return null;
         } else {
            return this.playedAt.size() == 1
               ? this.playedAt.getFirst()
               : this.playedAt.stream().min(Comparator.comparingDouble(p_347541_ -> p_347541_.location().distanceTo(p_347452_))).orElse(null);
         }
      }

      public void refresh(Vec3 p_94657_) {
         this.playedAt.removeIf(p_347631_ -> p_94657_.equals(p_347631_.location()));
         this.playedAt.add(new SubtitleOverlay.SoundPlayedAt(p_94657_, Util.getMillis()));
      }

      public boolean isAudibleFrom(Vec3 p_314706_) {
         if (Float.isInfinite(this.range)) {
            return true;
         } else if (this.playedAt.isEmpty()) {
            return false;
         } else {
            SubtitleOverlay.SoundPlayedAt $$1 = this.getClosest(p_314706_);
            return $$1 == null ? false : p_314706_.closerThan($$1.location, this.range);
         }
      }

      public void purgeOldInstances(double p_347730_) {
         long $$1 = Util.getMillis();
         this.playedAt.removeIf(p_347590_ -> $$1 - p_347590_.time() > p_347730_);
      }

      public boolean isStillActive() {
         return !this.playedAt.isEmpty();
      }
   }
}

package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.Queues;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MusicToastDisplayState;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ClientHooks;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ToastManager {
   private static final int SLOT_COUNT = 5;
   private static final int ALL_SLOTS_OCCUPIED = -1;
   final Minecraft minecraft;
   private final List<ToastManager.ToastInstance<?>> visibleToasts = new ArrayList<>();
   private final BitSet occupiedSlots = new BitSet(5);
   private final Deque<Toast> queued = Queues.newArrayDeque();
   private final Set<SoundEvent> playedToastSounds = new HashSet<>();
   private ToastManager.ToastInstance<NowPlayingToast> nowPlayingToast;

   public ToastManager(Minecraft p_361363_, Options p_426003_) {
      this.minecraft = p_361363_;
      this.initializeMusicToast(p_426003_.musicToast().get());
   }

   public void update() {
      MutableBoolean mutableboolean = new MutableBoolean(false);
      this.visibleToasts.removeIf(p_392505_ -> {
         Toast.Visibility toast$visibility = p_392505_.visibility;
         p_392505_.update();
         if (p_392505_.visibility != toast$visibility && mutableboolean.isFalse()) {
            mutableboolean.setTrue();
            p_392505_.visibility.playSound(this.minecraft.getSoundManager());
         }

         if (p_392505_.hasFinishedRendering()) {
            this.occupiedSlots.clear(p_392505_.firstSlotIndex, p_392505_.firstSlotIndex + p_392505_.occupiedSlotCount);
            return true;
         } else {
            return false;
         }
      });
      if (!this.queued.isEmpty() && this.freeSlotCount() > 0) {
         this.queued.removeIf(p_392506_ -> {
            int i = p_392506_.occcupiedSlotCount();
            int j = this.findFreeSlotsIndex(i);
            if (j == -1) {
               return false;
            } else {
               this.visibleToasts.add(new ToastManager.ToastInstance<>(p_392506_, j, i));
               this.occupiedSlots.set(j, j + i);
               SoundEvent soundevent = p_392506_.getSoundEvent();
               if (soundevent != null && this.playedToastSounds.add(soundevent)) {
                  this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundevent, 1.0F, 1.0F));
               }

               return true;
            }
         });
      }

      this.playedToastSounds.clear();
      if (this.nowPlayingToast != null) {
         this.nowPlayingToast.update();
      }
   }

   public void render(GuiGraphics p_362571_) {
      if (!this.minecraft.options.hideGui) {
         int i = p_362571_.guiWidth();
         if (!this.visibleToasts.isEmpty()) {
            p_362571_.nextStratum();
         }

         for (ToastManager.ToastInstance<?> toastinstance : this.visibleToasts) {
            toastinstance.render(p_362571_, i);
         }

         if (this.minecraft.options.musicToast().get().renderToast()
            && this.nowPlayingToast != null
            && (this.minecraft.screen == null || !(this.minecraft.screen instanceof PauseScreen))) {
            this.nowPlayingToast.render(p_362571_, i);
         }
      }
   }

   private int findFreeSlotsIndex(int p_362291_) {
      if (this.freeSlotCount() >= p_362291_) {
         int i = 0;

         for (int j = 0; j < 5; j++) {
            if (this.occupiedSlots.get(j)) {
               i = 0;
            } else if (++i == p_362291_) {
               return j + 1 - i;
            }
         }
      }

      return -1;
   }

   private int freeSlotCount() {
      return 5 - this.occupiedSlots.cardinality();
   }

   @Nullable
   public <T extends Toast> T getToast(Class<? extends T> p_361311_, Object p_361989_) {
      for (ToastManager.ToastInstance<?> toastinstance : this.visibleToasts) {
         if (p_361311_.isAssignableFrom(toastinstance.getToast().getClass()) && toastinstance.getToast().getToken().equals(p_361989_)) {
            return (T)toastinstance.getToast();
         }
      }

      for (Toast toast : this.queued) {
         if (p_361311_.isAssignableFrom(toast.getClass()) && toast.getToken().equals(p_361989_)) {
            return (T)toast;
         }
      }

      return null;
   }

   public void clear() {
      this.occupiedSlots.clear();
      this.visibleToasts.clear();
      this.queued.clear();
   }

   public void addToast(Toast p_362712_) {
      if (!ClientHooks.onToastAdd(p_362712_)) {
         this.queued.add(p_362712_);
      }
   }

   public void showNowPlayingToast() {
      if (this.nowPlayingToast != null) {
         this.nowPlayingToast.resetToast();
         this.nowPlayingToast.getToast().showToast(this.minecraft.options);
      }
   }

   public void hideNowPlayingToast() {
      if (this.nowPlayingToast != null) {
         this.nowPlayingToast.getToast().setWantedVisibility(Toast.Visibility.HIDE);
      }
   }

   public Minecraft getMinecraft() {
      return this.minecraft;
   }

   public double getNotificationDisplayTimeMultiplier() {
      return this.minecraft.options.notificationDisplayTime().get();
   }

   private void initializeMusicToast(MusicToastDisplayState p_470562_) {
      switch (p_470562_) {
         case PAUSE:
         case PAUSE_AND_TOAST:
            this.nowPlayingToast = new ToastManager.ToastInstance<>(new NowPlayingToast(), 0, 0);
      }
   }

   public void setMusicToastDisplayState(MusicToastDisplayState p_470568_) {
      switch (p_470568_) {
         case PAUSE:
            this.nowPlayingToast = new ToastManager.ToastInstance<>(new NowPlayingToast(), 0, 0);
            break;
         case PAUSE_AND_TOAST:
            this.nowPlayingToast = new ToastManager.ToastInstance<>(new NowPlayingToast(), 0, 0);
            if (this.minecraft.options.getFinalSoundSourceVolume(SoundSource.MUSIC) > 0.0F) {
               this.nowPlayingToast.getToast().showToast(this.minecraft.options);
            }
            break;
         case NEVER:
            this.nowPlayingToast = null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   class ToastInstance<T extends Toast> {
      private static final long SLIDE_ANIMATION_DURATION_MS = 600L;
      private final T toast;
      final int firstSlotIndex;
      final int occupiedSlotCount;
      private long animationStartTime;
      private long becameFullyVisibleAt;
      Toast.Visibility visibility;
      private long fullyVisibleFor;
      private float visiblePortion;
      protected boolean hasFinishedRendering;

      ToastInstance(T p_365175_, int p_365347_, int p_361399_) {
         this.toast = p_365175_;
         this.firstSlotIndex = p_365347_;
         this.occupiedSlotCount = p_361399_;
         this.resetToast();
      }

      public T getToast() {
         return this.toast;
      }

      public void resetToast() {
         this.animationStartTime = -1L;
         this.becameFullyVisibleAt = -1L;
         this.visibility = Toast.Visibility.HIDE;
         this.fullyVisibleFor = 0L;
         this.visiblePortion = 0.0F;
         this.hasFinishedRendering = false;
      }

      public boolean hasFinishedRendering() {
         return this.hasFinishedRendering;
      }

      private void calculateVisiblePortion(long p_362492_) {
         float f = Mth.clamp((float)(p_362492_ - this.animationStartTime) / 600.0F, 0.0F, 1.0F);
         f *= f;
         if (this.visibility == Toast.Visibility.HIDE) {
            this.visiblePortion = 1.0F - f;
         } else {
            this.visiblePortion = f;
         }
      }

      public void update() {
         long i = Util.getMillis();
         if (this.animationStartTime == -1L) {
            this.animationStartTime = i;
            this.visibility = Toast.Visibility.SHOW;
         }

         if (this.visibility == Toast.Visibility.SHOW && i - this.animationStartTime <= 600L) {
            this.becameFullyVisibleAt = i;
         }

         this.fullyVisibleFor = i - this.becameFullyVisibleAt;
         this.calculateVisiblePortion(i);
         this.toast.update(ToastManager.this, this.fullyVisibleFor);
         Toast.Visibility toast$visibility = this.toast.getWantedVisibility();
         if (toast$visibility != this.visibility) {
            this.animationStartTime = i - (int)((1.0F - this.visiblePortion) * 600.0F);
            this.visibility = toast$visibility;
         }

         boolean flag = this.hasFinishedRendering;
         this.hasFinishedRendering = this.visibility == Toast.Visibility.HIDE && i - this.animationStartTime > 600L;
         if (this.hasFinishedRendering && !flag) {
            this.toast.onFinishedRendering();
         }
      }

      public void render(GuiGraphics p_362825_, int p_361934_) {
         if (!this.hasFinishedRendering) {
            p_362825_.pose().pushMatrix();
            p_362825_.pose().translate(this.toast.xPos(p_361934_, this.visiblePortion), this.toast.yPos(this.firstSlotIndex));
            this.toast.render(p_362825_, ToastManager.this.minecraft.font, this.fullyVisibleFor);
            p_362825_.pose().popMatrix();
         }
      }
   }
}

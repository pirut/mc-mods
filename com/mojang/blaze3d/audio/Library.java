package com.mojang.blaze3d.audio;

import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.openal.ALUtil;
import org.lwjgl.openal.SOFTHRTF;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Library {
   static final Logger LOGGER = LogUtils.getLogger();
   private static final int NO_DEVICE = 0;
   private static final int DEFAULT_CHANNEL_COUNT = 30;
   private long currentDevice;
   private long context;
   private boolean supportsDisconnections;
   @Nullable
   private String defaultDeviceName;
   private static final Library.ChannelPool EMPTY = new Library.ChannelPool() {
      @Nullable
      @Override
      public Channel acquire() {
         return null;
      }

      @Override
      public boolean release(Channel p_83708_) {
         return false;
      }

      @Override
      public void cleanup() {
      }

      @Override
      public int getMaxCount() {
         return 0;
      }

      @Override
      public int getUsedCount() {
         return 0;
      }
   };
   private Library.ChannelPool staticChannels = EMPTY;
   private Library.ChannelPool streamingChannels = EMPTY;
   private final Listener listener = new Listener();

   public Library() {
      this.defaultDeviceName = getDefaultDeviceName();
   }

   public void init(@Nullable String p_231085_, boolean p_231086_) {
      this.currentDevice = openDeviceOrFallback(p_231085_);
      this.supportsDisconnections = false;
      ALCCapabilities $$2 = ALC.createCapabilities(this.currentDevice);
      if (OpenAlUtil.checkALCError(this.currentDevice, "Get capabilities")) {
         throw new IllegalStateException("Failed to get OpenAL capabilities");
      } else if (!$$2.OpenALC11) {
         throw new IllegalStateException("OpenAL 1.1 not supported");
      } else {
         this.setHrtf($$2.ALC_SOFT_HRTF && p_231086_);
         MemoryStack $$3 = MemoryStack.stackPush();

         try {
            IntBuffer $$4 = $$3.callocInt(3).put(6554).put(1).put(0).flip();
            this.context = ALC10.alcCreateContext(this.currentDevice, $$4);
         } catch (Throwable var9) {
            if ($$3 != null) {
               try {
                  $$3.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if ($$3 != null) {
            $$3.close();
         }

         if (OpenAlUtil.checkALCError(this.currentDevice, "Create context")) {
            throw new IllegalStateException("Unable to create OpenAL context");
         } else {
            ALC10.alcMakeContextCurrent(this.context);
            int $$5 = this.getChannelCount();
            int $$6 = Mth.clamp((int)Mth.sqrt($$5), 2, 8);
            int $$7 = Mth.clamp($$5 - $$6, 8, 255);
            this.staticChannels = new Library.CountingChannelPool($$7);
            this.streamingChannels = new Library.CountingChannelPool($$6);
            ALCapabilities $$8 = AL.createCapabilities($$2);
            OpenAlUtil.checkALError("Initialization");
            if (!$$8.AL_EXT_source_distance_model) {
               throw new IllegalStateException("AL_EXT_source_distance_model is not supported");
            } else {
               AL10.alEnable(512);
               if (!$$8.AL_EXT_LINEAR_DISTANCE) {
                  throw new IllegalStateException("AL_EXT_LINEAR_DISTANCE is not supported");
               } else {
                  OpenAlUtil.checkALError("Enable per-source distance models");
                  LOGGER.info("OpenAL initialized on device {}", this.getCurrentDeviceName());
                  this.supportsDisconnections = ALC10.alcIsExtensionPresent(this.currentDevice, "ALC_EXT_disconnect");
               }
            }
         }
      }
   }

   private void setHrtf(boolean p_242278_) {
      int $$1 = ALC10.alcGetInteger(this.currentDevice, 6548);
      if ($$1 > 0) {
         MemoryStack $$2 = MemoryStack.stackPush();

         try {
            IntBuffer $$3 = $$2.callocInt(10).put(6546).put(p_242278_ ? 1 : 0).put(6550).put(0).put(0).flip();
            if (!SOFTHRTF.alcResetDeviceSOFT(this.currentDevice, $$3)) {
               LOGGER.warn("Failed to reset device: {}", ALC10.alcGetString(this.currentDevice, ALC10.alcGetError(this.currentDevice)));
            }
         } catch (Throwable var7) {
            if ($$2 != null) {
               try {
                  $$2.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if ($$2 != null) {
            $$2.close();
         }
      }
   }

   private int getChannelCount() {
      MemoryStack $$0 = MemoryStack.stackPush();

      int var7;
      label58: {
         try {
            int $$1 = ALC10.alcGetInteger(this.currentDevice, 4098);
            if (OpenAlUtil.checkALCError(this.currentDevice, "Get attributes size")) {
               throw new IllegalStateException("Failed to get OpenAL attributes");
            }

            IntBuffer $$2 = $$0.mallocInt($$1);
            ALC10.alcGetIntegerv(this.currentDevice, 4099, $$2);
            if (OpenAlUtil.checkALCError(this.currentDevice, "Get attributes")) {
               throw new IllegalStateException("Failed to get OpenAL attributes");
            }

            int $$3 = 0;

            while ($$3 < $$1) {
               int $$4 = $$2.get($$3++);
               if ($$4 == 0) {
                  break;
               }

               int $$5 = $$2.get($$3++);
               if ($$4 == 4112) {
                  var7 = $$5;
                  break label58;
               }
            }
         } catch (Throwable var9) {
            if ($$0 != null) {
               try {
                  $$0.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if ($$0 != null) {
            $$0.close();
         }

         return 30;
      }

      if ($$0 != null) {
         $$0.close();
      }

      return var7;
   }

   @Nullable
   public static String getDefaultDeviceName() {
      if (!ALC10.alcIsExtensionPresent(0L, "ALC_ENUMERATE_ALL_EXT")) {
         return null;
      } else {
         ALUtil.getStringList(0L, 4115);
         return ALC10.alcGetString(0L, 4114);
      }
   }

   public String getCurrentDeviceName() {
      String $$0 = ALC10.alcGetString(this.currentDevice, 4115);
      if ($$0 == null) {
         $$0 = ALC10.alcGetString(this.currentDevice, 4101);
      }

      if ($$0 == null) {
         $$0 = "Unknown";
      }

      return $$0;
   }

   public synchronized boolean hasDefaultDeviceChanged() {
      String $$0 = getDefaultDeviceName();
      if (Objects.equals(this.defaultDeviceName, $$0)) {
         return false;
      } else {
         this.defaultDeviceName = $$0;
         return true;
      }
   }

   private static long openDeviceOrFallback(@Nullable String p_193473_) {
      OptionalLong $$1 = OptionalLong.empty();
      if (p_193473_ != null) {
         $$1 = tryOpenDevice(p_193473_);
      }

      if ($$1.isEmpty()) {
         $$1 = tryOpenDevice(getDefaultDeviceName());
      }

      if ($$1.isEmpty()) {
         $$1 = tryOpenDevice(null);
      }

      if ($$1.isEmpty()) {
         throw new IllegalStateException("Failed to open OpenAL device");
      } else {
         return $$1.getAsLong();
      }
   }

   private static OptionalLong tryOpenDevice(@Nullable String p_193476_) {
      long $$1 = ALC10.alcOpenDevice(p_193476_);
      return $$1 != 0L && !OpenAlUtil.checkALCError($$1, "Open device") ? OptionalLong.of($$1) : OptionalLong.empty();
   }

   public void cleanup() {
      this.staticChannels.cleanup();
      this.streamingChannels.cleanup();
      ALC10.alcDestroyContext(this.context);
      if (this.currentDevice != 0L) {
         ALC10.alcCloseDevice(this.currentDevice);
      }
   }

   public Listener getListener() {
      return this.listener;
   }

   @Nullable
   public Channel acquireChannel(Library.Pool p_83698_) {
      return (p_83698_ == Library.Pool.STREAMING ? this.streamingChannels : this.staticChannels).acquire();
   }

   public void releaseChannel(Channel p_83696_) {
      if (!this.staticChannels.release(p_83696_) && !this.streamingChannels.release(p_83696_)) {
         throw new IllegalStateException("Tried to release unknown channel");
      }
   }

   public String getDebugString() {
      return String.format(
         Locale.ROOT,
         "Sounds: %d/%d + %d/%d",
         this.staticChannels.getUsedCount(),
         this.staticChannels.getMaxCount(),
         this.streamingChannels.getUsedCount(),
         this.streamingChannels.getMaxCount()
      );
   }

   public List<String> getAvailableSoundDevices() {
      List<String> $$0 = ALUtil.getStringList(0L, 4115);
      return $$0 == null ? Collections.emptyList() : $$0;
   }

   public boolean isCurrentDeviceDisconnected() {
      return this.supportsDisconnections && ALC11.alcGetInteger(this.currentDevice, 787) == 0;
   }

   @OnlyIn(Dist.CLIENT)
   interface ChannelPool {
      @Nullable
      Channel acquire();

      boolean release(Channel var1);

      void cleanup();

      int getMaxCount();

      int getUsedCount();
   }

   @OnlyIn(Dist.CLIENT)
   static class CountingChannelPool implements Library.ChannelPool {
      private final int limit;
      private final Set<Channel> activeChannels = Sets.newIdentityHashSet();

      public CountingChannelPool(int p_83716_) {
         this.limit = p_83716_;
      }

      @Nullable
      @Override
      public Channel acquire() {
         if (this.activeChannels.size() >= this.limit) {
            if (SharedConstants.IS_RUNNING_IN_IDE) {
               Library.LOGGER.warn("Maximum sound pool size {} reached", this.limit);
            }

            return null;
         } else {
            Channel $$0 = Channel.create();
            if ($$0 != null) {
               this.activeChannels.add($$0);
            }

            return $$0;
         }
      }

      @Override
      public boolean release(Channel p_83719_) {
         if (!this.activeChannels.remove(p_83719_)) {
            return false;
         } else {
            p_83719_.destroy();
            return true;
         }
      }

      @Override
      public void cleanup() {
         this.activeChannels.forEach(Channel::destroy);
         this.activeChannels.clear();
      }

      @Override
      public int getMaxCount() {
         return this.limit;
      }

      @Override
      public int getUsedCount() {
         return this.activeChannels.size();
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Pool {
      STATIC,
      STREAMING;
   }
}

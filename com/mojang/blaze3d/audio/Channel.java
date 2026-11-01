package com.mojang.blaze3d.audio;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.AudioFormat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class Channel {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int QUEUED_BUFFER_COUNT = 4;
   public static final int BUFFER_DURATION_SECONDS = 1;
   private final int source;
   private final AtomicBoolean initialized = new AtomicBoolean(true);
   private int streamingBufferSize = 16384;
   @Nullable
   private AudioStream stream;

   @Nullable
   static Channel create() {
      int[] is = new int[1];
      AL10.alGenSources(is);
      return OpenAlUtil.checkALError("Allocate new source") ? null : new Channel(is[0]);
   }

   private Channel(int source) {
      this.source = source;
   }

   public void destroy() {
      if (this.initialized.compareAndSet(true, false)) {
         AL10.alSourceStop(this.source);
         OpenAlUtil.checkALError("Stop");
         if (this.stream != null) {
            try {
               this.stream.close();
            } catch (IOException var2) {
               LOGGER.error("Failed to close audio stream", var2);
            }

            this.removeProcessedBuffers();
            this.stream = null;
         }

         AL10.alDeleteSources(new int[]{this.source});
         OpenAlUtil.checkALError("Cleanup");
      }
   }

   public void play() {
      AL10.alSourcePlay(this.source);
   }

   private int getState() {
      return !this.initialized.get() ? 4116 : AL10.alGetSourcei(this.source, 4112);
   }

   public void pause() {
      if (this.getState() == 4114) {
         AL10.alSourcePause(this.source);
      }
   }

   public void unpause() {
      if (this.getState() == 4115) {
         AL10.alSourcePlay(this.source);
      }
   }

   public void stop() {
      if (this.initialized.get()) {
         AL10.alSourceStop(this.source);
         OpenAlUtil.checkALError("Stop");
      }
   }

   public boolean playing() {
      return this.getState() == 4114;
   }

   public boolean stopped() {
      return this.getState() == 4116;
   }

   public void setSelfPosition(Vec3 source) {
      AL10.alSourcefv(this.source, 4100, new float[]{(float)source.x, (float)source.y, (float)source.z});
   }

   public void setPitch(float pitch) {
      AL10.alSourcef(this.source, 4099, pitch);
   }

   public void setLooping(boolean looping) {
      AL10.alSourcei(this.source, 4103, looping ? 1 : 0);
   }

   public void setVolume(float volume) {
      AL10.alSourcef(this.source, 4106, volume);
   }

   public void disableAttenuation() {
      AL10.alSourcei(this.source, 53248, 0);
   }

   public void linearAttenuation(float linearAttenuation) {
      AL10.alSourcei(this.source, 53248, 53251);
      AL10.alSourcef(this.source, 4131, linearAttenuation);
      AL10.alSourcef(this.source, 4129, 1.0F);
      AL10.alSourcef(this.source, 4128, 0.0F);
   }

   public void setRelative(boolean relative) {
      AL10.alSourcei(this.source, 514, relative ? 1 : 0);
   }

   public void attachStaticBuffer(SoundBuffer buffer) {
      buffer.getAlBuffer().ifPresent(i -> AL10.alSourcei(this.source, 4105, i));
   }

   public void attachBufferStream(AudioStream stream) {
      this.stream = stream;
      AudioFormat audioFormat = stream.getFormat();
      this.streamingBufferSize = calculateBufferSize(audioFormat, 1);
      this.pumpBuffers(4);
   }

   private static int calculateBufferSize(AudioFormat format, int sampleAmount) {
      return (int)(sampleAmount * format.getSampleSizeInBits() / 8.0F * format.getChannels() * format.getSampleRate());
   }

   private void pumpBuffers(int readCount) {
      if (this.stream != null) {
         try {
            for (int i = 0; i < readCount; i++) {
               ByteBuffer byteBuffer = this.stream.read(this.streamingBufferSize);
               if (byteBuffer != null) {
                  new SoundBuffer(byteBuffer, this.stream.getFormat()).releaseAlBuffer().ifPresent(ix -> AL10.alSourceQueueBuffers(this.source, new int[]{ix}));
               }
            }
         } catch (IOException var4) {
            LOGGER.error("Failed to read from audio stream", var4);
         }
      }
   }

   public void updateStream() {
      if (this.stream != null) {
         int i = this.removeProcessedBuffers();
         this.pumpBuffers(i);
      }
   }

   private int removeProcessedBuffers() {
      int i = AL10.alGetSourcei(this.source, 4118);
      if (i > 0) {
         int[] is = new int[i];
         AL10.alSourceUnqueueBuffers(this.source, is);
         OpenAlUtil.checkALError("Unqueue buffers");
         AL10.alDeleteBuffers(is);
         OpenAlUtil.checkALError("Remove processed buffers");
      }

      return i;
   }
}

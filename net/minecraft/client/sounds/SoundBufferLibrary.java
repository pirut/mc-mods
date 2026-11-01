package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.audio.SoundBuffer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.Util;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SoundBufferLibrary {
   private final ResourceProvider resourceManager;
   private final Map<ResourceLocation, CompletableFuture<SoundBuffer>> cache = Maps.newHashMap();

   public SoundBufferLibrary(ResourceProvider p_248900_) {
      this.resourceManager = p_248900_;
   }

   public CompletableFuture<SoundBuffer> getCompleteBuffer(ResourceLocation p_120203_) {
      return this.cache.computeIfAbsent(p_120203_, p_340614_ -> CompletableFuture.supplyAsync(() -> {
         try {
            SoundBuffer var5;
            try (
               InputStream $$1 = this.resourceManager.open(p_340614_);
               FiniteAudioStream $$2 = new JOrbisAudioStream($$1);
            ) {
               ByteBuffer $$3 = $$2.readAll();
               var5 = new SoundBuffer($$3, $$2.getFormat());
            }

            return var5;
         } catch (IOException var10) {
            throw new CompletionException(var10);
         }
      }, Util.nonCriticalIoPool()));
   }

   public CompletableFuture<AudioStream> getStream(ResourceLocation p_120205_, boolean p_120206_) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            InputStream $$2 = this.resourceManager.open(p_120205_);
            return (AudioStream)(p_120206_ ? new LoopingAudioStream(JOrbisAudioStream::new, $$2) : new JOrbisAudioStream($$2));
         } catch (IOException var4) {
            throw new CompletionException(var4);
         }
      }, Util.nonCriticalIoPool());
   }

   public void clear() {
      this.cache.values().forEach(p_120201_ -> p_120201_.thenAccept(SoundBuffer::discardAlBuffer));
      this.cache.clear();
   }

   public CompletableFuture<?> preload(Collection<Sound> p_120199_) {
      return CompletableFuture.allOf(p_120199_.stream().map(p_120197_ -> this.getCompleteBuffer(p_120197_.getPath())).toArray(CompletableFuture[]::new));
   }
}

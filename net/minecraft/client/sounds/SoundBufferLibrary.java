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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.Util;

@Environment(EnvType.CLIENT)
public class SoundBufferLibrary {
   private final ResourceProvider resourceManager;
   private final Map<Identifier, CompletableFuture<SoundBuffer>> cache = Maps.newHashMap();

   public SoundBufferLibrary(ResourceProvider resourceManager) {
      this.resourceManager = resourceManager;
   }

   public CompletableFuture<SoundBuffer> getCompleteBuffer(Identifier soundID) {
      return this.cache.computeIfAbsent(soundID, identifier -> CompletableFuture.supplyAsync(() -> {
         try {
            SoundBuffer var5;
            try (
               InputStream inputStream = this.resourceManager.open(identifier);
               FiniteAudioStream finiteAudioStream = new JOrbisAudioStream(inputStream);
            ) {
               ByteBuffer byteBuffer = finiteAudioStream.readAll();
               var5 = new SoundBuffer(byteBuffer, finiteAudioStream.getFormat());
            }

            return var5;
         } catch (IOException var10) {
            throw new CompletionException(var10);
         }
      }, Util.nonCriticalIoPool()));
   }

   public CompletableFuture<AudioStream> getStream(Identifier resourceLocation, boolean isWrapper) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            InputStream inputStream = this.resourceManager.open(resourceLocation);
            return (AudioStream)(isWrapper ? new LoopingAudioStream(JOrbisAudioStream::new, inputStream) : new JOrbisAudioStream(inputStream));
         } catch (IOException var4) {
            throw new CompletionException(var4);
         }
      }, Util.nonCriticalIoPool());
   }

   public void clear() {
      this.cache.values().forEach(completableFuture -> completableFuture.thenAccept(SoundBuffer::discardAlBuffer));
      this.cache.clear();
   }

   public CompletableFuture<?> preload(Collection<Sound> sounds) {
      return CompletableFuture.allOf(sounds.stream().map(sound -> this.getCompleteBuffer(sound.getPath())).toArray(CompletableFuture[]::new));
   }
}

package net.minecraft.client.sounds;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ChannelAccess {
   private final Set<ChannelAccess.ChannelHandle> channels = Sets.newIdentityHashSet();
   final Library library;
   final Executor executor;

   public ChannelAccess(Library library, Executor executor) {
      this.library = library;
      this.executor = executor;
   }

   public CompletableFuture<ChannelAccess.ChannelHandle> createHandle(Library.Pool systemMode) {
      CompletableFuture<ChannelAccess.ChannelHandle> completableFuture = new CompletableFuture<>();
      this.executor.execute(() -> {
         Channel channel = this.library.acquireChannel(systemMode);
         if (channel != null) {
            ChannelAccess.ChannelHandle channelHandle = new ChannelAccess.ChannelHandle(channel);
            this.channels.add(channelHandle);
            completableFuture.complete(channelHandle);
         } else {
            completableFuture.complete(null);
         }
      });
      return completableFuture;
   }

   public void executeOnChannels(Consumer<Stream<Channel>> sourceStreamConsumer) {
      this.executor.execute(() -> sourceStreamConsumer.accept(this.channels.stream().map(channelHandle -> channelHandle.channel).filter(Objects::nonNull)));
   }

   public void scheduleTick() {
      this.executor.execute(() -> {
         Iterator<ChannelAccess.ChannelHandle> iterator = this.channels.iterator();

         while (iterator.hasNext()) {
            ChannelAccess.ChannelHandle channelHandle = iterator.next();
            channelHandle.channel.updateStream();
            if (channelHandle.channel.stopped()) {
               channelHandle.release();
               iterator.remove();
            }
         }
      });
   }

   public void clear() {
      this.channels.forEach(ChannelAccess.ChannelHandle::release);
      this.channels.clear();
   }

   @Environment(EnvType.CLIENT)
   public class ChannelHandle {
      @Nullable
      Channel channel;
      private boolean stopped;

      public boolean isStopped() {
         return this.stopped;
      }

      public ChannelHandle(final Channel channel) {
         this.channel = channel;
      }

      public void execute(Consumer<Channel> soundConsumer) {
         ChannelAccess.this.executor.execute(() -> {
            if (this.channel != null) {
               soundConsumer.accept(this.channel);
            }
         });
      }

      public void release() {
         this.stopped = true;
         ChannelAccess.this.library.releaseChannel(this.channel);
         this.channel = null;
      }
   }
}

package net.minecraft.util.debug;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public class DebugSubscription<T> {
   public static final int DOES_NOT_EXPIRE = 0;
   @Nullable
   final StreamCodec<? super RegistryFriendlyByteBuf, T> valueStreamCodec;
   private final int expireAfterTicks;

   public DebugSubscription(@Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> p_449213_, int p_449358_) {
      this.valueStreamCodec = p_449213_;
      this.expireAfterTicks = p_449358_;
   }

   public DebugSubscription(@Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> p_449146_) {
      this(p_449146_, 0);
   }

   public DebugSubscription.Update<T> packUpdate(@Nullable T p_449867_) {
      return new DebugSubscription.Update<>(this, Optional.ofNullable(p_449867_));
   }

   public DebugSubscription.Update<T> emptyUpdate() {
      return new DebugSubscription.Update<>(this, Optional.empty());
   }

   public DebugSubscription.Event<T> packEvent(T p_449157_) {
      return new DebugSubscription.Event<>(this, p_449157_);
   }

   @Override
   public String toString() {
      return Util.getRegisteredName(BuiltInRegistries.DEBUG_SUBSCRIPTION, this);
   }

   @Nullable
   public StreamCodec<? super RegistryFriendlyByteBuf, T> valueStreamCodec() {
      return this.valueStreamCodec;
   }

   public int expireAfterTicks() {
      return this.expireAfterTicks;
   }

   public record Event<T>(DebugSubscription<T> subscription, T value) {
      public static final StreamCodec<RegistryFriendlyByteBuf, DebugSubscription.Event<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.DEBUG_SUBSCRIPTION)
         .dispatch(DebugSubscription.Event::subscription, DebugSubscription.Event::streamCodec);

      private static <T> StreamCodec<? super RegistryFriendlyByteBuf, DebugSubscription.Event<T>> streamCodec(DebugSubscription<T> p_449772_) {
         return Objects.requireNonNull(p_449772_.valueStreamCodec)
            .map(p_449531_ -> new DebugSubscription.Event<>(p_449772_, (T)p_449531_), DebugSubscription.Event::value);
      }
   }

   public record Update<T>(DebugSubscription<T> subscription, Optional<T> value) {
      public static final StreamCodec<RegistryFriendlyByteBuf, DebugSubscription.Update<?>> STREAM_CODEC = ByteBufCodecs.registry(Registries.DEBUG_SUBSCRIPTION)
         .dispatch(DebugSubscription.Update::subscription, DebugSubscription.Update::streamCodec);

      private static <T> StreamCodec<? super RegistryFriendlyByteBuf, DebugSubscription.Update<T>> streamCodec(DebugSubscription<T> p_449390_) {
         return ByteBufCodecs.optional(Objects.requireNonNull(p_449390_.valueStreamCodec))
            .map(p_449343_ -> new DebugSubscription.Update<>(p_449390_, (Optional<T>)p_449343_), DebugSubscription.Update::value);
      }
   }
}

package net.neoforged.neoforge.attachment;

import com.google.common.base.Predicates;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

public final class AttachmentType<T> {
   final Function<IAttachmentHolder, T> defaultValueSupplier;
   @Nullable
   final IAttachmentSerializer<?, T> serializer;
   final boolean copyOnDeath;
   final IAttachmentCopyHandler<T> copyHandler;
   @Nullable
   AttachmentSyncHandler<T> syncHandler;

   private AttachmentType(AttachmentType.Builder<T> builder) {
      this.defaultValueSupplier = builder.defaultValueSupplier;
      this.serializer = builder.serializer;
      this.copyOnDeath = builder.copyOnDeath;
      this.copyHandler = builder.copyHandler != null ? builder.copyHandler : defaultCopyHandler(this.serializer);
      this.syncHandler = builder.syncHandler;
   }

   private static <T, H extends Tag> IAttachmentCopyHandler<T> defaultCopyHandler(@Nullable IAttachmentSerializer<H, T> serializer) {
      return serializer == null ? (attachment, holder, provider) -> {
         throw new UnsupportedOperationException("Cannot copy non-serializable attachments");
      } : (attachment, holder, provider) -> {
         H serialized = serializer.write(attachment, provider);
         return serialized != null ? serializer.read(holder, serialized, provider) : null;
      };
   }

   public static <T> AttachmentType.Builder<T> builder(Supplier<T> defaultValueSupplier) {
      return builder(holder -> defaultValueSupplier.get());
   }

   public static <T> AttachmentType.Builder<T> builder(Function<IAttachmentHolder, T> defaultValueConstructor) {
      return new AttachmentType.Builder<>(defaultValueConstructor);
   }

   public static <S extends Tag, T extends INBTSerializable<S>> AttachmentType.Builder<T> serializable(Supplier<T> defaultValueSupplier) {
      return serializable(holder -> defaultValueSupplier.get());
   }

   public static <S extends Tag, T extends INBTSerializable<S>> AttachmentType.Builder<T> serializable(
      final Function<IAttachmentHolder, T> defaultValueConstructor
   ) {
      return builder(defaultValueConstructor).serialize(new IAttachmentSerializer<S, T>() {
         public T read(IAttachmentHolder holder, S tag, HolderLookup.Provider provider) {
            T ret = defaultValueConstructor.apply(holder);
            ret.deserializeNBT(provider, tag);
            return ret;
         }

         @Nullable
         public S write(T attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT(provider);
         }
      });
   }

   public static class Builder<T> {
      private final Function<IAttachmentHolder, T> defaultValueSupplier;
      @Nullable
      private IAttachmentSerializer<?, T> serializer;
      private boolean copyOnDeath;
      @Nullable
      private IAttachmentCopyHandler<T> copyHandler;
      @Nullable
      private AttachmentSyncHandler<T> syncHandler;

      private Builder(Function<IAttachmentHolder, T> defaultValueSupplier) {
         this.defaultValueSupplier = defaultValueSupplier;
      }

      public AttachmentType.Builder<T> serialize(IAttachmentSerializer<?, T> serializer) {
         Objects.requireNonNull(serializer);
         if (this.serializer != null) {
            throw new IllegalStateException("Serializer already set");
         } else {
            this.serializer = serializer;
            return this;
         }
      }

      public AttachmentType.Builder<T> serialize(Codec<T> codec) {
         return this.serialize(codec, Predicates.alwaysTrue());
      }

      public AttachmentType.Builder<T> serialize(final Codec<T> codec, final Predicate<? super T> shouldSerialize) {
         Objects.requireNonNull(codec);
         return this.serialize(new IAttachmentSerializer<Tag, T>() {
            @Override
            public T read(IAttachmentHolder holder, Tag tag, HolderLookup.Provider provider) {
               DataResult<T> parsingResult = codec.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag);
               return (T)parsingResult.getOrThrow(msg -> this.buildException("read", msg));
            }

            @Nullable
            @Override
            public Tag write(T attachment, HolderLookup.Provider provider) {
               if (!shouldSerialize.test(attachment)) {
                  return null;
               } else {
                  DataResult<Tag> encodingResult = codec.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), attachment);
                  return (Tag)encodingResult.getOrThrow(msg -> this.buildException("write", msg));
               }
            }

            private RuntimeException buildException(String operation, String error) {
               return new IllegalStateException("Unable to " + operation + " attachment due to an internal codec error: " + error);
            }
         });
      }

      public AttachmentType.Builder<T> copyOnDeath() {
         if (this.serializer == null) {
            throw new IllegalStateException("copyOnDeath requires a serializer");
         } else {
            this.copyOnDeath = true;
            return this;
         }
      }

      public AttachmentType.Builder<T> copyHandler(IAttachmentCopyHandler<T> cloner) {
         Objects.requireNonNull(cloner);
         if (this.serializer == null) {
            throw new IllegalStateException("copyHandler requires a serializer");
         } else {
            this.copyHandler = cloner;
            return this;
         }
      }

      public AttachmentType.Builder<T> sync(AttachmentSyncHandler<T> syncHandler) {
         Objects.requireNonNull(syncHandler);
         this.syncHandler = syncHandler;
         return this;
      }

      public AttachmentType.Builder<T> sync(StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
         return this.sync((holder, to) -> true, streamCodec);
      }

      public AttachmentType.Builder<T> sync(
         final BiPredicate<IAttachmentHolder, ServerPlayer> sendToPlayer, final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec
      ) {
         Objects.requireNonNull(sendToPlayer);
         Objects.requireNonNull(streamCodec);
         return this.sync(new AttachmentSyncHandler<T>() {
            @Override
            public boolean sendToPlayer(IAttachmentHolder holder, ServerPlayer to) {
               return sendToPlayer.test(holder, to);
            }

            @Override
            public void write(RegistryFriendlyByteBuf buf, T attachment, boolean initialSync) {
               streamCodec.encode(buf, attachment);
            }

            @Override
            public T read(IAttachmentHolder holder, RegistryFriendlyByteBuf buf, @Nullable T previousValue) {
               return streamCodec.decode(buf);
            }
         });
      }

      public AttachmentType<T> build() {
         return new AttachmentType<>(this);
      }
   }
}

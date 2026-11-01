package net.neoforged.neoforge.attachment;

import com.google.common.base.Predicates;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import org.jspecify.annotations.Nullable;

public final class AttachmentType<T> {
   final Function<IAttachmentHolder, T> defaultValueSupplier;
   @Nullable
   final IAttachmentSerializer<T> serializer;
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

   private static <T> IAttachmentCopyHandler<T> defaultCopyHandler(@Nullable IAttachmentSerializer<T> serializer) {
      return serializer == null ? (attachment, holder, provider) -> {
         throw new UnsupportedOperationException("Cannot copy non-serializable attachments");
      } : (attachment, holder, provider) -> {
         ProblemReporter.Collector reporter = new ProblemReporter.Collector();
         TagValueOutput output = TagValueOutput.createWithContext(reporter, provider);
         if (!serializer.write(attachment, output)) {
            return null;
         } else if (!reporter.isEmpty()) {
            throw new IllegalArgumentException("Attachment failed to serialise during copy: " + reporter.getReport());
         } else {
            reporter = new ProblemReporter.Collector();
            ValueInput input = TagValueInput.create(reporter, provider, output.buildResult());
            T attach = serializer.read(holder, input);
            if (!reporter.isEmpty()) {
               throw new IllegalArgumentException("Attachment failed to deserialise during copy: " + reporter.getReport());
            } else {
               return attach;
            }
         }
      };
   }

   public static <T> AttachmentType.Builder<T> builder(Supplier<T> defaultValueSupplier) {
      return builder(holder -> defaultValueSupplier.get());
   }

   public static <T> AttachmentType.Builder<T> builder(Function<IAttachmentHolder, T> defaultValueConstructor) {
      return new AttachmentType.Builder<>(defaultValueConstructor);
   }

   public static <T extends ValueIOSerializable> AttachmentType.Builder<T> serializable(Supplier<T> defaultValueSupplier) {
      return serializable(holder -> defaultValueSupplier.get());
   }

   public static <T extends ValueIOSerializable> AttachmentType.Builder<T> serializable(final Function<IAttachmentHolder, T> defaultValueConstructor) {
      return builder(defaultValueConstructor).serialize(new IAttachmentSerializer<T>() {
         public T read(IAttachmentHolder holder, ValueInput input) {
            T ret = defaultValueConstructor.apply(holder);
            ret.deserialize(input);
            return ret;
         }

         public boolean write(T attachment, ValueOutput output) {
            attachment.serialize(output);
            return true;
         }
      });
   }

   public static class Builder<T> {
      private final Function<IAttachmentHolder, T> defaultValueSupplier;
      @Nullable
      private IAttachmentSerializer<T> serializer;
      private boolean copyOnDeath;
      @Nullable
      private IAttachmentCopyHandler<T> copyHandler;
      @Nullable
      private AttachmentSyncHandler<T> syncHandler;

      private Builder(Function<IAttachmentHolder, T> defaultValueSupplier) {
         this.defaultValueSupplier = defaultValueSupplier;
      }

      public AttachmentType.Builder<T> serialize(IAttachmentSerializer<T> serializer) {
         Objects.requireNonNull(serializer);
         if (this.serializer != null) {
            throw new IllegalStateException("Serializer already set");
         } else {
            this.serializer = serializer;
            return this;
         }
      }

      public AttachmentType.Builder<T> serialize(MapCodec<T> codec) {
         return this.serialize(codec, Predicates.alwaysTrue());
      }

      public AttachmentType.Builder<T> serialize(final MapCodec<T> codec, final Predicate<? super T> shouldSerialize) {
         Objects.requireNonNull(codec);
         return this.serialize(new IAttachmentSerializer<T>() {
            @Override
            public T read(IAttachmentHolder holder, ValueInput input) {
               Optional<T> parsingResult = input.read(codec);
               return parsingResult.orElseThrow(() -> this.buildException("read"));
            }

            @Override
            public boolean write(T attachment, ValueOutput output) {
               if (!shouldSerialize.test(attachment)) {
                  return false;
               } else {
                  output.store(codec, attachment);
                  return true;
               }
            }

            private RuntimeException buildException(String operation) {
               return new IllegalStateException("Unable to " + operation + " attachment due to an internal codec error.");
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

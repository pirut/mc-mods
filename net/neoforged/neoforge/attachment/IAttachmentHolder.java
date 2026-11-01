package net.neoforged.neoforge.attachment;

import java.util.Optional;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

public interface IAttachmentHolder {
   boolean hasAttachments();

   boolean hasData(AttachmentType<?> var1);

   default <T> boolean hasData(Supplier<AttachmentType<T>> type) {
      return this.hasData(type.get());
   }

   <T> T getData(AttachmentType<T> var1);

   default <T> T getData(Supplier<AttachmentType<T>> type) {
      return this.getData(type.get());
   }

   default <T> Optional<T> getExistingData(AttachmentType<T> type) {
      return Optional.ofNullable(this.getExistingDataOrNull(type));
   }

   default <T> Optional<T> getExistingData(Supplier<AttachmentType<T>> type) {
      return this.getExistingData(type.get());
   }

   @Nullable
   <T> T getExistingDataOrNull(AttachmentType<T> var1);

   @Nullable
   default <T> T getExistingDataOrNull(Supplier<AttachmentType<T>> type) {
      return this.getExistingDataOrNull(type.get());
   }

   @Nullable
   <T> T setData(AttachmentType<T> var1, T var2);

   @Nullable
   default <T> T setData(Supplier<AttachmentType<T>> type, T data) {
      return this.setData(type.get(), data);
   }

   @Nullable
   <T> T removeData(AttachmentType<T> var1);

   @Nullable
   default <T> T removeData(Supplier<AttachmentType<T>> type) {
      return this.removeData(type.get());
   }

   default void syncData(AttachmentType<?> type) {
   }

   default void syncData(Supplier<? extends AttachmentType<?>> type) {
      this.syncData((AttachmentType<?>)type.get());
   }
}

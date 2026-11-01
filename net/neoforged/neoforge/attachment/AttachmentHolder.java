package net.neoforged.neoforge.attachment;

import com.mojang.logging.LogUtils;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class AttachmentHolder implements IAttachmentHolder {
   public static final String ATTACHMENTS_NBT_KEY = "neoforge:attachments";
   private static final boolean IN_DEV = !FMLEnvironment.isProduction();
   private static final Logger LOGGER = LogUtils.getLogger();
   @Nullable
   Map<AttachmentType<?>, Object> attachments = null;

   private void validateAttachmentType(AttachmentType<?> type) {
      Objects.requireNonNull(type);
      if (IN_DEV) {
         if (!NeoForgeRegistries.ATTACHMENT_TYPES.containsValue(type)) {
            throw new IllegalArgumentException(
               "Data attachment type with default value " + type.defaultValueSupplier.apply(this.getExposedHolder()) + " must be registered!"
            );
         }
      }
   }

   final Map<AttachmentType<?>, Object> getAttachmentMap() {
      if (this.attachments == null) {
         this.attachments = new IdentityHashMap<>(4);
      }

      return this.attachments;
   }

   IAttachmentHolder getExposedHolder() {
      return this;
   }

   @Override
   public final boolean hasAttachments() {
      return this.attachments != null && !this.attachments.isEmpty();
   }

   @Override
   public final boolean hasData(AttachmentType<?> type) {
      this.validateAttachmentType(type);
      return this.attachments != null && this.attachments.containsKey(type);
   }

   @Override
   public final <T> T getData(AttachmentType<T> type) {
      this.validateAttachmentType(type);
      T ret = (T)this.getAttachmentMap().get(type);
      if (ret == null) {
         ret = type.defaultValueSupplier.apply(this.getExposedHolder());
         this.attachments.put(type, ret);
         this.syncData(type);
      }

      return ret;
   }

   @Nullable
   @Override
   public <T> T getExistingDataOrNull(AttachmentType<T> type) {
      this.validateAttachmentType(type);
      return (T)(this.attachments == null ? null : this.attachments.get(type));
   }

   @MustBeInvokedByOverriders
   @Nullable
   @Override
   public <T> T setData(AttachmentType<T> type, T data) {
      this.validateAttachmentType(type);
      Objects.requireNonNull(data);
      T previousData = (T)this.getAttachmentMap().put(type, data);
      this.syncData(type);
      return previousData;
   }

   @MustBeInvokedByOverriders
   @Nullable
   @Override
   public <T> T removeData(AttachmentType<T> type) {
      this.validateAttachmentType(type);
      if (this.attachments == null) {
         return null;
      } else {
         T previousData = (T)this.attachments.remove(type);
         this.syncData(type);
         return previousData;
      }
   }

   public final void serializeAttachments(ValueOutput tag) {
      if (this.attachments != null) {
         for (Entry<AttachmentType<?>, Object> entry : this.attachments.entrySet()) {
            AttachmentType<?> type = entry.getKey();
            Identifier key = NeoForgeRegistries.ATTACHMENT_TYPES.getKey(type);
            if (type.serializer != null) {
               try {
                  ValueOutput serialized = tag.child(key.toString());
                  boolean doSerialise = ((IAttachmentSerializer<Object>)type.serializer).write(entry.getValue(), serialized);
                  if (!doSerialise) {
                     tag.discard(key.toString());
                  }
               } catch (Exception var8) {
                  LOGGER.error("Failed to serialize data attachment {}. Skipping.", key, var8);
               }
            }
         }
      }
   }

   protected final void deserializeAttachments(ValueInput input) {
      for (String key : input.keySet()) {
         Identifier keyLocation = Identifier.tryParse(key);
         if (keyLocation == null) {
            LOGGER.error("Encountered invalid data attachment key {}. Skipping.", key);
         } else {
            AttachmentType<?> type = NeoForgeRegistries.ATTACHMENT_TYPES.getValue(keyLocation);
            if (type != null && type.serializer != null) {
               try {
                  this.getAttachmentMap().put(type, type.serializer.read(this.getExposedHolder(), input.rawChildOrEmpty(key)));
               } catch (Exception var7) {
                  LOGGER.error("Failed to deserialize data attachment {}. Skipping.", key, var7);
               }
            } else {
               LOGGER.error("Encountered unknown or non-serializable data attachment {}. Skipping.", key);
            }
         }
      }
   }

   public static class AsField extends AttachmentHolder {
      private final IAttachmentHolder exposedHolder;

      public AsField(IAttachmentHolder exposedHolder) {
         this.exposedHolder = exposedHolder;
      }

      @Override
      IAttachmentHolder getExposedHolder() {
         return this.exposedHolder;
      }

      public void deserializeInternal(HolderLookup.Provider provider, ValueInput tag) {
         this.deserializeAttachments(tag);
      }

      @Override
      public void syncData(AttachmentType<?> type) {
         this.exposedHolder.syncData(type);
      }
   }
}

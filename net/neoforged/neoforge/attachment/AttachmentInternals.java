package net.neoforged.neoforge.attachment;

import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.ApiStatus.Internal;

@EventBusSubscriber(
   modid = "neoforge"
)
@Internal
public final class AttachmentInternals {
   private static <H extends AttachmentHolder> void copyAttachments(HolderLookup.Provider provider, H from, H to, Predicate<AttachmentType<?>> filter) {
      if (from.attachments != null) {
         for (Entry<AttachmentType<?>, Object> entry : from.attachments.entrySet()) {
            AttachmentType<?> type = entry.getKey();
            if (type.serializer != null) {
               IAttachmentCopyHandler<Object> copyHandler = (IAttachmentCopyHandler<Object>)type.copyHandler;
               if (filter.test(type)) {
                  Object copy = copyHandler.copy(entry.getValue(), to.getExposedHolder(), provider);
                  if (copy != null) {
                     to.getAttachmentMap().put(type, copy);
                  }
               }
            }
         }
      }
   }

   public static void copyChunkAttachmentsOnPromotion(HolderLookup.Provider provider, AttachmentHolder.AsField from, AttachmentHolder.AsField to) {
      copyAttachments(provider, from, to, type -> true);
   }

   public static void copyEntityAttachments(Entity from, Entity to, boolean isDeath) {
      copyAttachments(from.registryAccess(), from, to, isDeath ? type -> type.copyOnDeath : type -> true);
   }

   @SubscribeEvent
   public static void onPlayerClone(PlayerEvent.Clone event) {
      event.getEntity().copyAttachmentsFrom(event.getOriginal(), event.isWasDeath());
   }

   @SubscribeEvent
   public static void onLivingConvert(LivingConversionEvent.Post event) {
      event.getOutcome().copyAttachmentsFrom(event.getEntity(), true);
   }

   private AttachmentInternals() {
   }
}

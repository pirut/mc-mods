package net.neoforged.neoforge.client.registries;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.payload.KnownRegistryDataMapsPayload;
import net.neoforged.neoforge.network.payload.KnownRegistryDataMapsReplyPayload;
import net.neoforged.neoforge.network.payload.RegistryDataMapSyncPayload;
import net.neoforged.neoforge.registries.BaseMappedRegistry;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.datamaps.DataMapsUpdatedEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

@Internal
public class ClientRegistryManager {
   private static final Logger LOGGER = LogUtils.getLogger();

   public static <R> void handleDataMapSync(RegistryDataMapSyncPayload<R> payload, IPayloadContext context) {
      context.enqueueWork(
         () -> {
            try {
               RegistryAccess regAccess = Minecraft.getInstance().level.registryAccess();
               BaseMappedRegistry<R> registry = (BaseMappedRegistry<R>)regAccess.<R>lookupOrThrow(payload.registryKey());
               registry.getDataMaps().clear();
               payload.dataMaps()
                  .forEach(
                     (attachKey, maps) -> registry.getDataMaps()
                        .put(RegistryManager.getDataMap(payload.registryKey(), attachKey), Collections.unmodifiableMap((Map<? extends ResourceKey<R>, ?>)maps))
                  );
               NeoForge.EVENT_BUS.post(new DataMapsUpdatedEvent(regAccess, registry, DataMapsUpdatedEvent.UpdateCause.CLIENT_SYNC));
            } catch (Throwable var4) {
               LOGGER.error("Failed to handle registry data map sync: ", var4);
               context.disconnect(Component.translatable("neoforge.network.data_maps.failed", payload.registryKey().identifier().toString(), var4.toString()));
            }
         }
      );
   }

   public static void handleKnownDataMaps(KnownRegistryDataMapsPayload payload, IPayloadContext context) {
      record MandatoryEntry(ResourceKey<? extends Registry<?>> registry, Identifier id) {
      }

      Set<MandatoryEntry> ourMandatory = new HashSet<>();
      RegistryManager.getDataMaps().forEach((reg, values) -> values.values().forEach(attach -> {
         if (attach.mandatorySync()) {
            ourMandatory.add(new MandatoryEntry((ResourceKey<? extends Registry<?>>)reg, attach.id()));
         }
      }));
      Set<MandatoryEntry> theirMandatory = new HashSet<>();
      payload.dataMaps().forEach((reg, values) -> values.forEach(attach -> {
         if (attach.mandatory()) {
            theirMandatory.add(new MandatoryEntry((ResourceKey<? extends Registry<?>>)reg, attach.id()));
         }
      }));
      List<Component> messages = new ArrayList<>();
      SetView<MandatoryEntry> missingOur = Sets.difference(ourMandatory, theirMandatory);
      if (!missingOur.isEmpty()) {
         messages.add(
            Component.translatable(
               "neoforge.network.data_maps.missing_our",
               Component.literal(missingOur.stream().map(e -> e.id() + " (" + e.registry().identifier() + ")").collect(Collectors.joining(", ")))
                  .withStyle(ChatFormatting.GOLD)
            )
         );
      }

      SetView<MandatoryEntry> missingTheir = Sets.difference(theirMandatory, ourMandatory);
      if (!missingTheir.isEmpty()) {
         messages.add(
            Component.translatable(
               "neoforge.network.data_maps.missing_their",
               Component.literal(missingTheir.stream().map(e -> e.id() + " (" + e.registry().identifier() + ")").collect(Collectors.joining(", ")))
                  .withStyle(ChatFormatting.GOLD)
            )
         );
      }

      if (!messages.isEmpty()) {
         MutableComponent message = Component.empty();
         Iterator<Component> itr = messages.iterator();

         while (itr.hasNext()) {
            message = message.append(itr.next());
            if (itr.hasNext()) {
               message = message.append("\n");
            }
         }

         context.disconnect(message);
      } else {
         HashMap<ResourceKey<? extends Registry<?>>, Collection<Identifier>> known = new HashMap<>();
         RegistryManager.getDataMaps().forEach((key, vals) -> known.put((ResourceKey<? extends Registry<?>>)key, vals.keySet()));
         context.reply(new KnownRegistryDataMapsReplyPayload(known));
      }
   }
}

package net.neoforged.neoforge.network.configuration;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.logging.LogUtils;
import io.netty.buffer.ByteBuf;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.asm.enumextension.ExtensionInfo;
import net.neoforged.fml.common.asm.enumextension.IExtensibleEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum;
import net.neoforged.fml.common.asm.enumextension.NetworkedEnum.NetworkCheck;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.payload.ExtensibleEnumAcknowledgePayload;
import net.neoforged.neoforge.network.payload.ExtensibleEnumDataPayload;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;

@Internal
public record CheckExtensibleEnums(ServerConfigurationPacketListener listener) implements ConfigurationTask {
   public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(ResourceLocation.fromNamespaceAndPath("neoforge", "check_extensible_enum"));
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final org.objectweb.asm.Type NETWORKED_ENUM = org.objectweb.asm.Type.getType(NetworkedEnum.class);
   private static final List<? extends Class<? extends Enum<?>>> NETWORKED_EXTENSIBLE_ENUM_CLASSES = collectNetworkedEnumClasses();
   private static Map<String, CheckExtensibleEnums.EnumEntry> enumEntries = null;

   @Override
   public void start(Consumer<Packet<?>> packetSender) {
      if (this.listener.getConnection().isMemoryConnection()) {
         this.listener.finishCurrentTask(TYPE);
      } else {
         Map<String, CheckExtensibleEnums.EnumEntry> enumEntries = getEnumEntries();
         if (this.listener.getConnectionType().isOther()) {
            List<CheckExtensibleEnums.EnumEntry> extendedClientboundEnums = enumEntries.values()
               .stream()
               .filter(entry -> entry.isClientbound() && entry.isExtended())
               .toList();
            if (!extendedClientboundEnums.isEmpty()) {
               this.listener
                  .disconnect(Component.literal("This server does not support vanilla clients as it has extended enums used in clientbound networking"));
            } else {
               this.listener.finishCurrentTask(TYPE);
            }
         } else {
            packetSender.accept(new ExtensibleEnumDataPayload(enumEntries).toVanillaClientbound());
         }
      }
   }

   public static void handleClientboundPayload(ExtensibleEnumDataPayload payload, IPayloadContext context) {
      Map<String, CheckExtensibleEnums.EnumEntry> localEnumEntries = getEnumEntries();
      Map<String, CheckExtensibleEnums.EnumEntry> remoteEnumEntries = payload.enumEntries();
      Map<String, CheckExtensibleEnums.Mismatch> mismatched = new HashMap<>();
      UnmodifiableIterator message = Sets.union(localEnumEntries.keySet(), remoteEnumEntries.keySet()).iterator();

      while (message.hasNext()) {
         String className = (String)message.next();
         CheckExtensibleEnums.EnumEntry localEntry = localEnumEntries.get(className);
         CheckExtensibleEnums.EnumEntry remoteEntry = remoteEnumEntries.get(className);
         if (localEntry == null && remoteEntry.isExtended() || remoteEntry == null && localEntry.isExtended()) {
            mismatched.put(className, CheckExtensibleEnums.Mismatch.EXTENSIBILITY);
         } else if (localEntry != null && localEntry.isExtended() || remoteEntry != null && remoteEntry.isExtended()) {
            if (localEntry.networkCheck != remoteEntry.networkCheck) {
               mismatched.put(className, CheckExtensibleEnums.Mismatch.NETWORK_CHECK);
            } else if (localEntry.isExtended() != remoteEntry.isExtended()) {
               mismatched.put(className, CheckExtensibleEnums.Mismatch.EXTENSION);
            } else {
               CheckExtensibleEnums.ExtensionData localData = localEntry.data.orElseThrow();
               CheckExtensibleEnums.ExtensionData remoteData = remoteEntry.data.orElseThrow();
               if (localData.vanillaCount == remoteData.vanillaCount && localData.totalCount == remoteData.totalCount) {
                  List<String> localValues = localData.entries;
                  List<String> remoteValues = remoteData.entries;

                  for (int i = 0; i < localData.totalCount - localData.vanillaCount; i++) {
                     if (!localValues.get(i).equals(remoteValues.get(i))) {
                        mismatched.put(className, CheckExtensibleEnums.Mismatch.ENTRY_MISMATCH);
                        break;
                     }
                  }
               } else {
                  mismatched.put(className, CheckExtensibleEnums.Mismatch.ENTRY_COUNT);
               }
            }
         }
      }

      if (!mismatched.isEmpty()) {
         context.disconnect(Component.translatable("neoforge.network.extensible_enums.enum_entry_mismatch"));
         StringBuilder messagex = new StringBuilder("The configuration or set of values added to extensible enums on the client and server do not match");

         for (Entry<String, CheckExtensibleEnums.Mismatch> entry : mismatched.entrySet()) {
            String enumClass = entry.getKey();
            messagex.append("\n").append(enumClass).append(": ");
            switch ((CheckExtensibleEnums.Mismatch)entry.getValue()) {
               case EXTENSIBILITY:
                  if (remoteEnumEntries.containsKey(enumClass)) {
                     messagex.append("Enum is extensible on the server but not on the client");
                  } else {
                     messagex.append("Enum is extensible on the client but not on the server");
                  }
                  break;
               case NETWORK_CHECK:
                  messagex.append("Mismatched NetworkCheck (server: ")
                     .append(remoteEnumEntries.get(enumClass).networkCheck)
                     .append(", client: ")
                     .append(localEnumEntries.get(enumClass).networkCheck)
                     .append(")");
                  break;
               case EXTENSION:
                  if (remoteEnumEntries.get(enumClass).isExtended()) {
                     messagex.append("Enum has additional entries on the server but not on the client");
                  } else {
                     messagex.append("Enum has additional entries on the client but not on the server");
                  }
                  break;
               case ENTRY_COUNT:
               case ENTRY_MISMATCH:
                  messagex.append("Set of entries does not match (server: ")
                     .append(remoteEnumEntries.get(enumClass).data.orElseThrow().entries)
                     .append(", client: ")
                     .append(localEnumEntries.get(enumClass).data.orElseThrow().entries)
                     .append(")");
            }
         }

         LOGGER.warn(messagex.toString());
      } else {
         context.reply(ExtensibleEnumAcknowledgePayload.INSTANCE);
      }
   }

   public static void handleServerboundPayload(ExtensibleEnumAcknowledgePayload payload, IPayloadContext context) {
      context.finishCurrentTask(TYPE);
   }

   public static boolean handleVanillaServerConnection(ClientConfigurationPacketListener listener) {
      Collection<CheckExtensibleEnums.EnumEntry> enumEntries = getEnumEntries().values();
      if (enumEntries.stream().anyMatch(entry -> entry.isServerbound() && entry.isExtended())) {
         listener.disconnect(Component.translatable("neoforge.network.extensible_enums.no_vanilla_server"));
         return false;
      } else {
         return true;
      }
   }

   private static synchronized Map<String, CheckExtensibleEnums.EnumEntry> getEnumEntries() {
      if (enumEntries == null) {
         Map<String, CheckExtensibleEnums.EnumEntry> entries = new HashMap<>();

         for (Class<? extends Enum<?>> enumClass : NETWORKED_EXTENSIBLE_ENUM_CLASSES) {
            Optional<CheckExtensibleEnums.ExtensionData> extData = Optional.empty();
            ExtensionInfo extInfo = getEnumExtensionInfo(enumClass);
            if (extInfo.extended()) {
               List<String> values = new ArrayList<>(extInfo.totalCount() - extInfo.vanillaCount());
               Enum<?>[] constants = (Enum<?>[])enumClass.getEnumConstants();

               for (int i = extInfo.vanillaCount(); i < extInfo.totalCount(); i++) {
                  values.add(constants[i].name());
               }

               extData = Optional.of(new CheckExtensibleEnums.ExtensionData(extInfo.vanillaCount(), extInfo.totalCount(), values));
            }

            String name = enumClass.getName();
            entries.put(
               name,
               new CheckExtensibleEnums.EnumEntry(
                  name, (NetworkCheck)Preconditions.checkNotNull(extInfo.netCheck(), "Enum %s does not have a NetworkCheck value", name), extData
               )
            );
         }

         enumEntries = entries;
      }

      return enumEntries;
   }

   private static ExtensionInfo getEnumExtensionInfo(Class<? extends Enum<?>> enumClass) {
      try {
         Method mth = enumClass.getDeclaredMethod("getExtensionInfo");
         return (ExtensionInfo)mth.invoke(null);
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException var2) {
         throw new RuntimeException(var2);
      }
   }

   private static List<? extends Class<? extends Enum<?>>> collectNetworkedEnumClasses() {
      return ModList.get()
         .getAllScanData()
         .stream()
         .map(ModFileScanData::getAnnotations)
         .flatMap(Collection::stream)
         .filter(a -> NETWORKED_ENUM.equals(a.annotationType()))
         .map(a -> classForName(a.clazz().getClassName()))
         .filter(Class::isEnum)
         .filter(IExtensibleEnum.class::isAssignableFrom)
         .map(c -> (Class<? extends Enum<?>>)c)
         .toList();
   }

   private static Class<?> classForName(String name) {
      try {
         return Class.forName(name);
      } catch (ClassNotFoundException var2) {
         throw new RuntimeException("Failed to load class specified by annotation data", var2);
      }
   }

   @Override
   public ConfigurationTask.Type type() {
      return TYPE;
   }

   public record EnumEntry(String className, NetworkCheck networkCheck, Optional<CheckExtensibleEnums.ExtensionData> data) {
      public static final StreamCodec<ByteBuf, CheckExtensibleEnums.EnumEntry> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.STRING_UTF8,
         CheckExtensibleEnums.EnumEntry::className,
         ByteBufCodecs.STRING_UTF8.map(NetworkCheck::valueOf, Enum::name),
         CheckExtensibleEnums.EnumEntry::networkCheck,
         ByteBufCodecs.optional(CheckExtensibleEnums.ExtensionData.STREAM_CODEC),
         CheckExtensibleEnums.EnumEntry::data,
         CheckExtensibleEnums.EnumEntry::new
      );

      public boolean isClientbound() {
         return this.networkCheck == NetworkCheck.CLIENTBOUND || this.networkCheck == NetworkCheck.BIDIRECTIONAL;
      }

      public boolean isServerbound() {
         return this.networkCheck == NetworkCheck.SERVERBOUND || this.networkCheck == NetworkCheck.BIDIRECTIONAL;
      }

      public boolean isExtended() {
         return this.data.isPresent();
      }
   }

   public record ExtensionData(int vanillaCount, int totalCount, List<String> entries) {
      public static final StreamCodec<ByteBuf, CheckExtensibleEnums.ExtensionData> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.VAR_INT,
         CheckExtensibleEnums.ExtensionData::vanillaCount,
         ByteBufCodecs.VAR_INT,
         CheckExtensibleEnums.ExtensionData::totalCount,
         ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
         CheckExtensibleEnums.ExtensionData::entries,
         CheckExtensibleEnums.ExtensionData::new
      );
   }

   private static enum Mismatch {
      EXTENSIBILITY,
      NETWORK_CHECK,
      EXTENSION,
      ENTRY_COUNT,
      ENTRY_MISMATCH;
   }
}

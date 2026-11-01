package net.neoforged.neoforge.registries;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMaps;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.Nullable;

public class RegistrySnapshot {
   private static final Comparator<Identifier> SORTER = Identifier::compareNamespaced;
   public static final StreamCodec<FriendlyByteBuf, RegistrySnapshot> STREAM_CODEC = new StreamCodec<FriendlyByteBuf, RegistrySnapshot>() {
      public RegistrySnapshot decode(FriendlyByteBuf buf) {
         RegistrySnapshot snapshot = new RegistrySnapshot();
         buf.readMap(size -> snapshot.ids, FriendlyByteBuf::readVarInt, FriendlyByteBuf::readIdentifier);
         buf.readMap(size -> snapshot.aliases, FriendlyByteBuf::readIdentifier, FriendlyByteBuf::readIdentifier);
         return snapshot;
      }

      public synchronized void encode(FriendlyByteBuf buf, RegistrySnapshot snapshot) {
         if (snapshot.binary == null) {
            FriendlyByteBuf pkt = new FriendlyByteBuf(Unpooled.buffer());

            try {
               pkt.writeMap(snapshot.ids, FriendlyByteBuf::writeVarInt, FriendlyByteBuf::writeIdentifier);
               pkt.writeMap(snapshot.aliases, FriendlyByteBuf::writeIdentifier, FriendlyByteBuf::writeIdentifier);
               snapshot.binary = new byte[pkt.readableBytes()];
               pkt.readBytes(snapshot.binary);
            } finally {
               pkt.release();
            }
         }

         buf.writeBytes(snapshot.binary);
      }
   };
   private final Int2ObjectSortedMap<Identifier> ids = new Int2ObjectRBTreeMap();
   private final Int2ObjectSortedMap<Identifier> idsView = Int2ObjectSortedMaps.unmodifiable(this.ids);
   private final Map<Identifier, Identifier> aliases = new TreeMap<>(SORTER);
   private final Map<Identifier, Identifier> aliasesView = Collections.unmodifiableMap(this.aliases);
   @Nullable
   private final Registry<?> fullBackup;
   private byte[] binary = null;

   private RegistrySnapshot() {
      this.fullBackup = null;
   }

   public <T> RegistrySnapshot(Registry<T> registry, boolean full) {
      registry.keySet().forEach(keyx -> this.ids.put(registry.getId(keyx), keyx));
      this.aliases.putAll(((BaseMappedRegistry)registry).aliases);
      if (full) {
         MappedRegistry<T> backup = new MappedRegistry<>(registry.key(), registry.registryLifecycle());

         for (Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
            ResourceKey<T> key = entry.getKey();
            T value = entry.getValue();
            backup.register(registry.getId(key), key, value, registry.registrationInfo(key).orElse(RegistrationInfo.BUILT_IN));
         }

         backup.freeze();
         this.fullBackup = backup;
      } else {
         this.fullBackup = null;
      }
   }

   public Int2ObjectSortedMap<Identifier> getIds() {
      return this.idsView;
   }

   public Map<Identifier, Identifier> getAliases() {
      return this.aliasesView;
   }

   @Nullable
   public <T> Registry<T> getFullBackup() {
      return (Registry<T>)this.fullBackup;
   }
}

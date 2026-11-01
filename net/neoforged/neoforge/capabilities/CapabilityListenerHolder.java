package net.neoforged.neoforge.capabilities;

import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class CapabilityListenerHolder {
   private final Long2ReferenceMap<Long2ReferenceMap<Set<CapabilityListenerHolder.ListenerReference>>> byChunkThenBlock = new Long2ReferenceOpenHashMap();
   private final ReferenceQueue<ICapabilityInvalidationListener> queue = new ReferenceQueue<>();

   public void addListener(BlockPos pos, ICapabilityInvalidationListener listener) {
      pos = pos.immutable();
      Long2ReferenceMap<Set<CapabilityListenerHolder.ListenerReference>> chunkHolder = (Long2ReferenceMap<Set<CapabilityListenerHolder.ListenerReference>>)this.byChunkThenBlock
         .computeIfAbsent(ChunkPos.asLong(pos), l -> new Long2ReferenceOpenHashMap());
      Set<CapabilityListenerHolder.ListenerReference> listenersSet = (Set<CapabilityListenerHolder.ListenerReference>)chunkHolder.computeIfAbsent(
         pos.asLong(), l -> new ObjectOpenHashSet()
      );
      CapabilityListenerHolder.ListenerReference reference = new CapabilityListenerHolder.ListenerReference(this.queue, pos, listener);
      if (!listenersSet.add(reference)) {
         reference.clear();
      }
   }

   public void invalidatePos(BlockPos pos) {
      Long2ReferenceMap<Set<CapabilityListenerHolder.ListenerReference>> chunkHolder = (Long2ReferenceMap<Set<CapabilityListenerHolder.ListenerReference>>)this.byChunkThenBlock
         .get(ChunkPos.asLong(pos));
      if (chunkHolder != null) {
         Set<CapabilityListenerHolder.ListenerReference> caches = (Set<CapabilityListenerHolder.ListenerReference>)chunkHolder.get(pos.asLong());
         if (caches != null) {
            this.invalidateList(caches);
         }
      }
   }

   public void invalidateChunk(ChunkPos chunkPos) {
      Long2ReferenceMap<Set<CapabilityListenerHolder.ListenerReference>> chunkHolder = (Long2ReferenceMap<Set<CapabilityListenerHolder.ListenerReference>>)this.byChunkThenBlock
         .get(chunkPos.toLong());
      if (chunkHolder != null) {
         ObjectIterator var3 = chunkHolder.values().iterator();

         while (var3.hasNext()) {
            Set<CapabilityListenerHolder.ListenerReference> caches = (Set<CapabilityListenerHolder.ListenerReference>)var3.next();
            this.invalidateList(caches);
         }
      }
   }

   private void invalidateList(Set<CapabilityListenerHolder.ListenerReference> caches) {
      caches.removeIf(ref -> {
         ICapabilityInvalidationListener listener = ref.get();
         return listener == null || !listener.onInvalidate();
      });
   }

   public void clean() {
      while (true) {
         CapabilityListenerHolder.ListenerReference ref = (CapabilityListenerHolder.ListenerReference)this.queue.poll();
         if (ref == null) {
            return;
         }

         Long2ReferenceMap<Set<CapabilityListenerHolder.ListenerReference>> chunkHolder = (Long2ReferenceMap<Set<CapabilityListenerHolder.ListenerReference>>)this.byChunkThenBlock
            .get(ChunkPos.asLong(ref.pos));
         if (chunkHolder != null) {
            Set<CapabilityListenerHolder.ListenerReference> set = (Set<CapabilityListenerHolder.ListenerReference>)chunkHolder.get(ref.pos.asLong());
            if (set != null) {
               boolean removed = set.remove(ref);
               if (removed && set.isEmpty()) {
                  chunkHolder.remove(ref.pos.asLong());
                  if (chunkHolder.isEmpty()) {
                     this.byChunkThenBlock.remove(ChunkPos.asLong(ref.pos));
                  }
               }
            }
         }
      }
   }

   private static class ListenerReference extends WeakReference<ICapabilityInvalidationListener> {
      private final BlockPos pos;
      private final int listenerHashCode;

      private ListenerReference(ReferenceQueue<ICapabilityInvalidationListener> queue, BlockPos pos, ICapabilityInvalidationListener listener) {
         super(listener, queue);
         this.pos = pos;
         this.listenerHashCode = System.identityHashCode(listener);
      }

      @Override
      public boolean equals(Object obj) {
         return !(obj instanceof CapabilityListenerHolder.ListenerReference otherRef)
            ? false
            : otherRef.listenerHashCode == this.listenerHashCode && otherRef.get() == this.get();
      }

      @Override
      public int hashCode() {
         return this.listenerHashCode;
      }
   }
}

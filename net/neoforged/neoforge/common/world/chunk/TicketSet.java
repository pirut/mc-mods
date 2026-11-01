package net.neoforged.neoforge.common.world.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

public record TicketSet(LongSet normal, LongSet naturalSpawning) {
   private static final Codec<LongSet> LONG_SET_CODEC = Codec.LONG_STREAM.xmap(LongOpenHashSet::toSet, LongCollection::longStream);
   public static final Codec<TicketSet> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            LONG_SET_CODEC.optionalFieldOf("normal", LongSets.emptySet()).forGetter(TicketSet::normal),
            LONG_SET_CODEC.optionalFieldOf("natural_spawning", LongSets.emptySet()).forGetter(TicketSet::naturalSpawning)
         )
         .apply(instance, TicketSet::new)
   );

   public boolean isEmpty() {
      return this.normal.isEmpty() && this.naturalSpawning.isEmpty();
   }

   public LongSet getChunks(boolean forceNaturalSpawning) {
      return forceNaturalSpawning ? this.naturalSpawning() : this.normal();
   }
}

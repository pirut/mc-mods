package net.minecraft.util.datafix;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.fixes.References;

public enum DataFixTypes {
   LEVEL(References.LEVEL),
   LEVEL_SUMMARY(References.LIGHTWEIGHT_LEVEL),
   PLAYER(References.PLAYER),
   CHUNK(References.CHUNK),
   HOTBAR(References.HOTBAR),
   OPTIONS(References.OPTIONS),
   STRUCTURE(References.STRUCTURE),
   STATS(References.STATS),
   SAVED_DATA_COMMAND_STORAGE(References.SAVED_DATA_COMMAND_STORAGE),
   SAVED_DATA_FORCED_CHUNKS(References.SAVED_DATA_TICKETS),
   SAVED_DATA_MAP_DATA(References.SAVED_DATA_MAP_DATA),
   SAVED_DATA_MAP_INDEX(References.SAVED_DATA_MAP_INDEX),
   SAVED_DATA_RAIDS(References.SAVED_DATA_RAIDS),
   SAVED_DATA_RANDOM_SEQUENCES(References.SAVED_DATA_RANDOM_SEQUENCES),
   SAVED_DATA_SCOREBOARD(References.SAVED_DATA_SCOREBOARD),
   SAVED_DATA_STOPWATCHES(References.SAVED_DATA_STOPWATCHES),
   SAVED_DATA_STRUCTURE_FEATURE_INDICES(References.SAVED_DATA_STRUCTURE_FEATURE_INDICES),
   SAVED_DATA_WORLD_BORDER(References.SAVED_DATA_WORLD_BORDER),
   ADVANCEMENTS(References.ADVANCEMENTS),
   POI_CHUNK(References.POI_CHUNK),
   WORLD_GEN_SETTINGS(References.WORLD_GEN_SETTINGS),
   ENTITY_CHUNK(References.ENTITY_CHUNK),
   DEBUG_PROFILE(References.DEBUG_PROFILE);

   public static final Set<TypeReference> TYPES_FOR_LEVEL_LIST;
   private final TypeReference type;

   private DataFixTypes(final TypeReference type) {
      this.type = type;
   }

   static int currentVersion() {
      return SharedConstants.getCurrentVersion().dataVersion().version();
   }

   public <A> Codec<A> wrapCodec(Codec<A> codec, DataFixer dataFixer, int dataVersion) {
      return new Codec<A>() {
         public <T> DataResult<T> encode(A object, DynamicOps<T> dynamicOps, T object2) {
            return codec.encode(object, dynamicOps, object2)
               .flatMap(objectx -> dynamicOps.mergeToMap(objectx, dynamicOps.createString("DataVersion"), dynamicOps.createInt(DataFixTypes.currentVersion())));
         }

         public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> dynamicOps, T object) {
            int i = dynamicOps.get(object, "DataVersion").flatMap(dynamicOps::getNumberValue).map(Number::intValue).result().orElse(dataVersion);
            Dynamic<T> dynamic = new Dynamic(dynamicOps, dynamicOps.remove(object, "DataVersion"));
            Dynamic<T> dynamic2 = DataFixTypes.this.updateToCurrentVersion(dataFixer, dynamic, i);
            return codec.decode(dynamic2);
         }
      };
   }

   public <T> Dynamic<T> update(DataFixer fixer, Dynamic<T> input, int version, int newVersion) {
      return fixer.update(this.type, input, version, newVersion);
   }

   public <T> Dynamic<T> updateToCurrentVersion(DataFixer fixer, Dynamic<T> input, int version) {
      return this.update(fixer, input, version, currentVersion());
   }

   public CompoundTag update(DataFixer fixer, CompoundTag tag, int version, int newVersion) {
      return (CompoundTag)this.update(fixer, new Dynamic(NbtOps.INSTANCE, tag), version, newVersion).getValue();
   }

   public CompoundTag updateToCurrentVersion(DataFixer fixer, CompoundTag tag, int version) {
      return this.update(fixer, tag, version, currentVersion());
   }

   static {
      TYPES_FOR_LEVEL_LIST = Set.of(LEVEL_SUMMARY.type);
   }
}

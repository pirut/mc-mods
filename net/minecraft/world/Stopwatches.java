package net.minecraft.world;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

public class Stopwatches extends SavedData {
   private static final Codec<Stopwatches> CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.LONG)
      .fieldOf("stopwatches")
      .codec()
      .xmap(Stopwatches::unpack, Stopwatches::pack);
   public static final SavedDataType<Stopwatches> TYPE = new SavedDataType<>("stopwatches", Stopwatches::new, CODEC, DataFixTypes.SAVED_DATA_STOPWATCHES);
   private final Map<Identifier, Stopwatch> stopwatches = new Object2ObjectOpenHashMap();

   private Stopwatches() {
   }

   private static Stopwatches unpack(Map<Identifier, Long> p_456035_) {
      Stopwatches stopwatches = new Stopwatches();
      long i = currentTime();
      p_456035_.forEach((p_469675_, p_455604_) -> stopwatches.stopwatches.put(p_469675_, new Stopwatch(i, p_455604_)));
      return stopwatches;
   }

   private Map<Identifier, Long> pack() {
      long i = currentTime();
      Map<Identifier, Long> map = new TreeMap<>();
      this.stopwatches.forEach((p_469798_, p_457398_) -> map.put(p_469798_, p_457398_.elapsedMilliseconds(i)));
      return map;
   }

   @Nullable
   public Stopwatch get(Identifier p_467940_) {
      return this.stopwatches.get(p_467940_);
   }

   public boolean add(Identifier p_469348_, Stopwatch p_455305_) {
      if (this.stopwatches.putIfAbsent(p_469348_, p_455305_) == null) {
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public boolean update(Identifier p_468001_, UnaryOperator<Stopwatch> p_454842_) {
      if (this.stopwatches.computeIfPresent(p_468001_, (p_468534_, p_455351_) -> p_454842_.apply(p_455351_)) != null) {
         this.setDirty();
         return true;
      } else {
         return false;
      }
   }

   public boolean remove(Identifier p_469070_) {
      boolean flag = this.stopwatches.remove(p_469070_) != null;
      if (flag) {
         this.setDirty();
      }

      return flag;
   }

   @Override
   public boolean isDirty() {
      return super.isDirty() || !this.stopwatches.isEmpty();
   }

   public List<Identifier> ids() {
      return List.copyOf(this.stopwatches.keySet());
   }

   public static long currentTime() {
      return Util.getMillis();
   }
}

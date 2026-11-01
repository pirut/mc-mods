package net.minecraft.world.level.saveddata.maps;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class MapIndex extends SavedData {
   public static final String FILE_NAME = "idcounts";
   private final Object2IntMap<String> usedAuxIds = new Object2IntOpenHashMap();

   public static SavedData.Factory<MapIndex> factory() {
      return new SavedData.Factory<>(MapIndex::new, MapIndex::load, DataFixTypes.SAVED_DATA_MAP_INDEX);
   }

   public MapIndex() {
      this.usedAuxIds.defaultReturnValue(-1);
   }

   public static MapIndex load(CompoundTag p_164763_, HolderLookup.Provider p_323806_) {
      MapIndex $$2 = new MapIndex();

      for (String $$3 : p_164763_.getAllKeys()) {
         if (p_164763_.contains($$3, 99)) {
            $$2.usedAuxIds.put($$3, p_164763_.getInt($$3));
         }
      }

      return $$2;
   }

   @Override
   public CompoundTag save(CompoundTag p_77884_, HolderLookup.Provider p_324049_) {
      ObjectIterator var3 = this.usedAuxIds.object2IntEntrySet().iterator();

      while (var3.hasNext()) {
         Entry<String> $$2 = (Entry<String>)var3.next();
         p_77884_.putInt((String)$$2.getKey(), $$2.getIntValue());
      }

      return p_77884_;
   }

   public MapId getFreeAuxValueForMap() {
      int $$0 = this.usedAuxIds.getInt("map") + 1;
      this.usedAuxIds.put("map", $$0);
      this.setDirty();
      return new MapId($$0);
   }
}

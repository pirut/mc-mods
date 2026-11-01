package net.neoforged.neoforge.common.util;

import java.io.File;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

@Deprecated(
   since = "1.21.1",
   forRemoval = true
)
public class DummySavedData extends SavedData {
   public static final DummySavedData DUMMY = new DummySavedData();

   private DummySavedData() {
   }

   @Override
   public CompoundTag save(CompoundTag compound, HolderLookup.Provider provider) {
      return null;
   }

   @Override
   public void save(File file, HolderLookup.Provider provider) {
   }
}

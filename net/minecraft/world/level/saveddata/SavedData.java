package net.minecraft.world.level.saveddata;

import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.neoforged.neoforge.common.IOUtilities;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public abstract class SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private boolean dirty;

   public abstract CompoundTag save(CompoundTag var1, HolderLookup.Provider var2);

   public void setDirty() {
      this.setDirty(true);
   }

   public void setDirty(boolean p_77761_) {
      this.dirty = p_77761_;
   }

   public boolean isDirty() {
      return this.dirty;
   }

   public void save(File p_77758_, HolderLookup.Provider p_324088_) {
      if (this.isDirty()) {
         CompoundTag compoundtag = new CompoundTag();
         compoundtag.put("data", this.save(new CompoundTag(), p_324088_));
         NbtUtils.addCurrentDataVersion(compoundtag);
         CompoundTag copied = compoundtag.copy();
         IOUtilities.withIOWorker(() -> {
            try {
               IOUtilities.writeNbtCompressed(copied, p_77758_.toPath());
            } catch (IOException var4x) {
               LOGGER.error("Could not save data {}", this, var4x);
            }
         });
         this.setDirty(false);
      }
   }

   public record Factory<T extends SavedData>(
      Supplier<T> constructor, BiFunction<CompoundTag, HolderLookup.Provider, T> deserializer, @Nullable DataFixTypes type
   ) {
      public Factory(Supplier<T> constructor, BiFunction<CompoundTag, HolderLookup.Provider, T> deserializer) {
         this(constructor, deserializer, null);
      }
   }
}

package net.neoforged.neoforge.attachment;

import java.util.Objects;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class LevelAttachmentsSavedData extends SavedData {
   private static final String NAME = "neoforge_data_attachments";
   private final ServerLevel level;

   public static void init(ServerLevel level) {
      SavedData.Factory<LevelAttachmentsSavedData> factory = new SavedData.Factory<>(
         () -> new LevelAttachmentsSavedData(level), (tag, prov) -> new LevelAttachmentsSavedData(level, tag)
      );
      level.getDataStorage().computeIfAbsent(factory, "neoforge_data_attachments");
   }

   public LevelAttachmentsSavedData(ServerLevel level) {
      this.level = level;
   }

   public LevelAttachmentsSavedData(ServerLevel level, CompoundTag tag) {
      this.level = level;
      level.deserializeAttachments(level.registryAccess(), tag);
   }

   @Override
   public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
      return Objects.requireNonNullElseGet(this.level.serializeAttachments(provider), CompoundTag::new);
   }

   @Override
   public boolean isDirty() {
      return true;
   }
}

package net.neoforged.neoforge.attachment;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

@Internal
public class LevelAttachmentsSavedData extends SavedData {
   public static final SavedDataType<LevelAttachmentsSavedData> TYPE = new SavedDataType<>(
      "neoforge_data_attachments", LevelAttachmentsSavedData::new, LevelAttachmentsSavedData::makeCodec
   );
   private final ServerLevel level;

   public static void init(ServerLevel level) {
      level.getDataStorage().computeIfAbsent(TYPE);
   }

   private static Codec<LevelAttachmentsSavedData> makeCodec(@Nullable ServerLevel level) {
      return CompoundTag.CODEC
         .flatXmap(
            tag -> {
               LevelAttachmentsSavedData data = new LevelAttachmentsSavedData(level);
               ProblemReporter.Collector reporter = new ProblemReporter.Collector();
               data.level.deserializeAttachments(TagValueInput.create(reporter, data.level.registryAccess(), tag));
               return !reporter.isEmpty()
                  ? DataResult.error(() -> "Deserialisation error in level attachments: " + reporter.getReport())
                  : DataResult.success(data);
            },
            data -> {
               ProblemReporter.Collector reporter = new ProblemReporter.Collector();
               TagValueOutput tag = TagValueOutput.createWithContext(reporter, data.level.registryAccess());
               data.level.serializeAttachments(tag);
               return !reporter.isEmpty()
                  ? DataResult.error(() -> "Serialisation error in level attachments: " + reporter.getReport())
                  : DataResult.success(tag.buildResult());
            }
         );
   }

   public LevelAttachmentsSavedData(@Nullable ServerLevel level) {
      this.level = Objects.requireNonNull(level, "level");
   }

   @Override
   public boolean isDirty() {
      return true;
   }
}

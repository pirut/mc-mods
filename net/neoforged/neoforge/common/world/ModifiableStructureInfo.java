package net.neoforged.neoforge.common.world;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class ModifiableStructureInfo {
   private final ModifiableStructureInfo.StructureInfo originalStructureInfo;
   @Nullable
   private ModifiableStructureInfo.StructureInfo modifiedStructureInfo = null;

   public ModifiableStructureInfo(ModifiableStructureInfo.StructureInfo originalStructureInfo) {
      this.originalStructureInfo = originalStructureInfo;
   }

   public ModifiableStructureInfo.StructureInfo get() {
      return this.modifiedStructureInfo == null ? this.originalStructureInfo : this.modifiedStructureInfo;
   }

   public ModifiableStructureInfo.StructureInfo getOriginalStructureInfo() {
      return this.originalStructureInfo;
   }

   @Nullable
   public ModifiableStructureInfo.StructureInfo getModifiedStructureInfo() {
      return this.modifiedStructureInfo;
   }

   @Internal
   public void applyStructureModifiers(Holder<Structure> structure, List<StructureModifier> structureModifiers) {
      if (this.modifiedStructureInfo == null) {
         ModifiableStructureInfo.StructureInfo original = this.getOriginalStructureInfo();
         ModifiableStructureInfo.StructureInfo.Builder builder = ModifiableStructureInfo.StructureInfo.Builder.copyOf(original);

         for (StructureModifier.Phase phase : StructureModifier.Phase.values()) {
            for (StructureModifier modifier : structureModifiers) {
               modifier.modify(structure, phase, builder);
            }
         }

         this.modifiedStructureInfo = builder.build();
      }
   }

   public record StructureInfo(Structure.StructureSettings structureSettings) {
      public static class Builder {
         private StructureSettingsBuilder structureSettings;

         public static ModifiableStructureInfo.StructureInfo.Builder copyOf(ModifiableStructureInfo.StructureInfo original) {
            StructureSettingsBuilder structureBuilder = StructureSettingsBuilder.copyOf(original.structureSettings());
            return new ModifiableStructureInfo.StructureInfo.Builder(structureBuilder);
         }

         private Builder(StructureSettingsBuilder structureSettings) {
            this.structureSettings = structureSettings;
         }

         public ModifiableStructureInfo.StructureInfo build() {
            return new ModifiableStructureInfo.StructureInfo(this.structureSettings.build());
         }

         public StructureSettingsBuilder getStructureSettings() {
            return this.structureSettings;
         }
      }
   }
}

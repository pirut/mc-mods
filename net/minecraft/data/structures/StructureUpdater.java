package net.minecraft.data.structures;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

public class StructureUpdater implements SnbtToNbt.Filter {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final String PREFIX = PackType.SERVER_DATA.getDirectory() + "/minecraft/structure/";

   @Override
   public CompoundTag apply(String p_126503_, CompoundTag p_126504_) {
      return p_126503_.startsWith(PREFIX) ? update(p_126503_, p_126504_) : p_126504_;
   }

   public static CompoundTag update(String p_176823_, CompoundTag p_176824_) {
      StructureTemplate $$2 = new StructureTemplate();
      int $$3 = NbtUtils.getDataVersion(p_176824_, 500);
      int $$4 = 3937;
      if ($$3 < 3937) {
         LOGGER.warn("SNBT Too old, do not forget to update: {} < {}: {}", new Object[]{$$3, 3937, p_176823_});
      }

      CompoundTag $$5 = DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), p_176824_, $$3);
      $$2.load(BuiltInRegistries.BLOCK.asLookup(), $$5);
      return $$2.save(new CompoundTag());
   }
}

package net.minecraft.client;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import java.nio.file.Path;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class HotbarManager {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final int NUM_HOTBAR_GROUPS = 9;
   private final Path optionsFile;
   private final DataFixer fixerUpper;
   private final Hotbar[] hotbars = new Hotbar[9];
   private boolean loaded;

   public HotbarManager(Path p_309673_, DataFixer p_90804_) {
      this.optionsFile = p_309673_.resolve("hotbar.nbt");
      this.fixerUpper = p_90804_;

      for (int $$2 = 0; $$2 < 9; $$2++) {
         this.hotbars[$$2] = new Hotbar();
      }
   }

   private void load() {
      try {
         CompoundTag $$0 = NbtIo.read(this.optionsFile);
         if ($$0 == null) {
            return;
         }

         int $$1 = NbtUtils.getDataVersion($$0, 1343);
         $$0 = DataFixTypes.HOTBAR.updateToCurrentVersion(this.fixerUpper, $$0, $$1);

         for (int $$2 = 0; $$2 < 9; $$2++) {
            this.hotbars[$$2] = Hotbar.CODEC
               .parse(NbtOps.INSTANCE, $$0.get(String.valueOf($$2)))
               .resultOrPartial(p_323544_ -> LOGGER.warn("Failed to parse hotbar: {}", p_323544_))
               .orElseGet(Hotbar::new);
         }
      } catch (Exception var4) {
         LOGGER.error("Failed to load creative mode options", var4);
      }
   }

   public void save() {
      try {
         CompoundTag $$0 = NbtUtils.addCurrentDataVersion(new CompoundTag());

         for (int $$1 = 0; $$1 < 9; $$1++) {
            Hotbar $$2 = this.get($$1);
            DataResult<Tag> $$3 = Hotbar.CODEC.encodeStart(NbtOps.INSTANCE, $$2);
            $$0.put(String.valueOf($$1), (Tag)$$3.getOrThrow());
         }

         NbtIo.write($$0, this.optionsFile);
      } catch (Exception var5) {
         LOGGER.error("Failed to save creative mode options", var5);
      }
   }

   public Hotbar get(int p_90807_) {
      if (!this.loaded) {
         this.load();
         this.loaded = true;
      }

      return this.hotbars[p_90807_];
   }
}

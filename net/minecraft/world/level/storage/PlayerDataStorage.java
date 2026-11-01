package net.minecraft.world.level.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.util.Optional;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.EventHooks;
import org.slf4j.Logger;

public class PlayerDataStorage {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final File playerDir;
   protected final DataFixer fixerUpper;

   public PlayerDataStorage(LevelStorageSource.LevelStorageAccess p_78430_, DataFixer p_78431_) {
      this.fixerUpper = p_78431_;
      this.playerDir = p_78430_.getLevelPath(LevelResource.PLAYER_DATA_DIR).toFile();
      this.playerDir.mkdirs();
   }

   public void save(Player p_78434_) {
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(p_78434_.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_78434_.registryAccess());
         p_78434_.saveWithoutId(tagvalueoutput);
         Path path = this.playerDir.toPath();
         Path path1 = Files.createTempFile(path, p_78434_.getStringUUID() + "-", ".dat");
         CompoundTag compoundtag = tagvalueoutput.buildResult();
         NbtIo.writeCompressed(compoundtag, path1);
         Path path2 = path.resolve(p_78434_.getStringUUID() + ".dat");
         Path path3 = path.resolve(p_78434_.getStringUUID() + ".dat_old");
         Util.safeReplaceFile(path2, path1, path3);
         EventHooks.firePlayerSavingEvent(p_78434_, this.playerDir, p_78434_.getStringUUID());
      } catch (Exception var11) {
         LOGGER.warn("Failed to save player data for {}", p_78434_.getPlainTextName());
      }
   }

   private void backup(NameAndId p_435829_, String p_316776_) {
      Path path = this.playerDir.toPath();
      String s = p_435829_.id().toString();
      Path path1 = path.resolve(s + p_316776_);
      Path path2 = path.resolve(s + "_corrupted_" + ZonedDateTime.now().format(FileNameDateFormatter.FORMATTER) + p_316776_);
      if (Files.isRegularFile(path1)) {
         try {
            Files.copy(path1, path2, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
         } catch (Exception var8) {
            LOGGER.warn("Failed to copy the player.dat file for {}", p_435829_.name(), var8);
         }
      }
   }

   private Optional<CompoundTag> load(NameAndId p_433756_, String p_434046_) {
      File file1 = new File(this.playerDir, p_433756_.id() + p_434046_);
      if (file1.exists() && file1.isFile()) {
         try {
            return Optional.of(NbtIo.readCompressed(file1.toPath(), NbtAccounter.unlimitedHeap()));
         } catch (Exception var5) {
            LOGGER.warn("Failed to load player data for {}", p_433756_.name());
         }
      }

      return Optional.empty();
   }

   public Optional<CompoundTag> load(NameAndId p_433996_) {
      Optional<CompoundTag> optional = this.load(p_433996_, ".dat");
      if (optional.isEmpty()) {
         this.backup(p_433996_, ".dat");
      }

      return optional.or(() -> this.load(p_433996_, ".dat_old")).map(p_458897_ -> {
         int i = NbtUtils.getDataVersion(p_458897_);
         return DataFixTypes.PLAYER.updateToCurrentVersion(this.fixerUpper, p_458897_, i);
      });
   }

   public File getPlayerDir() {
      return this.playerDir;
   }
}

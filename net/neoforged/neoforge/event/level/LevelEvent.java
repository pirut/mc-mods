package net.neoforged.neoforge.event.level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.storage.ServerLevelData;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.jetbrains.annotations.Nullable;

public abstract class LevelEvent extends Event {
   private final LevelAccessor level;

   public LevelEvent(LevelAccessor level) {
      this.level = level;
   }

   public LevelAccessor getLevel() {
      return this.level;
   }

   public static class CreateSpawnPosition extends LevelEvent implements ICancellableEvent {
      private final ServerLevelData settings;

      public CreateSpawnPosition(LevelAccessor level, ServerLevelData settings) {
         super(level);
         this.settings = settings;
      }

      public ServerLevelData getSettings() {
         return this.settings;
      }
   }

   public static class Load extends LevelEvent {
      public Load(LevelAccessor level) {
         super(level);
      }
   }

   public static class PotentialSpawns extends LevelEvent implements ICancellableEvent {
      private final MobCategory mobcategory;
      private final BlockPos pos;
      @Nullable
      private List<MobSpawnSettings.SpawnerData> list;
      private List<MobSpawnSettings.SpawnerData> view;

      public PotentialSpawns(LevelAccessor level, MobCategory category, BlockPos pos, WeightedRandomList<MobSpawnSettings.SpawnerData> oldList) {
         super(level);
         this.pos = pos;
         this.mobcategory = category;
         this.list = null;
         this.view = oldList.unwrap();
      }

      public MobCategory getMobCategory() {
         return this.mobcategory;
      }

      public BlockPos getPos() {
         return this.pos;
      }

      public List<MobSpawnSettings.SpawnerData> getSpawnerDataList() {
         return this.view;
      }

      private void makeList() {
         if (this.list == null) {
            this.list = new ArrayList<>(this.view);
            this.view = Collections.unmodifiableList(this.list);
         }
      }

      public void addSpawnerData(MobSpawnSettings.SpawnerData data) {
         this.makeList();
         this.list.add(data);
      }

      public boolean removeSpawnerData(MobSpawnSettings.SpawnerData data) {
         this.makeList();
         return this.list.remove(data);
      }
   }

   public static class Save extends LevelEvent {
      public Save(LevelAccessor level) {
         super(level);
      }
   }

   public static class Unload extends LevelEvent {
      public Unload(LevelAccessor level) {
         super(level);
      }
   }
}

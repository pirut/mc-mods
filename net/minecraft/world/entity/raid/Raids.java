package net.minecraft.world.entity.raid;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public class Raids extends SavedData {
   private static final String RAID_FILE_ID = "raids";
   private final Map<Integer, Raid> raidMap = Maps.newHashMap();
   private final ServerLevel level;
   private int nextAvailableID;
   private int tick;

   public static SavedData.Factory<Raids> factory(ServerLevel p_295840_) {
      return new SavedData.Factory<>(() -> new Raids(p_295840_), (p_294039_, p_324123_) -> load(p_295840_, p_294039_), DataFixTypes.SAVED_DATA_RAIDS);
   }

   public Raids(ServerLevel p_37956_) {
      this.level = p_37956_;
      this.nextAvailableID = 1;
      this.setDirty();
   }

   public Raid get(int p_37959_) {
      return this.raidMap.get(p_37959_);
   }

   public void tick() {
      this.tick++;
      Iterator<Raid> $$0 = this.raidMap.values().iterator();

      while ($$0.hasNext()) {
         Raid $$1 = $$0.next();
         if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
            $$1.stop();
         }

         if ($$1.isStopped()) {
            $$0.remove();
            this.setDirty();
         } else {
            $$1.tick();
         }
      }

      if (this.tick % 200 == 0) {
         this.setDirty();
      }

      DebugPackets.sendRaids(this.level, this.raidMap.values());
   }

   public static boolean canJoinRaid(Raider p_37966_, Raid p_37967_) {
      return p_37966_ != null && p_37967_ != null && p_37967_.getLevel() != null
         ? p_37966_.isAlive()
            && p_37966_.canJoinRaid()
            && p_37966_.getNoActionTime() <= 2400
            && p_37966_.level().dimensionType() == p_37967_.getLevel().dimensionType()
         : false;
   }

   @Nullable
   public Raid createOrExtendRaid(ServerPlayer p_37964_, BlockPos p_338602_) {
      if (p_37964_.isSpectator()) {
         return null;
      } else if (this.level.getGameRules().getBoolean(GameRules.RULE_DISABLE_RAIDS)) {
         return null;
      } else {
         DimensionType $$2 = p_37964_.level().dimensionType();
         if (!$$2.hasRaids()) {
            return null;
         } else {
            List<PoiRecord> $$3 = this.level
               .getPoiManager()
               .getInRange(p_219845_ -> p_219845_.is(PoiTypeTags.VILLAGE), p_338602_, 64, PoiManager.Occupancy.IS_OCCUPIED)
               .toList();
            int $$4 = 0;
            Vec3 $$5 = Vec3.ZERO;

            for (PoiRecord $$6 : $$3) {
               BlockPos $$7 = $$6.getPos();
               $$5 = $$5.add($$7.getX(), $$7.getY(), $$7.getZ());
               $$4++;
            }

            BlockPos $$8;
            if ($$4 > 0) {
               $$5 = $$5.scale(1.0 / $$4);
               $$8 = BlockPos.containing($$5);
            } else {
               $$8 = p_338602_;
            }

            Raid $$10 = this.getOrCreateRaid(p_37964_.serverLevel(), $$8);
            if (!$$10.isStarted() && !this.raidMap.containsKey($$10.getId())) {
               this.raidMap.put($$10.getId(), $$10);
            }

            if (!$$10.isStarted() || $$10.getRaidOmenLevel() < $$10.getMaxRaidOmenLevel()) {
               $$10.absorbRaidOmen(p_37964_);
            }

            this.setDirty();
            return $$10;
         }
      }
   }

   private Raid getOrCreateRaid(ServerLevel p_37961_, BlockPos p_37962_) {
      Raid $$2 = p_37961_.getRaidAt(p_37962_);
      return $$2 != null ? $$2 : new Raid(this.getUniqueId(), p_37961_, p_37962_);
   }

   public static Raids load(ServerLevel p_150236_, CompoundTag p_150237_) {
      Raids $$2 = new Raids(p_150236_);
      $$2.nextAvailableID = p_150237_.getInt("NextAvailableID");
      $$2.tick = p_150237_.getInt("Tick");
      ListTag $$3 = p_150237_.getList("Raids", 10);

      for (int $$4 = 0; $$4 < $$3.size(); $$4++) {
         CompoundTag $$5 = $$3.getCompound($$4);
         Raid $$6 = new Raid(p_150236_, $$5);
         $$2.raidMap.put($$6.getId(), $$6);
      }

      return $$2;
   }

   @Override
   public CompoundTag save(CompoundTag p_37976_, HolderLookup.Provider p_323890_) {
      p_37976_.putInt("NextAvailableID", this.nextAvailableID);
      p_37976_.putInt("Tick", this.tick);
      ListTag $$2 = new ListTag();

      for (Raid $$3 : this.raidMap.values()) {
         CompoundTag $$4 = new CompoundTag();
         $$3.save($$4);
         $$2.add($$4);
      }

      p_37976_.put("Raids", $$2);
      return p_37976_;
   }

   public static String getFileId(Holder<DimensionType> p_211597_) {
      return p_211597_.is(BuiltinDimensionTypes.END) ? "raids_end" : "raids";
   }

   private int getUniqueId() {
      return ++this.nextAvailableID;
   }

   @Nullable
   public Raid getNearbyRaid(BlockPos p_37971_, int p_37972_) {
      Raid $$2 = null;
      double $$3 = p_37972_;

      for (Raid $$4 : this.raidMap.values()) {
         double $$5 = $$4.getCenter().distSqr(p_37971_);
         if ($$4.isActive() && $$5 < $$3) {
            $$2 = $$4;
            $$3 = $$5;
         }
      }

      return $$2;
   }
}

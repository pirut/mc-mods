package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class MapItemSavedData extends SavedData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAP_SIZE = 128;
   private static final int HALF_MAP_SIZE = 64;
   public static final int MAX_SCALE = 4;
   public static final int TRACKED_DECORATION_LIMIT = 256;
   private static final String FRAME_PREFIX = "frame-";
   public final int centerX;
   public final int centerZ;
   public final ResourceKey<Level> dimension;
   private final boolean trackingPosition;
   private final boolean unlimitedTracking;
   public final byte scale;
   public byte[] colors = new byte[16384];
   public final boolean locked;
   private final List<MapItemSavedData.HoldingPlayer> carriedBy = Lists.newArrayList();
   private final Map<Player, MapItemSavedData.HoldingPlayer> carriedByPlayers = Maps.newHashMap();
   private final Map<String, MapBanner> bannerMarkers = Maps.newHashMap();
   final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
   private final Map<String, MapFrame> frameMarkers = Maps.newHashMap();
   private int trackedDecorationCount;

   public static SavedData.Factory<MapItemSavedData> factory() {
      return new SavedData.Factory<>(() -> {
         throw new IllegalStateException("Should never create an empty map saved data");
      }, MapItemSavedData::load, DataFixTypes.SAVED_DATA_MAP_DATA);
   }

   private MapItemSavedData(int p_164768_, int p_164769_, byte p_164770_, boolean p_164771_, boolean p_164772_, boolean p_164773_, ResourceKey<Level> p_164774_) {
      this.scale = p_164770_;
      this.centerX = p_164768_;
      this.centerZ = p_164769_;
      this.dimension = p_164774_;
      this.trackingPosition = p_164771_;
      this.unlimitedTracking = p_164772_;
      this.locked = p_164773_;
      this.setDirty();
   }

   public static MapItemSavedData createFresh(
      double p_164781_, double p_164782_, byte p_164783_, boolean p_164784_, boolean p_164785_, ResourceKey<Level> p_164786_
   ) {
      int $$6 = 128 * (1 << p_164783_);
      int $$7 = Mth.floor((p_164781_ + 64.0) / $$6);
      int $$8 = Mth.floor((p_164782_ + 64.0) / $$6);
      int $$9 = $$7 * $$6 + $$6 / 2 - 64;
      int $$10 = $$8 * $$6 + $$6 / 2 - 64;
      return new MapItemSavedData($$9, $$10, p_164783_, p_164784_, p_164785_, false, p_164786_);
   }

   public static MapItemSavedData createForClient(byte p_164777_, boolean p_164778_, ResourceKey<Level> p_164779_) {
      return new MapItemSavedData(0, 0, p_164777_, false, false, p_164778_, p_164779_);
   }

   public static MapItemSavedData load(CompoundTag p_164808_, HolderLookup.Provider p_324560_) {
      ResourceKey<Level> $$2 = (ResourceKey<Level>)DimensionType.parseLegacy(new Dynamic(NbtOps.INSTANCE, p_164808_.get("dimension")))
         .resultOrPartial(LOGGER::error)
         .orElseThrow(() -> new IllegalArgumentException("Invalid map dimension: " + p_164808_.get("dimension")));
      int $$3 = p_164808_.getInt("xCenter");
      int $$4 = p_164808_.getInt("zCenter");
      byte $$5 = (byte)Mth.clamp(p_164808_.getByte("scale"), 0, 4);
      boolean $$6 = !p_164808_.contains("trackingPosition", 1) || p_164808_.getBoolean("trackingPosition");
      boolean $$7 = p_164808_.getBoolean("unlimitedTracking");
      boolean $$8 = p_164808_.getBoolean("locked");
      MapItemSavedData $$9 = new MapItemSavedData($$3, $$4, $$5, $$6, $$7, $$8, $$2);
      byte[] $$10 = p_164808_.getByteArray("colors");
      if ($$10.length == 16384) {
         $$9.colors = $$10;
      }

      RegistryOps<Tag> $$11 = p_324560_.createSerializationContext(NbtOps.INSTANCE);

      for (MapBanner $$13 : MapBanner.LIST_CODEC
         .parse($$11, p_164808_.get("banners"))
         .resultOrPartial(p_323448_ -> LOGGER.warn("Failed to parse map banner: '{}'", p_323448_))
         .orElse(List.of())) {
         $$9.bannerMarkers.put($$13.getId(), $$13);
         $$9.addDecoration($$13.getDecoration(), null, $$13.getId(), $$13.pos().getX(), $$13.pos().getZ(), 180.0, $$13.name().orElse(null));
      }

      ListTag $$14 = p_164808_.getList("frames", 10);

      for (int $$15 = 0; $$15 < $$14.size(); $$15++) {
         MapFrame $$16 = MapFrame.load($$14.getCompound($$15));
         if ($$16 != null) {
            $$9.frameMarkers.put($$16.getId(), $$16);
            $$9.addDecoration(
               MapDecorationTypes.FRAME, null, getFrameKey($$16.getEntityId()), $$16.getPos().getX(), $$16.getPos().getZ(), $$16.getRotation(), null
            );
         }
      }

      return $$9;
   }

   @Override
   public CompoundTag save(CompoundTag p_77956_, HolderLookup.Provider p_323858_) {
      ResourceLocation.CODEC
         .encodeStart(NbtOps.INSTANCE, this.dimension.location())
         .resultOrPartial(LOGGER::error)
         .ifPresent(p_77954_ -> p_77956_.put("dimension", p_77954_));
      p_77956_.putInt("xCenter", this.centerX);
      p_77956_.putInt("zCenter", this.centerZ);
      p_77956_.putByte("scale", this.scale);
      p_77956_.putByteArray("colors", this.colors);
      p_77956_.putBoolean("trackingPosition", this.trackingPosition);
      p_77956_.putBoolean("unlimitedTracking", this.unlimitedTracking);
      p_77956_.putBoolean("locked", this.locked);
      RegistryOps<Tag> $$2 = p_323858_.createSerializationContext(NbtOps.INSTANCE);
      p_77956_.put("banners", (Tag)MapBanner.LIST_CODEC.encodeStart($$2, List.copyOf(this.bannerMarkers.values())).getOrThrow());
      ListTag $$3 = new ListTag();

      for (MapFrame $$4 : this.frameMarkers.values()) {
         $$3.add($$4.save());
      }

      p_77956_.put("frames", $$3);
      return p_77956_;
   }

   public MapItemSavedData locked() {
      MapItemSavedData $$0 = new MapItemSavedData(this.centerX, this.centerZ, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension);
      $$0.bannerMarkers.putAll(this.bannerMarkers);
      $$0.decorations.putAll(this.decorations);
      $$0.trackedDecorationCount = this.trackedDecorationCount;
      System.arraycopy(this.colors, 0, $$0.colors, 0, this.colors.length);
      $$0.setDirty();
      return $$0;
   }

   public MapItemSavedData scaled() {
      return createFresh(this.centerX, this.centerZ, (byte)Mth.clamp(this.scale + 1, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension);
   }

   private static Predicate<ItemStack> mapMatcher(ItemStack p_316807_) {
      MapId $$1 = p_316807_.get(DataComponents.MAP_ID);
      return p_330169_ -> p_330169_ == p_316807_ ? true : p_330169_.is(p_316807_.getItem()) && Objects.equals($$1, p_330169_.get(DataComponents.MAP_ID));
   }

   public void tickCarriedBy(Player p_77919_, ItemStack p_77920_) {
      if (!this.carriedByPlayers.containsKey(p_77919_)) {
         MapItemSavedData.HoldingPlayer $$2 = new MapItemSavedData.HoldingPlayer(p_77919_);
         this.carriedByPlayers.put(p_77919_, $$2);
         this.carriedBy.add($$2);
      }

      Predicate<ItemStack> $$3 = mapMatcher(p_77920_);
      if (!p_77919_.getInventory().contains($$3)) {
         this.removeDecoration(p_77919_.getName().getString());
      }

      for (int $$4 = 0; $$4 < this.carriedBy.size(); $$4++) {
         MapItemSavedData.HoldingPlayer $$5 = this.carriedBy.get($$4);
         String $$6 = $$5.player.getName().getString();
         if (!$$5.player.isRemoved() && ($$5.player.getInventory().contains($$3) || p_77920_.isFramed())) {
            if (!p_77920_.isFramed() && $$5.player.level().dimension() == this.dimension && this.trackingPosition) {
               this.addDecoration(MapDecorationTypes.PLAYER, $$5.player.level(), $$6, $$5.player.getX(), $$5.player.getZ(), $$5.player.getYRot(), null);
            }
         } else {
            this.carriedByPlayers.remove($$5.player);
            this.carriedBy.remove($$5);
            this.removeDecoration($$6);
         }
      }

      if (p_77920_.isFramed() && this.trackingPosition) {
         ItemFrame $$7 = p_77920_.getFrame();
         BlockPos $$8 = $$7.getPos();
         MapFrame $$9 = this.frameMarkers.get(MapFrame.frameId($$8));
         if ($$9 != null && $$7.getId() != $$9.getEntityId() && this.frameMarkers.containsKey($$9.getId())) {
            this.removeDecoration(getFrameKey($$9.getEntityId()));
         }

         MapFrame $$10 = new MapFrame($$8, $$7.getDirection().get2DDataValue() * 90, $$7.getId());
         this.addDecoration(
            MapDecorationTypes.FRAME, p_77919_.level(), getFrameKey($$7.getId()), $$8.getX(), $$8.getZ(), $$7.getDirection().get2DDataValue() * 90, null
         );
         this.frameMarkers.put($$10.getId(), $$10);
      }

      MapDecorations $$11 = p_77920_.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
      if (!this.decorations.keySet().containsAll($$11.decorations().keySet())) {
         $$11.decorations().forEach((p_352892_, p_352893_) -> {
            if (!this.decorations.containsKey(p_352892_)) {
               this.addDecoration(p_352893_.type(), p_77919_.level(), p_352892_, p_352893_.x(), p_352893_.z(), p_352893_.rotation(), null);
            }
         });
      }
   }

   public void removeDecoration(String p_164800_) {
      MapDecoration $$1 = this.decorations.remove(p_164800_);
      if ($$1 != null && $$1.type().value().trackCount()) {
         this.trackedDecorationCount--;
      }

      this.setDecorationsDirty();
   }

   public static void addTargetDecoration(ItemStack p_77926_, BlockPos p_77927_, String p_77928_, Holder<MapDecorationType> p_335759_) {
      MapDecorations.Entry $$4 = new MapDecorations.Entry(p_335759_, p_77927_.getX(), p_77927_.getZ(), 180.0F);
      p_77926_.update(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY, p_330166_ -> p_330166_.withDecoration(p_77928_, $$4));
      if (p_335759_.value().hasMapColor()) {
         p_77926_.set(DataComponents.MAP_COLOR, new MapItemColor(p_335759_.value().mapColor()));
      }
   }

   public void addDecoration(
      Holder<MapDecorationType> p_335830_,
      @Nullable LevelAccessor p_77939_,
      String p_77940_,
      double p_77941_,
      double p_77942_,
      double p_77943_,
      @Nullable Component p_77944_
   ) {
      int $$7 = 1 << this.scale;
      float $$8 = (float)(p_77941_ - this.centerX) / $$7;
      float $$9 = (float)(p_77942_ - this.centerZ) / $$7;
      byte $$10 = (byte)($$8 * 2.0F + 0.5);
      byte $$11 = (byte)($$9 * 2.0F + 0.5);
      int $$12 = 63;
      byte $$13;
      if ($$8 >= -63.0F && $$9 >= -63.0F && $$8 <= 63.0F && $$9 <= 63.0F) {
         p_77943_ += p_77943_ < 0.0 ? -8.0 : 8.0;
         $$13 = (byte)(p_77943_ * 16.0 / 360.0);
         if (this.dimension == Level.NETHER && p_77939_ != null) {
            int $$14 = (int)(p_77939_.getLevelData().getDayTime() / 10L);
            $$13 = (byte)($$14 * $$14 * 34187121 + $$14 * 121 >> 15 & 15);
         }
      } else {
         if (!p_335830_.is(MapDecorationTypes.PLAYER)) {
            this.removeDecoration(p_77940_);
            return;
         }

         int $$15 = 320;
         if (Math.abs($$8) < 320.0F && Math.abs($$9) < 320.0F) {
            p_335830_ = MapDecorationTypes.PLAYER_OFF_MAP;
         } else {
            if (!this.unlimitedTracking) {
               this.removeDecoration(p_77940_);
               return;
            }

            p_335830_ = MapDecorationTypes.PLAYER_OFF_LIMITS;
         }

         $$13 = 0;
         if ($$8 <= -63.0F) {
            $$10 = -128;
         }

         if ($$9 <= -63.0F) {
            $$11 = -128;
         }

         if ($$8 >= 63.0F) {
            $$10 = 127;
         }

         if ($$9 >= 63.0F) {
            $$11 = 127;
         }
      }

      MapDecoration $$18 = new MapDecoration(p_335830_, $$10, $$11, $$13, Optional.ofNullable(p_77944_));
      MapDecoration $$19 = this.decorations.put(p_77940_, $$18);
      if (!$$18.equals($$19)) {
         if ($$19 != null && $$19.type().value().trackCount()) {
            this.trackedDecorationCount--;
         }

         if (p_335830_.value().trackCount()) {
            this.trackedDecorationCount++;
         }

         this.setDecorationsDirty();
      }
   }

   @Nullable
   public Packet<?> getUpdatePacket(MapId p_323760_, Player p_164798_) {
      MapItemSavedData.HoldingPlayer $$2 = this.carriedByPlayers.get(p_164798_);
      return $$2 == null ? null : $$2.nextUpdatePacket(p_323760_);
   }

   private void setColorsDirty(int p_164790_, int p_164791_) {
      this.setDirty();

      for (MapItemSavedData.HoldingPlayer $$2 : this.carriedBy) {
         $$2.markColorsDirty(p_164790_, p_164791_);
      }
   }

   private void setDecorationsDirty() {
      this.setDirty();
      this.carriedBy.forEach(MapItemSavedData.HoldingPlayer::markDecorationsDirty);
   }

   public MapItemSavedData.HoldingPlayer getHoldingPlayer(Player p_77917_) {
      MapItemSavedData.HoldingPlayer $$1 = this.carriedByPlayers.get(p_77917_);
      if ($$1 == null) {
         $$1 = new MapItemSavedData.HoldingPlayer(p_77917_);
         this.carriedByPlayers.put(p_77917_, $$1);
         this.carriedBy.add($$1);
      }

      return $$1;
   }

   public boolean toggleBanner(LevelAccessor p_77935_, BlockPos p_77936_) {
      double $$2 = p_77936_.getX() + 0.5;
      double $$3 = p_77936_.getZ() + 0.5;
      int $$4 = 1 << this.scale;
      double $$5 = ($$2 - this.centerX) / $$4;
      double $$6 = ($$3 - this.centerZ) / $$4;
      int $$7 = 63;
      if ($$5 >= -63.0 && $$6 >= -63.0 && $$5 <= 63.0 && $$6 <= 63.0) {
         MapBanner $$8 = MapBanner.fromWorld(p_77935_, p_77936_);
         if ($$8 == null) {
            return false;
         }

         if (this.bannerMarkers.remove($$8.getId(), $$8)) {
            this.removeDecoration($$8.getId());
            return true;
         }

         if (!this.isTrackedCountOverLimit(256)) {
            this.bannerMarkers.put($$8.getId(), $$8);
            this.addDecoration($$8.getDecoration(), p_77935_, $$8.getId(), $$2, $$3, 180.0, $$8.name().orElse(null));
            return true;
         }
      }

      return false;
   }

   public void checkBanners(BlockGetter p_77931_, int p_77932_, int p_77933_) {
      Iterator<MapBanner> $$3 = this.bannerMarkers.values().iterator();

      while ($$3.hasNext()) {
         MapBanner $$4 = $$3.next();
         if ($$4.pos().getX() == p_77932_ && $$4.pos().getZ() == p_77933_) {
            MapBanner $$5 = MapBanner.fromWorld(p_77931_, $$4.pos());
            if (!$$4.equals($$5)) {
               $$3.remove();
               this.removeDecoration($$4.getId());
            }
         }
      }
   }

   public Collection<MapBanner> getBanners() {
      return this.bannerMarkers.values();
   }

   public void removedFromFrame(BlockPos p_77948_, int p_77949_) {
      this.removeDecoration(getFrameKey(p_77949_));
      this.frameMarkers.remove(MapFrame.frameId(p_77948_));
   }

   public boolean updateColor(int p_164793_, int p_164794_, byte p_164795_) {
      byte $$3 = this.colors[p_164793_ + p_164794_ * 128];
      if ($$3 != p_164795_) {
         this.setColor(p_164793_, p_164794_, p_164795_);
         return true;
      } else {
         return false;
      }
   }

   public void setColor(int p_164804_, int p_164805_, byte p_164806_) {
      this.colors[p_164804_ + p_164805_ * 128] = p_164806_;
      this.setColorsDirty(p_164804_, p_164805_);
   }

   public boolean isExplorationMap() {
      for (MapDecoration $$0 : this.decorations.values()) {
         if ($$0.type().value().explorationMapElement()) {
            return true;
         }
      }

      return false;
   }

   public void addClientSideDecorations(List<MapDecoration> p_164802_) {
      this.decorations.clear();
      this.trackedDecorationCount = 0;

      for (int $$1 = 0; $$1 < p_164802_.size(); $$1++) {
         MapDecoration $$2 = p_164802_.get($$1);
         this.decorations.put("icon-" + $$1, $$2);
         if ($$2.type().value().trackCount()) {
            this.trackedDecorationCount++;
         }
      }
   }

   public Iterable<MapDecoration> getDecorations() {
      return this.decorations.values();
   }

   public boolean isTrackedCountOverLimit(int p_181313_) {
      return this.trackedDecorationCount >= p_181313_;
   }

   private static String getFrameKey(int p_353065_) {
      return "frame-" + p_353065_;
   }

   public class HoldingPlayer {
      public final Player player;
      private boolean dirtyData = true;
      private int minDirtyX;
      private int minDirtyY;
      private int maxDirtyX = 127;
      private int maxDirtyY = 127;
      private boolean dirtyDecorations = true;
      private int tick;
      public int step;

      HoldingPlayer(Player p_77970_) {
         this.player = p_77970_;
      }

      private MapItemSavedData.MapPatch createPatch() {
         int $$0 = this.minDirtyX;
         int $$1 = this.minDirtyY;
         int $$2 = this.maxDirtyX + 1 - this.minDirtyX;
         int $$3 = this.maxDirtyY + 1 - this.minDirtyY;
         byte[] $$4 = new byte[$$2 * $$3];

         for (int $$5 = 0; $$5 < $$2; $$5++) {
            for (int $$6 = 0; $$6 < $$3; $$6++) {
               $$4[$$5 + $$6 * $$2] = MapItemSavedData.this.colors[$$0 + $$5 + ($$1 + $$6) * 128];
            }
         }

         return new MapItemSavedData.MapPatch($$0, $$1, $$2, $$3, $$4);
      }

      @Nullable
      Packet<?> nextUpdatePacket(MapId p_324558_) {
         MapItemSavedData.MapPatch $$1;
         if (this.dirtyData) {
            this.dirtyData = false;
            $$1 = this.createPatch();
         } else {
            $$1 = null;
         }

         Collection<MapDecoration> $$3;
         if (this.dirtyDecorations && this.tick++ % 5 == 0) {
            this.dirtyDecorations = false;
            $$3 = MapItemSavedData.this.decorations.values();
         } else {
            $$3 = null;
         }

         return $$3 == null && $$1 == null
            ? null
            : new ClientboundMapItemDataPacket(p_324558_, MapItemSavedData.this.scale, MapItemSavedData.this.locked, $$3, $$1);
      }

      void markColorsDirty(int p_164818_, int p_164819_) {
         if (this.dirtyData) {
            this.minDirtyX = Math.min(this.minDirtyX, p_164818_);
            this.minDirtyY = Math.min(this.minDirtyY, p_164819_);
            this.maxDirtyX = Math.max(this.maxDirtyX, p_164818_);
            this.maxDirtyY = Math.max(this.maxDirtyY, p_164819_);
         } else {
            this.dirtyData = true;
            this.minDirtyX = p_164818_;
            this.minDirtyY = p_164819_;
            this.maxDirtyX = p_164818_;
            this.maxDirtyY = p_164819_;
         }
      }

      private void markDecorationsDirty() {
         this.dirtyDecorations = true;
      }
   }

   public record MapPatch(int startX, int startY, int width, int height, byte[] mapColors) {
      public static final StreamCodec<ByteBuf, Optional<MapItemSavedData.MapPatch>> STREAM_CODEC = StreamCodec.of(
         MapItemSavedData.MapPatch::write, MapItemSavedData.MapPatch::read
      );

      private static void write(ByteBuf p_323934_, Optional<MapItemSavedData.MapPatch> p_323549_) {
         if (p_323549_.isPresent()) {
            MapItemSavedData.MapPatch $$2 = p_323549_.get();
            p_323934_.writeByte($$2.width);
            p_323934_.writeByte($$2.height);
            p_323934_.writeByte($$2.startX);
            p_323934_.writeByte($$2.startY);
            FriendlyByteBuf.writeByteArray(p_323934_, $$2.mapColors);
         } else {
            p_323934_.writeByte(0);
         }
      }

      private static Optional<MapItemSavedData.MapPatch> read(ByteBuf p_323587_) {
         int $$1 = p_323587_.readUnsignedByte();
         if ($$1 > 0) {
            int $$2 = p_323587_.readUnsignedByte();
            int $$3 = p_323587_.readUnsignedByte();
            int $$4 = p_323587_.readUnsignedByte();
            byte[] $$5 = FriendlyByteBuf.readByteArray(p_323587_);
            return Optional.of(new MapItemSavedData.MapPatch($$3, $$4, $$1, $$2, $$5));
         } else {
            return Optional.empty();
         }
      }

      public void applyToMap(MapItemSavedData p_164833_) {
         for (int $$1 = 0; $$1 < this.width; $$1++) {
            for (int $$2 = 0; $$2 < this.height; $$2++) {
               p_164833_.setColor(this.startX + $$1, this.startY + $$2, this.mapColors[$$1 + $$2 * this.width]);
            }
         }
      }
   }
}

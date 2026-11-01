package net.minecraft.world.level.block.entity;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ResourceLocationException;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class StructureBlockEntity extends BlockEntity {
   private static final int SCAN_CORNER_BLOCKS_RANGE = 5;
   public static final int MAX_OFFSET_PER_AXIS = 48;
   public static final int MAX_SIZE_PER_AXIS = 48;
   public static final String AUTHOR_TAG = "author";
   @Nullable
   private ResourceLocation structureName;
   private String author = "";
   private String metaData = "";
   private BlockPos structurePos = new BlockPos(0, 1, 0);
   private Vec3i structureSize = Vec3i.ZERO;
   private Mirror mirror = Mirror.NONE;
   private Rotation rotation = Rotation.NONE;
   private StructureMode mode;
   private boolean ignoreEntities = true;
   private boolean powered;
   private boolean showAir;
   private boolean showBoundingBox = true;
   private float integrity = 1.0F;
   private long seed;

   public StructureBlockEntity(BlockPos p_155779_, BlockState p_155780_) {
      super(BlockEntityType.STRUCTURE_BLOCK, p_155779_, p_155780_);
      this.mode = p_155780_.getValue(StructureBlock.MODE);
   }

   @Override
   protected void saveAdditional(CompoundTag p_187524_, HolderLookup.Provider p_323617_) {
      super.saveAdditional(p_187524_, p_323617_);
      p_187524_.putString("name", this.getStructureName());
      p_187524_.putString("author", this.author);
      p_187524_.putString("metadata", this.metaData);
      p_187524_.putInt("posX", this.structurePos.getX());
      p_187524_.putInt("posY", this.structurePos.getY());
      p_187524_.putInt("posZ", this.structurePos.getZ());
      p_187524_.putInt("sizeX", this.structureSize.getX());
      p_187524_.putInt("sizeY", this.structureSize.getY());
      p_187524_.putInt("sizeZ", this.structureSize.getZ());
      p_187524_.putString("rotation", this.rotation.toString());
      p_187524_.putString("mirror", this.mirror.toString());
      p_187524_.putString("mode", this.mode.toString());
      p_187524_.putBoolean("ignoreEntities", this.ignoreEntities);
      p_187524_.putBoolean("powered", this.powered);
      p_187524_.putBoolean("showair", this.showAir);
      p_187524_.putBoolean("showboundingbox", this.showBoundingBox);
      p_187524_.putFloat("integrity", this.integrity);
      p_187524_.putLong("seed", this.seed);
   }

   @Override
   protected void loadAdditional(CompoundTag p_155800_, HolderLookup.Provider p_324076_) {
      super.loadAdditional(p_155800_, p_324076_);
      this.setStructureName(p_155800_.getString("name"));
      this.author = p_155800_.getString("author");
      this.metaData = p_155800_.getString("metadata");
      int $$2 = Mth.clamp(p_155800_.getInt("posX"), -48, 48);
      int $$3 = Mth.clamp(p_155800_.getInt("posY"), -48, 48);
      int $$4 = Mth.clamp(p_155800_.getInt("posZ"), -48, 48);
      this.structurePos = new BlockPos($$2, $$3, $$4);
      int $$5 = Mth.clamp(p_155800_.getInt("sizeX"), 0, 48);
      int $$6 = Mth.clamp(p_155800_.getInt("sizeY"), 0, 48);
      int $$7 = Mth.clamp(p_155800_.getInt("sizeZ"), 0, 48);
      this.structureSize = new Vec3i($$5, $$6, $$7);

      try {
         this.rotation = Rotation.valueOf(p_155800_.getString("rotation"));
      } catch (IllegalArgumentException var12) {
         this.rotation = Rotation.NONE;
      }

      try {
         this.mirror = Mirror.valueOf(p_155800_.getString("mirror"));
      } catch (IllegalArgumentException var11) {
         this.mirror = Mirror.NONE;
      }

      try {
         this.mode = StructureMode.valueOf(p_155800_.getString("mode"));
      } catch (IllegalArgumentException var10) {
         this.mode = StructureMode.DATA;
      }

      this.ignoreEntities = p_155800_.getBoolean("ignoreEntities");
      this.powered = p_155800_.getBoolean("powered");
      this.showAir = p_155800_.getBoolean("showair");
      this.showBoundingBox = p_155800_.getBoolean("showboundingbox");
      if (p_155800_.contains("integrity")) {
         this.integrity = p_155800_.getFloat("integrity");
      } else {
         this.integrity = 1.0F;
      }

      this.seed = p_155800_.getLong("seed");
      this.updateBlockState();
   }

   private void updateBlockState() {
      if (this.level != null) {
         BlockPos $$0 = this.getBlockPos();
         BlockState $$1 = this.level.getBlockState($$0);
         if ($$1.is(Blocks.STRUCTURE_BLOCK)) {
            this.level.setBlock($$0, $$1.setValue(StructureBlock.MODE, this.mode), 2);
         }
      }
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_324257_) {
      return this.saveCustomOnly(p_324257_);
   }

   public boolean usedBy(Player p_59854_) {
      if (!p_59854_.canUseGameMasterBlocks()) {
         return false;
      } else {
         if (p_59854_.getCommandSenderWorld().isClientSide) {
            p_59854_.openStructureBlock(this);
         }

         return true;
      }
   }

   public String getStructureName() {
      return this.structureName == null ? "" : this.structureName.toString();
   }

   public boolean hasStructureName() {
      return this.structureName != null;
   }

   public void setStructureName(@Nullable String p_59869_) {
      this.setStructureName(StringUtil.isNullOrEmpty(p_59869_) ? null : ResourceLocation.tryParse(p_59869_));
   }

   public void setStructureName(@Nullable ResourceLocation p_59875_) {
      this.structureName = p_59875_;
   }

   public void createdBy(LivingEntity p_59852_) {
      this.author = p_59852_.getName().getString();
   }

   public BlockPos getStructurePos() {
      return this.structurePos;
   }

   public void setStructurePos(BlockPos p_59886_) {
      this.structurePos = p_59886_;
   }

   public Vec3i getStructureSize() {
      return this.structureSize;
   }

   public void setStructureSize(Vec3i p_155798_) {
      this.structureSize = p_155798_;
   }

   public Mirror getMirror() {
      return this.mirror;
   }

   public void setMirror(Mirror p_59882_) {
      this.mirror = p_59882_;
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public void setRotation(Rotation p_59884_) {
      this.rotation = p_59884_;
   }

   public String getMetaData() {
      return this.metaData;
   }

   public void setMetaData(String p_59888_) {
      this.metaData = p_59888_;
   }

   public StructureMode getMode() {
      return this.mode;
   }

   public void setMode(StructureMode p_59861_) {
      this.mode = p_59861_;
      BlockState $$1 = this.level.getBlockState(this.getBlockPos());
      if ($$1.is(Blocks.STRUCTURE_BLOCK)) {
         this.level.setBlock(this.getBlockPos(), $$1.setValue(StructureBlock.MODE, p_59861_), 2);
      }
   }

   public boolean isIgnoreEntities() {
      return this.ignoreEntities;
   }

   public void setIgnoreEntities(boolean p_59877_) {
      this.ignoreEntities = p_59877_;
   }

   public float getIntegrity() {
      return this.integrity;
   }

   public void setIntegrity(float p_59839_) {
      this.integrity = p_59839_;
   }

   public long getSeed() {
      return this.seed;
   }

   public void setSeed(long p_59841_) {
      this.seed = p_59841_;
   }

   public boolean detectSize() {
      if (this.mode != StructureMode.SAVE) {
         return false;
      } else {
         BlockPos $$0 = this.getBlockPos();
         int $$1 = 80;
         BlockPos $$2 = new BlockPos($$0.getX() - 80, this.level.getMinBuildHeight(), $$0.getZ() - 80);
         BlockPos $$3 = new BlockPos($$0.getX() + 80, this.level.getMaxBuildHeight() - 1, $$0.getZ() + 80);
         Stream<BlockPos> $$4 = this.getRelatedCorners($$2, $$3);
         return calculateEnclosingBoundingBox($$0, $$4).filter(p_155790_ -> {
            int $$2x = p_155790_.maxX() - p_155790_.minX();
            int $$3x = p_155790_.maxY() - p_155790_.minY();
            int $$4x = p_155790_.maxZ() - p_155790_.minZ();
            if ($$2x > 1 && $$3x > 1 && $$4x > 1) {
               this.structurePos = new BlockPos(p_155790_.minX() - $$0.getX() + 1, p_155790_.minY() - $$0.getY() + 1, p_155790_.minZ() - $$0.getZ() + 1);
               this.structureSize = new Vec3i($$2x - 1, $$3x - 1, $$4x - 1);
               this.setChanged();
               BlockState $$5 = this.level.getBlockState($$0);
               this.level.sendBlockUpdated($$0, $$5, $$5, 3);
               return true;
            } else {
               return false;
            }
         }).isPresent();
      }
   }

   private Stream<BlockPos> getRelatedCorners(BlockPos p_155792_, BlockPos p_155793_) {
      return BlockPos.betweenClosedStream(p_155792_, p_155793_)
         .filter(p_272561_ -> this.level.getBlockState(p_272561_).is(Blocks.STRUCTURE_BLOCK))
         .map(this.level::getBlockEntity)
         .filter(p_155802_ -> p_155802_ instanceof StructureBlockEntity)
         .map(p_155785_ -> (StructureBlockEntity)p_155785_)
         .filter(p_155787_ -> p_155787_.mode == StructureMode.CORNER && Objects.equals(this.structureName, p_155787_.structureName))
         .map(BlockEntity::getBlockPos);
   }

   private static Optional<BoundingBox> calculateEnclosingBoundingBox(BlockPos p_155795_, Stream<BlockPos> p_155796_) {
      Iterator<BlockPos> $$2 = p_155796_.iterator();
      if (!$$2.hasNext()) {
         return Optional.empty();
      } else {
         BlockPos $$3 = $$2.next();
         BoundingBox $$4 = new BoundingBox($$3);
         if ($$2.hasNext()) {
            $$2.forEachRemaining($$4::encapsulate);
         } else {
            $$4.encapsulate(p_155795_);
         }

         return Optional.of($$4);
      }
   }

   public boolean saveStructure() {
      return this.mode != StructureMode.SAVE ? false : this.saveStructure(true);
   }

   public boolean saveStructure(boolean p_59890_) {
      if (this.structureName == null) {
         return false;
      } else {
         BlockPos $$1 = this.getBlockPos().offset(this.structurePos);
         ServerLevel $$2 = (ServerLevel)this.level;
         StructureTemplateManager $$3 = $$2.getStructureManager();

         StructureTemplate $$4;
         try {
            $$4 = $$3.getOrCreate(this.structureName);
         } catch (ResourceLocationException var8) {
            return false;
         }

         $$4.fillFromWorld(this.level, $$1, this.structureSize, !this.ignoreEntities, Blocks.STRUCTURE_VOID);
         $$4.setAuthor(this.author);
         if (p_59890_) {
            try {
               return $$3.save(this.structureName);
            } catch (ResourceLocationException var7) {
               return false;
            }
         } else {
            return true;
         }
      }
   }

   public static RandomSource createRandom(long p_222889_) {
      return p_222889_ == 0L ? RandomSource.create(Util.getMillis()) : RandomSource.create(p_222889_);
   }

   public boolean placeStructureIfSameSize(ServerLevel p_309647_) {
      if (this.mode == StructureMode.LOAD && this.structureName != null) {
         StructureTemplate $$1 = p_309647_.getStructureManager().get(this.structureName).orElse(null);
         if ($$1 == null) {
            return false;
         } else if ($$1.getSize().equals(this.structureSize)) {
            this.placeStructure(p_309647_, $$1);
            return true;
         } else {
            this.loadStructureInfo($$1);
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean loadStructureInfo(ServerLevel p_309683_) {
      StructureTemplate $$1 = this.getStructureTemplate(p_309683_);
      if ($$1 == null) {
         return false;
      } else {
         this.loadStructureInfo($$1);
         return true;
      }
   }

   private void loadStructureInfo(StructureTemplate p_309715_) {
      this.author = !StringUtil.isNullOrEmpty(p_309715_.getAuthor()) ? p_309715_.getAuthor() : "";
      this.structureSize = p_309715_.getSize();
      this.setChanged();
   }

   public void placeStructure(ServerLevel p_309543_) {
      StructureTemplate $$1 = this.getStructureTemplate(p_309543_);
      if ($$1 != null) {
         this.placeStructure(p_309543_, $$1);
      }
   }

   @Nullable
   private StructureTemplate getStructureTemplate(ServerLevel p_309601_) {
      return this.structureName == null ? null : p_309601_.getStructureManager().get(this.structureName).orElse(null);
   }

   private void placeStructure(ServerLevel p_309691_, StructureTemplate p_309550_) {
      this.loadStructureInfo(p_309550_);
      StructurePlaceSettings $$2 = new StructurePlaceSettings().setMirror(this.mirror).setRotation(this.rotation).setIgnoreEntities(this.ignoreEntities);
      if (this.integrity < 1.0F) {
         $$2.clearProcessors().addProcessor(new BlockRotProcessor(Mth.clamp(this.integrity, 0.0F, 1.0F))).setRandom(createRandom(this.seed));
      }

      BlockPos $$3 = this.getBlockPos().offset(this.structurePos);
      p_309550_.placeInWorld(p_309691_, $$3, $$3, $$2, createRandom(this.seed), 2);
   }

   public void unloadStructure() {
      if (this.structureName != null) {
         ServerLevel $$0 = (ServerLevel)this.level;
         StructureTemplateManager $$1 = $$0.getStructureManager();
         $$1.remove(this.structureName);
      }
   }

   public boolean isStructureLoadable() {
      if (this.mode == StructureMode.LOAD && !this.level.isClientSide && this.structureName != null) {
         ServerLevel $$0 = (ServerLevel)this.level;
         StructureTemplateManager $$1 = $$0.getStructureManager();

         try {
            return $$1.get(this.structureName).isPresent();
         } catch (ResourceLocationException var4) {
            return false;
         }
      } else {
         return false;
      }
   }

   public boolean isPowered() {
      return this.powered;
   }

   public void setPowered(boolean p_59894_) {
      this.powered = p_59894_;
   }

   public boolean getShowAir() {
      return this.showAir;
   }

   public void setShowAir(boolean p_59897_) {
      this.showAir = p_59897_;
   }

   public boolean getShowBoundingBox() {
      return this.showBoundingBox;
   }

   public void setShowBoundingBox(boolean p_59899_) {
      this.showBoundingBox = p_59899_;
   }

   public static enum UpdateType {
      UPDATE_DATA,
      SAVE_AREA,
      LOAD_AREA,
      SCAN_AREA;
   }
}

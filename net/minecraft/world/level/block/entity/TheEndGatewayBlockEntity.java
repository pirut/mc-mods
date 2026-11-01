package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.EndFeatures;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.EndGatewayConfiguration;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class TheEndGatewayBlockEntity extends TheEndPortalBlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SPAWN_TIME = 200;
   private static final int COOLDOWN_TIME = 40;
   private static final int ATTENTION_INTERVAL = 2400;
   private static final int EVENT_COOLDOWN = 1;
   private static final int GATEWAY_HEIGHT_ABOVE_SURFACE = 10;
   private long age;
   private int teleportCooldown;
   @Nullable
   private BlockPos exitPortal;
   private boolean exactTeleport;

   public TheEndGatewayBlockEntity(BlockPos p_155813_, BlockState p_155814_) {
      super(BlockEntityType.END_GATEWAY, p_155813_, p_155814_);
   }

   @Override
   protected void saveAdditional(CompoundTag p_187527_, HolderLookup.Provider p_324553_) {
      super.saveAdditional(p_187527_, p_324553_);
      p_187527_.putLong("Age", this.age);
      if (this.exitPortal != null) {
         p_187527_.put("exit_portal", NbtUtils.writeBlockPos(this.exitPortal));
      }

      if (this.exactTeleport) {
         p_187527_.putBoolean("ExactTeleport", true);
      }
   }

   @Override
   protected void loadAdditional(CompoundTag p_155840_, HolderLookup.Provider p_323480_) {
      super.loadAdditional(p_155840_, p_323480_);
      this.age = p_155840_.getLong("Age");
      NbtUtils.readBlockPos(p_155840_, "exit_portal").filter(Level::isInSpawnableBounds).ifPresent(p_325870_ -> this.exitPortal = p_325870_);
      this.exactTeleport = p_155840_.getBoolean("ExactTeleport");
   }

   public static void beamAnimationTick(Level p_155835_, BlockPos p_155836_, BlockState p_155837_, TheEndGatewayBlockEntity p_155838_) {
      p_155838_.age++;
      if (p_155838_.isCoolingDown()) {
         p_155838_.teleportCooldown--;
      }
   }

   public static void portalTick(Level p_350841_, BlockPos p_350745_, BlockState p_350476_, TheEndGatewayBlockEntity p_350562_) {
      boolean $$4 = p_350562_.isSpawning();
      boolean $$5 = p_350562_.isCoolingDown();
      p_350562_.age++;
      if ($$5) {
         p_350562_.teleportCooldown--;
      } else if (p_350562_.age % 2400L == 0L) {
         triggerCooldown(p_350841_, p_350745_, p_350476_, p_350562_);
      }

      if ($$4 != p_350562_.isSpawning() || $$5 != p_350562_.isCoolingDown()) {
         setChanged(p_350841_, p_350745_, p_350476_);
      }
   }

   public boolean isSpawning() {
      return this.age < 200L;
   }

   public boolean isCoolingDown() {
      return this.teleportCooldown > 0;
   }

   public float getSpawnPercent(float p_59934_) {
      return Mth.clamp(((float)this.age + p_59934_) / 200.0F, 0.0F, 1.0F);
   }

   public float getCooldownPercent(float p_59968_) {
      return 1.0F - Mth.clamp((this.teleportCooldown - p_59968_) / 40.0F, 0.0F, 1.0F);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_324313_) {
      return this.saveCustomOnly(p_324313_);
   }

   public static void triggerCooldown(Level p_155850_, BlockPos p_155851_, BlockState p_155852_, TheEndGatewayBlockEntity p_155853_) {
      if (!p_155850_.isClientSide) {
         p_155853_.teleportCooldown = 40;
         p_155850_.blockEvent(p_155851_, p_155852_.getBlock(), 1, 0);
         setChanged(p_155850_, p_155851_, p_155852_);
      }
   }

   @Override
   public boolean triggerEvent(int p_59963_, int p_59964_) {
      if (p_59963_ == 1) {
         this.teleportCooldown = 40;
         return true;
      } else {
         return super.triggerEvent(p_59963_, p_59964_);
      }
   }

   @Nullable
   public Vec3 getPortalPosition(ServerLevel p_350561_, BlockPos p_350271_) {
      if (this.exitPortal == null && p_350561_.dimension() == Level.END) {
         BlockPos $$2 = findOrCreateValidTeleportPos(p_350561_, p_350271_);
         $$2 = $$2.above(10);
         LOGGER.debug("Creating portal at {}", $$2);
         spawnGatewayPortal(p_350561_, $$2, EndGatewayConfiguration.knownExit(p_350271_, false));
         this.setExitPosition($$2, this.exactTeleport);
      }

      if (this.exitPortal != null) {
         BlockPos $$3 = this.exactTeleport ? this.exitPortal : findExitPosition(p_350561_, this.exitPortal);
         return $$3.getBottomCenter();
      } else {
         return null;
      }
   }

   private static BlockPos findExitPosition(Level p_155826_, BlockPos p_155827_) {
      BlockPos $$2 = findTallestBlock(p_155826_, p_155827_.offset(0, 2, 0), 5, false);
      LOGGER.debug("Best exit position for portal at {} is {}", p_155827_, $$2);
      return $$2.above();
   }

   private static BlockPos findOrCreateValidTeleportPos(ServerLevel p_155819_, BlockPos p_155820_) {
      Vec3 $$2 = findExitPortalXZPosTentative(p_155819_, p_155820_);
      LevelChunk $$3 = getChunk(p_155819_, $$2);
      BlockPos $$4 = findValidSpawnInChunk($$3);
      if ($$4 == null) {
         BlockPos $$5 = BlockPos.containing($$2.x + 0.5, 75.0, $$2.z + 0.5);
         LOGGER.debug("Failed to find a suitable block to teleport to, spawning an island on {}", $$5);
         p_155819_.registryAccess()
            .registry(Registries.CONFIGURED_FEATURE)
            .flatMap(p_258975_ -> p_258975_.getHolder(EndFeatures.END_ISLAND))
            .ifPresent(p_256040_ -> p_256040_.value().place(p_155819_, p_155819_.getChunkSource().getGenerator(), RandomSource.create($$5.asLong()), $$5));
         $$4 = $$5;
      } else {
         LOGGER.debug("Found suitable block to teleport to: {}", $$4);
      }

      return findTallestBlock(p_155819_, $$4, 16, true);
   }

   private static Vec3 findExitPortalXZPosTentative(ServerLevel p_155842_, BlockPos p_155843_) {
      Vec3 $$2 = new Vec3(p_155843_.getX(), 0.0, p_155843_.getZ()).normalize();
      int $$3 = 1024;
      Vec3 $$4 = $$2.scale(1024.0);

      for (int $$5 = 16; !isChunkEmpty(p_155842_, $$4) && $$5-- > 0; $$4 = $$4.add($$2.scale(-16.0))) {
         LOGGER.debug("Skipping backwards past nonempty chunk at {}", $$4);
      }

      for (int var6 = 16; isChunkEmpty(p_155842_, $$4) && var6-- > 0; $$4 = $$4.add($$2.scale(16.0))) {
         LOGGER.debug("Skipping forward past empty chunk at {}", $$4);
      }

      LOGGER.debug("Found chunk at {}", $$4);
      return $$4;
   }

   private static boolean isChunkEmpty(ServerLevel p_155816_, Vec3 p_155817_) {
      return getChunk(p_155816_, p_155817_).getHighestFilledSectionIndex() == -1;
   }

   private static BlockPos findTallestBlock(BlockGetter p_59943_, BlockPos p_59944_, int p_59945_, boolean p_59946_) {
      BlockPos $$4 = null;

      for (int $$5 = -p_59945_; $$5 <= p_59945_; $$5++) {
         for (int $$6 = -p_59945_; $$6 <= p_59945_; $$6++) {
            if ($$5 != 0 || $$6 != 0 || p_59946_) {
               for (int $$7 = p_59943_.getMaxBuildHeight() - 1; $$7 > ($$4 == null ? p_59943_.getMinBuildHeight() : $$4.getY()); $$7--) {
                  BlockPos $$8 = new BlockPos(p_59944_.getX() + $$5, $$7, p_59944_.getZ() + $$6);
                  BlockState $$9 = p_59943_.getBlockState($$8);
                  if ($$9.isCollisionShapeFullBlock(p_59943_, $$8) && (p_59946_ || !$$9.is(Blocks.BEDROCK))) {
                     $$4 = $$8;
                     break;
                  }
               }
            }
         }
      }

      return $$4 == null ? p_59944_ : $$4;
   }

   private static LevelChunk getChunk(Level p_59948_, Vec3 p_59949_) {
      return p_59948_.getChunk(Mth.floor(p_59949_.x / 16.0), Mth.floor(p_59949_.z / 16.0));
   }

   @Nullable
   private static BlockPos findValidSpawnInChunk(LevelChunk p_59954_) {
      ChunkPos $$1 = p_59954_.getPos();
      BlockPos $$2 = new BlockPos($$1.getMinBlockX(), 30, $$1.getMinBlockZ());
      int $$3 = p_59954_.getHighestSectionPosition() + 16 - 1;
      BlockPos $$4 = new BlockPos($$1.getMaxBlockX(), $$3, $$1.getMaxBlockZ());
      BlockPos $$5 = null;
      double $$6 = 0.0;

      for (BlockPos $$7 : BlockPos.betweenClosed($$2, $$4)) {
         BlockState $$8 = p_59954_.getBlockState($$7);
         BlockPos $$9 = $$7.above();
         BlockPos $$10 = $$7.above(2);
         if ($$8.is(Blocks.END_STONE)
            && !p_59954_.getBlockState($$9).isCollisionShapeFullBlock(p_59954_, $$9)
            && !p_59954_.getBlockState($$10).isCollisionShapeFullBlock(p_59954_, $$10)) {
            double $$11 = $$7.distToCenterSqr(0.0, 0.0, 0.0);
            if ($$5 == null || $$11 < $$6) {
               $$5 = $$7;
               $$6 = $$11;
            }
         }
      }

      return $$5;
   }

   private static void spawnGatewayPortal(ServerLevel p_155822_, BlockPos p_155823_, EndGatewayConfiguration p_155824_) {
      Feature.END_GATEWAY.place(p_155824_, p_155822_, p_155822_.getChunkSource().getGenerator(), RandomSource.create(), p_155823_);
   }

   @Override
   public boolean shouldRenderFace(Direction p_59959_) {
      return Block.shouldRenderFace(this.getBlockState(), this.level, this.getBlockPos(), p_59959_, this.getBlockPos().relative(p_59959_));
   }

   public int getParticleAmount() {
      int $$0 = 0;

      for (Direction $$1 : Direction.values()) {
         $$0 += this.shouldRenderFace($$1) ? 1 : 0;
      }

      return $$0;
   }

   public void setExitPosition(BlockPos p_59956_, boolean p_59957_) {
      this.exactTeleport = p_59957_;
      this.exitPortal = p_59956_;
      this.setChanged();
   }
}

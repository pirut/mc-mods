package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.debug.DebugValueSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentHolder;
import net.neoforged.neoforge.attachment.AttachmentSync;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.extensions.IBlockEntityExtension;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class BlockEntity extends AttachmentHolder implements DebugValueSource, IBlockEntityExtension {
   private static final Codec<BlockEntityType<?>> TYPE_CODEC = BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec();
   private static final Logger LOGGER = LogUtils.getLogger();
   @Deprecated
   private final BlockEntityType<?> type;
   @Nullable
   protected Level level;
   protected final BlockPos worldPosition;
   protected boolean remove;
   private BlockState blockState;
   private DataComponentMap components = DataComponentMap.EMPTY;
   @Nullable
   private CompoundTag customPersistentData;

   public BlockEntity(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
      this.type = p_155228_;
      this.worldPosition = p_155229_.immutable();
      this.validateBlockState(p_155230_);
      this.blockState = p_155230_;
   }

   private void validateBlockState(BlockState p_353132_) {
      if (!this.isValidBlockState(p_353132_)) {
         throw new IllegalStateException("Invalid block entity " + this.getNameForReporting() + " state at " + this.worldPosition + ", got " + p_353132_);
      }
   }

   public boolean isValidBlockState(BlockState p_353131_) {
      return this.getType().isValid(p_353131_);
   }

   public static BlockPos getPosFromTag(ChunkPos p_412201_, CompoundTag p_187473_) {
      int i = p_187473_.getIntOr("x", 0);
      int j = p_187473_.getIntOr("y", 0);
      int k = p_187473_.getIntOr("z", 0);
      int l = SectionPos.blockToSectionCoord(i);
      int i1 = SectionPos.blockToSectionCoord(k);
      if (l != p_412201_.x || i1 != p_412201_.z) {
         LOGGER.warn("Block entity {} found in a wrong chunk, expected position from chunk {}", p_187473_, p_412201_);
         i = p_412201_.getBlockX(SectionPos.sectionRelative(i));
         k = p_412201_.getBlockZ(SectionPos.sectionRelative(k));
      }

      return new BlockPos(i, j, k);
   }

   @Nullable
   public Level getLevel() {
      return this.level;
   }

   public void setLevel(Level p_155231_) {
      this.level = p_155231_;
   }

   public boolean hasLevel() {
      return this.level != null;
   }

   protected void loadAdditional(ValueInput p_422607_) {
      p_422607_.<CompoundTag>read("NeoForgeData", CompoundTag.CODEC).ifPresent(neoData -> this.customPersistentData = neoData);
      p_422607_.child("neoforge:attachments").ifPresent(attachments -> this.deserializeAttachments(attachments));
   }

   public final void loadWithComponents(ValueInput p_422624_) {
      this.loadAdditional(p_422624_);
      this.components = p_422624_.<DataComponentMap>read("components", DataComponentMap.CODEC).orElse(DataComponentMap.EMPTY);
   }

   public final void loadCustomOnly(ValueInput p_422425_) {
      this.loadAdditional(p_422425_);
   }

   protected void saveAdditional(ValueOutput p_422639_) {
      if (this.customPersistentData != null) {
         p_422639_.store("NeoForgeData", CompoundTag.CODEC, this.customPersistentData.copy());
      }

      if (this.level != null) {
         this.level.registryAccess();
      }

      ValueOutput attachments = p_422639_.child("neoforge:attachments");
      this.serializeAttachments(attachments);
      if (attachments.isEmpty()) {
         p_422639_.discard("neoforge:attachments");
      }
   }

   public final CompoundTag saveWithFullMetadata(HolderLookup.Provider p_323767_) {
      CompoundTag compoundtag;
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_323767_);
         this.saveWithFullMetadata(tagvalueoutput);
         compoundtag = tagvalueoutput.buildResult();
      }

      return compoundtag;
   }

   public void saveWithFullMetadata(ValueOutput p_422554_) {
      this.saveWithoutMetadata(p_422554_);
      this.saveMetadata(p_422554_);
   }

   public void saveWithId(ValueOutput p_421636_) {
      this.saveWithoutMetadata(p_421636_);
      this.saveId(p_421636_);
   }

   public final CompoundTag saveWithoutMetadata(HolderLookup.Provider p_324030_) {
      CompoundTag compoundtag;
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_324030_);
         this.saveWithoutMetadata(tagvalueoutput);
         compoundtag = tagvalueoutput.buildResult();
      }

      return compoundtag;
   }

   public void saveWithoutMetadata(ValueOutput p_422668_) {
      this.saveAdditional(p_422668_);
      p_422668_.store("components", DataComponentMap.CODEC, this.components);
   }

   public final CompoundTag saveCustomOnly(HolderLookup.Provider p_338656_) {
      CompoundTag compoundtag;
      try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(this.problemPath(), LOGGER)) {
         TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, p_338656_);
         this.saveCustomOnly(tagvalueoutput);
         compoundtag = tagvalueoutput.buildResult();
      }

      return compoundtag;
   }

   public void saveCustomOnly(ValueOutput p_422286_) {
      this.saveAdditional(p_422286_);
   }

   private void saveId(ValueOutput p_422237_) {
      addEntityType(p_422237_, this.getType());
   }

   public static void addEntityType(ValueOutput p_422078_, BlockEntityType<?> p_187470_) {
      p_422078_.store("id", TYPE_CODEC, p_187470_);
   }

   private void saveMetadata(ValueOutput p_422642_) {
      this.saveId(p_422642_);
      p_422642_.putInt("x", this.worldPosition.getX());
      p_422642_.putInt("y", this.worldPosition.getY());
      p_422642_.putInt("z", this.worldPosition.getZ());
   }

   @Nullable
   public static BlockEntity loadStatic(BlockPos p_155242_, BlockState p_155243_, CompoundTag p_155244_, HolderLookup.Provider p_323542_) {
      BlockEntityType<?> blockentitytype = p_155244_.<BlockEntityType<?>>read("id", TYPE_CODEC).orElse(null);
      if (blockentitytype == null) {
         LOGGER.error("Skipping block entity with invalid type: {}", p_155244_.get("id"));
         return null;
      } else {
         BlockEntity blockentity;
         try {
            blockentity = blockentitytype.create(p_155242_, p_155243_);
         } catch (Throwable var13) {
            LOGGER.error("Failed to create block entity {} for block {} at position {} ", new Object[]{blockentitytype, p_155242_, p_155243_, var13});
            return null;
         }

         try {
            BlockEntity blockentity1;
            try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(blockentity.problemPath(), LOGGER)) {
               blockentity.loadWithComponents(TagValueInput.create(problemreporter$scopedcollector, p_323542_, p_155244_));
               blockentity1 = blockentity;
            }

            return blockentity1;
         } catch (Throwable var12) {
            LOGGER.error("Failed to load data for block entity {} for block {} at position {}", new Object[]{blockentitytype, p_155242_, p_155243_, var12});
            return null;
         }
      }
   }

   public void setChanged() {
      if (this.level != null) {
         setChanged(this.level, this.worldPosition, this.blockState);
      }
   }

   protected static void setChanged(Level p_155233_, BlockPos p_155234_, BlockState p_155235_) {
      p_155233_.blockEntityChanged(p_155234_);
      if (!p_155235_.isAir()) {
         p_155233_.updateNeighbourForOutputSignal(p_155234_, p_155235_.getBlock());
      }
   }

   public BlockPos getBlockPos() {
      return this.worldPosition;
   }

   public BlockState getBlockState() {
      return this.blockState;
   }

   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return null;
   }

   public CompoundTag getUpdateTag(HolderLookup.Provider p_323910_) {
      return new CompoundTag();
   }

   public boolean isRemoved() {
      return this.remove;
   }

   public void setRemoved() {
      this.remove = true;
      this.invalidateCapabilities();
      this.requestModelDataUpdate();
   }

   public void clearRemoved() {
      this.remove = false;
      this.invalidateCapabilities();
   }

   public void preRemoveSideEffects(BlockPos p_394577_, BlockState p_394161_) {
      if (this instanceof Container container && this.level != null) {
         Containers.dropContents(this.level, p_394577_, container);
      }
   }

   public boolean triggerEvent(int p_58889_, int p_58890_) {
      return false;
   }

   public void fillCrashReportCategory(CrashReportCategory p_58887_) {
      p_58887_.setDetail("Name", this::getNameForReporting);
      p_58887_.setDetail("Cached block", this.getBlockState()::toString);
      if (this.level == null) {
         p_58887_.setDetail("Block location", () -> this.worldPosition + " (world missing)");
      } else {
         p_58887_.setDetail("Actual block", this.level.getBlockState(this.worldPosition)::toString);
         CrashReportCategory.populateBlockLocationDetails(p_58887_, this.level, this.worldPosition);
      }
   }

   public String getNameForReporting() {
      return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.getType()) + " // " + this.getClass().getCanonicalName();
   }

   public BlockEntityType<?> getType() {
      return this.type;
   }

   @Override
   public CompoundTag getPersistentData() {
      if (this.customPersistentData == null) {
         this.customPersistentData = new CompoundTag();
      }

      return this.customPersistentData;
   }

   @Nullable
   @Override
   public final <T> T setData(AttachmentType<T> type, T data) {
      this.setChanged();
      return super.setData(type, data);
   }

   @Nullable
   @Override
   public final <T> T removeData(AttachmentType<T> type) {
      this.setChanged();
      return super.removeData(type);
   }

   @Override
   public final void syncData(AttachmentType<?> type) {
      AttachmentSync.syncBlockEntityUpdate(this, type);
   }

   @Deprecated
   public void setBlockState(BlockState p_155251_) {
      this.validateBlockState(p_155251_);
      this.blockState = p_155251_;
   }

   protected void applyImplicitComponents(DataComponentGetter p_397929_) {
   }

   public final void applyComponentsFromItemStack(ItemStack p_338428_) {
      this.applyComponents(p_338428_.getPrototype(), p_338428_.getComponentsPatch());
   }

   public final void applyComponents(DataComponentMap p_330364_, DataComponentPatch p_338381_) {
      final Set<DataComponentType<?>> set = new HashSet<>();
      set.add(DataComponents.BLOCK_ENTITY_DATA);
      set.add(DataComponents.BLOCK_STATE);
      final DataComponentMap datacomponentmap = PatchedDataComponentMap.fromPatch(p_330364_, p_338381_);
      this.applyImplicitComponents(new DataComponentGetter() {
         @Nullable
         @Override
         public <T> T get(DataComponentType<? extends T> p_338266_) {
            set.add(p_338266_);
            return datacomponentmap.get(p_338266_);
         }

         @Override
         public <T> T getOrDefault(DataComponentType<? extends T> p_338358_, T p_338352_) {
            set.add(p_338358_);
            return datacomponentmap.getOrDefault(p_338358_, p_338352_);
         }
      });
      DataComponentPatch datacomponentpatch = p_338381_.forget(set::contains);
      this.components = datacomponentpatch.split().added();
   }

   protected void collectImplicitComponents(DataComponentMap.Builder p_338210_) {
   }

   @Deprecated
   public void removeComponentsFromTag(ValueOutput p_422208_) {
   }

   public final DataComponentMap collectComponents() {
      DataComponentMap.Builder datacomponentmap$builder = DataComponentMap.builder();
      datacomponentmap$builder.addAll(this.components);
      this.collectImplicitComponents(datacomponentmap$builder);
      return datacomponentmap$builder.build();
   }

   public DataComponentMap components() {
      return this.components;
   }

   public void setComponents(DataComponentMap p_338529_) {
      this.components = p_338529_;
   }

   @Nullable
   public static Component parseCustomNameSafe(ValueInput p_422468_, String p_422388_) {
      return p_422468_.<Component>read(p_422388_, ComponentSerialization.CODEC).orElse(null);
   }

   public ProblemReporter.PathElement problemPath() {
      return new BlockEntity.BlockEntityPathElement(this);
   }

   @Override
   public void registerDebugValues(ServerLevel p_449833_, DebugValueSource.Registration p_449301_) {
   }

   record BlockEntityPathElement(BlockEntity blockEntity) implements ProblemReporter.PathElement {
      @Override
      public String get() {
         return this.blockEntity.getNameForReporting() + "@" + this.blockEntity.getBlockPos();
      }
   }
}

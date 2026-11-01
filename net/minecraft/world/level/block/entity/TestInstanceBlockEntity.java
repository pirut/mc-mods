package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.gametest.framework.FailedTestTracker;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.gametest.framework.RetryOptions;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestCommand;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.FileUtil;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class TestInstanceBlockEntity extends BlockEntity implements BeaconBeamOwner, BoundingBoxRenderable {
   private static final Component INVALID_TEST_NAME = Component.translatable("test_instance_block.invalid_test");
   private static final List<BeaconBeamOwner.Section> BEAM_CLEARED = List.of();
   private static final List<BeaconBeamOwner.Section> BEAM_RUNNING = List.of(new BeaconBeamOwner.Section(ARGB.color(128, 128, 128)));
   private static final List<BeaconBeamOwner.Section> BEAM_SUCCESS = List.of(new BeaconBeamOwner.Section(ARGB.color(0, 255, 0)));
   private static final List<BeaconBeamOwner.Section> BEAM_REQUIRED_FAILED = List.of(new BeaconBeamOwner.Section(ARGB.color(255, 0, 0)));
   private static final List<BeaconBeamOwner.Section> BEAM_OPTIONAL_FAILED = List.of(new BeaconBeamOwner.Section(ARGB.color(255, 128, 0)));
   private static final Vec3i STRUCTURE_OFFSET = new Vec3i(0, 1, 1);
   private TestInstanceBlockEntity.Data data;
   private final List<TestInstanceBlockEntity.ErrorMarker> errorMarkers = new ArrayList<>();

   public TestInstanceBlockEntity(BlockPos p_398018_, BlockState p_397894_) {
      super(BlockEntityType.TEST_INSTANCE_BLOCK, p_398018_, p_397894_);
      this.data = new TestInstanceBlockEntity.Data(Optional.empty(), Vec3i.ZERO, Rotation.NONE, false, TestInstanceBlockEntity.Status.CLEARED, Optional.empty());
   }

   public void set(TestInstanceBlockEntity.Data p_397682_) {
      this.data = p_397682_;
      this.setChanged();
   }

   public static Optional<Vec3i> getStructureSize(ServerLevel p_397895_, ResourceKey<GameTestInstance> p_397299_) {
      return getStructureTemplate(p_397895_, p_397299_).map(StructureTemplate::getSize);
   }

   public BoundingBox getStructureBoundingBox() {
      BlockPos blockpos = this.getStructurePos();
      BlockPos blockpos1 = blockpos.offset(this.getTransformedSize()).offset(-1, -1, -1);
      return BoundingBox.fromCorners(blockpos, blockpos1);
   }

   public AABB getStructureBounds() {
      return AABB.of(this.getStructureBoundingBox());
   }

   private static Optional<StructureTemplate> getStructureTemplate(ServerLevel p_397539_, ResourceKey<GameTestInstance> p_397417_) {
      return p_397539_.registryAccess()
         .get(p_397417_)
         .map(p_466693_ -> p_466693_.value().structure())
         .flatMap(p_466695_ -> p_397539_.getStructureManager().get(p_466695_));
   }

   public Optional<ResourceKey<GameTestInstance>> test() {
      return this.data.test();
   }

   public Component getTestName() {
      return this.test().map(p_466696_ -> Component.literal(p_466696_.identifier().toString())).orElse(INVALID_TEST_NAME);
   }

   private Optional<Holder.Reference<GameTestInstance>> getTestHolder() {
      return this.test().flatMap(this.level.registryAccess()::get);
   }

   public boolean ignoreEntities() {
      return this.data.ignoreEntities();
   }

   public Vec3i getSize() {
      return this.data.size();
   }

   public Rotation getRotation() {
      return this.getTestHolder().map(Holder::value).map(GameTestInstance::rotation).orElse(Rotation.NONE).getRotated(this.data.rotation());
   }

   public Optional<Component> errorMessage() {
      return this.data.errorMessage();
   }

   public void setErrorMessage(Component p_397179_) {
      this.set(this.data.withError(p_397179_));
   }

   public void setSuccess() {
      this.set(this.data.withStatus(TestInstanceBlockEntity.Status.FINISHED));
   }

   public void setRunning() {
      this.set(this.data.withStatus(TestInstanceBlockEntity.Status.RUNNING));
   }

   @Override
   public void setChanged() {
      super.setChanged();
      if (this.level instanceof ServerLevel) {
         this.level.sendBlockUpdated(this.getBlockPos(), Blocks.AIR.defaultBlockState(), this.getBlockState(), 3);
      }
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider p_397498_) {
      return this.saveCustomOnly(p_397498_);
   }

   @Override
   protected void loadAdditional(ValueInput p_421695_) {
      p_421695_.<TestInstanceBlockEntity.Data>read("data", TestInstanceBlockEntity.Data.CODEC).ifPresent(this::set);
      this.errorMarkers.clear();
      this.errorMarkers
         .addAll(
            p_421695_.<List<? extends TestInstanceBlockEntity.ErrorMarker>>read("errors", TestInstanceBlockEntity.ErrorMarker.LIST_CODEC).orElse(List.of())
         );
   }

   @Override
   protected void saveAdditional(ValueOutput p_421621_) {
      p_421621_.store("data", TestInstanceBlockEntity.Data.CODEC, this.data);
      if (!this.errorMarkers.isEmpty()) {
         p_421621_.store("errors", TestInstanceBlockEntity.ErrorMarker.LIST_CODEC, this.errorMarkers);
      }
   }

   @Override
   public BoundingBoxRenderable.Mode renderMode() {
      return BoundingBoxRenderable.Mode.BOX;
   }

   public BlockPos getStructurePos() {
      return getStructurePos(this.getBlockPos());
   }

   public static BlockPos getStructurePos(BlockPos p_397863_) {
      return p_397863_.offset(STRUCTURE_OFFSET);
   }

   @Override
   public BoundingBoxRenderable.RenderableBox getRenderableBox() {
      return new BoundingBoxRenderable.RenderableBox(new BlockPos(STRUCTURE_OFFSET), this.getTransformedSize());
   }

   @Override
   public List<BeaconBeamOwner.Section> getBeamSections() {
      return switch (this.data.status()) {
         case CLEARED -> BEAM_CLEARED;
         case RUNNING -> BEAM_RUNNING;
         case FINISHED -> this.errorMessage().isEmpty()
            ? BEAM_SUCCESS
            : (this.getTestHolder().map(Holder::value).map(GameTestInstance::required).orElse(true) ? BEAM_REQUIRED_FAILED : BEAM_OPTIONAL_FAILED);
      };
   }

   private Vec3i getTransformedSize() {
      Vec3i vec3i = this.getSize();
      Rotation rotation = this.getRotation();
      boolean flag = rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
      int i = flag ? vec3i.getZ() : vec3i.getX();
      int j = flag ? vec3i.getX() : vec3i.getZ();
      return new Vec3i(i, vec3i.getY(), j);
   }

   public void resetTest(Consumer<Component> p_397939_) {
      this.removeBarriers();
      this.clearErrorMarkers();
      boolean flag = this.placeStructure();
      if (flag) {
         p_397939_.accept(Component.translatable("test_instance_block.reset_success", this.getTestName()).withStyle(ChatFormatting.GREEN));
      }

      this.set(this.data.withStatus(TestInstanceBlockEntity.Status.CLEARED));
   }

   public Optional<Identifier> saveTest(Consumer<Component> p_398041_) {
      Optional<Holder.Reference<GameTestInstance>> optional = this.getTestHolder();
      Optional<Identifier> optional1;
      if (optional.isPresent()) {
         optional1 = Optional.of(optional.get().value().structure());
      } else {
         optional1 = this.test().map(ResourceKey::identifier);
      }

      if (optional1.isEmpty()) {
         BlockPos blockpos = this.getBlockPos();
         p_398041_.accept(
            Component.translatable("test_instance_block.error.unable_to_save", blockpos.getX(), blockpos.getY(), blockpos.getZ()).withStyle(ChatFormatting.RED)
         );
         return optional1;
      } else {
         if (this.level instanceof ServerLevel serverlevel) {
            StructureBlockEntity.saveStructure(
               serverlevel, optional1.get(), this.getStructurePos(), this.getSize(), this.ignoreEntities(), "", true, List.of(Blocks.AIR)
            );
         }

         return optional1;
      }
   }

   public boolean exportTest(Consumer<Component> p_396997_) {
      Optional<Identifier> optional = this.saveTest(p_396997_);
      return !optional.isEmpty() && this.level instanceof ServerLevel serverlevel ? export(serverlevel, optional.get(), p_396997_) : false;
   }

   public static boolean export(ServerLevel p_397725_, Identifier p_469242_, Consumer<Component> p_397846_) {
      Path path = StructureUtils.testStructuresDir;
      Path path1 = p_397725_.getStructureManager().createAndValidatePathToGeneratedStructure(p_469242_, ".nbt");
      Path path2 = NbtToSnbt.convertStructure(CachedOutput.NO_CACHE, path1, p_469242_.getPath(), path.resolve(p_469242_.getNamespace()).resolve("structure"));
      if (path2 == null) {
         p_397846_.accept(Component.literal("Failed to export " + path1).withStyle(ChatFormatting.RED));
         return true;
      } else {
         try {
            FileUtil.createDirectoriesSafe(path2.getParent());
         } catch (IOException var7) {
            p_397846_.accept(Component.literal("Could not create folder " + path2.getParent()).withStyle(ChatFormatting.RED));
            return true;
         }

         p_397846_.accept(Component.literal("Exported " + p_469242_ + " to " + path2.toAbsolutePath()));
         return false;
      }
   }

   public void runTest(Consumer<Component> p_397726_) {
      if (this.level instanceof ServerLevel serverlevel) {
         Optional optional = this.getTestHolder();
         BlockPos $$4 = this.getBlockPos();
         if (optional.isEmpty()) {
            p_397726_.accept(Component.translatable("test_instance_block.error.no_test", $$4.getX(), $$4.getY(), $$4.getZ()).withStyle(ChatFormatting.RED));
         } else if (!this.placeStructure()) {
            p_397726_.accept(
               Component.translatable("test_instance_block.error.no_test_structure", $$4.getX(), $$4.getY(), $$4.getZ()).withStyle(ChatFormatting.RED)
            );
         } else {
            this.clearErrorMarkers();
            GameTestTicker.SINGLETON.clear();
            FailedTestTracker.forgetFailedTests();
            p_397726_.accept(Component.translatable("test_instance_block.starting", ((Holder.Reference)optional.get()).getRegisteredName()));
            GameTestInfo gametestinfo = new GameTestInfo(
               (Holder.Reference<GameTestInstance>)optional.get(), this.data.rotation(), serverlevel, RetryOptions.noRetries()
            );
            gametestinfo.setTestBlockPos($$4);
            GameTestRunner gametestrunner = GameTestRunner.Builder.fromInfo(List.of(gametestinfo), serverlevel).build();
            TestCommand.trackAndStartRunner(serverlevel.getServer().createCommandSourceStack(), gametestrunner);
         }
      }
   }

   public boolean placeStructure() {
      if (this.level instanceof ServerLevel serverlevel) {
         Optional<StructureTemplate> optional = this.data
            .test()
            .flatMap(p_397292_ -> getStructureTemplate(serverlevel, (ResourceKey<GameTestInstance>)p_397292_));
         if (optional.isPresent()) {
            this.placeStructure(serverlevel, optional.get());
            return true;
         }
      }

      return false;
   }

   private void placeStructure(ServerLevel p_397984_, StructureTemplate p_397168_) {
      StructurePlaceSettings structureplacesettings = new StructurePlaceSettings()
         .setRotation(this.getRotation())
         .setIgnoreEntities(this.data.ignoreEntities())
         .setKnownShape(true);
      BlockPos blockpos = this.getStartCorner();
      this.forceLoadChunks();
      StructureUtils.clearSpaceForStructure(this.getStructureBoundingBox(), p_397984_);
      this.removeEntities();
      p_397168_.placeInWorld(p_397984_, blockpos, blockpos, structureplacesettings, p_397984_.getRandom(), 818);
   }

   private void removeEntities() {
      this.level.getEntities(null, this.getStructureBounds()).stream().filter(p_399456_ -> !(p_399456_ instanceof Player)).forEach(Entity::discard);
   }

   private void forceLoadChunks() {
      if (this.level instanceof ServerLevel serverlevel) {
         this.getStructureBoundingBox().intersectingChunks().forEach(p_397470_ -> serverlevel.setChunkForced(p_397470_.x, p_397470_.z, true));
      }
   }

   public BlockPos getStartCorner() {
      Vec3i vec3i = this.getSize();
      Rotation rotation = this.getRotation();
      BlockPos blockpos = this.getStructurePos();

      return switch (rotation) {
         case NONE -> blockpos;
         case CLOCKWISE_90 -> blockpos.offset(vec3i.getZ() - 1, 0, 0);
         case CLOCKWISE_180 -> blockpos.offset(vec3i.getX() - 1, 0, vec3i.getZ() - 1);
         case COUNTERCLOCKWISE_90 -> blockpos.offset(0, 0, vec3i.getX() - 1);
      };
   }

   public void encaseStructure() {
      this.processStructureBoundary(p_397469_ -> {
         if (!this.level.getBlockState(p_397469_).is(Blocks.TEST_INSTANCE_BLOCK)) {
            this.level.setBlockAndUpdate(p_397469_, Blocks.BARRIER.defaultBlockState());
         }
      });
   }

   public void removeBarriers() {
      this.processStructureBoundary(p_398011_ -> {
         if (this.level.getBlockState(p_398011_).is(Blocks.BARRIER)) {
            this.level.setBlockAndUpdate(p_398011_, Blocks.AIR.defaultBlockState());
         }
      });
   }

   public void processStructureBoundary(Consumer<BlockPos> p_397268_) {
      AABB aabb = this.getStructureBounds();
      boolean flag = !this.getTestHolder().map(p_397339_ -> p_397339_.value().skyAccess()).orElse(false);
      BlockPos blockpos = BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ).offset(-1, -1, -1);
      BlockPos blockpos1 = BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ);
      BlockPos.betweenClosedStream(blockpos, blockpos1)
         .forEach(
            p_397351_ -> {
               boolean flag1 = p_397351_.getX() == blockpos.getX()
                  || p_397351_.getX() == blockpos1.getX()
                  || p_397351_.getZ() == blockpos.getZ()
                  || p_397351_.getZ() == blockpos1.getZ()
                  || p_397351_.getY() == blockpos.getY();
               boolean flag2 = p_397351_.getY() == blockpos1.getY();
               if (flag1 || flag2 && flag) {
                  p_397268_.accept(p_397351_);
               }
            }
         );
   }

   public void markError(BlockPos p_449802_, Component p_449313_) {
      this.errorMarkers.add(new TestInstanceBlockEntity.ErrorMarker(p_449802_, p_449313_));
      this.setChanged();
   }

   public void clearErrorMarkers() {
      if (!this.errorMarkers.isEmpty()) {
         this.errorMarkers.clear();
         this.setChanged();
      }
   }

   public List<TestInstanceBlockEntity.ErrorMarker> getErrorMarkers() {
      return this.errorMarkers;
   }

   public record Data(
      Optional<ResourceKey<GameTestInstance>> test,
      Vec3i size,
      Rotation rotation,
      boolean ignoreEntities,
      TestInstanceBlockEntity.Status status,
      Optional<Component> errorMessage
   ) {
      public static final Codec<TestInstanceBlockEntity.Data> CODEC = RecordCodecBuilder.create(
         p_397596_ -> p_397596_.group(
               ResourceKey.codec(Registries.TEST_INSTANCE).optionalFieldOf("test").forGetter(TestInstanceBlockEntity.Data::test),
               Vec3i.CODEC.fieldOf("size").forGetter(TestInstanceBlockEntity.Data::size),
               Rotation.CODEC.fieldOf("rotation").forGetter(TestInstanceBlockEntity.Data::rotation),
               Codec.BOOL.fieldOf("ignore_entities").forGetter(TestInstanceBlockEntity.Data::ignoreEntities),
               TestInstanceBlockEntity.Status.CODEC.fieldOf("status").forGetter(TestInstanceBlockEntity.Data::status),
               ComponentSerialization.CODEC.optionalFieldOf("error_message").forGetter(TestInstanceBlockEntity.Data::errorMessage)
            )
            .apply(p_397596_, TestInstanceBlockEntity.Data::new)
      );
      public static final StreamCodec<RegistryFriendlyByteBuf, TestInstanceBlockEntity.Data> STREAM_CODEC = StreamCodec.composite(
         ByteBufCodecs.optional(ResourceKey.streamCodec(Registries.TEST_INSTANCE)),
         TestInstanceBlockEntity.Data::test,
         Vec3i.STREAM_CODEC,
         TestInstanceBlockEntity.Data::size,
         Rotation.STREAM_CODEC,
         TestInstanceBlockEntity.Data::rotation,
         ByteBufCodecs.BOOL,
         TestInstanceBlockEntity.Data::ignoreEntities,
         TestInstanceBlockEntity.Status.STREAM_CODEC,
         TestInstanceBlockEntity.Data::status,
         ByteBufCodecs.optional(ComponentSerialization.STREAM_CODEC),
         TestInstanceBlockEntity.Data::errorMessage,
         TestInstanceBlockEntity.Data::new
      );

      public TestInstanceBlockEntity.Data withSize(Vec3i p_397709_) {
         return new TestInstanceBlockEntity.Data(this.test, p_397709_, this.rotation, this.ignoreEntities, this.status, this.errorMessage);
      }

      public TestInstanceBlockEntity.Data withStatus(TestInstanceBlockEntity.Status p_398013_) {
         return new TestInstanceBlockEntity.Data(this.test, this.size, this.rotation, this.ignoreEntities, p_398013_, Optional.empty());
      }

      public TestInstanceBlockEntity.Data withError(Component p_397453_) {
         return new TestInstanceBlockEntity.Data(
            this.test, this.size, this.rotation, this.ignoreEntities, TestInstanceBlockEntity.Status.FINISHED, Optional.of(p_397453_)
         );
      }
   }

   public record ErrorMarker(BlockPos pos, Component text) {
      public static final Codec<TestInstanceBlockEntity.ErrorMarker> CODEC = RecordCodecBuilder.create(
         p_449367_ -> p_449367_.group(
               BlockPos.CODEC.fieldOf("pos").forGetter(TestInstanceBlockEntity.ErrorMarker::pos),
               ComponentSerialization.CODEC.fieldOf("text").forGetter(TestInstanceBlockEntity.ErrorMarker::text)
            )
            .apply(p_449367_, TestInstanceBlockEntity.ErrorMarker::new)
      );
      public static final Codec<List<TestInstanceBlockEntity.ErrorMarker>> LIST_CODEC = CODEC.listOf();
   }

   public static enum Status implements StringRepresentable {
      CLEARED("cleared", 0),
      RUNNING("running", 1),
      FINISHED("finished", 2);

      private static final IntFunction<TestInstanceBlockEntity.Status> ID_MAP = ByIdMap.continuous(
         p_397441_ -> p_397441_.index, values(), ByIdMap.OutOfBoundsStrategy.ZERO
      );
      public static final Codec<TestInstanceBlockEntity.Status> CODEC = StringRepresentable.fromEnum(TestInstanceBlockEntity.Status::values);
      public static final StreamCodec<ByteBuf, TestInstanceBlockEntity.Status> STREAM_CODEC = ByteBufCodecs.idMapper(
         TestInstanceBlockEntity.Status::byIndex, p_397803_ -> p_397803_.index
      );
      private final String id;
      private final int index;

      private Status(String p_397410_, int p_397632_) {
         this.id = p_397410_;
         this.index = p_397632_;
      }

      @Override
      public String getSerializedName() {
         return this.id;
      }

      public static TestInstanceBlockEntity.Status byIndex(int p_397227_) {
         return ID_MAP.apply(p_397227_);
      }
   }
}

package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.extensions.GameTestHelperExtension;
import org.jspecify.annotations.Nullable;

public class GameTestHelper implements GameTestHelperExtension {
   public final GameTestInfo testInfo;
   private boolean finalCheckAdded;

   public GameTestHelper(GameTestInfo p_127597_) {
      this.testInfo = p_127597_;
   }

   public GameTestAssertException assertionException(Component p_397303_) {
      return new GameTestAssertException(p_397303_, this.testInfo.getTick());
   }

   public GameTestAssertException assertionException(String p_397745_, Object... p_397507_) {
      return this.assertionException(Component.translatableEscape(p_397745_, p_397507_));
   }

   public GameTestAssertPosException assertionException(BlockPos p_398006_, Component p_397633_) {
      return new GameTestAssertPosException(p_397633_, this.absolutePos(p_398006_), p_398006_, this.testInfo.getTick());
   }

   public GameTestAssertPosException assertionException(BlockPos p_397073_, String p_398019_, Object... p_397677_) {
      return this.assertionException(p_397073_, Component.translatableEscape(p_398019_, p_397677_));
   }

   public ServerLevel getLevel() {
      return this.testInfo.getLevel();
   }

   public BlockState getBlockState(BlockPos p_177233_) {
      return this.getLevel().getBlockState(this.absolutePos(p_177233_));
   }

   public <T extends BlockEntity> T getBlockEntity(BlockPos p_177348_, Class<T> p_397272_) {
      BlockEntity blockentity = this.getLevel().getBlockEntity(this.absolutePos(p_177348_));
      if (blockentity == null) {
         throw this.assertionException(p_177348_, "test.error.missing_block_entity");
      } else if (p_397272_.isInstance(blockentity)) {
         return p_397272_.cast(blockentity);
      } else {
         throw this.assertionException(p_177348_, "test.error.wrong_block_entity", blockentity.getType().builtInRegistryHolder().getRegisteredName());
      }
   }

   public void killAllEntities() {
      this.killAllEntitiesOfClass(Entity.class);
   }

   public void killAllEntitiesOfClass(Class<? extends Entity> p_289538_) {
      AABB aabb = this.getBounds();
      List<? extends Entity> list = this.getLevel().getEntitiesOfClass(p_289538_, aabb.inflate(1.0), p_177131_ -> !(p_177131_ instanceof Player));
      list.forEach(p_375467_ -> p_375467_.kill(this.getLevel()));
   }

   public ItemEntity spawnItem(Item p_326872_, Vec3 p_326853_) {
      ServerLevel serverlevel = this.getLevel();
      Vec3 vec3 = this.absoluteVec(p_326853_);
      ItemEntity itementity = new ItemEntity(serverlevel, vec3.x, vec3.y, vec3.z, new ItemStack(p_326872_, 1));
      itementity.setDeltaMovement(0.0, 0.0, 0.0);
      serverlevel.addFreshEntity(itementity);
      return itementity;
   }

   public ItemEntity spawnItem(Item p_177190_, float p_177191_, float p_177192_, float p_177193_) {
      return this.spawnItem(p_177190_, new Vec3(p_177191_, p_177192_, p_177193_));
   }

   public ItemEntity spawnItem(Item p_251435_, BlockPos p_250287_) {
      return this.spawnItem(p_251435_, p_250287_.getX(), p_250287_.getY(), p_250287_.getZ());
   }

   public <E extends Entity> E spawn(EntityType<E> p_177177_, BlockPos p_177178_) {
      return this.spawn(p_177177_, Vec3.atBottomCenterOf(p_177178_));
   }

   public <E extends Entity> List<E> spawn(EntityType<E> p_434502_, BlockPos p_435888_, int p_436022_) {
      return this.spawn(p_434502_, Vec3.atBottomCenterOf(p_435888_), p_436022_);
   }

   public <E extends Entity> List<E> spawn(EntityType<E> p_433086_, Vec3 p_433822_, int p_434357_) {
      List<E> list = new ArrayList<>();

      for (int i = 0; i < p_434357_; i++) {
         list.add(this.spawn(p_433086_, p_433822_));
      }

      return list;
   }

   public <E extends Entity> E spawn(EntityType<E> p_177174_, Vec3 p_177175_) {
      return this.spawn(p_177174_, p_177175_, null);
   }

   public <E extends Entity> E spawn(EntityType<E> p_456237_, Vec3 p_456275_, @Nullable EntitySpawnReason p_455487_) {
      ServerLevel serverlevel = this.getLevel();
      E e = p_456237_.create(serverlevel, EntitySpawnReason.STRUCTURE);
      if (e == null) {
         throw this.assertionException(BlockPos.containing(p_456275_), "test.error.spawn_failure", p_456237_.builtInRegistryHolder().getRegisteredName());
      } else {
         if (e instanceof Mob mob) {
            mob.setPersistenceRequired();
         }

         Vec3 vec3 = this.absoluteVec(p_456275_);
         float f = e.rotate(this.getTestRotation());
         e.snapTo(vec3.x, vec3.y, vec3.z, f, e.getXRot());
         e.setYBodyRot(f);
         e.setYHeadRot(f);
         if (p_455487_ != null && e instanceof Mob mob1) {
            mob1.finalizeSpawn(this.getLevel(), this.getLevel().getCurrentDifficultyAt(mob1.blockPosition()), p_455487_, null);
         }

         serverlevel.addFreshEntityWithPassengers(e);
         return e;
      }
   }

   public <E extends Mob> E spawn(EntityType<E> p_455592_, int p_455860_, int p_456022_, int p_455923_, EntitySpawnReason p_455210_) {
      return this.spawn(p_455592_, new Vec3(p_455860_, p_456022_, p_455923_), p_455210_);
   }

   public void hurt(Entity p_376743_, DamageSource p_376383_, float p_376544_) {
      p_376743_.hurtServer(this.getLevel(), p_376383_, p_376544_);
   }

   public void kill(Entity p_376706_) {
      p_376706_.kill(this.getLevel());
   }

   public <E extends Entity> E findOneEntity(EntityType<E> p_320756_) {
      return this.findClosestEntity(p_320756_, 0, 0, 0, 2.147483647E9);
   }

   public <E extends Entity> E findClosestEntity(EntityType<E> p_320915_, int p_320152_, int p_319868_, int p_319912_, double p_320874_) {
      List<E> list = this.findEntities(p_320915_, p_320152_, p_319868_, p_319912_, p_320874_);
      if (list.isEmpty()) {
         throw this.assertionException("test.error.expected_entity_around", p_320915_.getDescription(), p_320152_, p_319868_, p_319912_);
      } else if (list.size() > 1) {
         throw this.assertionException("test.error.too_many_entities", p_320915_.toShortString(), p_320152_, p_319868_, p_319912_, list.size());
      } else {
         Vec3 vec3 = this.absoluteVec(new Vec3(p_320152_, p_319868_, p_319912_));
         list.sort((p_319453_, p_319454_) -> {
            double d0 = p_319453_.position().distanceTo(vec3);
            double d1 = p_319454_.position().distanceTo(vec3);
            return Double.compare(d0, d1);
         });
         return list.get(0);
      }
   }

   public <E extends Entity> List<E> findEntities(EntityType<E> p_320065_, int p_320533_, int p_320279_, int p_320721_, double p_320280_) {
      return this.findEntities(p_320065_, Vec3.atBottomCenterOf(new BlockPos(p_320533_, p_320279_, p_320721_)), p_320280_);
   }

   public <E extends Entity> List<E> findEntities(EntityType<E> p_320878_, Vec3 p_320734_, double p_320825_) {
      ServerLevel serverlevel = this.getLevel();
      Vec3 vec3 = this.absoluteVec(p_320734_);
      AABB aabb = this.testInfo.getStructureBounds();
      AABB aabb1 = new AABB(vec3.add(-p_320825_, -p_320825_, -p_320825_), vec3.add(p_320825_, p_320825_, p_320825_));
      return serverlevel.getEntities(p_320878_, aabb, p_319451_ -> p_319451_.getBoundingBox().intersects(aabb1) && p_319451_.isAlive());
   }

   public <E extends Entity> E spawn(EntityType<E> p_177169_, int p_177170_, int p_177171_, int p_177172_) {
      return this.spawn(p_177169_, new BlockPos(p_177170_, p_177171_, p_177172_));
   }

   public <E extends Entity> E spawn(EntityType<E> p_177164_, float p_177165_, float p_177166_, float p_177167_) {
      return this.spawn(p_177164_, new Vec3(p_177165_, p_177166_, p_177167_));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177330_, BlockPos p_177331_) {
      E e = (E)this.spawn(p_177330_, p_177331_);
      e.removeFreeWill();
      return e;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177322_, int p_177323_, int p_177324_, int p_177325_) {
      return this.spawnWithNoFreeWill(p_177322_, new BlockPos(p_177323_, p_177324_, p_177325_));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177327_, Vec3 p_177328_) {
      E e = (E)this.spawn(p_177327_, p_177328_);
      e.removeFreeWill();
      return e;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177317_, float p_177318_, float p_177319_, float p_177320_) {
      return this.spawnWithNoFreeWill(p_177317_, new Vec3(p_177318_, p_177319_, p_177320_));
   }

   public void moveTo(Mob p_326829_, float p_326862_, float p_326823_, float p_326844_) {
      Vec3 vec3 = this.absoluteVec(new Vec3(p_326862_, p_326823_, p_326844_));
      p_326829_.snapTo(vec3.x, vec3.y, vec3.z, p_326829_.getYRot(), p_326829_.getXRot());
   }

   public GameTestSequence walkTo(Mob p_177186_, BlockPos p_177187_, float p_177188_) {
      return this.startSequence().thenExecuteAfter(2, () -> {
         Path path = p_177186_.getNavigation().createPath(this.absolutePos(p_177187_), 0);
         p_177186_.getNavigation().moveTo(path, (double)p_177188_);
      });
   }

   public void pressButton(int p_177104_, int p_177105_, int p_177106_) {
      this.pressButton(new BlockPos(p_177104_, p_177105_, p_177106_));
   }

   public void pressButton(BlockPos p_177386_) {
      this.assertBlockTag(BlockTags.BUTTONS, p_177386_);
      BlockPos blockpos = this.absolutePos(p_177386_);
      BlockState blockstate = this.getLevel().getBlockState(blockpos);
      ButtonBlock buttonblock = (ButtonBlock)blockstate.getBlock();
      buttonblock.press(blockstate, this.getLevel(), blockpos, null);
   }

   public void useBlock(BlockPos p_177409_) {
      this.useBlock(p_177409_, this.makeMockPlayer(GameType.CREATIVE));
   }

   public void useBlock(BlockPos p_250131_, Player p_251507_) {
      BlockPos blockpos = this.absolutePos(p_250131_);
      this.useBlock(p_250131_, p_251507_, new BlockHitResult(Vec3.atCenterOf(blockpos), Direction.NORTH, blockpos, true));
   }

   public void useBlock(BlockPos p_262023_, Player p_261901_, BlockHitResult p_262040_) {
      BlockPos blockpos = this.absolutePos(p_262023_);
      BlockState blockstate = this.getLevel().getBlockState(blockpos);
      InteractionHand interactionhand = InteractionHand.MAIN_HAND;
      InteractionResult interactionresult = blockstate.useItemOn(
         p_261901_.getItemInHand(interactionhand), this.getLevel(), p_261901_, interactionhand, p_262040_
      );
      if (!interactionresult.consumesAction()
         && (
            !(interactionresult instanceof InteractionResult.TryEmptyHandInteraction)
               || !blockstate.useWithoutItem(this.getLevel(), p_261901_, p_262040_).consumesAction()
         )) {
         UseOnContext useoncontext = new UseOnContext(p_261901_, interactionhand, p_262040_);
         p_261901_.getItemInHand(interactionhand).useOn(useoncontext);
      }
   }

   public LivingEntity makeAboutToDrown(LivingEntity p_177184_) {
      p_177184_.setAirSupply(0);
      p_177184_.setHealth(0.25F);
      return p_177184_;
   }

   public LivingEntity withLowHealth(LivingEntity p_286794_) {
      p_286794_.setHealth(0.25F);
      return p_286794_;
   }

   public Player makeMockPlayer(final GameType p_323908_) {
      return new Player(this.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player")) {
         @Override
         public GameType gameMode() {
            return p_323908_;
         }

         @Override
         public boolean isClientAuthoritative() {
            return false;
         }
      };
   }

   @Deprecated(
      forRemoval = true
   )
   public ServerPlayer makeMockServerPlayerInLevel() {
      CommonListenerCookie commonlistenercookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
      ServerPlayer serverplayer = new ServerPlayer(
         this.getLevel().getServer(), this.getLevel(), commonlistenercookie.gameProfile(), commonlistenercookie.clientInformation()
      ) {
         @Override
         public GameType gameMode() {
            return GameType.CREATIVE;
         }
      };
      Connection connection = new Connection(PacketFlow.SERVERBOUND);
      new EmbeddedChannel(new ChannelHandler[]{connection});
      this.getLevel().getServer().getPlayerList().placeNewPlayer(connection, serverplayer, commonlistenercookie);
      return serverplayer;
   }

   public void pullLever(int p_177303_, int p_177304_, int p_177305_) {
      this.pullLever(new BlockPos(p_177303_, p_177304_, p_177305_));
   }

   public void pullLever(BlockPos p_177422_) {
      this.assertBlockPresent(Blocks.LEVER, p_177422_);
      BlockPos blockpos = this.absolutePos(p_177422_);
      BlockState blockstate = this.getLevel().getBlockState(blockpos);
      LeverBlock leverblock = (LeverBlock)blockstate.getBlock();
      leverblock.pull(blockstate, this.getLevel(), blockpos, null);
   }

   public void pulseRedstone(BlockPos p_177235_, long p_177236_) {
      this.setBlock(p_177235_, Blocks.REDSTONE_BLOCK);
      this.runAfterDelay(p_177236_, () -> this.setBlock(p_177235_, Blocks.AIR));
   }

   public void destroyBlock(BlockPos p_177435_) {
      this.getLevel().destroyBlock(this.absolutePos(p_177435_), false, null);
   }

   public void setBlock(int p_177108_, int p_177109_, int p_177110_, Block p_177111_) {
      this.setBlock(new BlockPos(p_177108_, p_177109_, p_177110_), p_177111_);
   }

   public void setBlock(int p_177113_, int p_177114_, int p_177115_, BlockState p_177116_) {
      this.setBlock(new BlockPos(p_177113_, p_177114_, p_177115_), p_177116_);
   }

   public void setBlock(BlockPos p_177246_, Block p_177247_) {
      this.setBlock(p_177246_, p_177247_.defaultBlockState());
   }

   public void setBlock(BlockPos p_177253_, BlockState p_177254_) {
      this.getLevel().setBlock(this.absolutePos(p_177253_), p_177254_, 3);
   }

   public void setBlock(BlockPos p_435712_, Block p_434627_, Direction p_434013_) {
      this.setBlock(p_435712_, p_434627_.defaultBlockState(), p_434013_);
   }

   public void setBlock(BlockPos p_435991_, BlockState p_435469_, Direction p_433761_) {
      BlockState blockstate = p_435469_;
      if (p_435469_.hasProperty(HorizontalDirectionalBlock.FACING)) {
         blockstate = p_435469_.setValue(HorizontalDirectionalBlock.FACING, p_433761_);
      }

      if (p_435469_.hasProperty(BlockStateProperties.FACING)) {
         blockstate = p_435469_.setValue(BlockStateProperties.FACING, p_433761_);
      }

      this.getLevel().setBlock(this.absolutePos(p_435991_), blockstate, 3);
   }

   public void assertBlockPresent(Block p_177204_, int p_177205_, int p_177206_, int p_177207_) {
      this.assertBlockPresent(p_177204_, new BlockPos(p_177205_, p_177206_, p_177207_));
   }

   public void assertBlockPresent(Block p_177209_, BlockPos p_177210_) {
      BlockState blockstate = this.getBlockState(p_177210_);
      this.assertBlock(
         p_177210_,
         p_177216_ -> blockstate.is(p_177209_),
         p_396374_ -> Component.translatable("test.error.expected_block", p_177209_.getName(), p_396374_.getName())
      );
   }

   public void assertBlockNotPresent(Block p_177337_, int p_177338_, int p_177339_, int p_177340_) {
      this.assertBlockNotPresent(p_177337_, new BlockPos(p_177338_, p_177339_, p_177340_));
   }

   public void assertBlockNotPresent(Block p_177342_, BlockPos p_177343_) {
      this.assertBlock(
         p_177343_,
         p_177251_ -> !this.getBlockState(p_177343_).is(p_177342_),
         p_396386_ -> Component.translatable("test.error.unexpected_block", p_177342_.getName())
      );
   }

   public void assertBlockTag(TagKey<Block> p_397176_, BlockPos p_397607_) {
      this.assertBlockState(
         p_397607_,
         p_396384_ -> p_396384_.is(p_397176_),
         p_466091_ -> Component.translatable("test.error.expected_block_tag", Component.translationArg(p_397176_.location()), p_466091_.getBlock().getName())
      );
   }

   public void succeedWhenBlockPresent(Block p_177378_, int p_177379_, int p_177380_, int p_177381_) {
      this.succeedWhenBlockPresent(p_177378_, new BlockPos(p_177379_, p_177380_, p_177381_));
   }

   public void succeedWhenBlockPresent(Block p_177383_, BlockPos p_177384_) {
      this.succeedWhen(() -> this.assertBlockPresent(p_177383_, p_177384_));
   }

   public void assertBlock(BlockPos p_177272_, Predicate<Block> p_177273_, Function<Block, Component> p_397415_) {
      this.assertBlockState(p_177272_, p_177296_ -> p_177273_.test(p_177296_.getBlock()), p_396382_ -> p_397415_.apply(p_396382_.getBlock()));
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos p_177256_, Property<T> p_177257_, T p_177258_) {
      BlockState blockstate = this.getBlockState(p_177256_);
      boolean flag = blockstate.hasProperty(p_177257_);
      if (!flag) {
         throw this.assertionException(p_177256_, "test.error.block_property_missing", p_177257_.getName(), p_177258_);
      } else if (!blockstate.<T>getValue(p_177257_).equals(p_177258_)) {
         throw this.assertionException(p_177256_, "test.error.block_property_mismatch", p_177257_.getName(), p_177258_, blockstate.getValue(p_177257_));
      }
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos p_177260_, Property<T> p_177261_, Predicate<T> p_177262_, Component p_397574_) {
      this.assertBlockState(p_177260_, p_277264_ -> {
         if (!p_277264_.hasProperty(p_177261_)) {
            return false;
         } else {
            T t = p_277264_.getValue(p_177261_);
            return p_177262_.test(t);
         }
      }, p_397919_ -> p_397574_);
   }

   public void assertBlockState(BlockPos p_397259_, BlockState p_397586_) {
      BlockState blockstate = this.getBlockState(p_397259_);
      if (!blockstate.equals(p_397586_)) {
         throw this.assertionException(p_397259_, "test.error.state_not_equal", p_397586_, blockstate);
      }
   }

   public void assertBlockState(BlockPos p_177358_, Predicate<BlockState> p_177359_, Function<BlockState, Component> p_397756_) {
      BlockState blockstate = this.getBlockState(p_177358_);
      if (!p_177359_.test(blockstate)) {
         throw this.assertionException(p_177358_, p_397756_.apply(blockstate));
      }
   }

   public <T extends BlockEntity> void assertBlockEntityData(BlockPos p_348547_, Class<T> p_397338_, Predicate<T> p_348669_, Supplier<Component> p_348486_) {
      T t = this.getBlockEntity(p_348547_, p_397338_);
      if (!p_348669_.test(t)) {
         throw this.assertionException(p_348547_, p_348486_.get());
      }
   }

   public void assertRedstoneSignal(BlockPos p_289644_, Direction p_289642_, IntPredicate p_289645_, Supplier<Component> p_289684_) {
      BlockPos blockpos = this.absolutePos(p_289644_);
      ServerLevel serverlevel = this.getLevel();
      BlockState blockstate = serverlevel.getBlockState(blockpos);
      int i = blockstate.getSignal(serverlevel, blockpos, p_289642_);
      if (!p_289645_.test(i)) {
         throw this.assertionException(p_289644_, p_289684_.get());
      }
   }

   public void assertEntityPresent(EntityType<?> p_177157_) {
      if (!this.getLevel().hasEntities(p_177157_, this.getBounds(), Entity::isAlive)) {
         throw this.assertionException("test.error.expected_entity_in_test", p_177157_.getDescription());
      }
   }

   public void assertEntityPresent(EntityType<?> p_177370_, int p_177371_, int p_177372_, int p_177373_) {
      this.assertEntityPresent(p_177370_, new BlockPos(p_177371_, p_177372_, p_177373_));
   }

   public void assertEntityPresent(EntityType<?> p_177375_, BlockPos p_177376_) {
      BlockPos blockpos = this.absolutePos(p_177376_);
      if (!this.getLevel().hasEntities(p_177375_, new AABB(blockpos), Entity::isAlive)) {
         throw this.assertionException(p_177376_, "test.error.expected_entity", p_177375_.getDescription());
      }
   }

   public void assertEntityPresent(EntityType<?> p_252010_, AABB p_364252_) {
      AABB aabb = this.absoluteAABB(p_364252_);
      if (!this.getLevel().hasEntities(p_252010_, aabb, Entity::isAlive)) {
         throw this.assertionException(BlockPos.containing(p_364252_.getCenter()), "test.error.expected_entity", p_252010_.getDescription());
      }
   }

   public void assertEntityPresent(EntityType<?> p_470565_, AABB p_470583_, Component p_470754_) {
      AABB aabb = this.absoluteAABB(p_470583_);
      if (!this.getLevel().hasEntities(p_470565_, aabb, Entity::isAlive)) {
         throw this.assertionException(BlockPos.containing(p_470583_.getCenter()), p_470754_);
      }
   }

   public void assertEntitiesPresent(EntityType<?> p_312835_, int p_312735_) {
      List<? extends Entity> list = this.getLevel().getEntities(p_312835_, this.getBounds(), Entity::isAlive);
      if (list.size() != p_312735_) {
         throw this.assertionException("test.error.expected_entity_count", p_312735_, p_312835_.getDescription(), list.size());
      }
   }

   public void assertEntitiesPresent(EntityType<?> p_239372_, BlockPos p_239373_, int p_239374_, double p_239375_) {
      BlockPos blockpos = this.absolutePos(p_239373_);
      List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)p_239372_, p_239373_, p_239375_);
      if (list.size() != p_239374_) {
         throw this.assertionException(p_239373_, "test.error.expected_entity_count", p_239374_, p_239372_.getDescription(), list.size());
      }
   }

   public void assertEntityPresent(EntityType<?> p_177180_, BlockPos p_177181_, double p_177182_) {
      List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)p_177180_, p_177181_, p_177182_);
      if (list.isEmpty()) {
         BlockPos blockpos = this.absolutePos(p_177181_);
         throw this.assertionException(p_177181_, "test.error.expected_entity", p_177180_.getDescription());
      }
   }

   public <T extends Entity> List<T> getEntities(EntityType<T> p_238400_, BlockPos p_238401_, double p_238402_) {
      BlockPos blockpos = this.absolutePos(p_238401_);
      return this.getLevel().getEntities(p_238400_, new AABB(blockpos).inflate(p_238402_), Entity::isAlive);
   }

   public <T extends Entity> List<T> getEntities(EntityType<T> p_320029_) {
      return this.getLevel().getEntities(p_320029_, this.getBounds(), Entity::isAlive);
   }

   public void assertEntityInstancePresent(Entity p_177133_, int p_177134_, int p_177135_, int p_177136_) {
      this.assertEntityInstancePresent(p_177133_, new BlockPos(p_177134_, p_177135_, p_177136_));
   }

   public void assertEntityInstancePresent(Entity p_177141_, BlockPos p_177142_) {
      BlockPos blockpos = this.absolutePos(p_177142_);
      List<? extends Entity> list = this.getLevel().getEntities(p_177141_.getType(), new AABB(blockpos), Entity::isAlive);
      list.stream()
         .filter(p_177139_ -> p_177139_ == p_177141_)
         .findFirst()
         .orElseThrow(() -> this.assertionException(p_177142_, "test.error.expected_entity", p_177141_.getType().getDescription()));
   }

   public void assertItemEntityCountIs(Item p_177199_, BlockPos p_177200_, double p_177201_, int p_177202_) {
      BlockPos blockpos = this.absolutePos(p_177200_);
      List<ItemEntity> list = this.getLevel().getEntities(EntityType.ITEM, new AABB(blockpos).inflate(p_177201_), Entity::isAlive);
      int i = 0;

      for (ItemEntity itementity : list) {
         ItemStack itemstack = itementity.getItem();
         if (itemstack.is(p_177199_)) {
            i += itemstack.getCount();
         }
      }

      if (i != p_177202_) {
         throw this.assertionException(p_177200_, "test.error.expected_items_count", p_177202_, p_177199_.getName(), i);
      }
   }

   public void assertItemEntityPresent(Item p_177195_, BlockPos p_177196_, double p_177197_) {
      BlockPos blockpos = this.absolutePos(p_177196_);
      Predicate<ItemEntity> predicate = p_445281_ -> p_445281_.isAlive() && p_445281_.getItem().is(p_177195_);
      if (!this.getLevel().hasEntities(EntityType.ITEM, new AABB(blockpos).inflate(p_177197_), predicate)) {
         throw this.assertionException(p_177196_, "test.error.expected_item", p_177195_.getName());
      }
   }

   public void assertItemEntityNotPresent(Item p_236779_, BlockPos p_236780_, double p_236781_) {
      BlockPos blockpos = this.absolutePos(p_236780_);
      Predicate<ItemEntity> predicate = p_445283_ -> p_445283_.isAlive() && p_445283_.getItem().is(p_236779_);
      if (this.getLevel().hasEntities(EntityType.ITEM, new AABB(blockpos).inflate(p_236781_), predicate)) {
         throw this.assertionException(p_236780_, "test.error.unexpected_item", p_236779_.getName());
      }
   }

   public void assertItemEntityPresent(Item p_304857_) {
      Predicate<ItemEntity> predicate = p_445287_ -> p_445287_.isAlive() && p_445287_.getItem().is(p_304857_);
      if (!this.getLevel().hasEntities(EntityType.ITEM, this.getBounds(), predicate)) {
         throw this.assertionException("test.error.expected_item", p_304857_.getName());
      }
   }

   public void assertItemEntityNotPresent(Item p_304474_) {
      Predicate<ItemEntity> predicate = p_445285_ -> p_445285_.isAlive() && p_445285_.getItem().is(p_304474_);
      if (this.getLevel().hasEntities(EntityType.ITEM, this.getBounds(), predicate)) {
         throw this.assertionException("test.error.unexpected_item", p_304474_.getName());
      }
   }

   public void assertEntityNotPresent(EntityType<?> p_177310_) {
      List<? extends Entity> list = this.getLevel().getEntities(p_177310_, this.getBounds(), Entity::isAlive);
      if (!list.isEmpty()) {
         throw this.assertionException(list.getFirst().blockPosition(), "test.error.unexpected_entity", p_177310_.getDescription());
      }
   }

   public void assertEntityNotPresent(EntityType<?> p_177398_, int p_177399_, int p_177400_, int p_177401_) {
      this.assertEntityNotPresent(p_177398_, new BlockPos(p_177399_, p_177400_, p_177401_));
   }

   public void assertEntityNotPresent(EntityType<?> p_177403_, BlockPos p_177404_) {
      BlockPos blockpos = this.absolutePos(p_177404_);
      if (this.getLevel().hasEntities(p_177403_, new AABB(blockpos), Entity::isAlive)) {
         throw this.assertionException(p_177404_, "test.error.unexpected_entity", p_177403_.getDescription());
      }
   }

   public void assertEntityNotPresent(EntityType<?> p_341944_, AABB p_362043_) {
      AABB aabb = this.absoluteAABB(p_362043_);
      List<? extends Entity> list = this.getLevel().getEntities(p_341944_, aabb, Entity::isAlive);
      if (!list.isEmpty()) {
         throw this.assertionException(list.getFirst().blockPosition(), "test.error.unexpected_entity", p_341944_.getDescription());
      }
   }

   public void assertEntityTouching(EntityType<?> p_177159_, double p_177160_, double p_177161_, double p_177162_) {
      Vec3 vec3 = new Vec3(p_177160_, p_177161_, p_177162_);
      Vec3 vec31 = this.absoluteVec(vec3);
      Predicate<? super Entity> predicate = p_177346_ -> p_177346_.getBoundingBox().intersects(vec31, vec31);
      if (!this.getLevel().hasEntities(p_177159_, this.getBounds(), predicate)) {
         throw this.assertionException(
            "test.error.expected_entity_touching", p_177159_.getDescription(), vec31.x(), vec31.y(), vec31.z(), p_177160_, p_177161_, p_177162_
         );
      }
   }

   public void assertEntityNotTouching(EntityType<?> p_177312_, double p_177313_, double p_177314_, double p_177315_) {
      Vec3 vec3 = new Vec3(p_177313_, p_177314_, p_177315_);
      Vec3 vec31 = this.absoluteVec(vec3);
      Predicate<? super Entity> predicate = p_177231_ -> !p_177231_.getBoundingBox().intersects(vec31, vec31);
      if (!this.getLevel().hasEntities(p_177312_, this.getBounds(), predicate)) {
         throw this.assertionException(
            "test.error.expected_entity_not_touching", p_177312_.getDescription(), vec31.x(), vec31.y(), vec31.z(), p_177313_, p_177314_, p_177315_
         );
      }
   }

   public <E extends Entity, T> void assertEntityData(BlockPos p_371245_, EntityType<E> p_371536_, Predicate<E> p_371765_) {
      BlockPos blockpos = this.absolutePos(p_371245_);
      List<E> list = this.getLevel().getEntities(p_371536_, new AABB(blockpos), Entity::isAlive);
      if (list.isEmpty()) {
         throw this.assertionException(p_371245_, "test.error.expected_entity", p_371536_.getDescription());
      } else {
         for (E e : list) {
            if (!p_371765_.test(e)) {
               throw this.assertionException(e.blockPosition(), "test.error.expected_entity_data_predicate", e.getName());
            }
         }
      }
   }

   public <E extends Entity, T> void assertEntityData(BlockPos p_177238_, EntityType<E> p_177239_, Function<? super E, T> p_177240_, @Nullable T p_177241_) {
      this.assertEntityData(new AABB(p_177238_), p_177239_, p_177240_, p_177241_);
   }

   public <E extends Entity, T> void assertEntityData(AABB p_457538_, EntityType<E> p_457786_, Function<? super E, T> p_457724_, @Nullable T p_458293_) {
      List<E> list = this.getLevel().getEntities(p_457786_, this.absoluteAABB(p_457538_), Entity::isAlive);
      if (list.isEmpty()) {
         throw this.assertionException(BlockPos.containing(p_457538_.getBottomCenter()), "test.error.expected_entity", p_457786_.getDescription());
      } else {
         for (E e : list) {
            T t = p_457724_.apply(e);
            if (!Objects.equals(t, p_458293_)) {
               throw this.assertionException(BlockPos.containing(p_457538_.getBottomCenter()), "test.error.expected_entity_data", p_458293_, t);
            }
         }
      }
   }

   public <E extends LivingEntity> void assertEntityIsHolding(BlockPos p_263501_, EntityType<E> p_263510_, Item p_263517_) {
      BlockPos blockpos = this.absolutePos(p_263501_);
      List<E> list = this.getLevel().getEntities(p_263510_, new AABB(blockpos), Entity::isAlive);
      if (list.isEmpty()) {
         throw this.assertionException(p_263501_, "test.error.expected_entity", p_263510_.getDescription());
      } else {
         for (E e : list) {
            if (e.isHolding(p_263517_)) {
               return;
            }
         }

         throw this.assertionException(p_263501_, "test.error.expected_entity_holding", p_263517_.getName());
      }
   }

   public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(BlockPos p_263495_, EntityType<E> p_263521_, Item p_263502_) {
      BlockPos blockpos = this.absolutePos(p_263495_);
      List<E> list = this.getLevel().getEntities(p_263521_, new AABB(blockpos), p_263479_ -> p_263479_.isAlive());
      if (list.isEmpty()) {
         throw this.assertionException(p_263495_, "test.error.expected_entity", p_263521_.getDescription());
      } else {
         for (E e : list) {
            if (e.getInventory().hasAnyMatching(p_263481_ -> p_263481_.is(p_263502_))) {
               return;
            }
         }

         throw this.assertionException(p_263495_, "test.error.expected_entity_having", p_263502_.getName());
      }
   }

   public void assertContainerEmpty(BlockPos p_177441_) {
      BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(p_177441_, BaseContainerBlockEntity.class);
      if (!basecontainerblockentity.isEmpty()) {
         throw this.assertionException(p_177441_, "test.error.expected_empty_container");
      }
   }

   public void assertContainerContainsSingle(BlockPos p_398052_, Item p_397477_) {
      BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(p_398052_, BaseContainerBlockEntity.class);
      if (basecontainerblockentity.countItem(p_397477_) != 1) {
         throw this.assertionException(p_398052_, "test.error.expected_container_contents_single", p_397477_.getName());
      }
   }

   public void assertContainerContains(BlockPos p_177243_, Item p_177244_) {
      BaseContainerBlockEntity basecontainerblockentity = this.getBlockEntity(p_177243_, BaseContainerBlockEntity.class);
      if (basecontainerblockentity.countItem(p_177244_) == 0) {
         throw this.assertionException(p_177243_, "test.error.expected_container_contents", p_177244_.getName());
      }
   }

   public void assertSameBlockStates(BoundingBox p_177225_, BlockPos p_177226_) {
      BlockPos.betweenClosedStream(p_177225_).forEach(p_177267_ -> {
         BlockPos blockpos = p_177226_.offset(p_177267_.getX() - p_177225_.minX(), p_177267_.getY() - p_177225_.minY(), p_177267_.getZ() - p_177225_.minZ());
         this.assertSameBlockState(p_177267_, blockpos);
      });
   }

   public void assertSameBlockState(BlockPos p_177269_, BlockPos p_177270_) {
      BlockState blockstate = this.getBlockState(p_177269_);
      BlockState blockstate1 = this.getBlockState(p_177270_);
      if (blockstate != blockstate1) {
         throw this.assertionException(p_177269_, "test.error.state_not_equal", blockstate1, blockstate);
      }
   }

   public void assertAtTickTimeContainerContains(long p_177124_, BlockPos p_177125_, Item p_177126_) {
      this.runAtTickTime(p_177124_, () -> this.assertContainerContainsSingle(p_177125_, p_177126_));
   }

   public void assertAtTickTimeContainerEmpty(long p_177121_, BlockPos p_177122_) {
      this.runAtTickTime(p_177121_, () -> this.assertContainerEmpty(p_177122_));
   }

   public <E extends Entity, T> void succeedWhenEntityData(BlockPos p_177350_, EntityType<E> p_177351_, Function<E, T> p_177352_, T p_177353_) {
      this.succeedWhen(() -> this.assertEntityData(p_177350_, p_177351_, p_177352_, p_177353_));
   }

   public <E extends Entity> void assertEntityProperty(E p_177153_, Predicate<E> p_177154_, Component p_397261_) {
      if (!p_177154_.test(p_177153_)) {
         throw this.assertionException(p_177153_.blockPosition(), "test.error.entity_property", p_177153_.getName(), p_397261_);
      }
   }

   public <E extends Entity, T> void assertEntityProperty(E p_177148_, Function<E, T> p_177149_, T p_177151_, Component p_397438_) {
      T t = p_177149_.apply(p_177148_);
      if (!t.equals(p_177151_)) {
         throw this.assertionException(p_177148_.blockPosition(), "test.error.entity_property_details", p_177148_.getName(), p_397438_, t, p_177151_);
      }
   }

   public void assertLivingEntityHasMobEffect(LivingEntity p_296040_, Holder<MobEffect> p_316528_, int p_294266_) {
      MobEffectInstance mobeffectinstance = p_296040_.getEffect(p_316528_);
      if (mobeffectinstance == null || mobeffectinstance.getAmplifier() != p_294266_) {
         throw this.assertionException("test.error.expected_entity_effect", p_296040_.getName(), PotionContents.getPotionDescription(p_316528_, p_294266_));
      }
   }

   public void succeedWhenEntityPresent(EntityType<?> p_177414_, int p_177415_, int p_177416_, int p_177417_) {
      this.succeedWhenEntityPresent(p_177414_, new BlockPos(p_177415_, p_177416_, p_177417_));
   }

   public void succeedWhenEntityPresent(EntityType<?> p_177419_, BlockPos p_177420_) {
      this.succeedWhen(() -> this.assertEntityPresent(p_177419_, p_177420_));
   }

   public void succeedWhenEntityNotPresent(EntityType<?> p_177427_, int p_177428_, int p_177429_, int p_177430_) {
      this.succeedWhenEntityNotPresent(p_177427_, new BlockPos(p_177428_, p_177429_, p_177430_));
   }

   public void succeedWhenEntityNotPresent(EntityType<?> p_177432_, BlockPos p_177433_) {
      this.succeedWhen(() -> this.assertEntityNotPresent(p_177432_, p_177433_));
   }

   public void succeed() {
      this.testInfo.succeed();
   }

   private void ensureSingleFinalCheck() {
      if (this.finalCheckAdded) {
         throw new IllegalStateException("This test already has final clause");
      } else {
         this.finalCheckAdded = true;
      }
   }

   public void succeedIf(Runnable p_177280_) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(0L, p_177280_).thenSucceed();
   }

   public void succeedWhen(Runnable p_177362_) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(p_177362_).thenSucceed();
   }

   public void succeedOnTickWhen(int p_177118_, Runnable p_177119_) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(p_177118_, p_177119_).thenSucceed();
   }

   public void runAtTickTime(long p_177128_, Runnable p_177129_) {
      this.testInfo.setRunAtTickTime(p_177128_, p_177129_);
   }

   public void runAfterDelay(long p_177307_, Runnable p_177308_) {
      this.runAtTickTime(this.testInfo.getTick() + p_177307_, p_177308_);
   }

   public void randomTick(BlockPos p_177447_) {
      BlockPos blockpos = this.absolutePos(p_177447_);
      ServerLevel serverlevel = this.getLevel();
      serverlevel.getBlockState(blockpos).randomTick(serverlevel, blockpos, serverlevel.random);
   }

   public void tickBlock(BlockPos p_393816_) {
      BlockPos blockpos = this.absolutePos(p_393816_);
      ServerLevel serverlevel = this.getLevel();
      serverlevel.getBlockState(blockpos).tick(serverlevel, blockpos, serverlevel.random);
   }

   public void tickPrecipitation(BlockPos p_313817_) {
      BlockPos blockpos = this.absolutePos(p_313817_);
      ServerLevel serverlevel = this.getLevel();
      serverlevel.tickPrecipitation(blockpos);
   }

   public void tickPrecipitation() {
      AABB aabb = this.getRelativeBounds();
      int i = (int)Math.floor(aabb.maxX);
      int j = (int)Math.floor(aabb.maxZ);
      int k = (int)Math.floor(aabb.maxY);

      for (int l = (int)Math.floor(aabb.minX); l < i; l++) {
         for (int i1 = (int)Math.floor(aabb.minZ); i1 < j; i1++) {
            this.tickPrecipitation(new BlockPos(l, k, i1));
         }
      }
   }

   public int getHeight(Heightmap.Types p_236775_, int p_236776_, int p_236777_) {
      BlockPos blockpos = this.absolutePos(new BlockPos(p_236776_, 0, p_236777_));
      return this.relativePos(this.getLevel().getHeightmapPos(p_236775_, blockpos)).getY();
   }

   public void fail(Component p_397814_, BlockPos p_397554_) {
      throw this.assertionException(p_397554_, p_397814_);
   }

   public void fail(Component p_397384_, Entity p_177288_) {
      throw this.assertionException(p_177288_.blockPosition(), p_397384_);
   }

   public void fail(Component p_397383_) {
      throw this.assertionException(p_397383_);
   }

   public void fail(String p_433138_) {
      throw this.assertionException(Component.literal(p_433138_));
   }

   public void failIf(Runnable p_177393_) {
      this.testInfo.createSequence().thenWaitUntil(p_177393_).thenFail(() -> this.assertionException("test.error.fail"));
   }

   public void failIfEver(Runnable p_177411_) {
      LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks())
         .forEach(p_177365_ -> this.testInfo.setRunAtTickTime(p_177365_, p_177411_::run));
   }

   public GameTestSequence startSequence() {
      return this.testInfo.createSequence();
   }

   public BlockPos absolutePos(BlockPos p_177450_) {
      BlockPos blockpos = this.testInfo.getTestOrigin();
      BlockPos blockpos1 = blockpos.offset(p_177450_);
      return StructureTemplate.transform(blockpos1, Mirror.NONE, this.testInfo.getRotation(), blockpos);
   }

   public BlockPos relativePos(BlockPos p_177453_) {
      BlockPos blockpos = this.testInfo.getTestOrigin();
      Rotation rotation = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
      BlockPos blockpos1 = StructureTemplate.transform(p_177453_, Mirror.NONE, rotation, blockpos);
      return blockpos1.subtract(blockpos);
   }

   public AABB absoluteAABB(AABB p_361591_) {
      Vec3 vec3 = this.absoluteVec(p_361591_.getMinPosition());
      Vec3 vec31 = this.absoluteVec(p_361591_.getMaxPosition());
      return new AABB(vec3, vec31);
   }

   public AABB relativeAABB(AABB p_362943_) {
      Vec3 vec3 = this.relativeVec(p_362943_.getMinPosition());
      Vec3 vec31 = this.relativeVec(p_362943_.getMaxPosition());
      return new AABB(vec3, vec31);
   }

   public Vec3 absoluteVec(Vec3 p_177228_) {
      Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
      return StructureTemplate.transform(vec3.add(p_177228_), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
   }

   public Vec3 relativeVec(Vec3 p_251543_) {
      Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
      return StructureTemplate.transform(p_251543_.subtract(vec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
   }

   public Rotation getTestRotation() {
      return this.testInfo.getRotation();
   }

   public Direction getTestDirection() {
      return this.testInfo.getRotation().rotate(Direction.SOUTH);
   }

   public Direction getAbsoluteDirection(Direction p_470776_) {
      return this.getTestRotation().rotate(p_470776_);
   }

   public void assertTrue(boolean p_249380_, Component p_397005_) {
      if (!p_249380_) {
         throw this.assertionException(p_397005_);
      }
   }

   public void assertTrue(boolean p_456064_, String p_455318_) {
      this.assertTrue(p_456064_, Component.literal(p_455318_));
   }

   public <N> void assertValueEqual(N p_457816_, N p_457623_, String p_457812_) {
      this.assertValueEqual(p_457816_, p_457623_, Component.literal(p_457812_));
   }

   public <N> void assertValueEqual(N p_324013_, N p_323869_, Component p_397364_) {
      if (!p_324013_.equals(p_323869_)) {
         throw this.assertionException("test.error.value_not_equal", p_397364_, p_324013_, p_323869_);
      }
   }

   public void assertFalse(boolean p_277974_, Component p_397844_) {
      this.assertTrue(!p_277974_, p_397844_);
   }

   public void assertFalse(boolean p_455796_, String p_455412_) {
      this.assertFalse(p_455796_, Component.literal(p_455412_));
   }

   public long getTick() {
      return this.testInfo.getTick();
   }

   public AABB getBounds() {
      return this.testInfo.getStructureBounds();
   }

   public AABB getRelativeBounds() {
      AABB aabb = this.testInfo.getStructureBounds();
      Rotation rotation = this.testInfo.getRotation();
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            return new AABB(0.0, 0.0, 0.0, aabb.getZsize(), aabb.getYsize(), aabb.getXsize());
         default:
            return new AABB(0.0, 0.0, 0.0, aabb.getXsize(), aabb.getYsize(), aabb.getZsize());
      }
   }

   public void forEveryBlockInStructure(Consumer<BlockPos> p_177293_) {
      AABB aabb = this.getRelativeBounds().contract(1.0, 1.0, 1.0);
      BlockPos.MutableBlockPos.betweenClosedStream(aabb).forEach(p_177293_);
   }

   public void onEachTick(Runnable p_177424_) {
      LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks())
         .forEach(p_177283_ -> this.testInfo.setRunAtTickTime(p_177283_, p_177424_::run));
   }

   public void placeAt(Player p_261595_, ItemStack p_262007_, BlockPos p_261973_, Direction p_262008_) {
      BlockPos blockpos = this.absolutePos(p_261973_.relative(p_262008_));
      BlockHitResult blockhitresult = new BlockHitResult(Vec3.atCenterOf(blockpos), p_262008_, blockpos, false);
      UseOnContext useoncontext = new UseOnContext(p_261595_, InteractionHand.MAIN_HAND, blockhitresult);
      p_262007_.useOn(useoncontext);
   }

   public void setBiome(ResourceKey<Biome> p_313921_) {
      AABB aabb = this.getBounds();
      BlockPos blockpos = BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ);
      BlockPos blockpos1 = BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ);
      Either<Integer, CommandSyntaxException> either = FillBiomeCommand.fill(
         this.getLevel(), blockpos, blockpos1, this.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(p_313921_)
      );
      if (either.right().isPresent()) {
         throw this.assertionException("test.error.set_biome");
      }
   }
}

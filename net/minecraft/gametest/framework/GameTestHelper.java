package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import io.netty.channel.ChannelHandler;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GameTestHelper {
   public final GameTestInfo testInfo;
   private boolean finalCheckAdded;

   public GameTestHelper(GameTestInfo p_127597_) {
      this.testInfo = p_127597_;
   }

   public ServerLevel getLevel() {
      return this.testInfo.getLevel();
   }

   public BlockState getBlockState(BlockPos p_177233_) {
      return this.getLevel().getBlockState(this.absolutePos(p_177233_));
   }

   public <T extends BlockEntity> T getBlockEntity(BlockPos p_177348_) {
      BlockEntity $$1 = this.getLevel().getBlockEntity(this.absolutePos(p_177348_));
      if ($$1 == null) {
         throw new GameTestAssertPosException("Missing block entity", this.absolutePos(p_177348_), p_177348_, this.testInfo.getTick());
      } else {
         return (T)$$1;
      }
   }

   public void killAllEntities() {
      this.killAllEntitiesOfClass(Entity.class);
   }

   public void killAllEntitiesOfClass(Class p_289538_) {
      AABB $$1 = this.getBounds();
      List<Entity> $$2 = this.getLevel().getEntitiesOfClass(p_289538_, $$1.inflate(1.0), p_177131_ -> !(p_177131_ instanceof Player));
      $$2.forEach(Entity::kill);
   }

   public ItemEntity spawnItem(Item p_326872_, Vec3 p_326853_) {
      ServerLevel $$2 = this.getLevel();
      Vec3 $$3 = this.absoluteVec(p_326853_);
      ItemEntity $$4 = new ItemEntity($$2, $$3.x, $$3.y, $$3.z, new ItemStack(p_326872_, 1));
      $$4.setDeltaMovement(0.0, 0.0, 0.0);
      $$2.addFreshEntity($$4);
      return $$4;
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

   public <E extends Entity> E spawn(EntityType<E> p_177174_, Vec3 p_177175_) {
      ServerLevel $$2 = this.getLevel();
      E $$3 = p_177174_.create($$2);
      if ($$3 == null) {
         throw new NullPointerException("Failed to create entity " + p_177174_.builtInRegistryHolder().key().location());
      } else {
         if ($$3 instanceof Mob $$4) {
            $$4.setPersistenceRequired();
         }

         Vec3 $$5 = this.absoluteVec(p_177175_);
         $$3.moveTo($$5.x, $$5.y, $$5.z, $$3.getYRot(), $$3.getXRot());
         $$2.addFreshEntity($$3);
         return $$3;
      }
   }

   public <E extends Entity> E findOneEntity(EntityType<E> p_320756_) {
      return this.findClosestEntity(p_320756_, 0, 0, 0, 2.147483647E9);
   }

   public <E extends Entity> E findClosestEntity(EntityType<E> p_320915_, int p_320152_, int p_319868_, int p_319912_, double p_320874_) {
      List<E> $$5 = this.findEntities(p_320915_, p_320152_, p_319868_, p_319912_, p_320874_);
      if ($$5.isEmpty()) {
         throw new GameTestAssertException("Expected " + p_320915_.toShortString() + " to exist around " + p_320152_ + "," + p_319868_ + "," + p_319912_);
      } else if ($$5.size() > 1) {
         throw new GameTestAssertException(
            "Expected only one "
               + p_320915_.toShortString()
               + " to exist around "
               + p_320152_
               + ","
               + p_319868_
               + ","
               + p_319912_
               + ", but found "
               + $$5.size()
         );
      } else {
         Vec3 $$6 = this.absoluteVec(new Vec3(p_320152_, p_319868_, p_319912_));
         $$5.sort((p_319453_, p_319454_) -> {
            double $$3 = p_319453_.position().distanceTo($$6);
            double $$4 = p_319454_.position().distanceTo($$6);
            return Double.compare($$3, $$4);
         });
         return $$5.get(0);
      }
   }

   public <E extends Entity> List<E> findEntities(EntityType<E> p_320065_, int p_320533_, int p_320279_, int p_320721_, double p_320280_) {
      return this.findEntities(p_320065_, Vec3.atBottomCenterOf(new BlockPos(p_320533_, p_320279_, p_320721_)), p_320280_);
   }

   public <E extends Entity> List<E> findEntities(EntityType<E> p_320878_, Vec3 p_320734_, double p_320825_) {
      ServerLevel $$3 = this.getLevel();
      Vec3 $$4 = this.absoluteVec(p_320734_);
      AABB $$5 = this.testInfo.getStructureBounds();
      AABB $$6 = new AABB($$4.add(-p_320825_, -p_320825_, -p_320825_), $$4.add(p_320825_, p_320825_, p_320825_));
      return $$3.getEntities(p_320878_, $$5, p_319451_ -> p_319451_.getBoundingBox().intersects($$6) && p_319451_.isAlive());
   }

   public <E extends Entity> E spawn(EntityType<E> p_177169_, int p_177170_, int p_177171_, int p_177172_) {
      return this.spawn(p_177169_, new BlockPos(p_177170_, p_177171_, p_177172_));
   }

   public <E extends Entity> E spawn(EntityType<E> p_177164_, float p_177165_, float p_177166_, float p_177167_) {
      return this.spawn(p_177164_, new Vec3(p_177165_, p_177166_, p_177167_));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177330_, BlockPos p_177331_) {
      E $$2 = (E)this.spawn(p_177330_, p_177331_);
      $$2.removeFreeWill();
      return $$2;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177322_, int p_177323_, int p_177324_, int p_177325_) {
      return this.spawnWithNoFreeWill(p_177322_, new BlockPos(p_177323_, p_177324_, p_177325_));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177327_, Vec3 p_177328_) {
      E $$2 = (E)this.spawn(p_177327_, p_177328_);
      $$2.removeFreeWill();
      return $$2;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> p_177317_, float p_177318_, float p_177319_, float p_177320_) {
      return this.spawnWithNoFreeWill(p_177317_, new Vec3(p_177318_, p_177319_, p_177320_));
   }

   public void moveTo(Mob p_326829_, float p_326862_, float p_326823_, float p_326844_) {
      Vec3 $$4 = this.absoluteVec(new Vec3(p_326862_, p_326823_, p_326844_));
      p_326829_.moveTo($$4.x, $$4.y, $$4.z, p_326829_.getYRot(), p_326829_.getXRot());
   }

   public GameTestSequence walkTo(Mob p_177186_, BlockPos p_177187_, float p_177188_) {
      return this.startSequence().thenExecuteAfter(2, () -> {
         Path $$3 = p_177186_.getNavigation().createPath(this.absolutePos(p_177187_), 0);
         p_177186_.getNavigation().moveTo($$3, (double)p_177188_);
      });
   }

   public void pressButton(int p_177104_, int p_177105_, int p_177106_) {
      this.pressButton(new BlockPos(p_177104_, p_177105_, p_177106_));
   }

   public void pressButton(BlockPos p_177386_) {
      this.assertBlockState(p_177386_, p_177212_ -> p_177212_.is(BlockTags.BUTTONS), () -> "Expected button");
      BlockPos $$1 = this.absolutePos(p_177386_);
      BlockState $$2 = this.getLevel().getBlockState($$1);
      ButtonBlock $$3 = (ButtonBlock)$$2.getBlock();
      $$3.press($$2, this.getLevel(), $$1, null);
   }

   public void useBlock(BlockPos p_177409_) {
      this.useBlock(p_177409_, this.makeMockPlayer(GameType.CREATIVE));
   }

   public void useBlock(BlockPos p_250131_, Player p_251507_) {
      BlockPos $$2 = this.absolutePos(p_250131_);
      this.useBlock(p_250131_, p_251507_, new BlockHitResult(Vec3.atCenterOf($$2), Direction.NORTH, $$2, true));
   }

   public void useBlock(BlockPos p_262023_, Player p_261901_, BlockHitResult p_262040_) {
      BlockPos $$3 = this.absolutePos(p_262023_);
      BlockState $$4 = this.getLevel().getBlockState($$3);
      InteractionHand $$5 = InteractionHand.MAIN_HAND;
      ItemInteractionResult $$6 = $$4.useItemOn(p_261901_.getItemInHand($$5), this.getLevel(), p_261901_, $$5, p_262040_);
      if (!$$6.consumesAction()) {
         if ($$6 != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION || !$$4.useWithoutItem(this.getLevel(), p_261901_, p_262040_).consumesAction()) {
            UseOnContext $$7 = new UseOnContext(p_261901_, $$5, p_262040_);
            p_261901_.getItemInHand($$5).useOn($$7);
         }
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
      return new Player(this.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
         @Override
         public boolean isSpectator() {
            return p_323908_ == GameType.SPECTATOR;
         }

         @Override
         public boolean isCreative() {
            return p_323908_.isCreative();
         }

         @Override
         public boolean isLocalPlayer() {
            return true;
         }
      };
   }

   @Deprecated(
      forRemoval = true
   )
   public ServerPlayer makeMockServerPlayerInLevel() {
      CommonListenerCookie $$0 = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
      ServerPlayer $$1 = new ServerPlayer(this.getLevel().getServer(), this.getLevel(), $$0.gameProfile(), $$0.clientInformation()) {
         @Override
         public boolean isSpectator() {
            return false;
         }

         @Override
         public boolean isCreative() {
            return true;
         }
      };
      Connection $$2 = new Connection(PacketFlow.SERVERBOUND);
      new EmbeddedChannel(new ChannelHandler[]{$$2});
      this.getLevel().getServer().getPlayerList().placeNewPlayer($$2, $$1, $$0);
      return $$1;
   }

   public void pullLever(int p_177303_, int p_177304_, int p_177305_) {
      this.pullLever(new BlockPos(p_177303_, p_177304_, p_177305_));
   }

   public void pullLever(BlockPos p_177422_) {
      this.assertBlockPresent(Blocks.LEVER, p_177422_);
      BlockPos $$1 = this.absolutePos(p_177422_);
      BlockState $$2 = this.getLevel().getBlockState($$1);
      LeverBlock $$3 = (LeverBlock)$$2.getBlock();
      $$3.pull($$2, this.getLevel(), $$1, null);
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

   public void setNight() {
      this.setDayTime(13000);
   }

   public void setDayTime(int p_177102_) {
      this.getLevel().setDayTime(p_177102_);
   }

   public void assertBlockPresent(Block p_177204_, int p_177205_, int p_177206_, int p_177207_) {
      this.assertBlockPresent(p_177204_, new BlockPos(p_177205_, p_177206_, p_177207_));
   }

   public void assertBlockPresent(Block p_177209_, BlockPos p_177210_) {
      BlockState $$2 = this.getBlockState(p_177210_);
      this.assertBlock(
         p_177210_, p_177216_ -> $$2.is(p_177209_), "Expected " + p_177209_.getName().getString() + ", got " + $$2.getBlock().getName().getString()
      );
   }

   public void assertBlockNotPresent(Block p_177337_, int p_177338_, int p_177339_, int p_177340_) {
      this.assertBlockNotPresent(p_177337_, new BlockPos(p_177338_, p_177339_, p_177340_));
   }

   public void assertBlockNotPresent(Block p_177342_, BlockPos p_177343_) {
      this.assertBlock(p_177343_, p_177251_ -> !this.getBlockState(p_177343_).is(p_177342_), "Did not expect " + p_177342_.getName().getString());
   }

   public void succeedWhenBlockPresent(Block p_177378_, int p_177379_, int p_177380_, int p_177381_) {
      this.succeedWhenBlockPresent(p_177378_, new BlockPos(p_177379_, p_177380_, p_177381_));
   }

   public void succeedWhenBlockPresent(Block p_177383_, BlockPos p_177384_) {
      this.succeedWhen(() -> this.assertBlockPresent(p_177383_, p_177384_));
   }

   public void assertBlock(BlockPos p_177272_, Predicate<Block> p_177273_, String p_177274_) {
      this.assertBlock(p_177272_, p_177273_, () -> p_177274_);
   }

   public void assertBlock(BlockPos p_177276_, Predicate<Block> p_177277_, Supplier<String> p_177278_) {
      this.assertBlockState(p_177276_, p_177296_ -> p_177277_.test(p_177296_.getBlock()), p_177278_);
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos p_177256_, Property<T> p_177257_, T p_177258_) {
      BlockState $$3 = this.getBlockState(p_177256_);
      boolean $$4 = $$3.hasProperty(p_177257_);
      if (!$$4 || !$$3.<T>getValue(p_177257_).equals(p_177258_)) {
         String $$5 = $$4 ? "was " + $$3.getValue(p_177257_) : "property " + p_177257_.getName() + " is missing";
         String $$6 = String.format(Locale.ROOT, "Expected property %s to be %s, %s", p_177257_.getName(), p_177258_, $$5);
         throw new GameTestAssertPosException($$6, this.absolutePos(p_177256_), p_177256_, this.testInfo.getTick());
      }
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos p_177260_, Property<T> p_177261_, Predicate<T> p_177262_, String p_177263_) {
      this.assertBlockState(p_177260_, p_277264_ -> {
         if (!p_277264_.hasProperty(p_177261_)) {
            return false;
         } else {
            T $$3 = p_277264_.getValue(p_177261_);
            return p_177262_.test($$3);
         }
      }, () -> p_177263_);
   }

   public void assertBlockState(BlockPos p_177358_, Predicate<BlockState> p_177359_, Supplier<String> p_177360_) {
      BlockState $$3 = this.getBlockState(p_177358_);
      if (!p_177359_.test($$3)) {
         throw new GameTestAssertPosException(p_177360_.get(), this.absolutePos(p_177358_), p_177358_, this.testInfo.getTick());
      }
   }

   public <T extends BlockEntity> void assertBlockEntityData(BlockPos p_348547_, Predicate<T> p_348669_, Supplier<String> p_348486_) {
      T $$3 = this.getBlockEntity(p_348547_);
      if (!p_348669_.test($$3)) {
         throw new GameTestAssertPosException(p_348486_.get(), this.absolutePos(p_348547_), p_348547_, this.testInfo.getTick());
      }
   }

   public void assertRedstoneSignal(BlockPos p_289644_, Direction p_289642_, IntPredicate p_289645_, Supplier<String> p_289684_) {
      BlockPos $$4 = this.absolutePos(p_289644_);
      ServerLevel $$5 = this.getLevel();
      BlockState $$6 = $$5.getBlockState($$4);
      int $$7 = $$6.getSignal($$5, $$4, p_289642_);
      if (!p_289645_.test($$7)) {
         throw new GameTestAssertPosException(p_289684_.get(), $$4, p_289644_, this.testInfo.getTick());
      }
   }

   public void assertEntityPresent(EntityType<?> p_177157_) {
      List<? extends Entity> $$1 = this.getLevel().getEntities(p_177157_, this.getBounds(), Entity::isAlive);
      if ($$1.isEmpty()) {
         throw new GameTestAssertException("Expected " + p_177157_.toShortString() + " to exist");
      }
   }

   public void assertEntityPresent(EntityType<?> p_177370_, int p_177371_, int p_177372_, int p_177373_) {
      this.assertEntityPresent(p_177370_, new BlockPos(p_177371_, p_177372_, p_177373_));
   }

   public void assertEntityPresent(EntityType<?> p_177375_, BlockPos p_177376_) {
      BlockPos $$2 = this.absolutePos(p_177376_);
      List<? extends Entity> $$3 = this.getLevel().getEntities(p_177375_, new AABB($$2), Entity::isAlive);
      if ($$3.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + p_177375_.toShortString(), $$2, p_177376_, this.testInfo.getTick());
      }
   }

   public void assertEntityPresent(EntityType<?> p_252010_, Vec3 p_249488_, Vec3 p_251186_) {
      List<? extends Entity> $$3 = this.getLevel().getEntities(p_252010_, new AABB(p_249488_, p_251186_), Entity::isAlive);
      if ($$3.isEmpty()) {
         throw new GameTestAssertPosException(
            "Expected " + p_252010_.toShortString() + " between ", BlockPos.containing(p_249488_), BlockPos.containing(p_251186_), this.testInfo.getTick()
         );
      }
   }

   public void assertEntitiesPresent(EntityType<?> p_312835_, int p_312735_) {
      List<? extends Entity> $$2 = this.getLevel().getEntities(p_312835_, this.getBounds(), Entity::isAlive);
      if ($$2.size() != p_312735_) {
         throw new GameTestAssertException("Expected " + p_312735_ + " of type " + p_312835_.toShortString() + " to exist, found " + $$2.size());
      }
   }

   public void assertEntitiesPresent(EntityType<?> p_239372_, BlockPos p_239373_, int p_239374_, double p_239375_) {
      BlockPos $$4 = this.absolutePos(p_239373_);
      List<? extends Entity> $$5 = this.getEntities((EntityType<? extends Entity>)p_239372_, p_239373_, p_239375_);
      if ($$5.size() != p_239374_) {
         throw new GameTestAssertPosException(
            "Expected " + p_239374_ + " entities of type " + p_239372_.toShortString() + ", actual number of entities found=" + $$5.size(),
            $$4,
            p_239373_,
            this.testInfo.getTick()
         );
      }
   }

   public void assertEntityPresent(EntityType<?> p_177180_, BlockPos p_177181_, double p_177182_) {
      List<? extends Entity> $$3 = this.getEntities((EntityType<? extends Entity>)p_177180_, p_177181_, p_177182_);
      if ($$3.isEmpty()) {
         BlockPos $$4 = this.absolutePos(p_177181_);
         throw new GameTestAssertPosException("Expected " + p_177180_.toShortString(), $$4, p_177181_, this.testInfo.getTick());
      }
   }

   public <T extends Entity> List<T> getEntities(EntityType<T> p_238400_, BlockPos p_238401_, double p_238402_) {
      BlockPos $$3 = this.absolutePos(p_238401_);
      return this.getLevel().getEntities(p_238400_, new AABB($$3).inflate(p_238402_), Entity::isAlive);
   }

   public <T extends Entity> List<T> getEntities(EntityType<T> p_320029_) {
      return this.getLevel().getEntities(p_320029_, this.getBounds(), Entity::isAlive);
   }

   public void assertEntityInstancePresent(Entity p_177133_, int p_177134_, int p_177135_, int p_177136_) {
      this.assertEntityInstancePresent(p_177133_, new BlockPos(p_177134_, p_177135_, p_177136_));
   }

   public void assertEntityInstancePresent(Entity p_177141_, BlockPos p_177142_) {
      BlockPos $$2 = this.absolutePos(p_177142_);
      List<? extends Entity> $$3 = this.getLevel().getEntities(p_177141_.getType(), new AABB($$2), Entity::isAlive);
      $$3.stream()
         .filter(p_177139_ -> p_177139_ == p_177141_)
         .findFirst()
         .orElseThrow(() -> new GameTestAssertPosException("Expected " + p_177141_.getType().toShortString(), $$2, p_177142_, this.testInfo.getTick()));
   }

   public void assertItemEntityCountIs(Item p_177199_, BlockPos p_177200_, double p_177201_, int p_177202_) {
      BlockPos $$4 = this.absolutePos(p_177200_);
      List<ItemEntity> $$5 = this.getLevel().getEntities(EntityType.ITEM, new AABB($$4).inflate(p_177201_), Entity::isAlive);
      int $$6 = 0;

      for (ItemEntity $$7 : $$5) {
         ItemStack $$8 = $$7.getItem();
         if ($$8.is(p_177199_)) {
            $$6 += $$8.getCount();
         }
      }

      if ($$6 != p_177202_) {
         throw new GameTestAssertPosException(
            "Expected " + p_177202_ + " " + p_177199_.getDescription().getString() + " items to exist (found " + $$6 + ")",
            $$4,
            p_177200_,
            this.testInfo.getTick()
         );
      }
   }

   public void assertItemEntityPresent(Item p_177195_, BlockPos p_177196_, double p_177197_) {
      BlockPos $$3 = this.absolutePos(p_177196_);

      for (Entity $$5 : this.getLevel().getEntities(EntityType.ITEM, new AABB($$3).inflate(p_177197_), Entity::isAlive)) {
         ItemEntity $$6 = (ItemEntity)$$5;
         if ($$6.getItem().getItem().equals(p_177195_)) {
            return;
         }
      }

      throw new GameTestAssertPosException("Expected " + p_177195_.getDescription().getString() + " item", $$3, p_177196_, this.testInfo.getTick());
   }

   public void assertItemEntityNotPresent(Item p_236779_, BlockPos p_236780_, double p_236781_) {
      BlockPos $$3 = this.absolutePos(p_236780_);

      for (Entity $$5 : this.getLevel().getEntities(EntityType.ITEM, new AABB($$3).inflate(p_236781_), Entity::isAlive)) {
         ItemEntity $$6 = (ItemEntity)$$5;
         if ($$6.getItem().getItem().equals(p_236779_)) {
            throw new GameTestAssertPosException("Did not expect " + p_236779_.getDescription().getString() + " item", $$3, p_236780_, this.testInfo.getTick());
         }
      }
   }

   public void assertItemEntityPresent(Item p_304857_) {
      for (Entity $$2 : this.getLevel().getEntities(EntityType.ITEM, this.getBounds(), Entity::isAlive)) {
         ItemEntity $$3 = (ItemEntity)$$2;
         if ($$3.getItem().getItem().equals(p_304857_)) {
            return;
         }
      }

      throw new GameTestAssertException("Expected " + p_304857_.getDescription().getString() + " item");
   }

   public void assertItemEntityNotPresent(Item p_304474_) {
      for (Entity $$2 : this.getLevel().getEntities(EntityType.ITEM, this.getBounds(), Entity::isAlive)) {
         ItemEntity $$3 = (ItemEntity)$$2;
         if ($$3.getItem().getItem().equals(p_304474_)) {
            throw new GameTestAssertException("Did not expect " + p_304474_.getDescription().getString() + " item");
         }
      }
   }

   public void assertEntityNotPresent(EntityType<?> p_177310_) {
      List<? extends Entity> $$1 = this.getLevel().getEntities(p_177310_, this.getBounds(), Entity::isAlive);
      if (!$$1.isEmpty()) {
         throw new GameTestAssertException("Did not expect " + p_177310_.toShortString() + " to exist");
      }
   }

   public void assertEntityNotPresent(EntityType<?> p_177398_, int p_177399_, int p_177400_, int p_177401_) {
      this.assertEntityNotPresent(p_177398_, new BlockPos(p_177399_, p_177400_, p_177401_));
   }

   public void assertEntityNotPresent(EntityType<?> p_177403_, BlockPos p_177404_) {
      BlockPos $$2 = this.absolutePos(p_177404_);
      List<? extends Entity> $$3 = this.getLevel().getEntities(p_177403_, new AABB($$2), Entity::isAlive);
      if (!$$3.isEmpty()) {
         throw new GameTestAssertPosException("Did not expect " + p_177403_.toShortString(), $$2, p_177404_, this.testInfo.getTick());
      }
   }

   public void assertEntityNotPresent(EntityType<?> p_341944_, Vec3 p_341926_, Vec3 p_341919_) {
      List<? extends Entity> $$3 = this.getLevel().getEntities(p_341944_, new AABB(p_341926_, p_341919_), Entity::isAlive);
      if (!$$3.isEmpty()) {
         throw new GameTestAssertPosException(
            "Did not expect " + p_341944_.toShortString() + " between ",
            BlockPos.containing(p_341926_),
            BlockPos.containing(p_341919_),
            this.testInfo.getTick()
         );
      }
   }

   public void assertEntityTouching(EntityType<?> p_177159_, double p_177160_, double p_177161_, double p_177162_) {
      Vec3 $$4 = new Vec3(p_177160_, p_177161_, p_177162_);
      Vec3 $$5 = this.absoluteVec($$4);
      Predicate<? super Entity> $$6 = p_177346_ -> p_177346_.getBoundingBox().intersects($$5, $$5);
      List<? extends Entity> $$7 = this.getLevel().getEntities(p_177159_, this.getBounds(), $$6);
      if ($$7.isEmpty()) {
         throw new GameTestAssertException("Expected " + p_177159_.toShortString() + " to touch " + $$5 + " (relative " + $$4 + ")");
      }
   }

   public void assertEntityNotTouching(EntityType<?> p_177312_, double p_177313_, double p_177314_, double p_177315_) {
      Vec3 $$4 = new Vec3(p_177313_, p_177314_, p_177315_);
      Vec3 $$5 = this.absoluteVec($$4);
      Predicate<? super Entity> $$6 = p_177231_ -> !p_177231_.getBoundingBox().intersects($$5, $$5);
      List<? extends Entity> $$7 = this.getLevel().getEntities(p_177312_, this.getBounds(), $$6);
      if ($$7.isEmpty()) {
         throw new GameTestAssertException("Did not expect " + p_177312_.toShortString() + " to touch " + $$5 + " (relative " + $$4 + ")");
      }
   }

   public <E extends Entity, T> void assertEntityData(BlockPos p_177238_, EntityType<E> p_177239_, Function<? super E, T> p_177240_, @Nullable T p_177241_) {
      BlockPos $$4 = this.absolutePos(p_177238_);
      List<E> $$5 = this.getLevel().getEntities(p_177239_, new AABB($$4), Entity::isAlive);
      if ($$5.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + p_177239_.toShortString(), $$4, p_177238_, this.testInfo.getTick());
      } else {
         for (E $$6 : $$5) {
            T $$7 = p_177240_.apply($$6);
            if ($$7 == null) {
               if (p_177241_ != null) {
                  throw new GameTestAssertException("Expected entity data to be: " + p_177241_ + ", but was: " + $$7);
               }
            } else if (!$$7.equals(p_177241_)) {
               throw new GameTestAssertException("Expected entity data to be: " + p_177241_ + ", but was: " + $$7);
            }
         }
      }
   }

   public <E extends LivingEntity> void assertEntityIsHolding(BlockPos p_263501_, EntityType<E> p_263510_, Item p_263517_) {
      BlockPos $$3 = this.absolutePos(p_263501_);
      List<E> $$4 = this.getLevel().getEntities(p_263510_, new AABB($$3), Entity::isAlive);
      if ($$4.isEmpty()) {
         throw new GameTestAssertPosException("Expected entity of type: " + p_263510_, $$3, p_263501_, this.getTick());
      } else {
         for (E $$5 : $$4) {
            if ($$5.isHolding(p_263517_)) {
               return;
            }
         }

         throw new GameTestAssertPosException("Entity should be holding: " + p_263517_, $$3, p_263501_, this.getTick());
      }
   }

   public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(BlockPos p_263495_, EntityType<E> p_263521_, Item p_263502_) {
      BlockPos $$3 = this.absolutePos(p_263495_);
      List<E> $$4 = this.getLevel().getEntities(p_263521_, new AABB($$3), p_263479_ -> p_263479_.isAlive());
      if ($$4.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + p_263521_.toShortString() + " to exist", $$3, p_263495_, this.getTick());
      } else {
         for (E $$5 : $$4) {
            if ($$5.getInventory().hasAnyMatching(p_263481_ -> p_263481_.is(p_263502_))) {
               return;
            }
         }

         throw new GameTestAssertPosException("Entity inventory should contain: " + p_263502_, $$3, p_263495_, this.getTick());
      }
   }

   public void assertContainerEmpty(BlockPos p_177441_) {
      BlockPos $$1 = this.absolutePos(p_177441_);
      BlockEntity $$2 = this.getLevel().getBlockEntity($$1);
      if ($$2 instanceof BaseContainerBlockEntity && !((BaseContainerBlockEntity)$$2).isEmpty()) {
         throw new GameTestAssertException("Container should be empty");
      }
   }

   public void assertContainerContains(BlockPos p_177243_, Item p_177244_) {
      BlockPos $$2 = this.absolutePos(p_177243_);
      BlockEntity $$3 = this.getLevel().getBlockEntity($$2);
      if (!($$3 instanceof BaseContainerBlockEntity)) {
         throw new GameTestAssertException("Expected a container at " + p_177243_ + ", found " + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey($$3.getType()));
      } else if (((BaseContainerBlockEntity)$$3).countItem(p_177244_) != 1) {
         throw new GameTestAssertException("Container should contain: " + p_177244_);
      }
   }

   public void assertSameBlockStates(BoundingBox p_177225_, BlockPos p_177226_) {
      BlockPos.betweenClosedStream(p_177225_).forEach(p_177267_ -> {
         BlockPos $$3 = p_177226_.offset(p_177267_.getX() - p_177225_.minX(), p_177267_.getY() - p_177225_.minY(), p_177267_.getZ() - p_177225_.minZ());
         this.assertSameBlockState(p_177267_, $$3);
      });
   }

   public void assertSameBlockState(BlockPos p_177269_, BlockPos p_177270_) {
      BlockState $$2 = this.getBlockState(p_177269_);
      BlockState $$3 = this.getBlockState(p_177270_);
      if ($$2 != $$3) {
         this.fail("Incorrect state. Expected " + $$3 + ", got " + $$2, p_177269_);
      }
   }

   public void assertAtTickTimeContainerContains(long p_177124_, BlockPos p_177125_, Item p_177126_) {
      this.runAtTickTime(p_177124_, () -> this.assertContainerContains(p_177125_, p_177126_));
   }

   public void assertAtTickTimeContainerEmpty(long p_177121_, BlockPos p_177122_) {
      this.runAtTickTime(p_177121_, () -> this.assertContainerEmpty(p_177122_));
   }

   public <E extends Entity, T> void succeedWhenEntityData(BlockPos p_177350_, EntityType<E> p_177351_, Function<E, T> p_177352_, T p_177353_) {
      this.succeedWhen(() -> this.assertEntityData(p_177350_, p_177351_, p_177352_, p_177353_));
   }

   public void assertEntityPosition(Entity p_353077_, AABB p_353054_, String p_353075_) {
      if (!p_353054_.contains(this.relativeVec(p_353077_.position()))) {
         this.fail(p_353075_);
      }
   }

   public <E extends Entity> void assertEntityProperty(E p_177153_, Predicate<E> p_177154_, String p_177155_) {
      if (!p_177154_.test(p_177153_)) {
         throw new GameTestAssertException("Entity " + p_177153_ + " failed " + p_177155_ + " test");
      }
   }

   public <E extends Entity, T> void assertEntityProperty(E p_177148_, Function<E, T> p_177149_, String p_177150_, T p_177151_) {
      T $$4 = p_177149_.apply(p_177148_);
      if (!$$4.equals(p_177151_)) {
         throw new GameTestAssertException("Entity " + p_177148_ + " value " + p_177150_ + "=" + $$4 + " is not equal to expected " + p_177151_);
      }
   }

   public void assertLivingEntityHasMobEffect(LivingEntity p_296040_, Holder<MobEffect> p_316528_, int p_294266_) {
      MobEffectInstance $$3 = p_296040_.getEffect(p_316528_);
      if ($$3 == null || $$3.getAmplifier() != p_294266_) {
         int $$4 = p_294266_ + 1;
         throw new GameTestAssertException("Entity " + p_296040_ + " failed has " + p_316528_.value().getDescriptionId() + " x " + $$4 + " test");
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
      BlockPos $$1 = this.absolutePos(p_177447_);
      ServerLevel $$2 = this.getLevel();
      $$2.getBlockState($$1).randomTick($$2, $$1, $$2.random);
   }

   public void tickPrecipitation(BlockPos p_313817_) {
      BlockPos $$1 = this.absolutePos(p_313817_);
      ServerLevel $$2 = this.getLevel();
      $$2.tickPrecipitation($$1);
   }

   public void tickPrecipitation() {
      AABB $$0 = this.getRelativeBounds();
      int $$1 = (int)Math.floor($$0.maxX);
      int $$2 = (int)Math.floor($$0.maxZ);
      int $$3 = (int)Math.floor($$0.maxY);

      for (int $$4 = (int)Math.floor($$0.minX); $$4 < $$1; $$4++) {
         for (int $$5 = (int)Math.floor($$0.minZ); $$5 < $$2; $$5++) {
            this.tickPrecipitation(new BlockPos($$4, $$3, $$5));
         }
      }
   }

   public int getHeight(Heightmap.Types p_236775_, int p_236776_, int p_236777_) {
      BlockPos $$3 = this.absolutePos(new BlockPos(p_236776_, 0, p_236777_));
      return this.relativePos(this.getLevel().getHeightmapPos(p_236775_, $$3)).getY();
   }

   public void fail(String p_177290_, BlockPos p_177291_) {
      throw new GameTestAssertPosException(p_177290_, this.absolutePos(p_177291_), p_177291_, this.getTick());
   }

   public void fail(String p_177287_, Entity p_177288_) {
      throw new GameTestAssertPosException(p_177287_, p_177288_.blockPosition(), this.relativePos(p_177288_.blockPosition()), this.getTick());
   }

   public void fail(String p_177285_) {
      throw new GameTestAssertException(p_177285_);
   }

   public void failIf(Runnable p_177393_) {
      this.testInfo.createSequence().thenWaitUntil(p_177393_).thenFail(() -> new GameTestAssertException("Fail conditions met"));
   }

   public void failIfEver(Runnable p_177411_) {
      LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks())
         .forEach(p_177365_ -> this.testInfo.setRunAtTickTime(p_177365_, p_177411_::run));
   }

   public GameTestSequence startSequence() {
      return this.testInfo.createSequence();
   }

   public BlockPos absolutePos(BlockPos p_177450_) {
      BlockPos $$1 = this.testInfo.getStructureBlockPos();
      BlockPos $$2 = $$1.offset(p_177450_);
      return StructureTemplate.transform($$2, Mirror.NONE, this.testInfo.getRotation(), $$1);
   }

   public BlockPos relativePos(BlockPos p_177453_) {
      BlockPos $$1 = this.testInfo.getStructureBlockPos();
      Rotation $$2 = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
      BlockPos $$3 = StructureTemplate.transform(p_177453_, Mirror.NONE, $$2, $$1);
      return $$3.subtract($$1);
   }

   public Vec3 absoluteVec(Vec3 p_177228_) {
      Vec3 $$1 = Vec3.atLowerCornerOf(this.testInfo.getStructureBlockPos());
      return StructureTemplate.transform($$1.add(p_177228_), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
   }

   public Vec3 relativeVec(Vec3 p_251543_) {
      Vec3 $$1 = Vec3.atLowerCornerOf(this.testInfo.getStructureBlockPos());
      return StructureTemplate.transform(p_251543_.subtract($$1), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
   }

   public Rotation getTestRotation() {
      return this.testInfo.getRotation();
   }

   public void assertTrue(boolean p_249380_, String p_248720_) {
      if (!p_249380_) {
         throw new GameTestAssertException(p_248720_);
      }
   }

   public <N> void assertValueEqual(N p_324013_, N p_323869_, String p_321732_) {
      if (!p_324013_.equals(p_323869_)) {
         throw new GameTestAssertException("Expected " + p_321732_ + " to be " + p_323869_ + ", but was " + p_324013_);
      }
   }

   public void assertFalse(boolean p_277974_, String p_277933_) {
      if (p_277974_) {
         throw new GameTestAssertException(p_277933_);
      }
   }

   public long getTick() {
      return this.testInfo.getTick();
   }

   public AABB getBounds() {
      return this.testInfo.getStructureBounds();
   }

   private AABB getRelativeBounds() {
      AABB $$0 = this.testInfo.getStructureBounds();
      return $$0.move(BlockPos.ZERO.subtract(this.absolutePos(BlockPos.ZERO)));
   }

   public void forEveryBlockInStructure(Consumer<BlockPos> p_177293_) {
      AABB $$1 = this.getRelativeBounds().contract(1.0, 1.0, 1.0);
      BlockPos.MutableBlockPos.betweenClosedStream($$1).forEach(p_177293_);
   }

   public void onEachTick(Runnable p_177424_) {
      LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks())
         .forEach(p_177283_ -> this.testInfo.setRunAtTickTime(p_177283_, p_177424_::run));
   }

   public void placeAt(Player p_261595_, ItemStack p_262007_, BlockPos p_261973_, Direction p_262008_) {
      BlockPos $$4 = this.absolutePos(p_261973_.relative(p_262008_));
      BlockHitResult $$5 = new BlockHitResult(Vec3.atCenterOf($$4), p_262008_, $$4, false);
      UseOnContext $$6 = new UseOnContext(p_261595_, InteractionHand.MAIN_HAND, $$5);
      p_262007_.useOn($$6);
   }

   public void setBiome(ResourceKey<Biome> p_313921_) {
      AABB $$1 = this.getBounds();
      BlockPos $$2 = BlockPos.containing($$1.minX, $$1.minY, $$1.minZ);
      BlockPos $$3 = BlockPos.containing($$1.maxX, $$1.maxY, $$1.maxZ);
      Either<Integer, CommandSyntaxException> $$4 = FillBiomeCommand.fill(
         this.getLevel(), $$2, $$3, this.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(p_313921_)
      );
      if ($$4.right().isPresent()) {
         this.fail("Failed to set biome for test");
      }
   }
}

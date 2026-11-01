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
import org.jspecify.annotations.Nullable;

public class GameTestHelper {
   private final GameTestInfo testInfo;
   private boolean finalCheckAdded;

   public GameTestHelper(GameTestInfo testInfo) {
      this.testInfo = testInfo;
   }

   public GameTestAssertException assertionException(Component message) {
      return new GameTestAssertException(message, this.testInfo.getTick());
   }

   public GameTestAssertException assertionException(String messageKey, Object... args) {
      return this.assertionException(Component.translatableEscape(messageKey, args));
   }

   public GameTestAssertPosException assertionException(BlockPos pos, Component message) {
      return new GameTestAssertPosException(message, this.absolutePos(pos), pos, this.testInfo.getTick());
   }

   public GameTestAssertPosException assertionException(BlockPos pos, String messageKey, Object... args) {
      return this.assertionException(pos, Component.translatableEscape(messageKey, args));
   }

   public ServerLevel getLevel() {
      return this.testInfo.getLevel();
   }

   public BlockState getBlockState(BlockPos pos) {
      return this.getLevel().getBlockState(this.absolutePos(pos));
   }

   public <T extends BlockEntity> T getBlockEntity(BlockPos pos, Class<T> clazz) {
      BlockEntity blockEntity = this.getLevel().getBlockEntity(this.absolutePos(pos));
      if (blockEntity == null) {
         throw this.assertionException(pos, "test.error.missing_block_entity");
      } else if (clazz.isInstance(blockEntity)) {
         return clazz.cast(blockEntity);
      } else {
         throw this.assertionException(pos, "test.error.wrong_block_entity", blockEntity.getType().builtInRegistryHolder().getRegisteredName());
      }
   }

   public void killAllEntities() {
      this.killAllEntitiesOfClass(Entity.class);
   }

   public void killAllEntitiesOfClass(Class<? extends Entity> entityClass) {
      AABB aABB = this.getBounds();
      List<? extends Entity> list = this.getLevel().getEntitiesOfClass(entityClass, aABB.inflate(1.0), entity -> !(entity instanceof Player));
      list.forEach(entity -> entity.kill(this.getLevel()));
   }

   public ItemEntity spawnItem(Item item, Vec3 pos) {
      ServerLevel serverLevel = this.getLevel();
      Vec3 vec3 = this.absoluteVec(pos);
      ItemEntity itemEntity = new ItemEntity(serverLevel, vec3.x, vec3.y, vec3.z, new ItemStack(item, 1));
      itemEntity.setDeltaMovement(0.0, 0.0, 0.0);
      serverLevel.addFreshEntity(itemEntity);
      return itemEntity;
   }

   public ItemEntity spawnItem(Item item, float x, float y, float z) {
      return this.spawnItem(item, new Vec3(x, y, z));
   }

   public ItemEntity spawnItem(Item item, BlockPos pos) {
      return this.spawnItem(item, pos.getX(), pos.getY(), pos.getZ());
   }

   public <E extends Entity> E spawn(EntityType<E> type, BlockPos pos) {
      return this.spawn(type, Vec3.atBottomCenterOf(pos));
   }

   public <E extends Entity> List<E> spawn(EntityType<E> type, BlockPos pos, int count) {
      return this.spawn(type, Vec3.atBottomCenterOf(pos), count);
   }

   public <E extends Entity> List<E> spawn(EntityType<E> type, Vec3 pos, int count) {
      List<E> list = new ArrayList<>();

      for (int i = 0; i < count; i++) {
         list.add(this.spawn(type, pos));
      }

      return list;
   }

   public <E extends Entity> E spawn(EntityType<E> type, Vec3 pos) {
      return this.spawn(type, pos, null);
   }

   public <E extends Entity> E spawn(EntityType<E> type, Vec3 pos, @Nullable EntitySpawnReason reason) {
      ServerLevel serverLevel = this.getLevel();
      E entity = type.create(serverLevel, EntitySpawnReason.STRUCTURE);
      if (entity == null) {
         throw this.assertionException(BlockPos.containing(pos), "test.error.spawn_failure", type.builtInRegistryHolder().getRegisteredName());
      } else {
         if (entity instanceof Mob mob) {
            mob.setPersistenceRequired();
         }

         Vec3 vec3 = this.absoluteVec(pos);
         float f = entity.rotate(this.getTestRotation());
         entity.snapTo(vec3.x, vec3.y, vec3.z, f, entity.getXRot());
         entity.setYBodyRot(f);
         entity.setYHeadRot(f);
         if (reason != null && entity instanceof Mob mob2) {
            mob2.finalizeSpawn(this.getLevel(), this.getLevel().getCurrentDifficultyAt(mob2.blockPosition()), reason, null);
         }

         serverLevel.addFreshEntityWithPassengers(entity);
         return entity;
      }
   }

   public <E extends Mob> E spawn(EntityType<E> type, int x, int y, int z, EntitySpawnReason reason) {
      return this.spawn(type, new Vec3(x, y, z), reason);
   }

   public void hurt(Entity entity, DamageSource damageSource, float amount) {
      entity.hurtServer(this.getLevel(), damageSource, amount);
   }

   public void kill(Entity entity) {
      entity.kill(this.getLevel());
   }

   public <E extends Entity> E findOneEntity(EntityType<E> type) {
      return this.findClosestEntity(type, 0, 0, 0, 2.147483647E9);
   }

   public <E extends Entity> E findClosestEntity(EntityType<E> type, int x, int y, int z, double radius) {
      List<E> list = this.findEntities(type, x, y, z, radius);
      if (list.isEmpty()) {
         throw this.assertionException("test.error.expected_entity_around", type.getDescription(), x, y, z);
      } else if (list.size() > 1) {
         throw this.assertionException("test.error.too_many_entities", type.toShortString(), x, y, z, list.size());
      } else {
         Vec3 vec3 = this.absoluteVec(new Vec3(x, y, z));
         list.sort((entity, entity2) -> {
            double d = entity.position().distanceTo(vec3);
            double e = entity2.position().distanceTo(vec3);
            return Double.compare(d, e);
         });
         return list.get(0);
      }
   }

   public <E extends Entity> List<E> findEntities(EntityType<E> type, int x, int y, int z, double radius) {
      return this.findEntities(type, Vec3.atBottomCenterOf(new BlockPos(x, y, z)), radius);
   }

   public <E extends Entity> List<E> findEntities(EntityType<E> type, Vec3 pos, double radius) {
      ServerLevel serverLevel = this.getLevel();
      Vec3 vec3 = this.absoluteVec(pos);
      AABB aABB = this.testInfo.getStructureBounds();
      AABB aABB2 = new AABB(vec3.add(-radius, -radius, -radius), vec3.add(radius, radius, radius));
      return serverLevel.getEntities(type, aABB, entity -> entity.getBoundingBox().intersects(aABB2) && entity.isAlive());
   }

   public <E extends Entity> E spawn(EntityType<E> type, int x, int y, int z) {
      return this.spawn(type, new BlockPos(x, y, z));
   }

   public <E extends Entity> E spawn(EntityType<E> type, float x, float y, float z) {
      return this.spawn(type, new Vec3(x, y, z));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, BlockPos pos) {
      E mob = (E)this.spawn(type, pos);
      mob.removeFreeWill();
      return mob;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, int x, int y, int z) {
      return this.spawnWithNoFreeWill(type, new BlockPos(x, y, z));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, Vec3 pos) {
      E mob = (E)this.spawn(type, pos);
      mob.removeFreeWill();
      return mob;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> type, float x, float y, float z) {
      return this.spawnWithNoFreeWill(type, new Vec3(x, y, z));
   }

   public void moveTo(Mob mob, float x, float y, float z) {
      Vec3 vec3 = this.absoluteVec(new Vec3(x, y, z));
      mob.snapTo(vec3.x, vec3.y, vec3.z, mob.getYRot(), mob.getXRot());
   }

   public GameTestSequence walkTo(Mob mob, BlockPos pos, float speed) {
      return this.startSequence().thenExecuteAfter(2, () -> {
         Path path = mob.getNavigation().createPath(this.absolutePos(pos), 0);
         mob.getNavigation().moveTo(path, (double)speed);
      });
   }

   public void pressButton(int x, int y, int z) {
      this.pressButton(new BlockPos(x, y, z));
   }

   public void pressButton(BlockPos pos) {
      this.assertBlockTag(BlockTags.BUTTONS, pos);
      BlockPos blockPos = this.absolutePos(pos);
      BlockState blockState = this.getLevel().getBlockState(blockPos);
      ButtonBlock buttonBlock = (ButtonBlock)blockState.getBlock();
      buttonBlock.press(blockState, this.getLevel(), blockPos, null);
   }

   public void useBlock(BlockPos pos) {
      this.useBlock(pos, this.makeMockPlayer(GameType.CREATIVE));
   }

   public void useBlock(BlockPos pos, Player player) {
      BlockPos blockPos = this.absolutePos(pos);
      this.useBlock(pos, player, new BlockHitResult(Vec3.atCenterOf(blockPos), Direction.NORTH, blockPos, true));
   }

   public void useBlock(BlockPos pos, Player player, BlockHitResult result) {
      BlockPos blockPos = this.absolutePos(pos);
      BlockState blockState = this.getLevel().getBlockState(blockPos);
      InteractionHand interactionHand = InteractionHand.MAIN_HAND;
      InteractionResult interactionResult = blockState.useItemOn(player.getItemInHand(interactionHand), this.getLevel(), player, interactionHand, result);
      if (!interactionResult.consumesAction()) {
         if (!(interactionResult instanceof InteractionResult.TryEmptyHandInteraction)
            || !blockState.useWithoutItem(this.getLevel(), player, result).consumesAction()) {
            UseOnContext useOnContext = new UseOnContext(player, interactionHand, result);
            player.getItemInHand(interactionHand).useOn(useOnContext);
         }
      }
   }

   public LivingEntity makeAboutToDrown(LivingEntity entity) {
      entity.setAirSupply(0);
      entity.setHealth(0.25F);
      return entity;
   }

   public LivingEntity withLowHealth(LivingEntity entity) {
      entity.setHealth(0.25F);
      return entity;
   }

   public Player makeMockPlayer(GameType gameType) {
      return new Player(this.getLevel(), new GameProfile(UUID.randomUUID(), "test-mock-player")) {
         @Override
         public GameType gameMode() {
            return gameType;
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
      CommonListenerCookie commonListenerCookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "test-mock-player"), false);
      ServerPlayer serverPlayer = new ServerPlayer(
         this.getLevel().getServer(), this.getLevel(), commonListenerCookie.gameProfile(), commonListenerCookie.clientInformation()
      ) {
         @Override
         public GameType gameMode() {
            return GameType.CREATIVE;
         }
      };
      Connection connection = new Connection(PacketFlow.SERVERBOUND);
      new EmbeddedChannel(new ChannelHandler[]{connection});
      this.getLevel().getServer().getPlayerList().placeNewPlayer(connection, serverPlayer, commonListenerCookie);
      return serverPlayer;
   }

   public void pullLever(int x, int y, int z) {
      this.pullLever(new BlockPos(x, y, z));
   }

   public void pullLever(BlockPos pos) {
      this.assertBlockPresent(Blocks.LEVER, pos);
      BlockPos blockPos = this.absolutePos(pos);
      BlockState blockState = this.getLevel().getBlockState(blockPos);
      LeverBlock leverBlock = (LeverBlock)blockState.getBlock();
      leverBlock.pull(blockState, this.getLevel(), blockPos, null);
   }

   public void pulseRedstone(BlockPos pos, long delay) {
      this.setBlock(pos, Blocks.REDSTONE_BLOCK);
      this.runAfterDelay(delay, () -> this.setBlock(pos, Blocks.AIR));
   }

   public void destroyBlock(BlockPos pos) {
      this.getLevel().destroyBlock(this.absolutePos(pos), false, null);
   }

   public void setBlock(int x, int y, int z, Block block) {
      this.setBlock(new BlockPos(x, y, z), block);
   }

   public void setBlock(int x, int y, int z, BlockState state) {
      this.setBlock(new BlockPos(x, y, z), state);
   }

   public void setBlock(BlockPos pos, Block block) {
      this.setBlock(pos, block.defaultBlockState());
   }

   public void setBlock(BlockPos pos, BlockState state) {
      this.getLevel().setBlock(this.absolutePos(pos), state, 3);
   }

   public void setBlock(BlockPos pos, Block block, Direction facing) {
      this.setBlock(pos, block.defaultBlockState(), facing);
   }

   public void setBlock(BlockPos pos, BlockState state, Direction facing) {
      BlockState blockState = state;
      if (state.hasProperty(HorizontalDirectionalBlock.FACING)) {
         blockState = state.setValue(HorizontalDirectionalBlock.FACING, facing);
      }

      if (state.hasProperty(BlockStateProperties.FACING)) {
         blockState = state.setValue(BlockStateProperties.FACING, facing);
      }

      this.getLevel().setBlock(this.absolutePos(pos), blockState, 3);
   }

   public void assertBlockPresent(Block block, int x, int y, int z) {
      this.assertBlockPresent(block, new BlockPos(x, y, z));
   }

   public void assertBlockPresent(Block block, BlockPos pos) {
      BlockState blockState = this.getBlockState(pos);
      this.assertBlock(pos, block2 -> blockState.is(block), block2 -> Component.translatable("test.error.expected_block", block.getName(), block2.getName()));
   }

   public void assertBlockNotPresent(Block block, int x, int y, int z) {
      this.assertBlockNotPresent(block, new BlockPos(x, y, z));
   }

   public void assertBlockNotPresent(Block block, BlockPos pos) {
      this.assertBlock(pos, block2 -> !this.getBlockState(pos).is(block), block2 -> Component.translatable("test.error.unexpected_block", block.getName()));
   }

   public void assertBlockTag(TagKey<Block> tag, BlockPos pos) {
      this.assertBlockState(
         pos,
         blockState -> blockState.is(tag),
         blockState -> Component.translatable("test.error.expected_block_tag", Component.translationArg(tag.location()), blockState.getBlock().getName())
      );
   }

   public void succeedWhenBlockPresent(Block block, int x, int y, int z) {
      this.succeedWhenBlockPresent(block, new BlockPos(x, y, z));
   }

   public void succeedWhenBlockPresent(Block block, BlockPos pos) {
      this.succeedWhen(() -> this.assertBlockPresent(block, pos));
   }

   public void assertBlock(BlockPos pos, Predicate<Block> predicate, Function<Block, Component> message) {
      this.assertBlockState(pos, blockState -> predicate.test(blockState.getBlock()), blockState -> message.apply(blockState.getBlock()));
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos pos, Property<T> property, T value) {
      BlockState blockState = this.getBlockState(pos);
      boolean bl = blockState.hasProperty(property);
      if (!bl) {
         throw this.assertionException(pos, "test.error.block_property_missing", property.getName(), value);
      } else if (!blockState.<T>getValue(property).equals(value)) {
         throw this.assertionException(pos, "test.error.block_property_mismatch", property.getName(), value, blockState.getValue(property));
      }
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos pos, Property<T> property, Predicate<T> predicate, Component message) {
      this.assertBlockState(pos, blockState -> {
         if (!blockState.hasProperty(property)) {
            return false;
         } else {
            T comparable = blockState.getValue(property);
            return predicate.test(comparable);
         }
      }, blockState -> message);
   }

   public void assertBlockState(BlockPos pos, BlockState state) {
      BlockState blockState = this.getBlockState(pos);
      if (!blockState.equals(state)) {
         throw this.assertionException(pos, "test.error.state_not_equal", state, blockState);
      }
   }

   public void assertBlockState(BlockPos pos, Predicate<BlockState> predicate, Function<BlockState, Component> message) {
      BlockState blockState = this.getBlockState(pos);
      if (!predicate.test(blockState)) {
         throw this.assertionException(pos, message.apply(blockState));
      }
   }

   public <T extends BlockEntity> void assertBlockEntityData(BlockPos pos, Class<T> blockEntityClass, Predicate<T> predicate, Supplier<Component> message) {
      T blockEntity = this.getBlockEntity(pos, blockEntityClass);
      if (!predicate.test(blockEntity)) {
         throw this.assertionException(pos, message.get());
      }
   }

   public void assertRedstoneSignal(BlockPos pos, Direction direction, IntPredicate signalStrengthPredicate, Supplier<Component> message) {
      BlockPos blockPos = this.absolutePos(pos);
      ServerLevel serverLevel = this.getLevel();
      BlockState blockState = serverLevel.getBlockState(blockPos);
      int i = blockState.getSignal(serverLevel, blockPos, direction);
      if (!signalStrengthPredicate.test(i)) {
         throw this.assertionException(pos, message.get());
      }
   }

   public void assertEntityPresent(EntityType<?> type) {
      if (!this.getLevel().hasEntities(type, this.getBounds(), Entity::isAlive)) {
         throw this.assertionException("test.error.expected_entity_in_test", type.getDescription());
      }
   }

   public void assertEntityPresent(EntityType<?> type, int x, int y, int z) {
      this.assertEntityPresent(type, new BlockPos(x, y, z));
   }

   public void assertEntityPresent(EntityType<?> type, BlockPos pos) {
      BlockPos blockPos = this.absolutePos(pos);
      if (!this.getLevel().hasEntities(type, new AABB(blockPos), Entity::isAlive)) {
         throw this.assertionException(pos, "test.error.expected_entity", type.getDescription());
      }
   }

   public void assertEntityPresent(EntityType<?> type, AABB box) {
      AABB aABB = this.absoluteAABB(box);
      if (!this.getLevel().hasEntities(type, aABB, Entity::isAlive)) {
         throw this.assertionException(BlockPos.containing(box.getCenter()), "test.error.expected_entity", type.getDescription());
      }
   }

   public void assertEntityPresent(EntityType<?> type, AABB box, Component message) {
      AABB aABB = this.absoluteAABB(box);
      if (!this.getLevel().hasEntities(type, aABB, Entity::isAlive)) {
         throw this.assertionException(BlockPos.containing(box.getCenter()), message);
      }
   }

   public void assertEntitiesPresent(EntityType<?> entityType, int count) {
      List<? extends Entity> list = this.getLevel().getEntities(entityType, this.getBounds(), Entity::isAlive);
      if (list.size() != count) {
         throw this.assertionException("test.error.expected_entity_count", count, entityType.getDescription(), list.size());
      }
   }

   public void assertEntitiesPresent(EntityType<?> entityType, BlockPos pos, int count, double radius) {
      BlockPos blockPos = this.absolutePos(pos);
      List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)entityType, pos, radius);
      if (list.size() != count) {
         throw this.assertionException(pos, "test.error.expected_entity_count", count, entityType.getDescription(), list.size());
      }
   }

   public void assertEntityPresent(EntityType<?> type, BlockPos pos, double expansionAmount) {
      List<? extends Entity> list = this.getEntities((EntityType<? extends Entity>)type, pos, expansionAmount);
      if (list.isEmpty()) {
         BlockPos blockPos = this.absolutePos(pos);
         throw this.assertionException(pos, "test.error.expected_entity", type.getDescription());
      }
   }

   public <T extends Entity> List<T> getEntities(EntityType<T> entityType, BlockPos pos, double radius) {
      BlockPos blockPos = this.absolutePos(pos);
      return this.getLevel().getEntities(entityType, new AABB(blockPos).inflate(radius), Entity::isAlive);
   }

   public <T extends Entity> List<T> getEntities(EntityType<T> entityType) {
      return this.getLevel().getEntities(entityType, this.getBounds(), Entity::isAlive);
   }

   public void assertEntityInstancePresent(Entity entity, int x, int y, int z) {
      this.assertEntityInstancePresent(entity, new BlockPos(x, y, z));
   }

   public void assertEntityInstancePresent(Entity entity, BlockPos pos) {
      BlockPos blockPos = this.absolutePos(pos);
      List<? extends Entity> list = this.getLevel().getEntities(entity.getType(), new AABB(blockPos), Entity::isAlive);
      list.stream()
         .filter(entity2 -> entity2 == entity)
         .findFirst()
         .orElseThrow(() -> this.assertionException(pos, "test.error.expected_entity", entity.getType().getDescription()));
   }

   public void assertItemEntityCountIs(Item item, BlockPos pos, double expansionAmount, int count) {
      BlockPos blockPos = this.absolutePos(pos);
      List<ItemEntity> list = this.getLevel().getEntities(EntityType.ITEM, new AABB(blockPos).inflate(expansionAmount), Entity::isAlive);
      int i = 0;

      for (ItemEntity itemEntity : list) {
         ItemStack itemStack = itemEntity.getItem();
         if (itemStack.is(item)) {
            i += itemStack.getCount();
         }
      }

      if (i != count) {
         throw this.assertionException(pos, "test.error.expected_items_count", count, item.getName(), i);
      }
   }

   public void assertItemEntityPresent(Item item, BlockPos pos, double expansionAmount) {
      BlockPos blockPos = this.absolutePos(pos);
      Predicate<ItemEntity> predicate = itemEntity -> itemEntity.isAlive() && itemEntity.getItem().is(item);
      if (!this.getLevel().hasEntities(EntityType.ITEM, new AABB(blockPos).inflate(expansionAmount), predicate)) {
         throw this.assertionException(pos, "test.error.expected_item", item.getName());
      }
   }

   public void assertItemEntityNotPresent(Item item, BlockPos pos, double radius) {
      BlockPos blockPos = this.absolutePos(pos);
      Predicate<ItemEntity> predicate = itemEntity -> itemEntity.isAlive() && itemEntity.getItem().is(item);
      if (this.getLevel().hasEntities(EntityType.ITEM, new AABB(blockPos).inflate(radius), predicate)) {
         throw this.assertionException(pos, "test.error.unexpected_item", item.getName());
      }
   }

   public void assertItemEntityPresent(Item item) {
      Predicate<ItemEntity> predicate = itemEntity -> itemEntity.isAlive() && itemEntity.getItem().is(item);
      if (!this.getLevel().hasEntities(EntityType.ITEM, this.getBounds(), predicate)) {
         throw this.assertionException("test.error.expected_item", item.getName());
      }
   }

   public void assertItemEntityNotPresent(Item item) {
      Predicate<ItemEntity> predicate = itemEntity -> itemEntity.isAlive() && itemEntity.getItem().is(item);
      if (this.getLevel().hasEntities(EntityType.ITEM, this.getBounds(), predicate)) {
         throw this.assertionException("test.error.unexpected_item", item.getName());
      }
   }

   public void assertEntityNotPresent(EntityType<?> type) {
      List<? extends Entity> list = this.getLevel().getEntities(type, this.getBounds(), Entity::isAlive);
      if (!list.isEmpty()) {
         throw this.assertionException(list.getFirst().blockPosition(), "test.error.unexpected_entity", type.getDescription());
      }
   }

   public void assertEntityNotPresent(EntityType<?> type, int x, int y, int z) {
      this.assertEntityNotPresent(type, new BlockPos(x, y, z));
   }

   public void assertEntityNotPresent(EntityType<?> type, BlockPos pos) {
      BlockPos blockPos = this.absolutePos(pos);
      if (this.getLevel().hasEntities(type, new AABB(blockPos), Entity::isAlive)) {
         throw this.assertionException(pos, "test.error.unexpected_entity", type.getDescription());
      }
   }

   public void assertEntityNotPresent(EntityType<?> type, AABB box) {
      AABB aABB = this.absoluteAABB(box);
      List<? extends Entity> list = this.getLevel().getEntities(type, aABB, Entity::isAlive);
      if (!list.isEmpty()) {
         throw this.assertionException(list.getFirst().blockPosition(), "test.error.unexpected_entity", type.getDescription());
      }
   }

   public void assertEntityTouching(EntityType<?> type, double x, double y, double z) {
      Vec3 vec3 = new Vec3(x, y, z);
      Vec3 vec32 = this.absoluteVec(vec3);
      Predicate<? super Entity> predicate = entity -> entity.getBoundingBox().intersects(vec32, vec32);
      if (!this.getLevel().hasEntities(type, this.getBounds(), predicate)) {
         throw this.assertionException("test.error.expected_entity_touching", type.getDescription(), vec32.x(), vec32.y(), vec32.z(), x, y, z);
      }
   }

   public void assertEntityNotTouching(EntityType<?> type, double x, double y, double z) {
      Vec3 vec3 = new Vec3(x, y, z);
      Vec3 vec32 = this.absoluteVec(vec3);
      Predicate<? super Entity> predicate = entity -> !entity.getBoundingBox().intersects(vec32, vec32);
      if (!this.getLevel().hasEntities(type, this.getBounds(), predicate)) {
         throw this.assertionException("test.error.expected_entity_not_touching", type.getDescription(), vec32.x(), vec32.y(), vec32.z(), x, y, z);
      }
   }

   public <E extends Entity, T> void assertEntityData(BlockPos pos, EntityType<E> type, Predicate<E> predicate) {
      BlockPos blockPos = this.absolutePos(pos);
      List<E> list = this.getLevel().getEntities(type, new AABB(blockPos), Entity::isAlive);
      if (list.isEmpty()) {
         throw this.assertionException(pos, "test.error.expected_entity", type.getDescription());
      } else {
         for (E entity : list) {
            if (!predicate.test(entity)) {
               throw this.assertionException(entity.blockPosition(), "test.error.expected_entity_data_predicate", entity.getName());
            }
         }
      }
   }

   public <E extends Entity, T> void assertEntityData(BlockPos pos, EntityType<E> type, Function<? super E, T> entityDataGetter, @Nullable T expectedValue) {
      this.assertEntityData(new AABB(pos), type, entityDataGetter, expectedValue);
   }

   public <E extends Entity, T> void assertEntityData(AABB area, EntityType<E> type, Function<? super E, T> entityDataGetter, @Nullable T expectedValue) {
      List<E> list = this.getLevel().getEntities(type, this.absoluteAABB(area), Entity::isAlive);
      if (list.isEmpty()) {
         throw this.assertionException(BlockPos.containing(area.getBottomCenter()), "test.error.expected_entity", type.getDescription());
      } else {
         for (E entity : list) {
            T object = entityDataGetter.apply(entity);
            if (!Objects.equals(object, expectedValue)) {
               throw this.assertionException(BlockPos.containing(area.getBottomCenter()), "test.error.expected_entity_data", expectedValue, object);
            }
         }
      }
   }

   public <E extends LivingEntity> void assertEntityIsHolding(BlockPos pos, EntityType<E> entityType, Item item) {
      BlockPos blockPos = this.absolutePos(pos);
      List<E> list = this.getLevel().getEntities(entityType, new AABB(blockPos), Entity::isAlive);
      if (list.isEmpty()) {
         throw this.assertionException(pos, "test.error.expected_entity", entityType.getDescription());
      } else {
         for (E livingEntity : list) {
            if (livingEntity.isHolding(item)) {
               return;
            }
         }

         throw this.assertionException(pos, "test.error.expected_entity_holding", item.getName());
      }
   }

   public <E extends Entity & InventoryCarrier> void assertEntityInventoryContains(BlockPos pos, EntityType<E> entityType, Item item) {
      BlockPos blockPos = this.absolutePos(pos);
      List<E> list = this.getLevel().getEntities(entityType, new AABB(blockPos), object -> object.isAlive());
      if (list.isEmpty()) {
         throw this.assertionException(pos, "test.error.expected_entity", entityType.getDescription());
      } else {
         for (E entity : list) {
            if (entity.getInventory().hasAnyMatching(itemStack -> itemStack.is(item))) {
               return;
            }
         }

         throw this.assertionException(pos, "test.error.expected_entity_having", item.getName());
      }
   }

   public void assertContainerEmpty(BlockPos pos) {
      BaseContainerBlockEntity baseContainerBlockEntity = this.getBlockEntity(pos, BaseContainerBlockEntity.class);
      if (!baseContainerBlockEntity.isEmpty()) {
         throw this.assertionException(pos, "test.error.expected_empty_container");
      }
   }

   public void assertContainerContainsSingle(BlockPos pos, Item item) {
      BaseContainerBlockEntity baseContainerBlockEntity = this.getBlockEntity(pos, BaseContainerBlockEntity.class);
      if (baseContainerBlockEntity.countItem(item) != 1) {
         throw this.assertionException(pos, "test.error.expected_container_contents_single", item.getName());
      }
   }

   public void assertContainerContains(BlockPos pos, Item item) {
      BaseContainerBlockEntity baseContainerBlockEntity = this.getBlockEntity(pos, BaseContainerBlockEntity.class);
      if (baseContainerBlockEntity.countItem(item) == 0) {
         throw this.assertionException(pos, "test.error.expected_container_contents", item.getName());
      }
   }

   public void assertSameBlockStates(BoundingBox boundingBox, BlockPos pos) {
      BlockPos.betweenClosedStream(boundingBox).forEach(blockPos2 -> {
         BlockPos blockPos3 = pos.offset(blockPos2.getX() - boundingBox.minX(), blockPos2.getY() - boundingBox.minY(), blockPos2.getZ() - boundingBox.minZ());
         this.assertSameBlockState(blockPos2, blockPos3);
      });
   }

   public void assertSameBlockState(BlockPos testPos, BlockPos comparisonPos) {
      BlockState blockState = this.getBlockState(testPos);
      BlockState blockState2 = this.getBlockState(comparisonPos);
      if (blockState != blockState2) {
         throw this.assertionException(testPos, "test.error.state_not_equal", blockState2, blockState);
      }
   }

   public void assertAtTickTimeContainerContains(long tickTime, BlockPos pos, Item item) {
      this.runAtTickTime(tickTime, () -> this.assertContainerContainsSingle(pos, item));
   }

   public void assertAtTickTimeContainerEmpty(long tickTime, BlockPos pos) {
      this.runAtTickTime(tickTime, () -> this.assertContainerEmpty(pos));
   }

   public <E extends Entity, T> void succeedWhenEntityData(BlockPos pos, EntityType<E> type, Function<E, T> entityDataGetter, T testEntityData) {
      this.succeedWhen(() -> this.assertEntityData(pos, type, entityDataGetter, testEntityData));
   }

   public <E extends Entity> void assertEntityProperty(E entity, Predicate<E> predicate, Component message) {
      if (!predicate.test(entity)) {
         throw this.assertionException(entity.blockPosition(), "test.error.entity_property", entity.getName(), message);
      }
   }

   public <E extends Entity, T> void assertEntityProperty(E entity, Function<E, T> valueGetter, T expectedValue, Component message) {
      T object = valueGetter.apply(entity);
      if (!object.equals(expectedValue)) {
         throw this.assertionException(entity.blockPosition(), "test.error.entity_property_details", entity.getName(), message, object, expectedValue);
      }
   }

   public void assertLivingEntityHasMobEffect(LivingEntity entity, Holder<MobEffect> effect, int amplifier) {
      MobEffectInstance mobEffectInstance = entity.getEffect(effect);
      if (mobEffectInstance == null || mobEffectInstance.getAmplifier() != amplifier) {
         throw this.assertionException("test.error.expected_entity_effect", entity.getName(), PotionContents.getPotionDescription(effect, amplifier));
      }
   }

   public void succeedWhenEntityPresent(EntityType<?> type, int x, int y, int z) {
      this.succeedWhenEntityPresent(type, new BlockPos(x, y, z));
   }

   public void succeedWhenEntityPresent(EntityType<?> type, BlockPos pos) {
      this.succeedWhen(() -> this.assertEntityPresent(type, pos));
   }

   public void succeedWhenEntityNotPresent(EntityType<?> type, int x, int y, int z) {
      this.succeedWhenEntityNotPresent(type, new BlockPos(x, y, z));
   }

   public void succeedWhenEntityNotPresent(EntityType<?> type, BlockPos pos) {
      this.succeedWhen(() -> this.assertEntityNotPresent(type, pos));
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

   public void succeedIf(Runnable criterion) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(0L, criterion).thenSucceed();
   }

   public void succeedWhen(Runnable criterion) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(criterion).thenSucceed();
   }

   public void succeedOnTickWhen(int tick, Runnable criterion) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(tick, criterion).thenSucceed();
   }

   public void runAtTickTime(long tickTime, Runnable task) {
      this.testInfo.setRunAtTickTime(tickTime, task);
   }

   public void runAfterDelay(long delay, Runnable task) {
      this.runAtTickTime(this.testInfo.getTick() + delay, task);
   }

   public void randomTick(BlockPos pos) {
      BlockPos blockPos = this.absolutePos(pos);
      ServerLevel serverLevel = this.getLevel();
      serverLevel.getBlockState(blockPos).randomTick(serverLevel, blockPos, serverLevel.random);
   }

   public void tickBlock(BlockPos pos) {
      BlockPos blockPos = this.absolutePos(pos);
      ServerLevel serverLevel = this.getLevel();
      serverLevel.getBlockState(blockPos).tick(serverLevel, blockPos, serverLevel.random);
   }

   public void tickPrecipitation(BlockPos pos) {
      BlockPos blockPos = this.absolutePos(pos);
      ServerLevel serverLevel = this.getLevel();
      serverLevel.tickPrecipitation(blockPos);
   }

   public void tickPrecipitation() {
      AABB aABB = this.getRelativeBounds();
      int i = (int)Math.floor(aABB.maxX);
      int j = (int)Math.floor(aABB.maxZ);
      int k = (int)Math.floor(aABB.maxY);

      for (int l = (int)Math.floor(aABB.minX); l < i; l++) {
         for (int m = (int)Math.floor(aABB.minZ); m < j; m++) {
            this.tickPrecipitation(new BlockPos(l, k, m));
         }
      }
   }

   public int getHeight(Heightmap.Types heightmapType, int x, int z) {
      BlockPos blockPos = this.absolutePos(new BlockPos(x, 0, z));
      return this.relativePos(this.getLevel().getHeightmapPos(heightmapType, blockPos)).getY();
   }

   public void fail(Component message, BlockPos pos) {
      throw this.assertionException(pos, message);
   }

   public void fail(Component message, Entity entity) {
      throw this.assertionException(entity.blockPosition(), message);
   }

   public void fail(Component message) {
      throw this.assertionException(message);
   }

   public void fail(String message) {
      throw this.assertionException(Component.literal(message));
   }

   public void failIf(Runnable criterion) {
      this.testInfo.createSequence().thenWaitUntil(criterion).thenFail(() -> this.assertionException("test.error.fail"));
   }

   public void failIfEver(Runnable criterion) {
      LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks()).forEach(l -> this.testInfo.setRunAtTickTime(l, criterion::run));
   }

   public GameTestSequence startSequence() {
      return this.testInfo.createSequence();
   }

   public BlockPos absolutePos(BlockPos pos) {
      BlockPos blockPos = this.testInfo.getTestOrigin();
      BlockPos blockPos2 = blockPos.offset(pos);
      return StructureTemplate.transform(blockPos2, Mirror.NONE, this.testInfo.getRotation(), blockPos);
   }

   public BlockPos relativePos(BlockPos pos) {
      BlockPos blockPos = this.testInfo.getTestOrigin();
      Rotation rotation = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
      BlockPos blockPos2 = StructureTemplate.transform(pos, Mirror.NONE, rotation, blockPos);
      return blockPos2.subtract(blockPos);
   }

   public AABB absoluteAABB(AABB aabb) {
      Vec3 vec3 = this.absoluteVec(aabb.getMinPosition());
      Vec3 vec32 = this.absoluteVec(aabb.getMaxPosition());
      return new AABB(vec3, vec32);
   }

   public AABB relativeAABB(AABB aabb) {
      Vec3 vec3 = this.relativeVec(aabb.getMinPosition());
      Vec3 vec32 = this.relativeVec(aabb.getMaxPosition());
      return new AABB(vec3, vec32);
   }

   public Vec3 absoluteVec(Vec3 relativeVec3) {
      Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
      return StructureTemplate.transform(vec3.add(relativeVec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
   }

   public Vec3 relativeVec(Vec3 absoluteVec3) {
      Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getTestOrigin());
      return StructureTemplate.transform(absoluteVec3.subtract(vec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getTestOrigin());
   }

   public Rotation getTestRotation() {
      return this.testInfo.getRotation();
   }

   public Direction getTestDirection() {
      return this.testInfo.getRotation().rotate(Direction.SOUTH);
   }

   public Direction getAbsoluteDirection(Direction direction) {
      return this.getTestRotation().rotate(direction);
   }

   public void assertTrue(boolean condition, Component message) {
      if (!condition) {
         throw this.assertionException(message);
      }
   }

   public void assertTrue(boolean condition, String message) {
      this.assertTrue(condition, Component.literal(message));
   }

   public <N> void assertValueEqual(N expected, N actual, String name) {
      this.assertValueEqual(expected, actual, Component.literal(name));
   }

   public <N> void assertValueEqual(N expected, N actual, Component name) {
      if (!expected.equals(actual)) {
         throw this.assertionException("test.error.value_not_equal", name, expected, actual);
      }
   }

   public void assertFalse(boolean condition, Component message) {
      this.assertTrue(!condition, message);
   }

   public void assertFalse(boolean condition, String message) {
      this.assertFalse(condition, Component.literal(message));
   }

   public long getTick() {
      return this.testInfo.getTick();
   }

   public AABB getBounds() {
      return this.testInfo.getStructureBounds();
   }

   public AABB getRelativeBounds() {
      AABB aABB = this.testInfo.getStructureBounds();
      Rotation rotation = this.testInfo.getRotation();
      switch (rotation) {
         case COUNTERCLOCKWISE_90:
         case CLOCKWISE_90:
            return new AABB(0.0, 0.0, 0.0, aABB.getZsize(), aABB.getYsize(), aABB.getXsize());
         default:
            return new AABB(0.0, 0.0, 0.0, aABB.getXsize(), aABB.getYsize(), aABB.getZsize());
      }
   }

   public void forEveryBlockInStructure(Consumer<BlockPos> consumer) {
      AABB aABB = this.getRelativeBounds().contract(1.0, 1.0, 1.0);
      BlockPos.MutableBlockPos.betweenClosedStream(aABB).forEach(consumer);
   }

   public void onEachTick(Runnable task) {
      LongStream.range(this.testInfo.getTick(), this.testInfo.getTimeoutTicks()).forEach(l -> this.testInfo.setRunAtTickTime(l, task::run));
   }

   public void placeAt(Player player, ItemStack stack, BlockPos pos, Direction direction) {
      BlockPos blockPos = this.absolutePos(pos.relative(direction));
      BlockHitResult blockHitResult = new BlockHitResult(Vec3.atCenterOf(blockPos), direction, blockPos, false);
      UseOnContext useOnContext = new UseOnContext(player, InteractionHand.MAIN_HAND, blockHitResult);
      stack.useOn(useOnContext);
   }

   public void setBiome(ResourceKey<Biome> biome) {
      AABB aABB = this.getBounds();
      BlockPos blockPos = BlockPos.containing(aABB.minX, aABB.minY, aABB.minZ);
      BlockPos blockPos2 = BlockPos.containing(aABB.maxX, aABB.maxY, aABB.maxZ);
      Either<Integer, CommandSyntaxException> either = FillBiomeCommand.fill(
         this.getLevel(), blockPos, blockPos2, this.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(biome)
      );
      if (either.right().isPresent()) {
         throw this.assertionException("test.error.set_biome");
      }
   }
}

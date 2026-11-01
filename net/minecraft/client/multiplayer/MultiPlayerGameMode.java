package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.google.common.primitives.Shorts;
import com.google.common.primitives.SignedBytes;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class MultiPlayerGameMode {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Minecraft minecraft;
   private final ClientPacketListener connection;
   private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);
   private ItemStack destroyingItem = ItemStack.EMPTY;
   private float destroyProgress;
   private float destroyTicks;
   private int destroyDelay;
   private boolean isDestroying;
   private GameType localPlayerMode = GameType.DEFAULT_MODE;
   @Nullable
   private GameType previousLocalPlayerMode;
   private int carriedIndex;

   public MultiPlayerGameMode(Minecraft minecraft, ClientPacketListener connection) {
      this.minecraft = minecraft;
      this.connection = connection;
   }

   public void adjustPlayer(Player player) {
      this.localPlayerMode.updatePlayerAbilities(player.getAbilities());
   }

   public void setLocalMode(GameType localPlayerMode, @Nullable GameType previousLocalPlayerMode) {
      this.localPlayerMode = localPlayerMode;
      this.previousLocalPlayerMode = previousLocalPlayerMode;
      this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
   }

   public void setLocalMode(GameType type) {
      if (type != this.localPlayerMode) {
         this.previousLocalPlayerMode = this.localPlayerMode;
      }

      this.localPlayerMode = type;
      this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
   }

   public boolean canHurtPlayer() {
      return this.localPlayerMode.isSurvival();
   }

   public boolean destroyBlock(BlockPos pos) {
      if (this.minecraft.player.blockActionRestricted(this.minecraft.level, pos, this.localPlayerMode)) {
         return false;
      } else {
         Level level = this.minecraft.level;
         BlockState blockState = level.getBlockState(pos);
         if (!this.minecraft.player.getMainHandItem().canDestroyBlock(blockState, level, pos, this.minecraft.player)) {
            return false;
         } else {
            Block block = blockState.getBlock();
            if (block instanceof GameMasterBlock && !this.minecraft.player.canUseGameMasterBlocks()) {
               return false;
            } else if (blockState.isAir()) {
               return false;
            } else {
               block.playerWillDestroy(level, pos, blockState, this.minecraft.player);
               FluidState fluidState = level.getFluidState(pos);
               boolean bl = level.setBlock(pos, fluidState.createLegacyBlock(), 11);
               if (bl) {
                  block.destroy(level, pos, blockState);
               }

               if (SharedConstants.DEBUG_BLOCK_BREAK) {
                  LOGGER.error("client broke {} {} -> {}", new Object[]{pos, blockState, level.getBlockState(pos)});
               }

               return bl;
            }
         }
      }
   }

   public boolean startDestroyBlock(BlockPos loc, Direction face) {
      if (this.minecraft.player.blockActionRestricted(this.minecraft.level, loc, this.localPlayerMode)) {
         return false;
      } else if (!this.minecraft.level.getWorldBorder().isWithinBounds(loc)) {
         return false;
      } else {
         if (this.minecraft.player.getAbilities().instabuild) {
            BlockState blockState = this.minecraft.level.getBlockState(loc);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, loc, blockState, 1.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
               LOGGER.info("Creative start {} {}", loc, blockState);
            }

            this.startPrediction(this.minecraft.level, i -> {
               this.destroyBlock(loc);
               return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, loc, face, i);
            });
            this.destroyDelay = 5;
         } else if (!this.isDestroying || !this.sameDestroyTarget(loc)) {
            if (this.isDestroying) {
               if (SharedConstants.DEBUG_BLOCK_BREAK) {
                  LOGGER.info("Abort old break {} {}", loc, this.minecraft.level.getBlockState(loc));
               }

               this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, face));
            }

            BlockState blockState = this.minecraft.level.getBlockState(loc);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, loc, blockState, 0.0F);
            if (SharedConstants.DEBUG_BLOCK_BREAK) {
               LOGGER.info("Start break {} {}", loc, blockState);
            }

            this.startPrediction(this.minecraft.level, i -> {
               boolean bl = !blockState.isAir();
               if (bl && this.destroyProgress == 0.0F) {
                  blockState.attack(this.minecraft.level, loc, this.minecraft.player);
               }

               if (bl && blockState.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), loc) >= 1.0F) {
                  this.destroyBlock(loc);
               } else {
                  this.isDestroying = true;
                  this.destroyBlockPos = loc;
                  this.destroyingItem = this.minecraft.player.getMainHandItem();
                  this.destroyProgress = 0.0F;
                  this.destroyTicks = 0.0F;
                  this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
               }

               return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, loc, face, i);
            });
         }

         return true;
      }
   }

   public void stopDestroyBlock() {
      if (this.isDestroying) {
         BlockState blockState = this.minecraft.level.getBlockState(this.destroyBlockPos);
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, this.destroyBlockPos, blockState, -1.0F);
         if (SharedConstants.DEBUG_BLOCK_BREAK) {
            LOGGER.info("Stop dest {} {}", this.destroyBlockPos, blockState);
         }

         this.connection
            .send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, Direction.DOWN));
         this.isDestroying = false;
         this.destroyProgress = 0.0F;
         this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, -1);
         this.minecraft.player.resetAttackStrengthTicker();
      }
   }

   public boolean continueDestroyBlock(BlockPos posBlock, Direction directionFacing) {
      this.ensureHasSentCarriedItem();
      if (this.destroyDelay > 0) {
         this.destroyDelay--;
         return true;
      } else if (this.minecraft.player.getAbilities().instabuild && this.minecraft.level.getWorldBorder().isWithinBounds(posBlock)) {
         this.destroyDelay = 5;
         BlockState blockState = this.minecraft.level.getBlockState(posBlock);
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, blockState, 1.0F);
         if (SharedConstants.DEBUG_BLOCK_BREAK) {
            LOGGER.info("Creative cont {} {}", posBlock, blockState);
         }

         this.startPrediction(this.minecraft.level, i -> {
            this.destroyBlock(posBlock);
            return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, posBlock, directionFacing, i);
         });
         return true;
      } else if (this.sameDestroyTarget(posBlock)) {
         BlockState blockState = this.minecraft.level.getBlockState(posBlock);
         if (blockState.isAir()) {
            this.isDestroying = false;
            return false;
         } else {
            this.destroyProgress = this.destroyProgress + blockState.getDestroyProgress(this.minecraft.player, this.minecraft.player.level(), posBlock);
            if (this.destroyTicks % 4.0F == 0.0F) {
               SoundType soundType = blockState.getSoundType();
               this.minecraft
                  .getSoundManager()
                  .play(
                     new SimpleSoundInstance(
                        soundType.getHitSound(),
                        SoundSource.BLOCKS,
                        (soundType.getVolume() + 1.0F) / 8.0F,
                        soundType.getPitch() * 0.5F,
                        SoundInstance.createUnseededRandom(),
                        posBlock
                     )
                  );
            }

            this.destroyTicks++;
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, posBlock, blockState, Mth.clamp(this.destroyProgress, 0.0F, 1.0F));
            if (this.destroyProgress >= 1.0F) {
               this.isDestroying = false;
               if (SharedConstants.DEBUG_BLOCK_BREAK) {
                  LOGGER.info("Finished breaking {} {}", posBlock, blockState);
               }

               this.startPrediction(this.minecraft.level, i -> {
                  this.destroyBlock(posBlock);
                  return new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, posBlock, directionFacing, i);
               });
               this.destroyProgress = 0.0F;
               this.destroyTicks = 0.0F;
               this.destroyDelay = 5;
            }

            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, this.getDestroyStage());
            return true;
         }
      } else {
         return this.startDestroyBlock(posBlock, directionFacing);
      }
   }

   private void startPrediction(ClientLevel level, PredictiveAction action) {
      try (BlockStatePredictionHandler blockStatePredictionHandler = level.getBlockStatePredictionHandler().startPredicting()) {
         int i = blockStatePredictionHandler.currentSequence();
         Packet<ServerGamePacketListener> packet = action.predict(i);
         this.connection.send(packet);
      }
   }

   public void tick() {
      this.ensureHasSentCarriedItem();
      if (this.connection.getConnection().isConnected()) {
         this.connection.getConnection().tick();
      } else {
         this.connection.getConnection().handleDisconnection();
      }
   }

   private boolean sameDestroyTarget(BlockPos pos) {
      ItemStack itemStack = this.minecraft.player.getMainHandItem();
      return pos.equals(this.destroyBlockPos) && ItemStack.isSameItemSameComponents(itemStack, this.destroyingItem);
   }

   private void ensureHasSentCarriedItem() {
      int i = this.minecraft.player.getInventory().getSelectedSlot();
      if (i != this.carriedIndex) {
         this.carriedIndex = i;
         this.connection.send(new ServerboundSetCarriedItemPacket(this.carriedIndex));
      }
   }

   public InteractionResult useItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult result) {
      this.ensureHasSentCarriedItem();
      if (!this.minecraft.level.getWorldBorder().isWithinBounds(result.getBlockPos())) {
         return InteractionResult.FAIL;
      } else {
         MutableObject<InteractionResult> mutableObject = new MutableObject();
         this.startPrediction(this.minecraft.level, i -> {
            mutableObject.setValue(this.performUseItemOn(player, hand, result));
            return new ServerboundUseItemOnPacket(hand, result, i);
         });
         return (InteractionResult)mutableObject.get();
      }
   }

   private InteractionResult performUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult result) {
      BlockPos blockPos = result.getBlockPos();
      ItemStack itemStack = player.getItemInHand(hand);
      if (this.localPlayerMode == GameType.SPECTATOR) {
         return InteractionResult.CONSUME;
      } else {
         boolean bl = !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty();
         boolean bl2 = player.isSecondaryUseActive() && bl;
         if (!bl2) {
            BlockState blockState = this.minecraft.level.getBlockState(blockPos);
            if (!this.connection.isFeatureEnabled(blockState.getBlock().requiredFeatures())) {
               return InteractionResult.FAIL;
            }

            InteractionResult interactionResult = blockState.useItemOn(player.getItemInHand(hand), this.minecraft.level, player, hand, result);
            if (interactionResult.consumesAction()) {
               return interactionResult;
            }

            if (interactionResult instanceof InteractionResult.TryEmptyHandInteraction && hand == InteractionHand.MAIN_HAND) {
               InteractionResult interactionResult2 = blockState.useWithoutItem(this.minecraft.level, player, result);
               if (interactionResult2.consumesAction()) {
                  return interactionResult2;
               }
            }
         }

         if (!itemStack.isEmpty() && !player.getCooldowns().isOnCooldown(itemStack)) {
            UseOnContext useOnContext = new UseOnContext(player, hand, result);
            InteractionResult interactionResult3;
            if (player.hasInfiniteMaterials()) {
               int i = itemStack.getCount();
               interactionResult3 = itemStack.useOn(useOnContext);
               itemStack.setCount(i);
            } else {
               interactionResult3 = itemStack.useOn(useOnContext);
            }

            return interactionResult3;
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   public InteractionResult useItem(Player player, InteractionHand hand) {
      if (this.localPlayerMode == GameType.SPECTATOR) {
         return InteractionResult.PASS;
      } else {
         this.ensureHasSentCarriedItem();
         MutableObject<InteractionResult> mutableObject = new MutableObject();
         this.startPrediction(this.minecraft.level, i -> {
            ServerboundUseItemPacket serverboundUseItemPacket = new ServerboundUseItemPacket(hand, i, player.getYRot(), player.getXRot());
            ItemStack itemStack = player.getItemInHand(hand);
            if (player.getCooldowns().isOnCooldown(itemStack)) {
               mutableObject.setValue(InteractionResult.PASS);
               return serverboundUseItemPacket;
            } else {
               InteractionResult interactionResult = itemStack.use(this.minecraft.level, player, hand);
               ItemStack itemStack2;
               if (interactionResult instanceof InteractionResult.Success success) {
                  itemStack2 = Objects.requireNonNullElseGet(success.heldItemTransformedTo(), () -> player.getItemInHand(hand));
               } else {
                  itemStack2 = player.getItemInHand(hand);
               }

               if (itemStack2 != itemStack) {
                  player.setItemInHand(hand, itemStack2);
               }

               mutableObject.setValue(interactionResult);
               return serverboundUseItemPacket;
            }
         });
         return (InteractionResult)mutableObject.get();
      }
   }

   public LocalPlayer createPlayer(ClientLevel level, StatsCounter stats, ClientRecipeBook recipeBook) {
      return this.createPlayer(level, stats, recipeBook, Input.EMPTY, false);
   }

   public LocalPlayer createPlayer(ClientLevel level, StatsCounter stats, ClientRecipeBook recipeBook, Input lastSentInput, boolean wasSprinting) {
      return new LocalPlayer(this.minecraft, level, this.connection, stats, recipeBook, lastSentInput, wasSprinting);
   }

   public void attack(Player player, Entity targetEntity) {
      this.ensureHasSentCarriedItem();
      this.connection.send(ServerboundInteractPacket.createAttackPacket(targetEntity, player.isShiftKeyDown()));
      if (this.localPlayerMode != GameType.SPECTATOR) {
         player.attack(targetEntity);
         player.resetAttackStrengthTicker();
      }
   }

   public InteractionResult interact(Player player, Entity target, InteractionHand hand) {
      this.ensureHasSentCarriedItem();
      this.connection.send(ServerboundInteractPacket.createInteractionPacket(target, player.isShiftKeyDown(), hand));
      return (InteractionResult)(this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : player.interactOn(target, hand));
   }

   public InteractionResult interactAt(Player player, Entity target, EntityHitResult ray, InteractionHand hand) {
      this.ensureHasSentCarriedItem();
      Vec3 vec3 = ray.getLocation().subtract(target.getX(), target.getY(), target.getZ());
      this.connection.send(ServerboundInteractPacket.createInteractionPacket(target, player.isShiftKeyDown(), hand, vec3));
      return (InteractionResult)(this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : target.interactAt(player, vec3, hand));
   }

   public void handleInventoryMouseClick(int containerId, int slotId, int mouseButton, ClickType clickType, Player player) {
      AbstractContainerMenu abstractContainerMenu = player.containerMenu;
      if (containerId != abstractContainerMenu.containerId) {
         LOGGER.warn("Ignoring click in mismatching container. Click in {}, player has {}.", containerId, abstractContainerMenu.containerId);
      } else {
         NonNullList<Slot> nonNullList = abstractContainerMenu.slots;
         int i = nonNullList.size();
         List<ItemStack> list = Lists.newArrayListWithCapacity(i);

         for (Slot slot : nonNullList) {
            list.add(slot.getItem().copy());
         }

         abstractContainerMenu.clicked(slotId, mouseButton, clickType, player);
         Int2ObjectMap<HashedStack> int2ObjectMap = new Int2ObjectOpenHashMap();

         for (int j = 0; j < i; j++) {
            ItemStack itemStack = list.get(j);
            ItemStack itemStack2 = nonNullList.get(j).getItem();
            if (!ItemStack.matches(itemStack, itemStack2)) {
               int2ObjectMap.put(j, HashedStack.create(itemStack2, this.connection.decoratedHashOpsGenenerator()));
            }
         }

         HashedStack hashedStack = HashedStack.create(abstractContainerMenu.getCarried(), this.connection.decoratedHashOpsGenenerator());
         this.connection
            .send(
               new ServerboundContainerClickPacket(
                  containerId,
                  abstractContainerMenu.getStateId(),
                  Shorts.checkedCast(slotId),
                  SignedBytes.checkedCast(mouseButton),
                  clickType,
                  int2ObjectMap,
                  hashedStack
               )
            );
      }
   }

   public void handlePlaceRecipe(int containerId, RecipeDisplayId recipe, boolean useMaxItems) {
      this.connection.send(new ServerboundPlaceRecipePacket(containerId, recipe, useMaxItems));
   }

   public void handleInventoryButtonClick(int containerId, int buttonId) {
      this.connection.send(new ServerboundContainerButtonClickPacket(containerId, buttonId));
   }

   public void handleCreativeModeItemAdd(ItemStack stack, int slotId) {
      if (this.minecraft.player.hasInfiniteMaterials() && this.connection.isFeatureEnabled(stack.getItem().requiredFeatures())) {
         this.connection.send(new ServerboundSetCreativeModeSlotPacket(slotId, stack));
      }
   }

   public void handleCreativeModeItemDrop(ItemStack stack) {
      boolean bl = this.minecraft.screen instanceof AbstractContainerScreen && !(this.minecraft.screen instanceof CreativeModeInventoryScreen);
      if (this.minecraft.player.hasInfiniteMaterials() && !bl && !stack.isEmpty() && this.connection.isFeatureEnabled(stack.getItem().requiredFeatures())) {
         this.connection.send(new ServerboundSetCreativeModeSlotPacket(-1, stack));
         this.minecraft.player.getDropSpamThrottler().increment();
      }
   }

   public void releaseUsingItem(Player player) {
      this.ensureHasSentCarriedItem();
      this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
      player.releaseUsingItem();
   }

   public void piercingAttack(PiercingWeapon weapon) {
      this.ensureHasSentCarriedItem();
      this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STAB, BlockPos.ZERO, Direction.DOWN));
      this.minecraft.player.onAttack();
      this.minecraft.player.lungeForwardMaybe();
      weapon.makeSound(this.minecraft.player);
   }

   public boolean hasExperience() {
      return this.localPlayerMode.isSurvival();
   }

   public boolean hasMissTime() {
      return !this.localPlayerMode.isCreative();
   }

   public boolean isServerControlledInventory() {
      return this.minecraft.player.isPassenger() && this.minecraft.player.getVehicle() instanceof HasCustomInventoryScreen;
   }

   public boolean isSpectator() {
      return this.localPlayerMode == GameType.SPECTATOR;
   }

   @Nullable
   public GameType getPreviousPlayerMode() {
      return this.previousLocalPlayerMode;
   }

   public GameType getPlayerMode() {
      return this.localPlayerMode;
   }

   public boolean isDestroying() {
      return this.isDestroying;
   }

   public int getDestroyStage() {
      return this.destroyProgress > 0.0F ? (int)(this.destroyProgress * 10.0F) : -1;
   }

   public void handlePickItemFromBlock(BlockPos pos, boolean includeData) {
      this.connection.send(new ServerboundPickItemFromBlockPacket(pos, includeData));
   }

   public void handlePickItemFromEntity(Entity entity, boolean includeData) {
      this.connection.send(new ServerboundPickItemFromEntityPacket(entity.getId(), includeData));
   }

   public void handleSlotStateChanged(int slotId, int containerId, boolean newState) {
      this.connection.send(new ServerboundContainerSlotStateChangedPacket(slotId, containerId, newState));
   }
}

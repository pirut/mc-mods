package net.minecraft.client.player;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.HangingSignEditScreen;
import net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.TestBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.TestInstanceBlockEditScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.sounds.AmbientSoundHandler;
import net.minecraft.client.resources.sounds.BiomeAmbientSoundsHandler;
import net.minecraft.client.resources.sounds.BubbleColumnAmbientSoundHandler;
import net.minecraft.client.resources.sounds.ElytraOnPlayerSoundInstance;
import net.minecraft.client.resources.sounds.RidingEntitySoundInstance;
import net.minecraft.client.resources.sounds.RidingMinecartSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundHandler;
import net.minecraft.client.resources.sounds.UnderwaterAmbientSoundInstances;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.TickThrottler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.HangingSignBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.entity.TestBlockEntity;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class LocalPlayer extends AbstractClientPlayer {
   public static final Logger LOGGER = LogUtils.getLogger();
   private static final int POSITION_REMINDER_INTERVAL = 20;
   private static final int WATER_VISION_MAX_TIME = 600;
   private static final int WATER_VISION_QUICK_TIME = 100;
   private static final float WATER_VISION_QUICK_PERCENT = 0.6F;
   private static final double SUFFOCATING_COLLISION_CHECK_SCALE = 0.35;
   private static final double MINOR_COLLISION_ANGLE_THRESHOLD_RADIAN = 0.13962634F;
   public final ClientPacketListener connection;
   private final StatsCounter stats;
   private final ClientRecipeBook recipeBook;
   private final TickThrottler dropSpamThrottler = new TickThrottler(20, 1280);
   private final List<AmbientSoundHandler> ambientSoundHandlers = Lists.newArrayList();
   private PermissionSet permissions = PermissionSet.NO_PERMISSIONS;
   private double xLast;
   private double yLast;
   private double zLast;
   private float yRotLast;
   private float xRotLast;
   private boolean lastOnGround;
   private boolean lastHorizontalCollision;
   private boolean crouching;
   private boolean wasSprinting;
   private int positionReminder;
   private boolean flashOnSetHealth;
   public ClientInput input = new ClientInput();
   private Input lastSentInput;
   protected final Minecraft minecraft;
   protected int sprintTriggerTime;
   private static final int EXPERIENCE_DISPLAY_UNREADY_TO_SET = Integer.MIN_VALUE;
   private static final int EXPERIENCE_DISPLAY_READY_TO_SET = -2147483647;
   public int experienceDisplayStartTick = Integer.MIN_VALUE;
   public float yBob;
   public float xBob;
   public float yBobO;
   public float xBobO;
   private int jumpRidingTicks;
   private float jumpRidingScale;
   public float portalEffectIntensity;
   public float oPortalEffectIntensity;
   private boolean startedUsingItem;
   @Nullable
   private InteractionHand usingItemHand;
   private boolean handsBusy;
   private boolean autoJumpEnabled = true;
   private int autoJumpTime;
   private boolean wasFallFlying;
   private int waterVisionTime;
   private boolean showDeathScreen = true;
   private boolean doLimitedCrafting = false;

   public LocalPlayer(
      Minecraft minecraft,
      ClientLevel level,
      ClientPacketListener connection,
      StatsCounter stats,
      ClientRecipeBook recipeBook,
      Input lastSentInput,
      boolean wasSprinting
   ) {
      super(level, connection.getLocalGameProfile());
      this.minecraft = minecraft;
      this.connection = connection;
      this.stats = stats;
      this.recipeBook = recipeBook;
      this.lastSentInput = lastSentInput;
      this.wasSprinting = wasSprinting;
      this.ambientSoundHandlers.add(new UnderwaterAmbientSoundHandler(this, minecraft.getSoundManager()));
      this.ambientSoundHandlers.add(new BubbleColumnAmbientSoundHandler(this));
      this.ambientSoundHandlers.add(new BiomeAmbientSoundsHandler(this, minecraft.getSoundManager()));
   }

   @Override
   public void heal(float healAmount) {
   }

   @Override
   public boolean startRiding(Entity vehicle, boolean force, boolean sendGameEvent) {
      if (!super.startRiding(vehicle, force, sendGameEvent)) {
         return false;
      } else {
         if (vehicle instanceof AbstractMinecart abstractMinecart) {
            this.minecraft
               .getSoundManager()
               .play(new RidingMinecartSoundInstance(this, abstractMinecart, true, SoundEvents.MINECART_INSIDE_UNDERWATER, 0.0F, 0.75F, 1.0F));
            this.minecraft
               .getSoundManager()
               .play(new RidingMinecartSoundInstance(this, abstractMinecart, false, SoundEvents.MINECART_INSIDE, 0.0F, 0.75F, 1.0F));
         } else if (vehicle instanceof HappyGhast happyGhast) {
            this.minecraft
               .getSoundManager()
               .play(new RidingEntitySoundInstance(this, happyGhast, false, SoundEvents.HAPPY_GHAST_RIDING, happyGhast.getSoundSource(), 0.0F, 1.0F, 5.0F));
         } else if (vehicle instanceof AbstractNautilus abstractNautilus) {
            this.minecraft
               .getSoundManager()
               .play(
                  new RidingEntitySoundInstance(this, abstractNautilus, true, SoundEvents.NAUTILUS_RIDING, abstractNautilus.getSoundSource(), 0.0F, 1.0F, 5.0F)
               );
         }

         return true;
      }
   }

   @Override
   public void removeVehicle() {
      super.removeVehicle();
      this.handsBusy = false;
   }

   @Override
   public float getViewXRot(float partialTick) {
      return this.getXRot();
   }

   @Override
   public float getViewYRot(float partialTick) {
      return this.isPassenger() ? super.getViewYRot(partialTick) : this.getYRot();
   }

   @Override
   public void tick() {
      if (this.connection.hasClientLoaded()) {
         this.dropSpamThrottler.tick();
         super.tick();
         if (!this.lastSentInput.equals(this.input.keyPresses)) {
            this.connection.send(new ServerboundPlayerInputPacket(this.input.keyPresses));
            this.lastSentInput = this.input.keyPresses;
         }

         if (this.isPassenger()) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision));
            Entity entity = this.getRootVehicle();
            if (entity != this && entity.isLocalInstanceAuthoritative()) {
               this.connection.send(ServerboundMoveVehiclePacket.fromEntity(entity));
               this.sendIsSprintingIfNeeded();
            }
         } else {
            this.sendPosition();
         }

         for (AmbientSoundHandler ambientSoundHandler : this.ambientSoundHandlers) {
            ambientSoundHandler.tick();
         }
      }
   }

   public float getCurrentMood() {
      for (AmbientSoundHandler ambientSoundHandler : this.ambientSoundHandlers) {
         if (ambientSoundHandler instanceof BiomeAmbientSoundsHandler) {
            return ((BiomeAmbientSoundsHandler)ambientSoundHandler).getMoodiness();
         }
      }

      return 0.0F;
   }

   private void sendPosition() {
      this.sendIsSprintingIfNeeded();
      if (this.isControlledCamera()) {
         double d = this.getX() - this.xLast;
         double e = this.getY() - this.yLast;
         double f = this.getZ() - this.zLast;
         double g = this.getYRot() - this.yRotLast;
         double h = this.getXRot() - this.xRotLast;
         this.positionReminder++;
         boolean bl = Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4) || this.positionReminder >= 20;
         boolean bl2 = g != 0.0 || h != 0.0;
         if (bl && bl2) {
            this.connection
               .send(new ServerboundMovePlayerPacket.PosRot(this.position(), this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision));
         } else if (bl) {
            this.connection.send(new ServerboundMovePlayerPacket.Pos(this.position(), this.onGround(), this.horizontalCollision));
         } else if (bl2) {
            this.connection.send(new ServerboundMovePlayerPacket.Rot(this.getYRot(), this.getXRot(), this.onGround(), this.horizontalCollision));
         } else if (this.lastOnGround != this.onGround() || this.lastHorizontalCollision != this.horizontalCollision) {
            this.connection.send(new ServerboundMovePlayerPacket.StatusOnly(this.onGround(), this.horizontalCollision));
         }

         if (bl) {
            this.xLast = this.getX();
            this.yLast = this.getY();
            this.zLast = this.getZ();
            this.positionReminder = 0;
         }

         if (bl2) {
            this.yRotLast = this.getYRot();
            this.xRotLast = this.getXRot();
         }

         this.lastOnGround = this.onGround();
         this.lastHorizontalCollision = this.horizontalCollision;
         this.autoJumpEnabled = this.minecraft.options.autoJump().get();
      }
   }

   private void sendIsSprintingIfNeeded() {
      boolean bl = this.isSprinting();
      if (bl != this.wasSprinting) {
         ServerboundPlayerCommandPacket.Action action = bl
            ? ServerboundPlayerCommandPacket.Action.START_SPRINTING
            : ServerboundPlayerCommandPacket.Action.STOP_SPRINTING;
         this.connection.send(new ServerboundPlayerCommandPacket(this, action));
         this.wasSprinting = bl;
      }
   }

   public boolean drop(boolean fullStack) {
      ServerboundPlayerActionPacket.Action action = fullStack
         ? ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
         : ServerboundPlayerActionPacket.Action.DROP_ITEM;
      ItemStack itemStack = this.getInventory().removeFromSelected(fullStack);
      this.connection.send(new ServerboundPlayerActionPacket(action, BlockPos.ZERO, Direction.DOWN));
      return !itemStack.isEmpty();
   }

   @Override
   public void swing(InteractionHand hand) {
      super.swing(hand);
      this.connection.send(new ServerboundSwingPacket(hand));
   }

   public void respawn() {
      this.connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
      KeyMapping.resetToggleKeys();
   }

   @Override
   public void closeContainer() {
      this.connection.send(new ServerboundContainerClosePacket(this.containerMenu.containerId));
      this.clientSideCloseContainer();
   }

   public void clientSideCloseContainer() {
      super.closeContainer();
      this.minecraft.setScreen(null);
   }

   public void hurtTo(float health) {
      if (this.flashOnSetHealth) {
         float f = this.getHealth() - health;
         if (f <= 0.0F) {
            this.setHealth(health);
            if (f < 0.0F) {
               this.invulnerableTime = 10;
            }
         } else {
            this.lastHurt = f;
            this.invulnerableTime = 20;
            this.setHealth(health);
            this.hurtDuration = 10;
            this.hurtTime = this.hurtDuration;
         }
      } else {
         this.setHealth(health);
         this.flashOnSetHealth = true;
      }
   }

   @Override
   public void onUpdateAbilities() {
      this.connection.send(new ServerboundPlayerAbilitiesPacket(this.getAbilities()));
   }

   @Override
   public void setReducedDebugInfo(boolean reducedDebugInfo) {
      super.setReducedDebugInfo(reducedDebugInfo);
      this.minecraft.debugEntries.rebuildCurrentList();
   }

   @Override
   public boolean isLocalPlayer() {
      return true;
   }

   @Override
   public boolean isSuppressingSlidingDownLadder() {
      return !this.getAbilities().flying && super.isSuppressingSlidingDownLadder();
   }

   @Override
   public boolean canSpawnSprintParticle() {
      return !this.getAbilities().flying && super.canSpawnSprintParticle();
   }

   protected void sendRidingJump() {
      this.connection
         .send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_RIDING_JUMP, Mth.floor(this.getJumpRidingScale() * 100.0F)));
   }

   public void sendOpenInventory() {
      this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY));
   }

   public StatsCounter getStats() {
      return this.stats;
   }

   public ClientRecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   public void removeRecipeHighlight(RecipeDisplayId recipe) {
      if (this.recipeBook.willHighlight(recipe)) {
         this.recipeBook.removeHighlight(recipe);
         this.connection.send(new ServerboundRecipeBookSeenRecipePacket(recipe));
      }
   }

   @Override
   public PermissionSet permissions() {
      return this.permissions;
   }

   public void setPermissions(PermissionSet permissions) {
      this.permissions = permissions;
   }

   @Override
   public void displayClientMessage(Component chatComponent, boolean actionBar) {
      this.minecraft.getChatListener().handleSystemMessage(chatComponent, actionBar);
   }

   private void moveTowardsClosestSpace(double x, double z) {
      BlockPos blockPos = BlockPos.containing(x, this.getY(), z);
      if (this.suffocatesAt(blockPos)) {
         double d = x - blockPos.getX();
         double e = z - blockPos.getZ();
         Direction direction = null;
         double f = Double.MAX_VALUE;
         Direction[] directions = new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH};

         for (Direction direction2 : directions) {
            double g = direction2.getAxis().choose(d, 0.0, e);
            double h = direction2.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - g : g;
            if (h < f && !this.suffocatesAt(blockPos.relative(direction2))) {
               f = h;
               direction = direction2;
            }
         }

         if (direction != null) {
            Vec3 vec3 = this.getDeltaMovement();
            if (direction.getAxis() == Direction.Axis.X) {
               this.setDeltaMovement(0.1 * direction.getStepX(), vec3.y, vec3.z);
            } else {
               this.setDeltaMovement(vec3.x, vec3.y, 0.1 * direction.getStepZ());
            }
         }
      }
   }

   private boolean suffocatesAt(BlockPos pos) {
      AABB aABB = this.getBoundingBox();
      AABB aABB2 = new AABB(pos.getX(), aABB.minY, pos.getZ(), pos.getX() + 1.0, aABB.maxY, pos.getZ() + 1.0).deflate(1.0E-7);
      return this.level().collidesWithSuffocatingBlock(this, aABB2);
   }

   public void setExperienceValues(float currentXP, int maxXP, int level) {
      if (currentXP != this.experienceProgress) {
         this.setExperienceDisplayStartTickToTickCount();
      }

      this.experienceProgress = currentXP;
      this.totalExperience = maxXP;
      this.experienceLevel = level;
   }

   private void setExperienceDisplayStartTickToTickCount() {
      if (this.experienceDisplayStartTick == Integer.MIN_VALUE) {
         this.experienceDisplayStartTick = -2147483647;
      } else {
         this.experienceDisplayStartTick = this.tickCount;
      }
   }

   @Override
   public void handleEntityEvent(byte id) {
      switch (id) {
         case 24:
            this.setPermissions(PermissionSet.NO_PERMISSIONS);
            break;
         case 25:
            this.setPermissions(LevelBasedPermissionSet.MODERATOR);
            break;
         case 26:
            this.setPermissions(LevelBasedPermissionSet.GAMEMASTER);
            break;
         case 27:
            this.setPermissions(LevelBasedPermissionSet.ADMIN);
            break;
         case 28:
            this.setPermissions(LevelBasedPermissionSet.OWNER);
            break;
         default:
            super.handleEntityEvent(id);
      }
   }

   public void setShowDeathScreen(boolean show) {
      this.showDeathScreen = show;
   }

   public boolean shouldShowDeathScreen() {
      return this.showDeathScreen;
   }

   public void setDoLimitedCrafting(boolean doLimitedCrafting) {
      this.doLimitedCrafting = doLimitedCrafting;
   }

   public boolean getDoLimitedCrafting() {
      return this.doLimitedCrafting;
   }

   @Override
   public void playSound(SoundEvent sound, float volume, float pitch) {
      this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), volume, pitch, false);
   }

   @Override
   public void startUsingItem(InteractionHand hand) {
      ItemStack itemStack = this.getItemInHand(hand);
      if (!itemStack.isEmpty() && !this.isUsingItem()) {
         super.startUsingItem(hand);
         this.startedUsingItem = true;
         this.usingItemHand = hand;
      }
   }

   @Override
   public boolean isUsingItem() {
      return this.startedUsingItem;
   }

   private boolean isSlowDueToUsingItem() {
      return this.isUsingItem() && !this.useItem.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).canSprint();
   }

   private float itemUseSpeedMultiplier() {
      return this.useItem.getOrDefault(DataComponents.USE_EFFECTS, UseEffects.DEFAULT).speedMultiplier();
   }

   @Override
   public void stopUsingItem() {
      super.stopUsingItem();
      this.startedUsingItem = false;
   }

   @Override
   public InteractionHand getUsedItemHand() {
      return Objects.requireNonNullElse(this.usingItemHand, InteractionHand.MAIN_HAND);
   }

   @Override
   public void onSyncedDataUpdated(EntityDataAccessor<?> dataAccessor) {
      super.onSyncedDataUpdated(dataAccessor);
      if (DATA_LIVING_ENTITY_FLAGS.equals(dataAccessor)) {
         boolean bl = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
         InteractionHand interactionHand = (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
         if (bl && !this.startedUsingItem) {
            this.startUsingItem(interactionHand);
         } else if (!bl && this.startedUsingItem) {
            this.stopUsingItem();
         }
      }

      if (DATA_SHARED_FLAGS_ID.equals(dataAccessor) && this.isFallFlying() && !this.wasFallFlying) {
         this.minecraft.getSoundManager().play(new ElytraOnPlayerSoundInstance(this));
      }
   }

   @Nullable
   public PlayerRideableJumping jumpableVehicle() {
      return this.getControlledVehicle() instanceof PlayerRideableJumping playerRideableJumping && playerRideableJumping.canJump()
         ? playerRideableJumping
         : null;
   }

   public float getJumpRidingScale() {
      return this.jumpRidingScale;
   }

   @Override
   public boolean isTextFilteringEnabled() {
      return this.minecraft.isTextFilteringEnabled();
   }

   @Override
   public void openTextEdit(SignBlockEntity signEntity, boolean isFrontText) {
      if (signEntity instanceof HangingSignBlockEntity hangingSignBlockEntity) {
         this.minecraft.setScreen(new HangingSignEditScreen(hangingSignBlockEntity, isFrontText, this.minecraft.isTextFilteringEnabled()));
      } else {
         this.minecraft.setScreen(new SignEditScreen(signEntity, isFrontText, this.minecraft.isTextFilteringEnabled()));
      }
   }

   @Override
   public void openMinecartCommandBlock(MinecartCommandBlock commandBlock) {
      this.minecraft.setScreen(new MinecartCommandBlockEditScreen(commandBlock));
   }

   @Override
   public void openCommandBlock(CommandBlockEntity commandBlockEntity) {
      this.minecraft.setScreen(new CommandBlockEditScreen(commandBlockEntity));
   }

   @Override
   public void openStructureBlock(StructureBlockEntity structureEntity) {
      this.minecraft.setScreen(new StructureBlockEditScreen(structureEntity));
   }

   @Override
   public void openTestBlock(TestBlockEntity testBlockEntity) {
      this.minecraft.setScreen(new TestBlockEditScreen(testBlockEntity));
   }

   @Override
   public void openTestInstanceBlock(TestInstanceBlockEntity testInstanceBlockEntity) {
      this.minecraft.setScreen(new TestInstanceBlockEditScreen(testInstanceBlockEntity));
   }

   @Override
   public void openJigsawBlock(JigsawBlockEntity jigsawBlockEntity) {
      this.minecraft.setScreen(new JigsawBlockEditScreen(jigsawBlockEntity));
   }

   @Override
   public void openDialog(Holder<Dialog> dialog) {
      this.connection.showDialog(dialog, this.minecraft.screen);
   }

   @Override
   public void openItemGui(ItemStack stack, InteractionHand hand) {
      WritableBookContent writableBookContent = stack.get(DataComponents.WRITABLE_BOOK_CONTENT);
      if (writableBookContent != null) {
         this.minecraft.setScreen(new BookEditScreen(this, stack, hand, writableBookContent));
      }
   }

   @Override
   public void crit(Entity entityHit) {
      this.minecraft.particleEngine.createTrackingEmitter(entityHit, ParticleTypes.CRIT);
   }

   @Override
   public void magicCrit(Entity entityHit) {
      this.minecraft.particleEngine.createTrackingEmitter(entityHit, ParticleTypes.ENCHANTED_HIT);
   }

   @Override
   public boolean isShiftKeyDown() {
      return this.input.keyPresses.shift();
   }

   @Override
   public boolean isCrouching() {
      return this.crouching;
   }

   public boolean isMovingSlowly() {
      return this.isCrouching() || this.isVisuallyCrawling();
   }

   @Override
   public void applyInput() {
      if (this.isControlledCamera()) {
         Vec2 vec2 = this.modifyInput(this.input.getMoveVector());
         this.xxa = vec2.x;
         this.zza = vec2.y;
         this.jumping = this.input.keyPresses.jump();
         this.yBobO = this.yBob;
         this.xBobO = this.xBob;
         this.xBob = this.xBob + (this.getXRot() - this.xBob) * 0.5F;
         this.yBob = this.yBob + (this.getYRot() - this.yBob) * 0.5F;
      } else {
         super.applyInput();
      }
   }

   private Vec2 modifyInput(Vec2 moveVector) {
      if (moveVector.lengthSquared() == 0.0F) {
         return moveVector;
      } else {
         Vec2 vec2 = moveVector.scale(0.98F);
         if (this.isUsingItem() && !this.isPassenger()) {
            vec2 = vec2.scale(this.itemUseSpeedMultiplier());
         }

         if (this.isMovingSlowly()) {
            float f = (float)this.getAttributeValue(Attributes.SNEAKING_SPEED);
            vec2 = vec2.scale(f);
         }

         return modifyInputSpeedForSquareMovement(vec2);
      }
   }

   private static Vec2 modifyInputSpeedForSquareMovement(Vec2 moveVector) {
      float f = moveVector.length();
      if (f <= 0.0F) {
         return moveVector;
      } else {
         Vec2 vec2 = moveVector.scale(1.0F / f);
         float g = distanceToUnitSquare(vec2);
         float h = Math.min(f * g, 1.0F);
         return vec2.scale(h);
      }
   }

   private static float distanceToUnitSquare(Vec2 moveVector) {
      float f = Math.abs(moveVector.x);
      float g = Math.abs(moveVector.y);
      float h = g > f ? f / g : g / f;
      return Mth.sqrt(1.0F + Mth.square(h));
   }

   protected boolean isControlledCamera() {
      return this.minecraft.getCameraEntity() == this;
   }

   public void resetPos() {
      this.setPose(Pose.STANDING);
      if (this.level() != null) {
         for (double d = this.getY(); d > this.level().getMinY() && d <= this.level().getMaxY(); d++) {
            this.setPos(this.getX(), d, this.getZ());
            if (this.level().noCollision(this)) {
               break;
            }
         }

         this.setDeltaMovement(Vec3.ZERO);
         this.setXRot(0.0F);
      }

      this.setHealth(this.getMaxHealth());
      this.deathTime = 0;
   }

   @Override
   public void aiStep() {
      if (this.sprintTriggerTime > 0) {
         this.sprintTriggerTime--;
      }

      if (!(this.minecraft.screen instanceof LevelLoadingScreen)) {
         this.handlePortalTransitionEffect(this.getActivePortalLocalTransition() == Portal.Transition.CONFUSION);
         this.processPortalCooldown();
      }

      boolean bl = this.input.keyPresses.jump();
      boolean bl2 = this.input.keyPresses.shift();
      boolean bl3 = this.input.hasForwardImpulse();
      Abilities abilities = this.getAbilities();
      this.crouching = !abilities.flying
         && !this.isSwimming()
         && !this.isPassenger()
         && this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.CROUCHING)
         && (this.isShiftKeyDown() || !this.isSleeping() && !this.canPlayerFitWithinBlocksAndEntitiesWhen(Pose.STANDING));
      this.input.tick();
      this.minecraft.getTutorial().onInput(this.input);
      boolean bl4 = false;
      if (this.autoJumpTime > 0) {
         this.autoJumpTime--;
         bl4 = true;
         this.input.makeJump();
      }

      if (!this.noPhysics) {
         this.moveTowardsClosestSpace(this.getX() - this.getBbWidth() * 0.35, this.getZ() + this.getBbWidth() * 0.35);
         this.moveTowardsClosestSpace(this.getX() - this.getBbWidth() * 0.35, this.getZ() - this.getBbWidth() * 0.35);
         this.moveTowardsClosestSpace(this.getX() + this.getBbWidth() * 0.35, this.getZ() - this.getBbWidth() * 0.35);
         this.moveTowardsClosestSpace(this.getX() + this.getBbWidth() * 0.35, this.getZ() + this.getBbWidth() * 0.35);
      }

      if (bl2 || this.isSlowDueToUsingItem() && !this.isPassenger() || this.input.keyPresses.backward()) {
         this.sprintTriggerTime = 0;
      }

      if (this.canStartSprinting()) {
         if (!bl3) {
            if (this.sprintTriggerTime > 0) {
               this.setSprinting(true);
            } else {
               this.sprintTriggerTime = this.minecraft.options.sprintWindow().get();
            }
         }

         if (this.input.keyPresses.sprint()) {
            this.setSprinting(true);
         }
      }

      if (this.isSprinting()) {
         if (this.isSwimming()) {
            if (this.shouldStopSwimSprinting()) {
               this.setSprinting(false);
            }
         } else if (this.shouldStopRunSprinting()) {
            this.setSprinting(false);
         }
      }

      boolean bl5 = false;
      if (abilities.mayfly) {
         if (this.minecraft.gameMode.isSpectator()) {
            if (!abilities.flying) {
               abilities.flying = true;
               bl5 = true;
               this.onUpdateAbilities();
            }
         } else if (!bl && this.input.keyPresses.jump() && !bl4) {
            if (this.jumpTriggerTime == 0) {
               this.jumpTriggerTime = 7;
            } else if (!this.isSwimming() && (this.getVehicle() == null || this.jumpableVehicle() != null)) {
               abilities.flying = !abilities.flying;
               if (abilities.flying && this.onGround()) {
                  this.jumpFromGround();
               }

               bl5 = true;
               this.onUpdateAbilities();
               this.jumpTriggerTime = 0;
            }
         }
      }

      if (this.input.keyPresses.jump() && !bl5 && !bl && !this.onClimbable() && this.tryToStartFallFlying()) {
         this.connection.send(new ServerboundPlayerCommandPacket(this, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
      }

      this.wasFallFlying = this.isFallFlying();
      if (this.isInWater() && this.input.keyPresses.shift() && this.isAffectedByFluids()) {
         this.goDownInWater();
      }

      if (this.isEyeInFluid(FluidTags.WATER)) {
         int i = this.isSpectator() ? 10 : 1;
         this.waterVisionTime = Mth.clamp(this.waterVisionTime + i, 0, 600);
      } else if (this.waterVisionTime > 0) {
         this.isEyeInFluid(FluidTags.WATER);
         this.waterVisionTime = Mth.clamp(this.waterVisionTime - 10, 0, 600);
      }

      if (abilities.flying && this.isControlledCamera()) {
         int i = 0;
         if (this.input.keyPresses.shift()) {
            i--;
         }

         if (this.input.keyPresses.jump()) {
            i++;
         }

         if (i != 0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, i * abilities.getFlyingSpeed() * 3.0F, 0.0));
         }
      }

      PlayerRideableJumping playerRideableJumping = this.jumpableVehicle();
      if (playerRideableJumping != null && playerRideableJumping.getJumpCooldown() == 0) {
         if (this.jumpRidingTicks < 0) {
            this.jumpRidingTicks++;
            if (this.jumpRidingTicks == 0) {
               this.jumpRidingScale = 0.0F;
            }
         }

         if (bl && !this.input.keyPresses.jump()) {
            this.jumpRidingTicks = -10;
            playerRideableJumping.onPlayerJump(Mth.floor(this.getJumpRidingScale() * 100.0F));
            this.sendRidingJump();
         } else if (!bl && this.input.keyPresses.jump()) {
            this.jumpRidingTicks = 0;
            this.jumpRidingScale = 0.0F;
         } else if (bl) {
            this.jumpRidingTicks++;
            if (this.jumpRidingTicks < 10) {
               this.jumpRidingScale = this.jumpRidingTicks * 0.1F;
            } else {
               this.jumpRidingScale = 0.8F + 2.0F / (this.jumpRidingTicks - 9) * 0.1F;
            }
         }
      } else {
         this.jumpRidingScale = 0.0F;
      }

      super.aiStep();
      if (this.onGround() && abilities.flying && !this.minecraft.gameMode.isSpectator()) {
         abilities.flying = false;
         this.onUpdateAbilities();
      }
   }

   private boolean shouldStopRunSprinting() {
      return !this.isSprintingPossible(this.getAbilities().flying)
         || !this.input.hasForwardImpulse()
         || this.horizontalCollision && !this.minorHorizontalCollision;
   }

   private boolean shouldStopSwimSprinting() {
      return !this.isSprintingPossible(true) || !this.isInWater() || !this.input.hasForwardImpulse() && !this.onGround() && !this.input.keyPresses.shift();
   }

   public Portal.Transition getActivePortalLocalTransition() {
      return this.portalProcess == null ? Portal.Transition.NONE : this.portalProcess.getPortalLocalTransition();
   }

   @Override
   protected void tickDeath() {
      this.deathTime++;
      if (this.deathTime == 20) {
         this.remove(Entity.RemovalReason.KILLED);
      }
   }

   private void handlePortalTransitionEffect(boolean inPortal) {
      this.oPortalEffectIntensity = this.portalEffectIntensity;
      float f = 0.0F;
      if (inPortal && this.portalProcess != null && this.portalProcess.isInsidePortalThisTick()) {
         if (this.minecraft.screen != null && !this.minecraft.screen.isAllowedInPortal()) {
            if (this.minecraft.screen instanceof AbstractContainerScreen) {
               this.closeContainer();
            }

            this.minecraft.setScreen(null);
         }

         if (this.portalEffectIntensity == 0.0F) {
            this.minecraft
               .getSoundManager()
               .play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRIGGER, this.random.nextFloat() * 0.4F + 0.8F, 0.25F));
         }

         f = 0.0125F;
         this.portalProcess.setAsInsidePortalThisTick(false);
      } else if (this.portalEffectIntensity > 0.0F) {
         f = -0.05F;
      }

      this.portalEffectIntensity = Mth.clamp(this.portalEffectIntensity + f, 0.0F, 1.0F);
   }

   @Override
   public void rideTick() {
      super.rideTick();
      this.handsBusy = false;
      if (this.getControlledVehicle() instanceof AbstractBoat abstractBoat) {
         abstractBoat.setInput(this.input.keyPresses.left(), this.input.keyPresses.right(), this.input.keyPresses.forward(), this.input.keyPresses.backward());
         this.handsBusy = this.handsBusy
            | (this.input.keyPresses.left() || this.input.keyPresses.right() || this.input.keyPresses.forward() || this.input.keyPresses.backward());
      }
   }

   public boolean isHandsBusy() {
      return this.handsBusy;
   }

   @Override
   public void move(MoverType type, Vec3 movement) {
      double d = this.getX();
      double e = this.getZ();
      super.move(type, movement);
      float f = (float)(this.getX() - d);
      float g = (float)(this.getZ() - e);
      this.updateAutoJump(f, g);
      this.addWalkedDistance(Mth.length(f, g) * 0.6F);
   }

   public boolean isAutoJumpEnabled() {
      return this.autoJumpEnabled;
   }

   @Override
   public boolean shouldRotateWithMinecart() {
      return this.minecraft.options.rotateWithMinecart().get();
   }

   protected void updateAutoJump(float movementX, float movementZ) {
      if (this.canAutoJump()) {
         Vec3 vec3 = this.position();
         Vec3 vec32 = vec3.add(movementX, 0.0, movementZ);
         Vec3 vec33 = new Vec3(movementX, 0.0, movementZ);
         float f = this.getSpeed();
         float g = (float)vec33.lengthSqr();
         if (g <= 0.001F) {
            Vec2 vec2 = this.input.getMoveVector();
            float h = f * vec2.x;
            float i = f * vec2.y;
            float j = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
            float k = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
            vec33 = new Vec3(h * k - i * j, vec33.y, i * k + h * j);
            g = (float)vec33.lengthSqr();
            if (g <= 0.001F) {
               return;
            }
         }

         float l = Mth.invSqrt(g);
         Vec3 vec34 = vec33.scale(l);
         Vec3 vec35 = this.getForward();
         float j = (float)(vec35.x * vec34.x + vec35.z * vec34.z);
         if (!(j < -0.15F)) {
            CollisionContext collisionContext = CollisionContext.of(this);
            BlockPos blockPos = BlockPos.containing(this.getX(), this.getBoundingBox().maxY, this.getZ());
            BlockState blockState = this.level().getBlockState(blockPos);
            if (blockState.getCollisionShape(this.level(), blockPos, collisionContext).isEmpty()) {
               blockPos = blockPos.above();
               BlockState blockState2 = this.level().getBlockState(blockPos);
               if (blockState2.getCollisionShape(this.level(), blockPos, collisionContext).isEmpty()) {
                  float m = 7.0F;
                  float n = 1.2F;
                  if (this.hasEffect(MobEffects.JUMP_BOOST)) {
                     n += (this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.75F;
                  }

                  float o = Math.max(f * 7.0F, 1.0F / l);
                  Vec3 vec37 = vec32.add(vec34.scale(o));
                  float p = this.getBbWidth();
                  float q = this.getBbHeight();
                  AABB aABB = new AABB(vec3, vec37.add(0.0, q, 0.0)).inflate(p, 0.0, p);
                  Vec3 vec36 = vec3.add(0.0, 0.51F, 0.0);
                  vec37 = vec37.add(0.0, 0.51F, 0.0);
                  Vec3 vec38 = vec34.cross(new Vec3(0.0, 1.0, 0.0));
                  Vec3 vec39 = vec38.scale(p * 0.5F);
                  Vec3 vec310 = vec36.subtract(vec39);
                  Vec3 vec311 = vec37.subtract(vec39);
                  Vec3 vec312 = vec36.add(vec39);
                  Vec3 vec313 = vec37.add(vec39);
                  Iterable<VoxelShape> iterable = this.level().getCollisions(this, aABB);
                  Iterator<AABB> iterator = StreamSupport.stream(iterable.spliterator(), false)
                     .flatMap(voxelShapex -> voxelShapex.toAabbs().stream())
                     .iterator();
                  float r = Float.MIN_VALUE;

                  while (iterator.hasNext()) {
                     AABB aABB2 = iterator.next();
                     if (aABB2.intersects(vec310, vec311) || aABB2.intersects(vec312, vec313)) {
                        r = (float)aABB2.maxY;
                        Vec3 vec314 = aABB2.getCenter();
                        BlockPos blockPos2 = BlockPos.containing(vec314);

                        for (int s = 1; s < n; s++) {
                           BlockPos blockPos3 = blockPos2.above(s);
                           BlockState blockState3 = this.level().getBlockState(blockPos3);
                           VoxelShape voxelShape;
                           if (!(voxelShape = blockState3.getCollisionShape(this.level(), blockPos3, collisionContext)).isEmpty()) {
                              r = (float)voxelShape.max(Direction.Axis.Y) + blockPos3.getY();
                              if (r - this.getY() > n) {
                                 return;
                              }
                           }

                           if (s > 1) {
                              blockPos = blockPos.above();
                              BlockState blockState4 = this.level().getBlockState(blockPos);
                              if (!blockState4.getCollisionShape(this.level(), blockPos, collisionContext).isEmpty()) {
                                 return;
                              }
                           }
                        }
                        break;
                     }
                  }

                  if (r != Float.MIN_VALUE) {
                     float t = (float)(r - this.getY());
                     if (!(t <= 0.5F) && !(t > n)) {
                        this.autoJumpTime = 1;
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   protected boolean isHorizontalCollisionMinor(Vec3 deltaMovement) {
      float f = this.getYRot() * (float) (Math.PI / 180.0);
      double d = Mth.sin(f);
      double e = Mth.cos(f);
      double g = this.xxa * e - this.zza * d;
      double h = this.zza * e + this.xxa * d;
      double i = Mth.square(g) + Mth.square(h);
      double j = Mth.square(deltaMovement.x) + Mth.square(deltaMovement.z);
      if (!(i < 1.0E-5F) && !(j < 1.0E-5F)) {
         double k = g * deltaMovement.x + h * deltaMovement.z;
         double l = Math.acos(k / Math.sqrt(i * j));
         return l < 0.13962634F;
      } else {
         return false;
      }
   }

   private boolean canAutoJump() {
      return this.isAutoJumpEnabled()
         && this.autoJumpTime <= 0
         && this.onGround()
         && !this.isStayingOnGroundSurface()
         && !this.isPassenger()
         && this.isMoving()
         && this.getBlockJumpFactor() >= 1.0;
   }

   private boolean isMoving() {
      return this.input.getMoveVector().lengthSquared() > 0.0F;
   }

   private boolean isSprintingPossible(boolean flying) {
      return !this.isMobilityRestricted()
         && (this.isPassenger() ? this.vehicleCanSprint(this.getVehicle()) : this.hasEnoughFoodToDoExhaustiveManoeuvres())
         && (flying || !this.isInShallowWater());
   }

   private boolean canStartSprinting() {
      return !this.isSprinting()
         && this.input.hasForwardImpulse()
         && this.isSprintingPossible(this.getAbilities().flying)
         && !this.isSlowDueToUsingItem()
         && (!this.isFallFlying() || this.isUnderWater())
         && (!this.isMovingSlowly() || this.isUnderWater());
   }

   private boolean vehicleCanSprint(Entity vehicle) {
      return vehicle.canSprint() && vehicle.isLocalInstanceAuthoritative();
   }

   public float getWaterVision() {
      if (!this.isEyeInFluid(FluidTags.WATER)) {
         return 0.0F;
      } else {
         float f = 600.0F;
         float g = 100.0F;
         if (this.waterVisionTime >= 600.0F) {
            return 1.0F;
         } else {
            float h = Mth.clamp(this.waterVisionTime / 100.0F, 0.0F, 1.0F);
            float i = this.waterVisionTime < 100.0F ? 0.0F : Mth.clamp((this.waterVisionTime - 100.0F) / 500.0F, 0.0F, 1.0F);
            return h * 0.6F + i * 0.39999998F;
         }
      }
   }

   public void onGameModeChanged(GameType gameMode) {
      if (gameMode == GameType.SPECTATOR) {
         this.setDeltaMovement(this.getDeltaMovement().with(Direction.Axis.Y, 0.0));
      }
   }

   @Override
   public boolean isUnderWater() {
      return this.wasUnderwater;
   }

   @Override
   protected boolean updateIsUnderwater() {
      boolean bl = this.wasUnderwater;
      boolean bl2 = super.updateIsUnderwater();
      if (this.isSpectator()) {
         return this.wasUnderwater;
      } else {
         if (!bl && bl2) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_ENTER, SoundSource.AMBIENT, 1.0F, 1.0F, false);
            this.minecraft.getSoundManager().play(new UnderwaterAmbientSoundInstances.UnderwaterAmbientSoundInstance(this));
         }

         if (bl && !bl2) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.AMBIENT_UNDERWATER_EXIT, SoundSource.AMBIENT, 1.0F, 1.0F, false);
         }

         return this.wasUnderwater;
      }
   }

   @Override
   public Vec3 getRopeHoldPosition(float partialTicks) {
      if (this.minecraft.options.getCameraType().isFirstPerson()) {
         float f = Mth.lerp(partialTicks * 0.5F, this.getYRot(), this.yRotO) * (float) (Math.PI / 180.0);
         float g = Mth.lerp(partialTicks * 0.5F, this.getXRot(), this.xRotO) * (float) (Math.PI / 180.0);
         double d = this.getMainArm() == HumanoidArm.RIGHT ? -1.0 : 1.0;
         Vec3 vec3 = new Vec3(0.39 * d, -0.6, 0.3);
         return vec3.xRot(-g).yRot(-f).add(this.getEyePosition(partialTicks));
      } else {
         return super.getRopeHoldPosition(partialTicks);
      }
   }

   @Override
   public void updateTutorialInventoryAction(ItemStack carried, ItemStack clicked, ClickAction action) {
      this.minecraft.getTutorial().onInventoryAction(carried, clicked, action);
   }

   @Override
   public float getVisualRotationYInDegrees() {
      return this.getYRot();
   }

   @Override
   public void handleCreativeModeItemDrop(ItemStack stack) {
      this.minecraft.gameMode.handleCreativeModeItemDrop(stack);
   }

   @Override
   public boolean canDropItems() {
      return this.dropSpamThrottler.isUnderThreshold();
   }

   public TickThrottler getDropSpamThrottler() {
      return this.dropSpamThrottler;
   }

   public Input getLastSentInput() {
      return this.lastSentInput;
   }

   public HitResult raycastHitResult(float partialTick, Entity cameraEntity) {
      ItemStack itemStack = this.getActiveItem();
      AttackRange attackRange = itemStack.get(DataComponents.ATTACK_RANGE);
      double d = this.blockInteractionRange();
      HitResult hitResult = null;
      if (attackRange != null) {
         hitResult = attackRange.getClosesetHit(cameraEntity, partialTick, EntitySelector.CAN_BE_PICKED);
         if (hitResult instanceof BlockHitResult) {
            hitResult = filterHitResult(hitResult, cameraEntity.getEyePosition(partialTick), d);
         }
      }

      if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
         double e = this.entityInteractionRange();
         hitResult = pick(cameraEntity, d, e, partialTick);
      }

      return hitResult;
   }

   private static HitResult pick(Entity cameraEntity, double blockRange, double entityRange, float partialTick) {
      double d = Math.max(blockRange, entityRange);
      double e = Mth.square(d);
      Vec3 vec3 = cameraEntity.getEyePosition(partialTick);
      HitResult hitResult = cameraEntity.pick(d, partialTick, false);
      double f = hitResult.getLocation().distanceToSqr(vec3);
      if (hitResult.getType() != HitResult.Type.MISS) {
         e = f;
         d = Math.sqrt(f);
      }

      Vec3 vec32 = cameraEntity.getViewVector(partialTick);
      Vec3 vec33 = vec3.add(vec32.x * d, vec32.y * d, vec32.z * d);
      float g = 1.0F;
      AABB aABB = cameraEntity.getBoundingBox().expandTowards(vec32.scale(d)).inflate(1.0, 1.0, 1.0);
      EntityHitResult entityHitResult = ProjectileUtil.getEntityHitResult(cameraEntity, vec3, vec33, aABB, EntitySelector.CAN_BE_PICKED, e);
      return entityHitResult != null && entityHitResult.getLocation().distanceToSqr(vec3) < f
         ? filterHitResult(entityHitResult, vec3, entityRange)
         : filterHitResult(hitResult, vec3, blockRange);
   }

   private static HitResult filterHitResult(HitResult hitResult, Vec3 eyePos, double blockRange) {
      Vec3 vec3 = hitResult.getLocation();
      if (!vec3.closerThan(eyePos, blockRange)) {
         Vec3 vec32 = hitResult.getLocation();
         Direction direction = Direction.getApproximateNearest(vec32.x - eyePos.x, vec32.y - eyePos.y, vec32.z - eyePos.z);
         return BlockHitResult.miss(vec32, direction, BlockPos.containing(vec32));
      } else {
         return hitResult;
      }
   }
}

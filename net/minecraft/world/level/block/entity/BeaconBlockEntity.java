package net.minecraft.world.level.block.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public class BeaconBlockEntity extends BlockEntity implements MenuProvider, Nameable, BeaconBeamOwner {
   private static final int MAX_LEVELS = 4;
   public static final List<List<Holder<MobEffect>>> BEACON_EFFECTS = List.of(
      List.of(MobEffects.SPEED, MobEffects.HASTE),
      List.of(MobEffects.RESISTANCE, MobEffects.JUMP_BOOST),
      List.of(MobEffects.STRENGTH),
      List.of(MobEffects.REGENERATION)
   );
   private static final Set<Holder<MobEffect>> VALID_EFFECTS = BEACON_EFFECTS.stream().flatMap(Collection::stream).collect(Collectors.toSet());
   public static final int DATA_LEVELS = 0;
   public static final int DATA_PRIMARY = 1;
   public static final int DATA_SECONDARY = 2;
   public static final int NUM_DATA_VALUES = 3;
   private static final int BLOCKS_CHECK_PER_TICK = 10;
   private static final Component DEFAULT_NAME = Component.translatable("container.beacon");
   private static final String TAG_PRIMARY = "primary_effect";
   private static final String TAG_SECONDARY = "secondary_effect";
   List<BeaconBeamOwner.Section> beamSections = new ArrayList<>();
   private List<BeaconBeamOwner.Section> checkingBeamSections = new ArrayList<>();
   int levels;
   private int lastCheckY;
   @Nullable
   Holder<MobEffect> primaryPower;
   @Nullable
   Holder<MobEffect> secondaryPower;
   @Nullable
   private Component name;
   private LockCode lockKey = LockCode.NO_LOCK;
   private final ContainerData dataAccess = new ContainerData() {
      @Override
      public int get(int index) {
         return switch (index) {
            case 0 -> BeaconBlockEntity.this.levels;
            case 1 -> BeaconMenu.encodeEffect(BeaconBlockEntity.this.primaryPower);
            case 2 -> BeaconMenu.encodeEffect(BeaconBlockEntity.this.secondaryPower);
            default -> 0;
         };
      }

      @Override
      public void set(int index, int value) {
         switch (index) {
            case 0:
               BeaconBlockEntity.this.levels = value;
               break;
            case 1:
               if (!BeaconBlockEntity.this.level.isClientSide() && !BeaconBlockEntity.this.beamSections.isEmpty()) {
                  BeaconBlockEntity.playSound(BeaconBlockEntity.this.level, BeaconBlockEntity.this.worldPosition, SoundEvents.BEACON_POWER_SELECT);
               }

               BeaconBlockEntity.this.primaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(value));
               break;
            case 2:
               BeaconBlockEntity.this.secondaryPower = BeaconBlockEntity.filterEffect(BeaconMenu.decodeEffect(value));
         }
      }

      @Override
      public int getCount() {
         return 3;
      }
   };

   @Nullable
   static Holder<MobEffect> filterEffect(@Nullable Holder<MobEffect> effect) {
      return VALID_EFFECTS.contains(effect) ? effect : null;
   }

   public BeaconBlockEntity(BlockPos pos, BlockState blockState) {
      super(BlockEntityType.BEACON, pos, blockState);
   }

   public static void tick(Level level, BlockPos pos, BlockState state, BeaconBlockEntity blockEntity) {
      int i = pos.getX();
      int j = pos.getY();
      int k = pos.getZ();
      BlockPos blockPos;
      if (blockEntity.lastCheckY < j) {
         blockPos = pos;
         blockEntity.checkingBeamSections = Lists.newArrayList();
         blockEntity.lastCheckY = pos.getY() - 1;
      } else {
         blockPos = new BlockPos(i, blockEntity.lastCheckY + 1, k);
      }

      BeaconBeamOwner.Section section = blockEntity.checkingBeamSections.isEmpty()
         ? null
         : blockEntity.checkingBeamSections.get(blockEntity.checkingBeamSections.size() - 1);
      int l = level.getHeight(Heightmap.Types.WORLD_SURFACE, i, k);

      for (int m = 0; m < 10 && blockPos.getY() <= l; m++) {
         BlockState blockState = level.getBlockState(blockPos);
         if (blockState.getBlock() instanceof BeaconBeamBlock beaconBeamBlock) {
            int n = beaconBeamBlock.getColor().getTextureDiffuseColor();
            if (blockEntity.checkingBeamSections.size() <= 1) {
               section = new BeaconBeamOwner.Section(n);
               blockEntity.checkingBeamSections.add(section);
            } else if (section != null) {
               if (n == section.getColor()) {
                  section.increaseHeight();
               } else {
                  section = new BeaconBeamOwner.Section(ARGB.average(section.getColor(), n));
                  blockEntity.checkingBeamSections.add(section);
               }
            }
         } else {
            if (section == null || blockState.getLightBlock() >= 15 && !blockState.is(Blocks.BEDROCK)) {
               blockEntity.checkingBeamSections.clear();
               blockEntity.lastCheckY = l;
               break;
            }

            section.increaseHeight();
         }

         blockPos = blockPos.above();
         blockEntity.lastCheckY++;
      }

      int m = blockEntity.levels;
      if (level.getGameTime() % 80L == 0L) {
         if (!blockEntity.beamSections.isEmpty()) {
            blockEntity.levels = updateBase(level, i, j, k);
         }

         if (blockEntity.levels > 0 && !blockEntity.beamSections.isEmpty()) {
            applyEffects(level, pos, blockEntity.levels, blockEntity.primaryPower, blockEntity.secondaryPower);
            playSound(level, pos, SoundEvents.BEACON_AMBIENT);
         }
      }

      if (blockEntity.lastCheckY >= l) {
         blockEntity.lastCheckY = level.getMinY() - 1;
         boolean bl = m > 0;
         blockEntity.beamSections = blockEntity.checkingBeamSections;
         if (!level.isClientSide()) {
            boolean bl2 = blockEntity.levels > 0;
            if (!bl && bl2) {
               playSound(level, pos, SoundEvents.BEACON_ACTIVATE);

               for (ServerPlayer serverPlayer : level.getEntitiesOfClass(ServerPlayer.class, new AABB(i, j, k, i, j - 4, k).inflate(10.0, 5.0, 10.0))) {
                  CriteriaTriggers.CONSTRUCT_BEACON.trigger(serverPlayer, blockEntity.levels);
               }
            } else if (bl && !bl2) {
               playSound(level, pos, SoundEvents.BEACON_DEACTIVATE);
            }
         }
      }
   }

   private static int updateBase(Level level, int x, int y, int z) {
      int i = 0;

      for (int j = 1; j <= 4; i = j++) {
         int k = y - j;
         if (k < level.getMinY()) {
            break;
         }

         boolean bl = true;

         for (int l = x - j; l <= x + j && bl; l++) {
            for (int m = z - j; m <= z + j; m++) {
               if (!level.getBlockState(new BlockPos(l, k, m)).is(BlockTags.BEACON_BASE_BLOCKS)) {
                  bl = false;
                  break;
               }
            }
         }

         if (!bl) {
            break;
         }
      }

      return i;
   }

   @Override
   public void setRemoved() {
      playSound(this.level, this.worldPosition, SoundEvents.BEACON_DEACTIVATE);
      super.setRemoved();
   }

   private static void applyEffects(
      Level level, BlockPos pos, int beaconLevel, @Nullable Holder<MobEffect> primaryEffect, @Nullable Holder<MobEffect> secondaryEffect
   ) {
      if (!level.isClientSide() && primaryEffect != null) {
         double d = beaconLevel * 10 + 10;
         int i = 0;
         if (beaconLevel >= 4 && Objects.equals(primaryEffect, secondaryEffect)) {
            i = 1;
         }

         int j = (9 + beaconLevel * 2) * 20;
         AABB aABB = new AABB(pos).inflate(d).expandTowards(0.0, level.getHeight(), 0.0);
         List<Player> list = level.getEntitiesOfClass(Player.class, aABB);

         for (Player player : list) {
            player.addEffect(new MobEffectInstance(primaryEffect, j, i, true, true));
         }

         if (beaconLevel >= 4 && !Objects.equals(primaryEffect, secondaryEffect) && secondaryEffect != null) {
            for (Player player : list) {
               player.addEffect(new MobEffectInstance(secondaryEffect, j, 0, true, true));
            }
         }
      }
   }

   public static void playSound(Level level, BlockPos pos, SoundEvent sound) {
      level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
   }

   @Override
   public List<BeaconBeamOwner.Section> getBeamSections() {
      return (List<BeaconBeamOwner.Section>)(this.levels == 0 ? ImmutableList.of() : this.beamSections);
   }

   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   @Override
   public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
      return this.saveCustomOnly(registries);
   }

   private static void storeEffect(ValueOutput output, String key, @Nullable Holder<MobEffect> effect) {
      if (effect != null) {
         effect.unwrapKey().ifPresent(resourceKey -> output.putString(key, resourceKey.identifier().toString()));
      }
   }

   @Nullable
   private static Holder<MobEffect> loadEffect(ValueInput input, String key) {
      return input.<Holder<MobEffect>>read(key, BuiltInRegistries.MOB_EFFECT.holderByNameCodec()).filter(VALID_EFFECTS::contains).orElse(null);
   }

   @Override
   protected void loadAdditional(ValueInput input) {
      super.loadAdditional(input);
      this.primaryPower = loadEffect(input, "primary_effect");
      this.secondaryPower = loadEffect(input, "secondary_effect");
      this.name = parseCustomNameSafe(input, "CustomName");
      this.lockKey = LockCode.fromTag(input);
   }

   @Override
   protected void saveAdditional(ValueOutput output) {
      super.saveAdditional(output);
      storeEffect(output, "primary_effect", this.primaryPower);
      storeEffect(output, "secondary_effect", this.secondaryPower);
      output.putInt("Levels", this.levels);
      output.storeNullable("CustomName", ComponentSerialization.CODEC, this.name);
      this.lockKey.addToTag(output);
   }

   public void setCustomName(@Nullable Component name) {
      this.name = name;
   }

   @Nullable
   @Override
   public Component getCustomName() {
      return this.name;
   }

   @Nullable
   @Override
   public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
      if (this.lockKey.canUnlock(player)) {
         return new BeaconMenu(i, inventory, this.dataAccess, ContainerLevelAccess.create(this.level, this.getBlockPos()));
      } else {
         BaseContainerBlockEntity.sendChestLockedNotifications(this.getBlockPos().getCenter(), player, this.getDisplayName());
         return null;
      }
   }

   @Override
   public Component getDisplayName() {
      return this.getName();
   }

   @Override
   public Component getName() {
      return this.name != null ? this.name : DEFAULT_NAME;
   }

   @Override
   protected void applyImplicitComponents(DataComponentGetter componentGetter) {
      super.applyImplicitComponents(componentGetter);
      this.name = componentGetter.get(DataComponents.CUSTOM_NAME);
      this.lockKey = componentGetter.getOrDefault(DataComponents.LOCK, LockCode.NO_LOCK);
   }

   @Override
   protected void collectImplicitComponents(DataComponentMap.Builder components) {
      super.collectImplicitComponents(components);
      components.set(DataComponents.CUSTOM_NAME, this.name);
      if (!this.lockKey.equals(LockCode.NO_LOCK)) {
         components.set(DataComponents.LOCK, this.lockKey);
      }
   }

   @Override
   public void removeComponentsFromTag(ValueOutput output) {
      output.discard("CustomName");
      output.discard("lock");
   }

   @Override
   public void setLevel(Level level) {
      super.setLevel(level);
      this.lastCheckY = level.getMinY() - 1;
   }
}

package net.minecraft.world.item;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.DependantName;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureElement;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.ProvidesTrimMaterial;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IItemExtension;
import net.neoforged.neoforge.common.extensions.IItemPropertiesExtensions;
import net.neoforged.neoforge.internal.RegistrationEvents;
import net.neoforged.neoforge.registries.GameData;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class Item implements FeatureElement, ItemLike, IItemExtension {
   public static final Codec<Holder<Item>> CODEC = BuiltInRegistries.ITEM
      .holderByNameCodec()
      .validate(
         p_381630_ -> p_381630_.is(Items.AIR.builtInRegistryHolder())
            ? DataResult.error(() -> "Item must not be minecraft:air")
            : DataResult.success(p_381630_)
      );
   public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Item>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ITEM);
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Map<Block, Item> BY_BLOCK = GameData.getBlockItemMap();
   public static final Identifier BASE_ATTACK_DAMAGE_ID = Identifier.withDefaultNamespace("base_attack_damage");
   public static final Identifier BASE_ATTACK_SPEED_ID = Identifier.withDefaultNamespace("base_attack_speed");
   public static final int DEFAULT_MAX_STACK_SIZE = 64;
   public static final int ABSOLUTE_MAX_STACK_SIZE = 99;
   public static final int MAX_BAR_WIDTH = 13;
   protected static final int APPROXIMATELY_INFINITE_USE_DURATION = 72000;
   private final Holder.Reference<Item> builtInRegistryHolder = BuiltInRegistries.ITEM.createIntrusiveHolder(this);
   private DataComponentMap components;
   @Nullable
   private final Item craftingRemainingItem;
   protected final String descriptionId;
   private final FeatureFlagSet requiredFeatures;
   @Nullable
   private ItemResource defaultResource;
   protected final boolean canCombineRepair;

   public static int getId(Item p_41394_) {
      return p_41394_ == null ? 0 : BuiltInRegistries.ITEM.getId(p_41394_);
   }

   public static Item byId(int p_41446_) {
      return BuiltInRegistries.ITEM.byId(p_41446_);
   }

   @Deprecated
   public static Item byBlock(Block p_41440_) {
      return BY_BLOCK.getOrDefault(p_41440_, Items.AIR);
   }

   public Item(Item.Properties p_41383_) {
      this.descriptionId = p_41383_.effectiveDescriptionId();
      this.components = p_41383_.buildAndValidateComponents(Component.translatable(this.descriptionId), p_41383_.effectiveModel());
      this.craftingRemainingItem = p_41383_.craftingRemainingItem;
      this.requiredFeatures = p_41383_.requiredFeatures;
      if (SharedConstants.IS_RUNNING_IN_IDE) {
      }

      this.canCombineRepair = p_41383_.canCombineRepair;
   }

   @Deprecated
   public Holder.Reference<Item> builtInRegistryHolder() {
      return this.builtInRegistryHolder;
   }

   public DataComponentMap components() {
      return this.components;
   }

   @Deprecated
   @Internal
   public void modifyDefaultComponentsFrom(DataComponentPatch patch) {
      if (!RegistrationEvents.canModifyComponents()) {
         throw new IllegalStateException("Default components cannot be modified now!");
      } else {
         DataComponentMap.Builder builder = DataComponentMap.builder().addAll(this.components);
         patch.entrySet().forEach(entry -> builder.set(entry.getKey(), entry.getValue().orElse(null)));
         this.components = Item.Properties.validateComponents(builder.build());
         this.defaultResource = null;
      }
   }

   @Deprecated
   @Internal
   public ItemResource computeDefaultResource(Function<Item, ItemResource> resourceConstructor) {
      if (this.defaultResource == null) {
         this.defaultResource = resourceConstructor.apply(this);
      }

      return this.defaultResource;
   }

   public int getDefaultMaxStackSize() {
      return this.components.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
   }

   public void onUseTick(Level p_41428_, LivingEntity p_41429_, ItemStack p_41430_, int p_41431_) {
   }

   @Deprecated
   public void onDestroyed(ItemEntity p_150887_) {
   }

   public boolean canDestroyBlock(ItemStack p_394530_, BlockState p_394066_, Level p_394541_, BlockPos p_394506_, LivingEntity p_394296_) {
      Tool tool = p_394530_.get(DataComponents.TOOL);
      return tool != null && !tool.canDestroyBlocksInCreative() ? !(p_394296_ instanceof Player player && player.getAbilities().instabuild) : true;
   }

   @Override
   public Item asItem() {
      return this;
   }

   public InteractionResult useOn(UseOnContext p_41427_) {
      return InteractionResult.PASS;
   }

   public float getDestroySpeed(ItemStack p_41425_, BlockState p_41426_) {
      Tool tool = p_41425_.get(DataComponents.TOOL);
      return tool != null ? tool.getMiningSpeed(p_41426_) : 1.0F;
   }

   public InteractionResult use(Level p_41432_, Player p_41433_, InteractionHand p_41434_) {
      ItemStack itemstack = p_41433_.getItemInHand(p_41434_);
      Consumable consumable = itemstack.get(DataComponents.CONSUMABLE);
      if (consumable != null) {
         return consumable.startConsuming(p_41433_, itemstack, p_41434_);
      } else {
         Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
         if (equippable != null && equippable.swappable()) {
            return equippable.swapWithEquipmentSlot(itemstack, p_41433_);
         } else if (itemstack.has(DataComponents.BLOCKS_ATTACKS)) {
            p_41433_.startUsingItem(p_41434_);
            return InteractionResult.CONSUME;
         } else {
            KineticWeapon kineticweapon = itemstack.get(DataComponents.KINETIC_WEAPON);
            if (kineticweapon != null) {
               p_41433_.startUsingItem(p_41434_);
               kineticweapon.makeSound(p_41433_);
               return InteractionResult.CONSUME;
            } else {
               return InteractionResult.PASS;
            }
         }
      }
   }

   public ItemStack finishUsingItem(ItemStack p_41409_, Level p_41410_, LivingEntity p_41411_) {
      Consumable consumable = p_41409_.get(DataComponents.CONSUMABLE);
      return consumable != null ? consumable.onConsume(p_41410_, p_41411_, p_41409_) : p_41409_;
   }

   public boolean isBarVisible(ItemStack p_150899_) {
      return p_150899_.isDamaged();
   }

   public int getBarWidth(ItemStack p_150900_) {
      return Mth.clamp(Math.round(13.0F - p_150900_.getDamageValue() * 13.0F / p_150900_.getMaxDamage()), 0, 13);
   }

   public int getBarColor(ItemStack p_150901_) {
      int i = p_150901_.getMaxDamage();
      float f = Math.max(0.0F, ((float)i - p_150901_.getDamageValue()) / i);
      return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
   }

   public boolean overrideStackedOnOther(ItemStack p_150888_, Slot p_150889_, ClickAction p_150890_, Player p_150891_) {
      return false;
   }

   public boolean overrideOtherStackedOnMe(
      ItemStack p_150892_, ItemStack p_150893_, Slot p_150894_, ClickAction p_150895_, Player p_150896_, SlotAccess p_150897_
   ) {
      return false;
   }

   public float getAttackDamageBonus(Entity p_345249_, float p_336179_, DamageSource p_345403_) {
      return 0.0F;
   }

   @Deprecated
   @Nullable
   public DamageSource getItemDamageSource(LivingEntity p_373049_) {
      return null;
   }

   public void hurtEnemy(ItemStack p_41395_, LivingEntity p_41396_, LivingEntity p_41397_) {
   }

   public void postHurtEnemy(ItemStack p_346136_, LivingEntity p_346250_, LivingEntity p_346014_) {
   }

   public boolean mineBlock(ItemStack p_41416_, Level p_41417_, BlockState p_41418_, BlockPos p_41419_, LivingEntity p_41420_) {
      Tool tool = p_41416_.get(DataComponents.TOOL);
      if (tool == null) {
         return false;
      } else {
         if (!p_41417_.isClientSide() && p_41418_.getDestroySpeed(p_41417_, p_41419_) != 0.0F && tool.damagePerBlock() > 0) {
            p_41416_.hurtAndBreak(tool.damagePerBlock(), p_41420_, EquipmentSlot.MAINHAND);
         }

         return true;
      }
   }

   public boolean isCorrectToolForDrops(ItemStack p_336002_, BlockState p_41450_) {
      Tool tool = p_336002_.get(DataComponents.TOOL);
      return tool != null && tool.isCorrectForDrops(p_41450_);
   }

   public InteractionResult interactLivingEntity(ItemStack p_41398_, Player p_41399_, LivingEntity p_41400_, InteractionHand p_41401_) {
      return InteractionResult.PASS;
   }

   @Override
   public String toString() {
      return BuiltInRegistries.ITEM.wrapAsHolder(this).getRegisteredName();
   }

   @Deprecated
   public final ItemStack getCraftingRemainder() {
      return this.craftingRemainingItem == null ? ItemStack.EMPTY : new ItemStack(this.craftingRemainingItem);
   }

   public void inventoryTick(ItemStack p_41404_, ServerLevel p_401805_, Entity p_41406_, @Nullable EquipmentSlot p_401900_) {
   }

   public void onCraftedBy(ItemStack p_41447_, Player p_41449_) {
      this.onCraftedPostProcess(p_41447_, p_41449_.level());
   }

   public void onCraftedPostProcess(ItemStack p_307483_, Level p_307537_) {
   }

   public ItemUseAnimation getUseAnimation(ItemStack p_41452_) {
      Consumable consumable = p_41452_.get(DataComponents.CONSUMABLE);
      if (consumable != null) {
         return consumable.animation();
      } else if (p_41452_.has(DataComponents.BLOCKS_ATTACKS)) {
         return ItemUseAnimation.BLOCK;
      } else {
         return p_41452_.has(DataComponents.KINETIC_WEAPON) ? ItemUseAnimation.SPEAR : ItemUseAnimation.NONE;
      }
   }

   public int getUseDuration(ItemStack p_41454_, LivingEntity p_344979_) {
      Consumable consumable = p_41454_.get(DataComponents.CONSUMABLE);
      if (consumable != null) {
         return consumable.consumeTicks();
      } else {
         return !p_41454_.has(DataComponents.BLOCKS_ATTACKS) && !p_41454_.has(DataComponents.KINETIC_WEAPON) ? 0 : 72000;
      }
   }

   public boolean releaseUsing(ItemStack p_41412_, Level p_41413_, LivingEntity p_41414_, int p_41415_) {
      return false;
   }

   @Deprecated
   public void appendHoverText(ItemStack p_41421_, Item.TooltipContext p_339594_, TooltipDisplay p_399753_, Consumer<Component> p_399884_, TooltipFlag p_41424_) {
   }

   public Optional<TooltipComponent> getTooltipImage(ItemStack p_150902_) {
      return Optional.empty();
   }

   @VisibleForTesting
   public final String getDescriptionId() {
      return this.descriptionId;
   }

   public final Component getName() {
      return this.components.getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
   }

   public Component getName(ItemStack p_41458_) {
      return p_41458_.getComponents().getOrDefault(DataComponents.ITEM_NAME, CommonComponents.EMPTY);
   }

   public boolean isFoil(ItemStack p_41453_) {
      return p_41453_.isEnchanted();
   }

   public static BlockHitResult getPlayerPOVHitResult(Level p_41436_, Player p_41437_, ClipContext.Fluid p_41438_) {
      Vec3 vec3 = p_41437_.getEyePosition();
      Vec3 vec31 = vec3.add(p_41437_.calculateViewVector(p_41437_.getXRot(), p_41437_.getYRot()).scale(p_41437_.blockInteractionRange()));
      return p_41436_.clip(new ClipContext(vec3, vec31, ClipContext.Block.OUTLINE, p_41438_, p_41437_));
   }

   public boolean useOnRelease(ItemStack p_41464_) {
      return p_41464_.getItem() == Items.CROSSBOW;
   }

   @Override
   public boolean isCombineRepairable(ItemStack stack) {
      return this.canCombineRepair && this.isDamageable(stack);
   }

   public ItemStack getDefaultInstance() {
      return new ItemStack(this);
   }

   @Deprecated
   public boolean canFitInsideContainerItems() {
      return true;
   }

   @Override
   public FeatureFlagSet requiredFeatures() {
      return this.requiredFeatures;
   }

   public boolean shouldPrintOpWarning(ItemStack p_390446_, @Nullable Player p_390503_) {
      return false;
   }

   public static class Properties implements IItemPropertiesExtensions {
      private static final DependantName<Item, String> BLOCK_DESCRIPTION_ID = p_466642_ -> Util.makeDescriptionId("block", p_466642_.identifier());
      private static final DependantName<Item, String> ITEM_DESCRIPTION_ID = p_466643_ -> Util.makeDescriptionId("item", p_466643_.identifier());
      private final DataComponentMap.Builder components = DataComponentMap.builder().addAll(DataComponents.COMMON_ITEM_COMPONENTS);
      @Nullable
      Item craftingRemainingItem;
      FeatureFlagSet requiredFeatures = FeatureFlags.VANILLA_SET;
      @Nullable
      private ResourceKey<Item> id;
      private DependantName<Item, String> descriptionId = ITEM_DESCRIPTION_ID;
      private final DependantName<Item, Identifier> model = ResourceKey::identifier;
      private boolean canCombineRepair = true;

      public Item.Properties setNoCombineRepair() {
         this.canCombineRepair = false;
         return this;
      }

      public Item.Properties food(FoodProperties p_41490_) {
         return this.food(p_41490_, Consumables.DEFAULT_FOOD);
      }

      public Item.Properties food(FoodProperties p_366775_, Consumable p_366862_) {
         return this.component(DataComponents.FOOD, p_366775_).component(DataComponents.CONSUMABLE, p_366862_);
      }

      public Item.Properties usingConvertsTo(Item p_366596_) {
         return this.component(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStack(p_366596_)));
      }

      public Item.Properties useCooldown(float p_366451_) {
         return this.component(DataComponents.USE_COOLDOWN, new UseCooldown(p_366451_));
      }

      public Item.Properties stacksTo(int p_41488_) {
         return this.component(DataComponents.MAX_STACK_SIZE, p_41488_);
      }

      public Item.Properties durability(int p_41504_) {
         this.component(DataComponents.MAX_DAMAGE, p_41504_);
         this.component(DataComponents.MAX_STACK_SIZE, 1);
         this.component(DataComponents.DAMAGE, 0);
         return this;
      }

      public Item.Properties craftRemainder(Item p_41496_) {
         this.craftingRemainingItem = p_41496_;
         return this;
      }

      public Item.Properties rarity(Rarity p_41498_) {
         return this.component(DataComponents.RARITY, p_41498_);
      }

      public Item.Properties fireResistant() {
         return this.component(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE));
      }

      public Item.Properties jukeboxPlayable(ResourceKey<JukeboxSong> p_350862_) {
         return this.component(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(new EitherHolder<>(p_350862_)));
      }

      public Item.Properties enchantable(int p_363982_) {
         return this.component(DataComponents.ENCHANTABLE, new Enchantable(p_363982_));
      }

      public Item.Properties repairable(Item p_364906_) {
         return this.component(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(p_364906_.builtInRegistryHolder())));
      }

      public Item.Properties repairable(TagKey<Item> p_361677_) {
         HolderGetter<Item> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ITEM);
         return this.component(DataComponents.REPAIRABLE, new Repairable(holdergetter.getOrThrow(p_361677_)));
      }

      public Item.Properties equippable(EquipmentSlot p_371906_) {
         return this.component(DataComponents.EQUIPPABLE, Equippable.builder(p_371906_).build());
      }

      public Item.Properties equippableUnswappable(EquipmentSlot p_372855_) {
         return this.component(DataComponents.EQUIPPABLE, Equippable.builder(p_372855_).setSwappable(false).build());
      }

      public Item.Properties tool(ToolMaterial p_394089_, TagKey<Block> p_394329_, float p_393634_, float p_393761_, float p_399670_) {
         return p_394089_.applyToolProperties(this, p_394329_, p_393634_, p_393761_, p_399670_);
      }

      public Item.Properties pickaxe(ToolMaterial p_393962_, float p_393849_, float p_393522_) {
         return this.tool(p_393962_, BlockTags.MINEABLE_WITH_PICKAXE, p_393849_, p_393522_, 0.0F);
      }

      public Item.Properties axe(ToolMaterial p_399843_, float p_399631_, float p_399629_) {
         return this.tool(p_399843_, BlockTags.MINEABLE_WITH_AXE, p_399631_, p_399629_, 5.0F);
      }

      public Item.Properties hoe(ToolMaterial p_399582_, float p_400114_, float p_399891_) {
         return this.tool(p_399582_, BlockTags.MINEABLE_WITH_HOE, p_400114_, p_399891_, 0.0F);
      }

      public Item.Properties shovel(ToolMaterial p_400072_, float p_399950_, float p_400109_) {
         return this.tool(p_400072_, BlockTags.MINEABLE_WITH_SHOVEL, p_399950_, p_400109_, 0.0F);
      }

      public Item.Properties sword(ToolMaterial p_394325_, float p_394629_, float p_394038_) {
         return p_394325_.applySwordProperties(this, p_394629_, p_394038_);
      }

      public Item.Properties spear(
         ToolMaterial p_454883_,
         float p_455562_,
         float p_455988_,
         float p_456285_,
         float p_454838_,
         float p_454742_,
         float p_455901_,
         float p_454715_,
         float p_454675_,
         float p_456150_
      ) {
         return this.durability(p_454883_.durability())
            .repairable(p_454883_.repairItems())
            .enchantable(p_454883_.enchantmentValue())
            .component(DataComponents.DAMAGE_TYPE, new EitherHolder<>(DamageTypes.SPEAR))
            .component(
               DataComponents.KINETIC_WEAPON,
               new KineticWeapon(
                  10,
                  (int)(p_456285_ * 20.0F),
                  KineticWeapon.Condition.ofAttackerSpeed((int)(p_454838_ * 20.0F), p_454742_),
                  KineticWeapon.Condition.ofAttackerSpeed((int)(p_455901_ * 20.0F), p_454715_),
                  KineticWeapon.Condition.ofRelativeSpeed((int)(p_454675_ * 20.0F), p_456150_),
                  0.38F,
                  p_455988_,
                  Optional.of(p_454883_ == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_USE : SoundEvents.SPEAR_USE),
                  Optional.of(p_454883_ == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT)
               )
            )
            .component(
               DataComponents.PIERCING_WEAPON,
               new PiercingWeapon(
                  true,
                  false,
                  Optional.of(p_454883_ == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_ATTACK : SoundEvents.SPEAR_ATTACK),
                  Optional.of(p_454883_ == ToolMaterial.WOOD ? SoundEvents.SPEAR_WOOD_HIT : SoundEvents.SPEAR_HIT)
               )
            )
            .component(DataComponents.ATTACK_RANGE, new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F))
            .component(DataComponents.MINIMUM_ATTACK_CHARGE, 1.0F)
            .component(DataComponents.SWING_ANIMATION, new SwingAnimation(SwingAnimationType.STAB, (int)(p_455562_ * 20.0F)))
            .attributes(
               ItemAttributeModifiers.builder()
                  .add(
                     Attributes.ATTACK_DAMAGE,
                     new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 0.0F + p_454883_.attackDamageBonus(), AttributeModifier.Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .add(
                     Attributes.ATTACK_SPEED,
                     new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, 1.0F / p_455562_ - 4.0, AttributeModifier.Operation.ADD_VALUE),
                     EquipmentSlotGroup.MAINHAND
                  )
                  .build()
            )
            .component(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0F))
            .component(DataComponents.WEAPON, new Weapon(1));
      }

      public Item.Properties spawnEgg(EntityType<?> p_433964_) {
         return this.component(DataComponents.ENTITY_DATA, TypedEntityData.of(p_433964_, new CompoundTag()));
      }

      public Item.Properties humanoidArmor(ArmorMaterial p_394389_, ArmorType p_393823_) {
         return this.durability(p_393823_.getDurability(p_394389_.durability()))
            .attributes(p_394389_.createAttributes(p_393823_))
            .enchantable(p_394389_.enchantmentValue())
            .component(
               DataComponents.EQUIPPABLE, Equippable.builder(p_393823_.getSlot()).setEquipSound(p_394389_.equipSound()).setAsset(p_394389_.assetId()).build()
            )
            .repairable(p_394389_.repairIngredient());
      }

      public Item.Properties wolfArmor(ArmorMaterial p_399934_) {
         return this.durability(ArmorType.BODY.getDurability(p_399934_.durability()))
            .attributes(p_399934_.createAttributes(ArmorType.BODY))
            .repairable(p_399934_.repairIngredient())
            .component(
               DataComponents.EQUIPPABLE,
               Equippable.builder(EquipmentSlot.BODY)
                  .setEquipSound(p_399934_.equipSound())
                  .setAsset(p_399934_.assetId())
                  .setAllowedEntities(HolderSet.direct(EntityType.WOLF.builtInRegistryHolder()))
                  .setCanBeSheared(true)
                  .setShearingSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.ARMOR_UNEQUIP_WOLF))
                  .build()
            )
            .component(DataComponents.BREAK_SOUND, SoundEvents.WOLF_ARMOR_BREAK)
            .stacksTo(1);
      }

      public Item.Properties horseArmor(ArmorMaterial p_400284_) {
         HolderGetter<EntityType<?>> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
         return this.attributes(p_400284_.createAttributes(ArmorType.BODY))
            .component(
               DataComponents.EQUIPPABLE,
               Equippable.builder(EquipmentSlot.BODY)
                  .setEquipSound(SoundEvents.HORSE_ARMOR)
                  .setAsset(p_400284_.assetId())
                  .setAllowedEntities(holdergetter.getOrThrow(EntityTypeTags.CAN_WEAR_HORSE_ARMOR))
                  .setDamageOnHurt(false)
                  .setCanBeSheared(true)
                  .setShearingSound(SoundEvents.HORSE_ARMOR_UNEQUIP)
                  .build()
            )
            .stacksTo(1);
      }

      public Item.Properties nautilusArmor(ArmorMaterial p_455886_) {
         HolderGetter<EntityType<?>> holdergetter = BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.ENTITY_TYPE);
         return this.attributes(p_455886_.createAttributes(ArmorType.BODY))
            .component(
               DataComponents.EQUIPPABLE,
               Equippable.builder(EquipmentSlot.BODY)
                  .setEquipSound(SoundEvents.ARMOR_EQUIP_NAUTILUS)
                  .setAsset(p_455886_.assetId())
                  .setAllowedEntities(holdergetter.getOrThrow(EntityTypeTags.CAN_WEAR_NAUTILUS_ARMOR))
                  .setDamageOnHurt(false)
                  .setEquipOnInteract(true)
                  .setCanBeSheared(true)
                  .setShearingSound(SoundEvents.ARMOR_UNEQUIP_NAUTILUS)
                  .build()
            )
            .stacksTo(1);
      }

      public Item.Properties trimMaterial(ResourceKey<TrimMaterial> p_400136_) {
         return this.component(DataComponents.PROVIDES_TRIM_MATERIAL, new ProvidesTrimMaterial(p_400136_));
      }

      public Item.Properties requiredFeatures(FeatureFlag... p_250948_) {
         this.requiredFeatures = FeatureFlags.REGISTRY.subset(p_250948_);
         return this;
      }

      public Item.Properties setId(ResourceKey<Item> p_371930_) {
         this.id = p_371930_;
         return this;
      }

      public Item.Properties overrideDescription(String p_371287_) {
         this.descriptionId = DependantName.fixed(p_371287_);
         return this;
      }

      public Item.Properties useBlockDescriptionPrefix() {
         this.descriptionId = BLOCK_DESCRIPTION_ID;
         return this;
      }

      public Item.Properties useItemDescriptionPrefix() {
         this.descriptionId = ITEM_DESCRIPTION_ID;
         return this;
      }

      protected String effectiveDescriptionId() {
         return this.descriptionId.get(Objects.requireNonNull(this.id, "Item id not set"));
      }

      public Identifier effectiveModel() {
         return this.model.get(Objects.requireNonNull(this.id, "Item id not set"));
      }

      public <T> Item.Properties component(DataComponentType<T> p_330871_, T p_330323_) {
         CommonHooks.validateComponent(p_330323_);
         this.components.set(p_330871_, p_330323_);
         return this;
      }

      public Item.Properties attributes(ItemAttributeModifiers p_331533_) {
         return this.component(DataComponents.ATTRIBUTE_MODIFIERS, p_331533_);
      }

      DataComponentMap buildAndValidateComponents(Component p_371796_, Identifier p_467042_) {
         DataComponentMap datacomponentmap = this.components.set(DataComponents.ITEM_NAME, p_371796_).set(DataComponents.ITEM_MODEL, p_467042_).build();
         return validateComponents(datacomponentmap);
      }

      public static DataComponentMap validateComponents(DataComponentMap datacomponentmap) {
         if (datacomponentmap.has(DataComponents.DAMAGE) && datacomponentmap.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
            throw new IllegalStateException("Item cannot have both durability and be stackable");
         } else {
            return datacomponentmap;
         }
      }
   }

   public interface TooltipContext {
      Item.TooltipContext EMPTY = new Item.TooltipContext() {
         @Nullable
         @Override
         public HolderLookup.Provider registries() {
            return null;
         }

         @Override
         public float tickRate() {
            return 20.0F;
         }

         @Nullable
         @Override
         public MapItemSavedData mapData(MapId p_339618_) {
            return null;
         }

         @Override
         public boolean isPeaceful() {
            return false;
         }
      };

      @Nullable
      HolderLookup.Provider registries();

      float tickRate();

      @Nullable
      MapItemSavedData mapData(MapId var1);

      boolean isPeaceful();

      @Nullable
      default Level level() {
         return null;
      }

      static Item.TooltipContext of(@Nullable final Level p_339599_) {
         return p_339599_ == null ? EMPTY : new Item.TooltipContext() {
            @Override
            public HolderLookup.Provider registries() {
               return p_339599_.registryAccess();
            }

            @Override
            public float tickRate() {
               return p_339599_.tickRateManager().tickrate();
            }

            @Override
            public MapItemSavedData mapData(MapId p_339628_) {
               return p_339599_.getMapData(p_339628_);
            }

            @Override
            public boolean isPeaceful() {
               return p_339599_.getDifficulty() == Difficulty.PEACEFUL;
            }

            @Override
            public Level level() {
               return p_339599_;
            }
         };
      }

      static Item.TooltipContext of(final HolderLookup.Provider p_339633_) {
         return new Item.TooltipContext() {
            @Override
            public HolderLookup.Provider registries() {
               return p_339633_;
            }

            @Override
            public float tickRate() {
               return 20.0F;
            }

            @Nullable
            @Override
            public MapItemSavedData mapData(MapId p_339679_) {
               return null;
            }

            @Override
            public boolean isPeaceful() {
               return false;
            }
         };
      }
   }
}

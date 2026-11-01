package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

public interface IItemStackExtension {
   private ItemStack self() {
      return (ItemStack)this;
   }

   default ItemStack getCraftingRemainingItem() {
      return this.self().getItem().getCraftingRemainingItem(this.self());
   }

   default boolean hasCraftingRemainingItem() {
      return this.self().getItem().hasCraftingRemainingItem(this.self());
   }

   default int getBurnTime(@Nullable RecipeType<?> recipeType) {
      if (this.self().isEmpty()) {
         return 0;
      } else {
         int burnTime = this.self().getItem().getBurnTime(this.self(), recipeType);
         if (burnTime < 0) {
            throw new IllegalStateException("Stack of item " + BuiltInRegistries.ITEM.getKey(this.self().getItem()) + " has a negative burn time");
         } else {
            return EventHooks.getItemBurnTime(this.self(), burnTime, recipeType);
         }
      }
   }

   default InteractionResult onItemUseFirst(UseOnContext context) {
      Player entityplayer = context.getPlayer();
      BlockPos blockpos = context.getClickedPos();
      BlockInWorld blockworldstate = new BlockInWorld(context.getLevel(), blockpos, false);
      AdventureModePredicate adventureModePredicate = this.self().get(DataComponents.CAN_PLACE_ON);
      if (entityplayer == null || entityplayer.getAbilities().mayBuild || adventureModePredicate != null && adventureModePredicate.test(blockworldstate)) {
         Item item = this.self().getItem();
         InteractionResult enumactionresult = item.onItemUseFirst(this.self(), context);
         if (entityplayer != null && enumactionresult == InteractionResult.SUCCESS) {
            entityplayer.awardStat(Stats.ITEM_USED.get(item));
         }

         return enumactionresult;
      } else {
         return InteractionResult.PASS;
      }
   }

   default boolean canPerformAction(ItemAbility itemAbility) {
      return this.self().getItem().canPerformAction(this.self(), itemAbility);
   }

   default boolean shouldCauseBlockBreakReset(ItemStack newStack) {
      return this.self().getItem().shouldCauseBlockBreakReset(this.self(), newStack);
   }

   default boolean isPrimaryItemFor(Holder<Enchantment> enchantment) {
      return this.self().getItem().isPrimaryItemFor(this.self(), enchantment);
   }

   default boolean supportsEnchantment(Holder<Enchantment> enchantment) {
      return this.self().getItem().supportsEnchantment(this.self(), enchantment);
   }

   default int getEnchantmentLevel(Holder<Enchantment> enchantment) {
      int level = this.self().getItem().getEnchantmentLevel(this.self(), enchantment);
      return EventHooks.getEnchantmentLevelSpecific(level, this.self(), enchantment);
   }

   default ItemEnchantments getAllEnchantments(HolderLookup.RegistryLookup<Enchantment> lookup) {
      ItemEnchantments enchantments = this.self().getItem().getAllEnchantments(this.self(), lookup);
      return EventHooks.getAllEnchantmentLevels(enchantments, this.self(), lookup);
   }

   default int getEnchantmentValue() {
      return this.self().getItem().getEnchantmentValue(this.self());
   }

   @Nullable
   default EquipmentSlot getEquipmentSlot() {
      return this.self().getItem().getEquipmentSlot(this.self());
   }

   default boolean canDisableShield(ItemStack shield, LivingEntity entity, LivingEntity attacker) {
      return this.self().getItem().canDisableShield(this.self(), shield, entity, attacker);
   }

   @Deprecated(
      forRemoval = true,
      since = "21.1"
   )
   default boolean onEntitySwing(LivingEntity entity) {
      return this.self().getItem().onEntitySwing(this.self(), entity);
   }

   default boolean onEntitySwing(LivingEntity entity, InteractionHand hand) {
      return this.self().getItem().onEntitySwing(this.self(), entity, hand);
   }

   default void onStopUsing(LivingEntity entity, int count) {
      this.self().getItem().onStopUsing(this.self(), entity, count);
   }

   default int getEntityLifespan(Level level) {
      return this.self().getItem().getEntityLifespan(this.self(), level);
   }

   default boolean onEntityItemUpdate(ItemEntity entity) {
      return this.self().getItem().onEntityItemUpdate(this.self(), entity);
   }

   default float getXpRepairRatio() {
      return this.self().getItem().getXpRepairRatio(this.self());
   }

   default void onAnimalArmorTick(Level level, Mob horse) {
      this.self().getItem().onAnimalArmorTick(this.self(), level, horse);
   }

   default boolean canEquip(EquipmentSlot armorType, LivingEntity entity) {
      return this.self().getItem().canEquip(this.self(), armorType, entity);
   }

   default boolean isBookEnchantable(ItemStack book) {
      return this.self().getItem().isBookEnchantable(this.self(), book);
   }

   default boolean onDroppedByPlayer(Player player) {
      return this.self().getItem().onDroppedByPlayer(this.self(), player);
   }

   default Component getHighlightTip(Component displayName) {
      return this.self().getItem().getHighlightTip(this.self(), displayName);
   }

   default boolean doesSneakBypassUse(LevelReader level, BlockPos pos, Player player) {
      return this.self().isEmpty() || this.self().getItem().doesSneakBypassUse(this.self(), level, pos, player);
   }

   default boolean isRepairable() {
      return this.self().getItem().isRepairable(this.self());
   }

   default boolean isPiglinCurrency() {
      return this.self().getItem().isPiglinCurrency(this.self());
   }

   default boolean makesPiglinsNeutral(LivingEntity wearer) {
      return this.self().getItem().makesPiglinsNeutral(this.self(), wearer);
   }

   default boolean isEnderMask(Player player, EnderMan endermanEntity) {
      return this.self().getItem().isEnderMask(this.self(), player, endermanEntity);
   }

   default boolean canElytraFly(LivingEntity entity) {
      return this.self().getItem().canElytraFly(this.self(), entity);
   }

   default boolean elytraFlightTick(LivingEntity entity, int flightTicks) {
      return this.self().getItem().elytraFlightTick(this.self(), entity, flightTicks);
   }

   default boolean canWalkOnPowderedSnow(LivingEntity wearer) {
      return this.self().getItem().canWalkOnPowderedSnow(this.self(), wearer);
   }

   default AABB getSweepHitBox(Player player, Entity target) {
      return this.self().getItem().getSweepHitBox(this.self(), player, target);
   }

   default void onDestroyed(ItemEntity itemEntity, DamageSource damageSource) {
      this.self().getItem().onDestroyed(itemEntity, damageSource);
   }

   @Nullable
   default FoodProperties getFoodProperties(@Nullable LivingEntity entity) {
      return this.self().getItem().getFoodProperties(this.self(), entity);
   }

   default boolean isNotReplaceableByPickAction(Player player, int inventorySlot) {
      return this.self().getItem().isNotReplaceableByPickAction(this.self(), player, inventorySlot);
   }

   default boolean canGrindstoneRepair() {
      return this.self().getItem().canGrindstoneRepair(this.self());
   }

   @Nullable
   default <T, C> T getCapability(ItemCapability<T, C> capability, C context) {
      return capability.getCapability(this.self(), context);
   }

   @Nullable
   default <T> T getCapability(ItemCapability<T, Void> capability) {
      return capability.getCapability(this.self(), null);
   }

   default ItemAttributeModifiers getAttributeModifiers() {
      ItemAttributeModifiers defaultModifiers = this.self().getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      if (defaultModifiers.modifiers().isEmpty()) {
         defaultModifiers = this.self().getItem().getDefaultAttributeModifiers(this.self());
      }

      return CommonHooks.computeModifiedAttributes(this.self(), defaultModifiers);
   }

   default boolean canFitInsideContainerItems() {
      return this.self().getItem().canFitInsideContainerItems(this.self());
   }
}

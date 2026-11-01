package net.neoforged.neoforge.common.extensions;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

public interface IItemExtension {
   private Item self() {
      return (Item)this;
   }

   default ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
      return this.self().getDefaultAttributeModifiers();
   }

   default boolean onDroppedByPlayer(ItemStack item, Player player) {
      return true;
   }

   default Component getHighlightTip(ItemStack item, Component displayName) {
      return displayName;
   }

   default InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
      return InteractionResult.PASS;
   }

   default boolean isPiglinCurrency(ItemStack stack) {
      return stack.getItem() == PiglinAi.BARTERING_ITEM;
   }

   default boolean makesPiglinsNeutral(ItemStack stack, LivingEntity wearer) {
      return stack.getItem() instanceof ArmorItem && ((ArmorItem)stack.getItem()).getMaterial() == ArmorMaterials.GOLD;
   }

   boolean isRepairable(ItemStack var1);

   default float getXpRepairRatio(ItemStack stack) {
      return 1.0F;
   }

   default void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
   }

   default boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
      return false;
   }

   default ItemStack getCraftingRemainingItem(ItemStack itemStack) {
      return !this.hasCraftingRemainingItem(itemStack) ? ItemStack.EMPTY : new ItemStack(this.self().getCraftingRemainingItem());
   }

   default boolean hasCraftingRemainingItem(ItemStack stack) {
      return this.self().hasCraftingRemainingItem();
   }

   default int getEntityLifespan(ItemStack itemStack, Level level) {
      return 6000;
   }

   default boolean hasCustomEntity(ItemStack stack) {
      return false;
   }

   @Nullable
   default Entity createEntity(Level level, Entity location, ItemStack stack) {
      return null;
   }

   default boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
      return false;
   }

   default boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
      return false;
   }

   default boolean canEquip(ItemStack stack, EquipmentSlot armorType, LivingEntity entity) {
      return entity.getEquipmentSlotForItem(stack) == armorType;
   }

   @Nullable
   default EquipmentSlot getEquipmentSlot(ItemStack stack) {
      return null;
   }

   default boolean isBookEnchantable(ItemStack stack, ItemStack book) {
      return true;
   }

   @Nullable
   default ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
      return null;
   }

   @Deprecated(
      forRemoval = true,
      since = "21.1"
   )
   default boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      return false;
   }

   default boolean onEntitySwing(ItemStack stack, LivingEntity entity, InteractionHand hand) {
      return this.onEntitySwing(stack, entity);
   }

   default int getDamage(ItemStack stack) {
      return Mth.clamp(stack.getOrDefault(DataComponents.DAMAGE, 0), 0, stack.getMaxDamage());
   }

   default int getMaxDamage(ItemStack stack) {
      return stack.getOrDefault(DataComponents.MAX_DAMAGE, 0);
   }

   default boolean isDamaged(ItemStack stack) {
      return stack.getDamageValue() > 0;
   }

   default void setDamage(ItemStack stack, int damage) {
      stack.set(DataComponents.DAMAGE, Mth.clamp(damage, 0, stack.getMaxDamage()));
   }

   default boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
      return false;
   }

   default int getMaxStackSize(ItemStack stack) {
      return stack.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
   }

   default int getEnchantmentValue(ItemStack stack) {
      return this.self().getEnchantmentValue();
   }

   @OverrideOnly
   default boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
      if (stack.getItem() == Items.BOOK) {
         return true;
      } else {
         Optional<HolderSet<Item>> primaryItems = enchantment.value().definition().primaryItems();
         return this.supportsEnchantment(stack, enchantment) && (primaryItems.isEmpty() || stack.is(primaryItems.get()));
      }
   }

   @OverrideOnly
   default boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
      return stack.is(Items.ENCHANTED_BOOK) || enchantment.value().isSupportedItem(stack);
   }

   @OverrideOnly
   default int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
      ItemEnchantments itemenchantments = stack.getTagEnchantments();
      return itemenchantments.getLevel(enchantment);
   }

   @OverrideOnly
   default ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
      return stack.getTagEnchantments();
   }

   default boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return !oldStack.equals(newStack);
   }

   default boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
      if (!newStack.is(oldStack.getItem())) {
         return true;
      } else if (newStack.isDamageableItem() && oldStack.isDamageableItem()) {
         DataComponentMap newComponents = newStack.getComponents();
         DataComponentMap oldComponents = oldStack.getComponents();
         if (!newComponents.isEmpty() && !oldComponents.isEmpty()) {
            Set<DataComponentType<?>> newKeys = new HashSet<>(newComponents.keySet());
            Set<DataComponentType<?>> oldKeys = new HashSet<>(oldComponents.keySet());
            newKeys.remove(DataComponents.DAMAGE);
            oldKeys.remove(DataComponents.DAMAGE);
            return !newKeys.equals(oldKeys)
               ? true
               : !newKeys.stream().allMatch(key -> Objects.equals(newComponents.get((DataComponentType<?>)key), oldComponents.get((DataComponentType<?>)key)));
         } else {
            return !newComponents.isEmpty() || !oldComponents.isEmpty();
         }
      } else {
         return !ItemStack.isSameItemSameComponents(newStack, oldStack);
      }
   }

   default boolean canContinueUsing(ItemStack oldStack, ItemStack newStack) {
      return oldStack == newStack ? true : !oldStack.isEmpty() && !newStack.isEmpty() && ItemStack.isSameItem(newStack, oldStack);
   }

   @Nullable
   default String getCreatorModId(ItemStack itemStack) {
      return CommonHooks.getDefaultCreatorModId(itemStack);
   }

   default boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
      return this instanceof AxeItem;
   }

   @OverrideOnly
   default int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
      FurnaceFuel furnaceFuel = this.self().builtInRegistryHolder().getData(NeoForgeDataMaps.FURNACE_FUELS);
      return furnaceFuel == null ? 0 : furnaceFuel.burnTime();
   }

   @OverrideOnly
   default void onAnimalArmorTick(ItemStack stack, Level level, Mob horse) {
   }

   default <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
      return amount;
   }

   default void onDestroyed(ItemEntity itemEntity, DamageSource damageSource) {
      this.self().onDestroyed(itemEntity);
   }

   default boolean isEnderMask(ItemStack stack, Player player, EnderMan endermanEntity) {
      return stack.getItem() == Blocks.CARVED_PUMPKIN.asItem();
   }

   default boolean canElytraFly(ItemStack stack, LivingEntity entity) {
      return false;
   }

   default boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
      return false;
   }

   default boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
      return stack.is(Items.LEATHER_BOOTS);
   }

   default boolean isDamageable(ItemStack stack) {
      return stack.has(DataComponents.MAX_DAMAGE);
   }

   default AABB getSweepHitBox(ItemStack stack, Player player, Entity target) {
      return target.getBoundingBox().inflate(1.0, 0.25, 1.0);
   }

   @Nullable
   default FoodProperties getFoodProperties(ItemStack stack, @Nullable LivingEntity entity) {
      return stack.get(DataComponents.FOOD);
   }

   default boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
      return stack.isEnchanted();
   }

   default boolean canGrindstoneRepair(ItemStack stack) {
      return false;
   }

   default boolean canBeHurtBy(ItemStack stack, DamageSource source) {
      return true;
   }

   default ItemStack applyEnchantments(ItemStack stack, List<EnchantmentInstance> enchantments) {
      if (stack.is(Items.BOOK)) {
         stack = stack.transmuteCopy(Items.ENCHANTED_BOOK);
      }

      for (EnchantmentInstance inst : enchantments) {
         stack.enchant(inst.enchantment, inst.level);
      }

      return stack;
   }

   default boolean canFitInsideContainerItems(ItemStack stack) {
      return this.self().canFitInsideContainerItems();
   }
}

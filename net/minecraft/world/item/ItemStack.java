package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.DataResult.Error;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.NullOps;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gameevent.GameEvent;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class ItemStack implements DataComponentHolder {
   private static final List<Component> OP_NBT_WARNING = List.of(
      Component.translatable("item.op_warning.line1").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
      Component.translatable("item.op_warning.line2").withStyle(ChatFormatting.RED),
      Component.translatable("item.op_warning.line3").withStyle(ChatFormatting.RED)
   );
   private static final Component UNBREAKABLE_TOOLTIP = Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE);
   private static final Component INTANGIBLE_TOOLTIP = Component.translatable("item.intangible").withStyle(ChatFormatting.GRAY);
   public static final MapCodec<ItemStack> MAP_CODEC = MapCodec.recursive(
      "ItemStack",
      codec -> RecordCodecBuilder.mapCodec(
         instance -> instance.group(
               Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
               ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
               DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(itemStack -> itemStack.components.asPatch())
            )
            .apply(instance, ItemStack::new)
      )
   );
   public static final Codec<ItemStack> CODEC = Codec.lazyInitialized(MAP_CODEC::codec);
   public static final Codec<ItemStack> SINGLE_ITEM_CODEC = Codec.lazyInitialized(
      () -> RecordCodecBuilder.create(
         instance -> instance.group(
               Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
               DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(itemStack -> itemStack.components.asPatch())
            )
            .apply(instance, (holder, dataComponentPatch) -> new ItemStack(holder, 1, dataComponentPatch))
      )
   );
   public static final Codec<ItemStack> STRICT_CODEC = CODEC.validate(ItemStack::validateStrict);
   public static final Codec<ItemStack> STRICT_SINGLE_ITEM_CODEC = SINGLE_ITEM_CODEC.validate(ItemStack::validateStrict);
   public static final Codec<ItemStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
      .xmap(optional -> optional.orElse(ItemStack.EMPTY), itemStack -> itemStack.isEmpty() ? Optional.empty() : Optional.of(itemStack));
   public static final Codec<ItemStack> SIMPLE_ITEM_CODEC = Item.CODEC.xmap(ItemStack::new, ItemStack::getItemHolder);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_STREAM_CODEC = createOptionalStreamCodec(DataComponentPatch.STREAM_CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_UNTRUSTED_STREAM_CODEC = createOptionalStreamCodec(
      DataComponentPatch.DELIMITED_STREAM_CODEC
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
      public ItemStack decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
         ItemStack itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(registryFriendlyByteBuf);
         if (itemStack.isEmpty()) {
            throw new DecoderException("Empty ItemStack not allowed");
         } else {
            return itemStack;
         }
      }

      public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, ItemStack itemStack) {
         if (itemStack.isEmpty()) {
            throw new EncoderException("Empty ItemStack not allowed");
         } else {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(registryFriendlyByteBuf, itemStack);
         }
      }
   };
   public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> OPTIONAL_LIST_STREAM_CODEC = OPTIONAL_STREAM_CODEC.apply(
      ByteBufCodecs.collection(NonNullList::createWithCapacity)
   );
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final ItemStack EMPTY = new ItemStack((Void)null);
   private static final Component DISABLED_ITEM_TOOLTIP = Component.translatable("item.disabled").withStyle(ChatFormatting.RED);
   private int count;
   private int popTime;
   @Deprecated
   @Nullable
   private final Item item;
   final PatchedDataComponentMap components;
   @Nullable
   private Entity entityRepresentation;

   public static DataResult<ItemStack> validateStrict(ItemStack stack) {
      DataResult<Unit> dataResult = validateComponents(stack.getComponents());
      if (dataResult.isError()) {
         return dataResult.map(unit -> stack);
      } else {
         return stack.getCount() > stack.getMaxStackSize()
            ? DataResult.error(() -> "Item stack with stack size of " + stack.getCount() + " was larger than maximum: " + stack.getMaxStackSize())
            : DataResult.success(stack);
      }
   }

   private static StreamCodec<RegistryFriendlyByteBuf, ItemStack> createOptionalStreamCodec(StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> codec) {
      return new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
         public ItemStack decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            int i = registryFriendlyByteBuf.readVarInt();
            if (i <= 0) {
               return ItemStack.EMPTY;
            } else {
               Holder<Item> holder = Item.STREAM_CODEC.decode(registryFriendlyByteBuf);
               DataComponentPatch dataComponentPatch = codec.decode(registryFriendlyByteBuf);
               return new ItemStack(holder, i, dataComponentPatch);
            }
         }

         public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, ItemStack itemStack) {
            if (itemStack.isEmpty()) {
               registryFriendlyByteBuf.writeVarInt(0);
            } else {
               registryFriendlyByteBuf.writeVarInt(itemStack.getCount());
               Item.STREAM_CODEC.encode(registryFriendlyByteBuf, itemStack.getItemHolder());
               codec.encode(registryFriendlyByteBuf, itemStack.components.asPatch());
            }
         }
      };
   }

   public static StreamCodec<RegistryFriendlyByteBuf, ItemStack> validatedStreamCodec(StreamCodec<RegistryFriendlyByteBuf, ItemStack> codec) {
      return new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
         public ItemStack decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
            ItemStack itemStack = codec.decode(registryFriendlyByteBuf);
            if (!itemStack.isEmpty()) {
               RegistryOps<Unit> registryOps = registryFriendlyByteBuf.registryAccess().createSerializationContext(NullOps.INSTANCE);
               ItemStack.CODEC.encodeStart(registryOps, itemStack).getOrThrow(DecoderException::new);
            }

            return itemStack;
         }

         public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, ItemStack itemStack) {
            codec.encode(registryFriendlyByteBuf, itemStack);
         }
      };
   }

   public Optional<TooltipComponent> getTooltipImage() {
      return this.getItem().getTooltipImage(this);
   }

   @Override
   public DataComponentMap getComponents() {
      return (DataComponentMap)(!this.isEmpty() ? this.components : DataComponentMap.EMPTY);
   }

   public DataComponentMap getPrototype() {
      return !this.isEmpty() ? this.getItem().components() : DataComponentMap.EMPTY;
   }

   public DataComponentPatch getComponentsPatch() {
      return !this.isEmpty() ? this.components.asPatch() : DataComponentPatch.EMPTY;
   }

   public DataComponentMap immutableComponents() {
      return !this.isEmpty() ? this.components.toImmutableMap() : DataComponentMap.EMPTY;
   }

   public boolean hasNonDefault(DataComponentType<?> component) {
      return !this.isEmpty() && this.components.hasNonDefault(component);
   }

   public ItemStack(ItemLike item) {
      this(item, 1);
   }

   public ItemStack(Holder<Item> tag) {
      this(tag.value(), 1);
   }

   public ItemStack(Holder<Item> tag, int count, DataComponentPatch components) {
      this(tag.value(), count, PatchedDataComponentMap.fromPatch(tag.value().components(), components));
   }

   public ItemStack(Holder<Item> item, int count) {
      this(item.value(), count);
   }

   public ItemStack(ItemLike item, int count) {
      this(item, count, new PatchedDataComponentMap(item.asItem().components()));
   }

   private ItemStack(ItemLike item, int count, PatchedDataComponentMap components) {
      this.item = item.asItem();
      this.count = count;
      this.components = components;
   }

   private ItemStack(@Nullable Void unused) {
      this.item = null;
      this.components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
   }

   public static DataResult<Unit> validateComponents(DataComponentMap components) {
      if (components.has(DataComponents.MAX_DAMAGE) && components.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
         return DataResult.error(() -> "Item cannot be both damageable and stackable");
      } else {
         ItemContainerContents itemContainerContents = components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

         for (ItemStack itemStack : itemContainerContents.nonEmptyItems()) {
            int i = itemStack.getCount();
            int j = itemStack.getMaxStackSize();
            if (i > j) {
               return DataResult.error(() -> "Item stack with count of " + i + " was larger than maximum: " + j);
            }
         }

         return DataResult.success(Unit.INSTANCE);
      }
   }

   public boolean isEmpty() {
      return this == EMPTY || this.item == Items.AIR || this.count <= 0;
   }

   public boolean isItemEnabled(FeatureFlagSet enabledFlags) {
      return this.isEmpty() || this.getItem().isEnabled(enabledFlags);
   }

   public ItemStack split(int amount) {
      int i = Math.min(amount, this.getCount());
      ItemStack itemStack = this.copyWithCount(i);
      this.shrink(i);
      return itemStack;
   }

   public ItemStack copyAndClear() {
      if (this.isEmpty()) {
         return EMPTY;
      } else {
         ItemStack itemStack = this.copy();
         this.setCount(0);
         return itemStack;
      }
   }

   public Item getItem() {
      return this.isEmpty() ? Items.AIR : this.item;
   }

   public Holder<Item> getItemHolder() {
      return this.getItem().builtInRegistryHolder();
   }

   public boolean is(TagKey<Item> tag) {
      return this.getItem().builtInRegistryHolder().is(tag);
   }

   public boolean is(Item item) {
      return this.getItem() == item;
   }

   public boolean is(Predicate<Holder<Item>> item) {
      return item.test(this.getItem().builtInRegistryHolder());
   }

   public boolean is(Holder<Item> item) {
      return this.getItem().builtInRegistryHolder() == item;
   }

   public boolean is(HolderSet<Item> item) {
      return item.contains(this.getItemHolder());
   }

   public Stream<TagKey<Item>> getTags() {
      return this.getItem().builtInRegistryHolder().tags();
   }

   public InteractionResult useOn(UseOnContext context) {
      Player player = context.getPlayer();
      BlockPos blockPos = context.getClickedPos();
      if (player != null && !player.getAbilities().mayBuild && !this.canPlaceOnBlockInAdventureMode(new BlockInWorld(context.getLevel(), blockPos, false))) {
         return InteractionResult.PASS;
      } else {
         Item item = this.getItem();
         InteractionResult interactionResult = item.useOn(context);
         if (player != null && interactionResult instanceof InteractionResult.Success success && success.wasItemInteraction()) {
            player.awardStat(Stats.ITEM_USED.get(item));
         }

         return interactionResult;
      }
   }

   public float getDestroySpeed(BlockState state) {
      return this.getItem().getDestroySpeed(this, state);
   }

   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      ItemStack itemStack = this.copy();
      boolean bl = this.getUseDuration(player) <= 0;
      InteractionResult interactionResult = this.getItem().use(level, player, hand);
      return (InteractionResult)(bl && interactionResult instanceof InteractionResult.Success success
         ? success.heldItemTransformedTo(
            success.heldItemTransformedTo() == null
               ? this.applyAfterUseComponentSideEffects(player, itemStack)
               : success.heldItemTransformedTo().applyAfterUseComponentSideEffects(player, itemStack)
         )
         : interactionResult);
   }

   public ItemStack finishUsingItem(Level level, LivingEntity livingEntity) {
      ItemStack itemStack = this.copy();
      ItemStack itemStack2 = this.getItem().finishUsingItem(this, level, livingEntity);
      return itemStack2.applyAfterUseComponentSideEffects(livingEntity, itemStack);
   }

   private ItemStack applyAfterUseComponentSideEffects(LivingEntity entity, ItemStack stack) {
      UseRemainder useRemainder = stack.get(DataComponents.USE_REMAINDER);
      UseCooldown useCooldown = stack.get(DataComponents.USE_COOLDOWN);
      int i = stack.getCount();
      ItemStack itemStack = this;
      if (useRemainder != null) {
         itemStack = useRemainder.convertIntoRemainder(this, i, entity.hasInfiniteMaterials(), entity::handleExtraItemsCreatedOnUse);
      }

      if (useCooldown != null) {
         useCooldown.apply(stack, entity);
      }

      return itemStack;
   }

   public int getMaxStackSize() {
      return this.getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
   }

   public boolean isStackable() {
      return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
   }

   public boolean isDamageableItem() {
      return this.has(DataComponents.MAX_DAMAGE) && !this.has(DataComponents.UNBREAKABLE) && this.has(DataComponents.DAMAGE);
   }

   public boolean isDamaged() {
      return this.isDamageableItem() && this.getDamageValue() > 0;
   }

   public int getDamageValue() {
      return Mth.clamp(this.getOrDefault(DataComponents.DAMAGE, 0), 0, this.getMaxDamage());
   }

   public void setDamageValue(int damage) {
      this.set(DataComponents.DAMAGE, Mth.clamp(damage, 0, this.getMaxDamage()));
   }

   public int getMaxDamage() {
      return this.getOrDefault(DataComponents.MAX_DAMAGE, 0);
   }

   public boolean isBroken() {
      return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage();
   }

   public boolean nextDamageWillBreak() {
      return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage() - 1;
   }

   public void hurtAndBreak(int damage, ServerLevel level, @Nullable ServerPlayer player, Consumer<Item> onBreak) {
      int i = this.processDurabilityChange(damage, level, player);
      if (i != 0) {
         this.applyDamage(this.getDamageValue() + i, player, onBreak);
      }
   }

   private int processDurabilityChange(int damage, ServerLevel level, @Nullable ServerPlayer player) {
      if (!this.isDamageableItem()) {
         return 0;
      } else if (player != null && player.hasInfiniteMaterials()) {
         return 0;
      } else {
         return damage > 0 ? EnchantmentHelper.processDurabilityChange(level, this, damage) : damage;
      }
   }

   private void applyDamage(int damage, @Nullable ServerPlayer player, Consumer<Item> onBreak) {
      if (player != null) {
         CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(player, this, damage);
      }

      this.setDamageValue(damage);
      if (this.isBroken()) {
         Item item = this.getItem();
         this.shrink(1);
         onBreak.accept(item);
      }
   }

   public void hurtWithoutBreaking(int damage, Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         int i = this.processDurabilityChange(damage, serverPlayer.level(), serverPlayer);
         if (i == 0) {
            return;
         }

         int j = Math.min(this.getDamageValue() + i, this.getMaxDamage() - 1);
         this.applyDamage(j, serverPlayer, item -> {});
      }
   }

   public void hurtAndBreak(int amount, LivingEntity entity, InteractionHand hand) {
      this.hurtAndBreak(amount, entity, hand.asEquipmentSlot());
   }

   public void hurtAndBreak(int amount, LivingEntity entity, EquipmentSlot slot) {
      if (entity.level() instanceof ServerLevel serverLevel) {
         this.hurtAndBreak(
            amount, serverLevel, entity instanceof ServerPlayer serverPlayer ? serverPlayer : null, item -> entity.onEquippedItemBroken(item, slot)
         );
      }
   }

   public ItemStack hurtAndConvertOnBreak(int amount, ItemLike item, LivingEntity entity, EquipmentSlot slot) {
      this.hurtAndBreak(amount, entity, slot);
      if (this.isEmpty()) {
         ItemStack itemStack = this.transmuteCopyIgnoreEmpty(item, 1);
         if (itemStack.isDamageableItem()) {
            itemStack.setDamageValue(0);
         }

         return itemStack;
      } else {
         return this;
      }
   }

   public boolean isBarVisible() {
      return this.getItem().isBarVisible(this);
   }

   public int getBarWidth() {
      return this.getItem().getBarWidth(this);
   }

   public int getBarColor() {
      return this.getItem().getBarColor(this);
   }

   public boolean overrideStackedOnOther(Slot slot, ClickAction action, Player player) {
      return this.getItem().overrideStackedOnOther(this, slot, action, player);
   }

   public boolean overrideOtherStackedOnMe(ItemStack stack, Slot slot, ClickAction action, Player player, SlotAccess access) {
      return this.getItem().overrideOtherStackedOnMe(this, stack, slot, action, player, access);
   }

   public boolean hurtEnemy(LivingEntity enemy, LivingEntity attacker) {
      Item item = this.getItem();
      item.hurtEnemy(this, enemy, attacker);
      if (this.has(DataComponents.WEAPON)) {
         if (attacker instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(item));
         }

         return true;
      } else {
         return false;
      }
   }

   public void postHurtEnemy(LivingEntity enemy, LivingEntity attacker) {
      this.getItem().postHurtEnemy(this, enemy, attacker);
      Weapon weapon = this.get(DataComponents.WEAPON);
      if (weapon != null) {
         this.hurtAndBreak(weapon.itemDamagePerAttack(), attacker, EquipmentSlot.MAINHAND);
      }
   }

   public void mineBlock(Level level, BlockState state, BlockPos pos, Player player) {
      Item item = this.getItem();
      if (item.mineBlock(this, level, state, pos, player)) {
         player.awardStat(Stats.ITEM_USED.get(item));
      }
   }

   public boolean isCorrectToolForDrops(BlockState state) {
      return this.getItem().isCorrectToolForDrops(this, state);
   }

   public InteractionResult interactLivingEntity(Player player, LivingEntity entity, InteractionHand usedHand) {
      Equippable equippable = this.get(DataComponents.EQUIPPABLE);
      if (equippable != null && equippable.equipOnInteract()) {
         InteractionResult interactionResult = equippable.equipOnTarget(player, entity, this);
         if (interactionResult != InteractionResult.PASS) {
            return interactionResult;
         }
      }

      return this.getItem().interactLivingEntity(this, player, entity, usedHand);
   }

   public ItemStack copy() {
      if (this.isEmpty()) {
         return EMPTY;
      } else {
         ItemStack itemStack = new ItemStack(this.getItem(), this.count, this.components.copy());
         itemStack.setPopTime(this.getPopTime());
         return itemStack;
      }
   }

   public ItemStack copyWithCount(int count) {
      if (this.isEmpty()) {
         return EMPTY;
      } else {
         ItemStack itemStack = this.copy();
         itemStack.setCount(count);
         return itemStack;
      }
   }

   public ItemStack transmuteCopy(ItemLike item) {
      return this.transmuteCopy(item, this.getCount());
   }

   public ItemStack transmuteCopy(ItemLike item, int count) {
      return this.isEmpty() ? EMPTY : this.transmuteCopyIgnoreEmpty(item, count);
   }

   private ItemStack transmuteCopyIgnoreEmpty(ItemLike item, int count) {
      return new ItemStack(item.asItem().builtInRegistryHolder(), count, this.components.asPatch());
   }

   public static boolean matches(ItemStack stack, ItemStack other) {
      if (stack == other) {
         return true;
      } else {
         return stack.getCount() != other.getCount() ? false : isSameItemSameComponents(stack, other);
      }
   }

   @Deprecated
   public static boolean listMatches(List<ItemStack> list, List<ItemStack> other) {
      if (list.size() != other.size()) {
         return false;
      } else {
         for (int i = 0; i < list.size(); i++) {
            if (!matches(list.get(i), other.get(i))) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean isSameItem(ItemStack stack, ItemStack other) {
      return stack.is(other.getItem());
   }

   public static boolean isSameItemSameComponents(ItemStack stack, ItemStack other) {
      if (!stack.is(other.getItem())) {
         return false;
      } else {
         return stack.isEmpty() && other.isEmpty() ? true : Objects.equals(stack.components, other.components);
      }
   }

   public static boolean matchesIgnoringComponents(ItemStack stack, ItemStack other, Predicate<DataComponentType<?>> shouldIgnore) {
      if (stack == other) {
         return true;
      } else if (stack.getCount() != other.getCount()) {
         return false;
      } else if (!stack.is(other.getItem())) {
         return false;
      } else if (stack.isEmpty() && other.isEmpty()) {
         return true;
      } else if (stack.components.size() != other.components.size()) {
         return false;
      } else {
         for (DataComponentType<?> dataComponentType : stack.components.keySet()) {
            Object object = stack.components.get(dataComponentType);
            Object object2 = other.components.get(dataComponentType);
            if (object == null || object2 == null) {
               return false;
            }

            if (!Objects.equals(object, object2) && !shouldIgnore.test(dataComponentType)) {
               return false;
            }
         }

         return true;
      }
   }

   public static MapCodec<ItemStack> lenientOptionalFieldOf(String fieldName) {
      return CODEC.lenientOptionalFieldOf(fieldName)
         .xmap(optional -> optional.orElse(EMPTY), itemStack -> itemStack.isEmpty() ? Optional.empty() : Optional.of(itemStack));
   }

   public static int hashItemAndComponents(@Nullable ItemStack stack) {
      if (stack != null) {
         int i = 31 + stack.getItem().hashCode();
         return 31 * i + stack.getComponents().hashCode();
      } else {
         return 0;
      }
   }

   @Deprecated
   public static int hashStackList(List<ItemStack> list) {
      int i = 0;

      for (ItemStack itemStack : list) {
         i = i * 31 + hashItemAndComponents(itemStack);
      }

      return i;
   }

   @Override
   public String toString() {
      return this.getCount() + " " + this.getItem();
   }

   public void inventoryTick(Level level, Entity entity, @Nullable EquipmentSlot slot) {
      if (this.popTime > 0) {
         this.popTime--;
      }

      if (level instanceof ServerLevel serverLevel) {
         this.getItem().inventoryTick(this, serverLevel, entity, slot);
      }
   }

   public void onCraftedBy(Player player, int amount) {
      player.awardStat(Stats.ITEM_CRAFTED.get(this.getItem()), amount);
      this.getItem().onCraftedBy(this, player);
   }

   public void onCraftedBySystem(Level level) {
      this.getItem().onCraftedPostProcess(this, level);
   }

   public int getUseDuration(LivingEntity entity) {
      return this.getItem().getUseDuration(this, entity);
   }

   public ItemUseAnimation getUseAnimation() {
      return this.getItem().getUseAnimation(this);
   }

   public void releaseUsing(Level level, LivingEntity livingEntity, int timeLeft) {
      ItemStack itemStack = this.copy();
      if (this.getItem().releaseUsing(this, level, livingEntity, timeLeft)) {
         ItemStack itemStack2 = this.applyAfterUseComponentSideEffects(livingEntity, itemStack);
         if (itemStack2 != this) {
            livingEntity.setItemInHand(livingEntity.getUsedItemHand(), itemStack2);
         }
      }
   }

   public void causeUseVibration(Entity entity, Holder.Reference<GameEvent> gameEvent) {
      UseEffects useEffects = this.get(DataComponents.USE_EFFECTS);
      if (useEffects != null && useEffects.interactVibrations()) {
         entity.gameEvent(gameEvent);
      }
   }

   public boolean useOnRelease() {
      return this.getItem().useOnRelease(this);
   }

   @Nullable
   public <T> T set(DataComponentType<T> component, @Nullable T value) {
      return this.components.set(component, value);
   }

   @Nullable
   public <T> T set(TypedDataComponent<T> component) {
      return this.components.set(component);
   }

   public <T> void copyFrom(DataComponentType<T> componentType, DataComponentGetter componentGetter) {
      this.set(componentType, componentGetter.get(componentType));
   }

   @Nullable
   public <T, U> T update(DataComponentType<T> component, T defaultValue, U updateValue, BiFunction<T, U, T> updater) {
      return this.set(component, updater.apply(this.getOrDefault(component, defaultValue), updateValue));
   }

   @Nullable
   public <T> T update(DataComponentType<T> component, T defaultValue, UnaryOperator<T> updater) {
      T object = this.getOrDefault(component, defaultValue);
      return this.set(component, updater.apply(object));
   }

   @Nullable
   public <T> T remove(DataComponentType<? extends T> component) {
      return this.components.remove(component);
   }

   public void applyComponentsAndValidate(DataComponentPatch components) {
      DataComponentPatch dataComponentPatch = this.components.asPatch();
      this.components.applyPatch(components);
      Optional<Error<ItemStack>> optional = validateStrict(this).error();
      if (optional.isPresent()) {
         LOGGER.error("Failed to apply component patch '{}' to item: '{}'", components, optional.get().message());
         this.components.restorePatch(dataComponentPatch);
      }
   }

   public void applyComponents(DataComponentPatch components) {
      this.components.applyPatch(components);
   }

   public void applyComponents(DataComponentMap components) {
      this.components.setAll(components);
   }

   public Component getHoverName() {
      Component component = this.getCustomName();
      return component != null ? component : this.getItemName();
   }

   @Nullable
   public Component getCustomName() {
      Component component = this.get(DataComponents.CUSTOM_NAME);
      if (component != null) {
         return component;
      } else {
         WrittenBookContent writtenBookContent = this.get(DataComponents.WRITTEN_BOOK_CONTENT);
         if (writtenBookContent != null) {
            String string = writtenBookContent.title().raw();
            if (!StringUtil.isBlank(string)) {
               return Component.literal(string);
            }
         }

         return null;
      }
   }

   public Component getItemName() {
      return this.getItem().getName(this);
   }

   public Component getStyledHoverName() {
      MutableComponent mutableComponent = Component.empty().append(this.getHoverName()).withStyle(this.getRarity().color());
      if (this.has(DataComponents.CUSTOM_NAME)) {
         mutableComponent.withStyle(ChatFormatting.ITALIC);
      }

      return mutableComponent;
   }

   public <T extends TooltipProvider> void addToTooltip(
      DataComponentType<T> component, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder, TooltipFlag tooltipFlag
   ) {
      T tooltipProvider = (T)this.get(component);
      if (tooltipProvider != null && tooltipDisplay.shows(component)) {
         tooltipProvider.addToTooltip(context, tooltipAdder, tooltipFlag, this.components);
      }
   }

   public List<Component> getTooltipLines(Item.TooltipContext tooltipContext, @Nullable Player player, TooltipFlag tooltipFlag) {
      TooltipDisplay tooltipDisplay = this.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
      if (!tooltipFlag.isCreative() && tooltipDisplay.hideTooltip()) {
         boolean bl = this.getItem().shouldPrintOpWarning(this, player);
         return bl ? OP_NBT_WARNING : List.of();
      } else {
         List<Component> list = Lists.newArrayList();
         list.add(this.getStyledHoverName());
         this.addDetailsToTooltip(tooltipContext, tooltipDisplay, player, tooltipFlag, list::add);
         return list;
      }
   }

   public void addDetailsToTooltip(
      Item.TooltipContext context, TooltipDisplay tooltipDisplay, @Nullable Player player, TooltipFlag tooltipFlag, Consumer<Component> tooltipAdder
   ) {
      this.getItem().appendHoverText(this, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.TROPICAL_FISH_PATTERN, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.INSTRUMENT, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.MAP_ID, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.BEES, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.CONTAINER_LOOT, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.CONTAINER, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.BANNER_PATTERNS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.POT_DECORATIONS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.WRITTEN_BOOK_CONTENT, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.CHARGED_PROJECTILES, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.FIREWORKS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.FIREWORK_EXPLOSION, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.POTION_CONTENTS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.JUKEBOX_PLAYABLE, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.TRIM, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.STORED_ENCHANTMENTS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.ENCHANTMENTS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.DYED_COLOR, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.PROFILE, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.LORE, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addAttributeTooltips(tooltipAdder, tooltipDisplay, player);
      this.addUnitComponentToTooltip(DataComponents.INTANGIBLE_PROJECTILE, INTANGIBLE_TOOLTIP, tooltipDisplay, tooltipAdder);
      this.addUnitComponentToTooltip(DataComponents.UNBREAKABLE, UNBREAKABLE_TOOLTIP, tooltipDisplay, tooltipAdder);
      this.addToTooltip(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.SUSPICIOUS_STEW_EFFECTS, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.BLOCK_STATE, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      this.addToTooltip(DataComponents.ENTITY_DATA, context, tooltipDisplay, tooltipAdder, tooltipFlag);
      if ((this.is(Items.SPAWNER) || this.is(Items.TRIAL_SPAWNER)) && tooltipDisplay.shows(DataComponents.BLOCK_ENTITY_DATA)) {
         TypedEntityData<BlockEntityType<?>> typedEntityData = this.get(DataComponents.BLOCK_ENTITY_DATA);
         Spawner.appendHoverText(typedEntityData, tooltipAdder, "SpawnData");
      }

      AdventureModePredicate adventureModePredicate = this.get(DataComponents.CAN_BREAK);
      if (adventureModePredicate != null && tooltipDisplay.shows(DataComponents.CAN_BREAK)) {
         tooltipAdder.accept(CommonComponents.EMPTY);
         tooltipAdder.accept(AdventureModePredicate.CAN_BREAK_HEADER);
         adventureModePredicate.addToTooltip(tooltipAdder);
      }

      AdventureModePredicate adventureModePredicate2 = this.get(DataComponents.CAN_PLACE_ON);
      if (adventureModePredicate2 != null && tooltipDisplay.shows(DataComponents.CAN_PLACE_ON)) {
         tooltipAdder.accept(CommonComponents.EMPTY);
         tooltipAdder.accept(AdventureModePredicate.CAN_PLACE_HEADER);
         adventureModePredicate2.addToTooltip(tooltipAdder);
      }

      if (tooltipFlag.isAdvanced()) {
         if (this.isDamaged() && tooltipDisplay.shows(DataComponents.DAMAGE)) {
            tooltipAdder.accept(Component.translatable("item.durability", this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()));
         }

         tooltipAdder.accept(Component.literal(BuiltInRegistries.ITEM.getKey(this.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
         int i = this.components.size();
         if (i > 0) {
            tooltipAdder.accept(Component.translatable("item.components", i).withStyle(ChatFormatting.DARK_GRAY));
         }
      }

      if (player != null && !this.getItem().isEnabled(player.level().enabledFeatures())) {
         tooltipAdder.accept(DISABLED_ITEM_TOOLTIP);
      }

      boolean bl = this.getItem().shouldPrintOpWarning(this, player);
      if (bl) {
         OP_NBT_WARNING.forEach(tooltipAdder);
      }
   }

   private void addUnitComponentToTooltip(DataComponentType<?> component, Component tooltip, TooltipDisplay tooltipDisplay, Consumer<Component> tooltipAdder) {
      if (this.has(component) && tooltipDisplay.shows(component)) {
         tooltipAdder.accept(tooltip);
      }
   }

   private void addAttributeTooltips(Consumer<Component> tooltipAdder, TooltipDisplay tooltipDisplay, @Nullable Player player) {
      if (tooltipDisplay.shows(DataComponents.ATTRIBUTE_MODIFIERS)) {
         for (EquipmentSlotGroup equipmentSlotGroup : EquipmentSlotGroup.values()) {
            MutableBoolean mutableBoolean = new MutableBoolean(true);
            this.forEachModifier(equipmentSlotGroup, (holder, attributeModifier, display) -> {
               if (display != ItemAttributeModifiers.Display.hidden()) {
                  if (mutableBoolean.isTrue()) {
                     tooltipAdder.accept(CommonComponents.EMPTY);
                     tooltipAdder.accept(Component.translatable("item.modifiers." + equipmentSlotGroup.getSerializedName()).withStyle(ChatFormatting.GRAY));
                     mutableBoolean.setFalse();
                  }

                  display.apply(tooltipAdder, player, holder, attributeModifier);
               }
            });
         }
      }
   }

   public boolean hasFoil() {
      Boolean boolean_ = this.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
      return boolean_ != null ? boolean_ : this.getItem().isFoil(this);
   }

   public Rarity getRarity() {
      Rarity rarity = this.getOrDefault(DataComponents.RARITY, Rarity.COMMON);
      if (!this.isEnchanted()) {
         return rarity;
      } else {
         return switch (rarity) {
            case COMMON, UNCOMMON -> Rarity.RARE;
            case RARE -> Rarity.EPIC;
            default -> rarity;
         };
      }
   }

   public boolean isEnchantable() {
      if (!this.has(DataComponents.ENCHANTABLE)) {
         return false;
      } else {
         ItemEnchantments itemEnchantments = this.get(DataComponents.ENCHANTMENTS);
         return itemEnchantments != null && itemEnchantments.isEmpty();
      }
   }

   public void enchant(Holder<Enchantment> enchantment, int level) {
      EnchantmentHelper.updateEnchantments(this, mutable -> mutable.upgrade(enchantment, level));
   }

   public boolean isEnchanted() {
      return !this.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
   }

   public ItemEnchantments getEnchantments() {
      return this.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
   }

   public boolean isFramed() {
      return this.entityRepresentation instanceof ItemFrame;
   }

   public void setEntityRepresentation(@Nullable Entity entity) {
      if (!this.isEmpty()) {
         this.entityRepresentation = entity;
      }
   }

   @Nullable
   public ItemFrame getFrame() {
      return this.entityRepresentation instanceof ItemFrame ? (ItemFrame)this.getEntityRepresentation() : null;
   }

   @Nullable
   public Entity getEntityRepresentation() {
      return !this.isEmpty() ? this.entityRepresentation : null;
   }

   public void forEachModifier(EquipmentSlotGroup slot, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> action) {
      ItemAttributeModifiers itemAttributeModifiers = this.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      itemAttributeModifiers.forEach(slot, action);
      EnchantmentHelper.forEachModifier(
         this, slot, (holder, attributeModifier) -> action.accept(holder, attributeModifier, ItemAttributeModifiers.Display.attributeModifiers())
      );
   }

   public void forEachModifier(EquipmentSlot equipmentSLot, BiConsumer<Holder<Attribute>, AttributeModifier> action) {
      ItemAttributeModifiers itemAttributeModifiers = this.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
      itemAttributeModifiers.forEach(equipmentSLot, action);
      EnchantmentHelper.forEachModifier(this, equipmentSLot, action);
   }

   public Component getDisplayName() {
      MutableComponent mutableComponent = Component.empty().append(this.getHoverName());
      if (this.has(DataComponents.CUSTOM_NAME)) {
         mutableComponent.withStyle(ChatFormatting.ITALIC);
      }

      MutableComponent mutableComponent2 = ComponentUtils.wrapInSquareBrackets(mutableComponent);
      if (!this.isEmpty()) {
         mutableComponent2.withStyle(this.getRarity().color()).withStyle(style -> style.withHoverEvent(new HoverEvent.ShowItem(this)));
      }

      return mutableComponent2;
   }

   public SwingAnimation getSwingAnimation() {
      return this.getOrDefault(DataComponents.SWING_ANIMATION, SwingAnimation.DEFAULT);
   }

   public boolean canPlaceOnBlockInAdventureMode(BlockInWorld block) {
      AdventureModePredicate adventureModePredicate = this.get(DataComponents.CAN_PLACE_ON);
      return adventureModePredicate != null && adventureModePredicate.test(block);
   }

   public boolean canBreakBlockInAdventureMode(BlockInWorld block) {
      AdventureModePredicate adventureModePredicate = this.get(DataComponents.CAN_BREAK);
      return adventureModePredicate != null && adventureModePredicate.test(block);
   }

   public int getPopTime() {
      return this.popTime;
   }

   public void setPopTime(int popTime) {
      this.popTime = popTime;
   }

   public int getCount() {
      return this.isEmpty() ? 0 : this.count;
   }

   public void setCount(int count) {
      this.count = count;
   }

   public void limitSize(int maxSize) {
      if (!this.isEmpty() && this.getCount() > maxSize) {
         this.setCount(maxSize);
      }
   }

   public void grow(int increment) {
      this.setCount(this.getCount() + increment);
   }

   public void shrink(int decrement) {
      this.grow(-decrement);
   }

   public void consume(int amount, @Nullable LivingEntity entity) {
      if (entity == null || !entity.hasInfiniteMaterials()) {
         this.shrink(amount);
      }
   }

   public ItemStack consumeAndReturn(int amount, @Nullable LivingEntity entity) {
      ItemStack itemStack = this.copyWithCount(amount);
      this.consume(amount, entity);
      return itemStack;
   }

   public void onUseTick(Level level, LivingEntity livingEntity, int remainingUseDuration) {
      Consumable consumable = this.get(DataComponents.CONSUMABLE);
      if (consumable != null && consumable.shouldEmitParticlesAndSounds(remainingUseDuration)) {
         consumable.emitParticlesAndSounds(livingEntity.getRandom(), livingEntity, this, 5);
      }

      KineticWeapon kineticWeapon = this.get(DataComponents.KINETIC_WEAPON);
      if (kineticWeapon != null && !level.isClientSide()) {
         kineticWeapon.damageEntities(this, remainingUseDuration, livingEntity, livingEntity.getUsedItemHand().asEquipmentSlot());
      } else {
         this.getItem().onUseTick(level, livingEntity, this, remainingUseDuration);
      }
   }

   public void onDestroyed(ItemEntity itemEntity) {
      this.getItem().onDestroyed(itemEntity);
   }

   public boolean canBeHurtBy(DamageSource damageSource) {
      DamageResistant damageResistant = this.get(DataComponents.DAMAGE_RESISTANT);
      return damageResistant == null || !damageResistant.isResistantTo(damageSource);
   }

   public boolean isValidRepairItem(ItemStack item) {
      Repairable repairable = this.get(DataComponents.REPAIRABLE);
      return repairable != null && repairable.isValidRepairItem(item);
   }

   public boolean canDestroyBlock(BlockState state, Level level, BlockPos pos, Player player) {
      return this.getItem().canDestroyBlock(this, state, level, pos, player);
   }

   public DamageSource getDamageSource(LivingEntity attacker, Supplier<DamageSource> defaultGetter) {
      return Optional.ofNullable(this.get(DataComponents.DAMAGE_TYPE))
         .flatMap(eitherHolder -> eitherHolder.unwrap(attacker.registryAccess()))
         .map(holder -> new DamageSource((Holder<DamageType>)holder, attacker))
         .or(() -> Optional.ofNullable(this.getItem().getItemDamageSource(attacker)))
         .orElseGet(defaultGetter);
   }
}

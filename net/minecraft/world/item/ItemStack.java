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
import java.util.function.Function;
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
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.MutableDataComponentHolder;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IItemStackExtension;
import net.neoforged.neoforge.common.util.AttributeTooltipContext;
import net.neoforged.neoforge.common.util.AttributeUtil;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import org.apache.commons.lang3.function.TriConsumer;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class ItemStack implements DataComponentHolder, MutableDataComponentHolder, IItemStackExtension {
   private static final List<Component> OP_NBT_WARNING = List.of(
      Component.translatable("item.op_warning.line1").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
      Component.translatable("item.op_warning.line2").withStyle(ChatFormatting.RED),
      Component.translatable("item.op_warning.line3").withStyle(ChatFormatting.RED)
   );
   private static final Component UNBREAKABLE_TOOLTIP = Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE);
   private static final Component INTANGIBLE_TOOLTIP = Component.translatable("item.intangible").withStyle(ChatFormatting.GRAY);
   public static final MapCodec<ItemStack> MAP_CODEC = MapCodec.recursive(
      "ItemStack",
      p_393271_ -> RecordCodecBuilder.mapCodec(
         p_381569_ -> p_381569_.group(
               Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
               ExtraCodecs.intRange(1, 99).fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
               DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(p_330103_ -> p_330103_.components.asPatch())
            )
            .apply(p_381569_, ItemStack::new)
      )
   );
   public static final Codec<ItemStack> CODEC = Codec.lazyInitialized(MAP_CODEC::codec);
   public static final Codec<ItemStack> SINGLE_ITEM_CODEC = Codec.lazyInitialized(
      () -> RecordCodecBuilder.create(
         p_381570_ -> p_381570_.group(
               Item.CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
               DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY).forGetter(p_332616_ -> p_332616_.components.asPatch())
            )
            .apply(p_381570_, (p_332614_, p_332615_) -> new ItemStack(p_332614_, 1, p_332615_))
      )
   );
   public static final Codec<ItemStack> STRICT_CODEC = CODEC.validate(ItemStack::validateStrict);
   public static final Codec<ItemStack> STRICT_SINGLE_ITEM_CODEC = SINGLE_ITEM_CODEC.validate(ItemStack::validateStrict);
   public static final Codec<ItemStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
      .xmap(p_330099_ -> p_330099_.orElse(ItemStack.EMPTY), p_330101_ -> p_330101_.isEmpty() ? Optional.empty() : Optional.of(p_330101_));
   public static final Codec<ItemStack> SIMPLE_ITEM_CODEC = Item.CODEC.xmap(ItemStack::new, ItemStack::getItemHolder);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_STREAM_CODEC = createOptionalStreamCodec(DataComponentPatch.STREAM_CODEC);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_UNTRUSTED_STREAM_CODEC = createOptionalStreamCodec(
      DataComponentPatch.DELIMITED_STREAM_CODEC
   );
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> STREAM_CODEC = new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
      public ItemStack decode(RegistryFriendlyByteBuf p_330597_) {
         ItemStack itemstack = ItemStack.OPTIONAL_STREAM_CODEC.decode(p_330597_);
         if (itemstack.isEmpty()) {
            throw new DecoderException("Empty ItemStack not allowed");
         } else {
            return itemstack;
         }
      }

      public void encode(RegistryFriendlyByteBuf p_331762_, ItemStack p_331138_) {
         if (p_331138_.isEmpty()) {
            throw new EncoderException("Empty ItemStack not allowed");
         } else {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(p_331762_, p_331138_);
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

   public static DataResult<ItemStack> validateStrict(ItemStack p_340966_) {
      DataResult<Unit> dataresult = validateComponents(p_340966_.getComponents());
      if (dataresult.isError()) {
         return dataresult.map(p_340777_ -> p_340966_);
      } else {
         return p_340966_.getCount() > p_340966_.getMaxStackSize()
            ? DataResult.error(() -> "Item stack with stack size of " + p_340966_.getCount() + " was larger than maximum: " + p_340966_.getMaxStackSize())
            : DataResult.success(p_340966_);
      }
   }

   private static StreamCodec<RegistryFriendlyByteBuf, ItemStack> createOptionalStreamCodec(
      final StreamCodec<RegistryFriendlyByteBuf, DataComponentPatch> p_412358_
   ) {
      return new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
         public ItemStack decode(RegistryFriendlyByteBuf p_320491_) {
            int i = p_320491_.readVarInt();
            if (i <= 0) {
               return ItemStack.EMPTY;
            } else {
               Holder<Item> holder = Item.STREAM_CODEC.decode(p_320491_);
               DataComponentPatch datacomponentpatch = p_412358_.decode(p_320491_);
               return new ItemStack(holder, i, datacomponentpatch);
            }
         }

         public void encode(RegistryFriendlyByteBuf p_320527_, ItemStack p_320873_) {
            if (p_320873_.isEmpty()) {
               p_320527_.writeVarInt(0);
            } else {
               p_320527_.writeVarInt(p_320873_.getCount());
               Item.STREAM_CODEC.encode(p_320527_, p_320873_.getItemHolder());
               p_412358_.encode(p_320527_, p_320873_.components.asPatch());
            }
         }
      };
   }

   public static StreamCodec<RegistryFriendlyByteBuf, ItemStack> validatedStreamCodec(final StreamCodec<RegistryFriendlyByteBuf, ItemStack> p_340962_) {
      return new StreamCodec<RegistryFriendlyByteBuf, ItemStack>() {
         public ItemStack decode(RegistryFriendlyByteBuf p_341238_) {
            ItemStack itemstack = p_340962_.decode(p_341238_);
            if (!itemstack.isEmpty()) {
               RegistryOps<Unit> registryops = p_341238_.registryAccess().createSerializationContext(NullOps.INSTANCE);
               ItemStack.CODEC.encodeStart(registryops, itemstack).getOrThrow(DecoderException::new);
            }

            return itemstack;
         }

         public void encode(RegistryFriendlyByteBuf p_341112_, ItemStack p_341358_) {
            p_340962_.encode(p_341112_, p_341358_);
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

   public boolean hasNonDefault(DataComponentType<?> p_389426_) {
      return !this.isEmpty() && this.components.hasNonDefault(p_389426_);
   }

   public boolean isComponentsPatchEmpty() {
      return this.isEmpty() || this.components.isPatchEmpty();
   }

   public ItemStack(ItemLike p_41599_) {
      this(p_41599_, 1);
   }

   public ItemStack(Holder<Item> p_204116_) {
      this(p_204116_.value(), 1);
   }

   public ItemStack(Holder<Item> p_312081_, int p_41605_, DataComponentPatch p_330362_) {
      this(p_312081_.value(), p_41605_, PatchedDataComponentMap.fromPatch(p_312081_.value().components(), p_330362_));
   }

   public ItemStack(Holder<Item> p_220155_, int p_220156_) {
      this(p_220155_.value(), p_220156_);
   }

   public ItemStack(ItemLike p_41601_, int p_41602_) {
      this(p_41601_, p_41602_, new PatchedDataComponentMap(p_41601_.asItem().components()));
   }

   private ItemStack(ItemLike p_330978_, int p_330639_, PatchedDataComponentMap p_330546_) {
      this.item = p_330978_.asItem();
      this.count = p_330639_;
      this.components = p_330546_;
   }

   private ItemStack(@Nullable Void p_282703_) {
      this.item = null;
      this.components = new PatchedDataComponentMap(DataComponentMap.EMPTY);
   }

   public static DataResult<Unit> validateComponents(DataComponentMap p_341201_) {
      if (p_341201_.has(DataComponents.MAX_DAMAGE) && p_341201_.getOrDefault(DataComponents.MAX_STACK_SIZE, 1) > 1) {
         return DataResult.error(() -> "Item cannot be both damageable and stackable");
      } else {
         ItemContainerContents itemcontainercontents = p_341201_.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);

         for (ItemStack itemstack : itemcontainercontents.nonEmptyItems()) {
            int i = itemstack.getCount();
            int j = itemstack.getMaxStackSize();
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

   public boolean isItemEnabled(FeatureFlagSet p_250869_) {
      return this.isEmpty() || this.getItem().isEnabled(p_250869_);
   }

   public ItemStack split(int p_41621_) {
      int i = Math.min(p_41621_, this.getCount());
      ItemStack itemstack = this.copyWithCount(i);
      this.shrink(i);
      return itemstack;
   }

   public ItemStack copyAndClear() {
      if (this.isEmpty()) {
         return EMPTY;
      } else {
         ItemStack itemstack = this.copy();
         this.setCount(0);
         return itemstack;
      }
   }

   public Item getItem() {
      return this.isEmpty() ? Items.AIR : this.item;
   }

   public Holder<Item> getItemHolder() {
      return this.getItem().builtInRegistryHolder();
   }

   public boolean is(TagKey<Item> p_204118_) {
      return this.getItem().builtInRegistryHolder().is(p_204118_);
   }

   public boolean is(Item p_150931_) {
      return this.getItem() == p_150931_;
   }

   public boolean is(Predicate<Holder<Item>> p_220168_) {
      return p_220168_.test(this.getItem().builtInRegistryHolder());
   }

   public boolean is(Holder<Item> p_220166_) {
      return this.is(p_220166_.value());
   }

   public boolean is(HolderSet<Item> p_298683_) {
      return p_298683_.contains(this.getItemHolder());
   }

   public Stream<TagKey<Item>> getTags() {
      return this.getItem().builtInRegistryHolder().tags();
   }

   public InteractionResult useOn(UseOnContext p_41662_) {
      UseItemOnBlockEvent e = (UseItemOnBlockEvent)NeoForge.EVENT_BUS.post(new UseItemOnBlockEvent(p_41662_, UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK));
      if (e.isCanceled()) {
         return e.getCancellationResult();
      } else {
         return !p_41662_.getLevel().isClientSide()
            ? CommonHooks.onPlaceItemIntoWorld(p_41662_)
            : this.onItemUse(p_41662_, c -> this.getItem().useOn(p_41662_));
      }
   }

   @Override
   public InteractionResult onItemUseFirst(UseOnContext p_41662_) {
      UseItemOnBlockEvent e = (UseItemOnBlockEvent)NeoForge.EVENT_BUS.post(new UseItemOnBlockEvent(p_41662_, UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK));
      return e.isCanceled() ? e.getCancellationResult() : this.onItemUse(p_41662_, c -> this.getItem().onItemUseFirst(this, p_41662_));
   }

   private InteractionResult onItemUse(UseOnContext p_41662_, Function<UseOnContext, InteractionResult> callback) {
      Player player = p_41662_.getPlayer();
      BlockPos blockpos = p_41662_.getClickedPos();
      if (player != null && !player.getAbilities().mayBuild && !this.canPlaceOnBlockInAdventureMode(new BlockInWorld(p_41662_.getLevel(), blockpos, false))) {
         return InteractionResult.PASS;
      } else {
         Item item = this.getItem();
         InteractionResult interactionresult = callback.apply(p_41662_);
         if (player != null
            && interactionresult instanceof InteractionResult.Success interactionresult$success
            && interactionresult$success.wasItemInteraction()) {
            player.awardStat(Stats.ITEM_USED.get(item));
         }

         return interactionresult;
      }
   }

   public float getDestroySpeed(BlockState p_41692_) {
      return this.getItem().getDestroySpeed(this, p_41692_);
   }

   public InteractionResult use(Level p_41683_, Player p_41684_, InteractionHand p_41685_) {
      ItemStack itemstack = this.copy();
      boolean flag = this.getUseDuration(p_41684_) <= 0;
      InteractionResult interactionresult = this.getItem().use(p_41683_, p_41684_, p_41685_);
      return (InteractionResult)(flag && interactionresult instanceof InteractionResult.Success interactionresult$success
         ? interactionresult$success.heldItemTransformedTo(
            interactionresult$success.heldItemTransformedTo() == null
               ? this.applyAfterUseComponentSideEffects(p_41684_, itemstack)
               : interactionresult$success.heldItemTransformedTo().applyAfterUseComponentSideEffects(p_41684_, itemstack)
         )
         : interactionresult);
   }

   public ItemStack finishUsingItem(Level p_41672_, LivingEntity p_41673_) {
      ItemStack itemstack = this.copy();
      ItemStack itemstack1 = this.getItem().finishUsingItem(this, p_41672_, p_41673_);
      return itemstack1.applyAfterUseComponentSideEffects(p_41673_, itemstack);
   }

   private ItemStack applyAfterUseComponentSideEffects(LivingEntity p_366464_, ItemStack p_366810_) {
      UseRemainder useremainder = p_366810_.get(DataComponents.USE_REMAINDER);
      UseCooldown usecooldown = p_366810_.get(DataComponents.USE_COOLDOWN);
      int i = p_366810_.getCount();
      ItemStack itemstack = this;
      if (useremainder != null) {
         itemstack = useremainder.convertIntoRemainder(this, i, p_366464_.hasInfiniteMaterials(), p_366464_::handleExtraItemsCreatedOnUse);
      }

      if (usecooldown != null) {
         usecooldown.apply(p_366810_, p_366464_);
      }

      return itemstack;
   }

   public int getMaxStackSize() {
      return this.getItem().getMaxStackSize(this);
   }

   public boolean isStackable() {
      return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
   }

   public boolean isDamageableItem() {
      return this.has(DataComponents.MAX_DAMAGE) && !this.has(DataComponents.UNBREAKABLE) && this.has(DataComponents.DAMAGE);
   }

   public boolean isDamaged() {
      return this.isDamageableItem() && this.getItem().isDamaged(this);
   }

   public int getDamageValue() {
      return this.getItem().getDamage(this);
   }

   public void setDamageValue(int p_41722_) {
      this.getItem().setDamage(this, p_41722_);
   }

   public int getMaxDamage() {
      return this.getItem().getMaxDamage(this);
   }

   public boolean isBroken() {
      return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage();
   }

   public boolean nextDamageWillBreak() {
      return this.isDamageableItem() && this.getDamageValue() >= this.getMaxDamage() - 1;
   }

   public void hurtAndBreak(int p_220158_, ServerLevel p_346256_, @Nullable ServerPlayer p_220160_, Consumer<Item> p_348596_) {
      this.hurtAndBreak(p_220158_, p_346256_, (LivingEntity)p_220160_, p_348596_);
   }

   public void hurtAndBreak(int p_220158_, ServerLevel p_346256_, @Nullable LivingEntity p_220160_, Consumer<Item> p_348596_) {
      p_220158_ = this.getItem().damageItem(this, p_220158_, p_220160_, p_348596_);
      int i = this.processDurabilityChange(p_220158_, p_346256_, p_220160_);
      if (i != 0) {
         this.applyDamage(this.getDamageValue() + i, p_220160_, p_348596_);
      }
   }

   private int processDurabilityChange(int p_361290_, ServerLevel p_361409_, @Nullable ServerPlayer p_364940_) {
      return this.processDurabilityChange(p_361290_, p_361409_, (LivingEntity)p_364940_);
   }

   private int processDurabilityChange(int p_361290_, ServerLevel p_361409_, @Nullable LivingEntity p_364940_) {
      if (!this.isDamageableItem()) {
         return 0;
      } else if (p_364940_ != null && p_364940_.hasInfiniteMaterials()) {
         return 0;
      } else {
         return p_361290_ > 0 ? EnchantmentHelper.processDurabilityChange(p_361409_, this, p_361290_) : p_361290_;
      }
   }

   private void applyDamage(int p_361754_, @Nullable ServerPlayer p_364853_, Consumer<Item> p_360895_) {
      this.applyDamage(p_361754_, (LivingEntity)p_364853_, p_360895_);
   }

   private void applyDamage(int p_361754_, @Nullable LivingEntity p_364853_, Consumer<Item> p_360895_) {
      if (p_364853_ instanceof ServerPlayer serverPlayer) {
         CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(serverPlayer, this, p_361754_);
      }

      this.setDamageValue(p_361754_);
      if (this.isBroken()) {
         Item item = this.getItem();
         this.shrink(1);
         p_360895_.accept(item);
      }
   }

   public void hurtWithoutBreaking(int p_362924_, Player p_363410_) {
      if (p_363410_ instanceof ServerPlayer serverplayer) {
         int i = this.processDurabilityChange(p_362924_, serverplayer.level(), serverplayer);
         if (i == 0) {
            return;
         }

         int j = Math.min(this.getDamageValue() + i, this.getMaxDamage() - 1);
         this.applyDamage(j, serverplayer, p_360034_ -> {});
      }
   }

   public void hurtAndBreak(int p_418267_, LivingEntity p_418272_, InteractionHand p_418180_) {
      this.hurtAndBreak(p_418267_, p_418272_, p_418180_.asEquipmentSlot());
   }

   public void hurtAndBreak(int p_41623_, LivingEntity p_41624_, EquipmentSlot p_319898_) {
      if (p_41624_.level() instanceof ServerLevel serverlevel) {
         this.hurtAndBreak(p_41623_, serverlevel, p_41624_, p_348383_ -> p_41624_.onEquippedItemBroken(p_348383_, p_319898_));
      }
   }

   public ItemStack hurtAndConvertOnBreak(int p_352405_, ItemLike p_352337_, LivingEntity p_352175_, EquipmentSlot p_352058_) {
      this.hurtAndBreak(p_352405_, p_352175_, p_352058_);
      if (this.isEmpty()) {
         ItemStack itemstack = this.transmuteCopyIgnoreEmpty(p_352337_, 1);
         if (itemstack.isDamageableItem()) {
            itemstack.setDamageValue(0);
         }

         return itemstack;
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

   public boolean overrideStackedOnOther(Slot p_150927_, ClickAction p_150928_, Player p_150929_) {
      return this.getItem().overrideStackedOnOther(this, p_150927_, p_150928_, p_150929_);
   }

   public boolean overrideOtherStackedOnMe(ItemStack p_150933_, Slot p_150934_, ClickAction p_150935_, Player p_150936_, SlotAccess p_150937_) {
      return this.getItem().overrideOtherStackedOnMe(this, p_150933_, p_150934_, p_150935_, p_150936_, p_150937_);
   }

   public boolean hurtEnemy(LivingEntity p_41641_, LivingEntity p_364087_) {
      Item item = this.getItem();
      item.hurtEnemy(this, p_41641_, p_364087_);
      if (this.has(DataComponents.WEAPON)) {
         if (p_364087_ instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(item));
         }

         return true;
      } else {
         return false;
      }
   }

   public void postHurtEnemy(LivingEntity p_346409_, LivingEntity p_363649_) {
      this.getItem().postHurtEnemy(this, p_346409_, p_363649_);
      Weapon weapon = this.get(DataComponents.WEAPON);
      if (weapon != null) {
         this.hurtAndBreak(weapon.itemDamagePerAttack(), p_363649_, EquipmentSlot.MAINHAND);
      }
   }

   public void mineBlock(Level p_41687_, BlockState p_41688_, BlockPos p_41689_, Player p_41690_) {
      Item item = this.getItem();
      if (item.mineBlock(this, p_41687_, p_41688_, p_41689_, p_41690_)) {
         p_41690_.awardStat(Stats.ITEM_USED.get(item));
      }
   }

   public boolean isCorrectToolForDrops(BlockState p_41736_) {
      return this.getItem().isCorrectToolForDrops(this, p_41736_);
   }

   public InteractionResult interactLivingEntity(Player p_41648_, LivingEntity p_41649_, InteractionHand p_41650_) {
      Equippable equippable = this.get(DataComponents.EQUIPPABLE);
      if (equippable != null && equippable.equipOnInteract()) {
         InteractionResult interactionresult = equippable.equipOnTarget(p_41648_, p_41649_, this);
         if (interactionresult != InteractionResult.PASS) {
            return interactionresult;
         }
      }

      return this.getItem().interactLivingEntity(this, p_41648_, p_41649_, p_41650_);
   }

   public ItemStack copy() {
      if (this.isEmpty()) {
         return EMPTY;
      } else {
         ItemStack itemstack = new ItemStack(this.getItem(), this.count, this.components.copy());
         itemstack.setPopTime(this.getPopTime());
         return itemstack;
      }
   }

   public ItemStack copyWithCount(int p_256354_) {
      if (this.isEmpty()) {
         return EMPTY;
      } else {
         ItemStack itemstack = this.copy();
         itemstack.setCount(p_256354_);
         return itemstack;
      }
   }

   public ItemStack transmuteCopy(ItemLike p_347550_) {
      return this.transmuteCopy(p_347550_, this.getCount());
   }

   public ItemStack transmuteCopy(ItemLike p_323864_, int p_323647_) {
      return this.isEmpty() ? EMPTY : this.transmuteCopyIgnoreEmpty(p_323864_, p_323647_);
   }

   private ItemStack transmuteCopyIgnoreEmpty(ItemLike p_323811_, int p_323856_) {
      return new ItemStack(p_323811_.asItem().builtInRegistryHolder(), p_323856_, this.components.asPatch());
   }

   public static boolean matches(ItemStack p_41729_, ItemStack p_41730_) {
      if (p_41729_ == p_41730_) {
         return true;
      } else {
         return p_41729_.getCount() != p_41730_.getCount() ? false : isSameItemSameComponents(p_41729_, p_41730_);
      }
   }

   @Deprecated
   public static boolean listMatches(List<ItemStack> p_331725_, List<ItemStack> p_331113_) {
      if (p_331725_.size() != p_331113_.size()) {
         return false;
      } else {
         for (int i = 0; i < p_331725_.size(); i++) {
            if (!matches(p_331725_.get(i), p_331113_.get(i))) {
               return false;
            }
         }

         return true;
      }
   }

   public static boolean isSameItem(ItemStack p_287761_, ItemStack p_287676_) {
      return p_287761_.is(p_287676_.getItem());
   }

   public static boolean isSameItemSameComponents(ItemStack p_150943_, ItemStack p_150944_) {
      if (!p_150943_.is(p_150944_.getItem())) {
         return false;
      } else {
         return p_150943_.isEmpty() && p_150944_.isEmpty() ? true : Objects.equals(p_150943_.components, p_150944_.components);
      }
   }

   public static boolean matchesIgnoringComponents(ItemStack p_455588_, ItemStack p_455766_, Predicate<DataComponentType<?>> p_455377_) {
      if (p_455588_ == p_455766_) {
         return true;
      } else if (p_455588_.getCount() != p_455766_.getCount()) {
         return false;
      } else if (!p_455588_.is(p_455766_.getItem())) {
         return false;
      } else if (p_455588_.isEmpty() && p_455766_.isEmpty()) {
         return true;
      } else if (p_455588_.components.size() != p_455766_.components.size()) {
         return false;
      } else {
         for (DataComponentType<?> datacomponenttype : p_455588_.components.keySet()) {
            Object object = p_455588_.components.get(datacomponenttype);
            Object object1 = p_455766_.components.get(datacomponenttype);
            if (object == null || object1 == null) {
               return false;
            }

            if (!Objects.equals(object, object1) && !p_455377_.test(datacomponenttype)) {
               return false;
            }
         }

         return true;
      }
   }

   public static MapCodec<ItemStack> lenientOptionalFieldOf(String p_338501_) {
      return CODEC.lenientOptionalFieldOf(p_338501_)
         .xmap(p_323389_ -> p_323389_.orElse(EMPTY), p_323388_ -> p_323388_.isEmpty() ? Optional.empty() : Optional.of(p_323388_));
   }

   public static int hashItemAndComponents(@Nullable ItemStack p_331961_) {
      if (p_331961_ != null) {
         int i = 31 + p_331961_.getItem().hashCode();
         return 31 * i + p_331961_.getComponents().hashCode();
      } else {
         return 0;
      }
   }

   @Deprecated
   public static int hashStackList(List<ItemStack> p_332135_) {
      int i = 0;

      for (ItemStack itemstack : p_332135_) {
         i = i * 31 + hashItemAndComponents(itemstack);
      }

      return i;
   }

   @Override
   public String toString() {
      return this.getCount() + " " + this.getItem();
   }

   public void inventoryTick(Level p_41667_, Entity p_41668_, @Nullable EquipmentSlot p_401778_) {
      if (this.popTime > 0) {
         this.popTime--;
      }

      if (p_41667_ instanceof ServerLevel serverlevel) {
         this.getItem().inventoryTick(this, serverlevel, p_41668_, p_401778_);
      }
   }

   public void onCraftedBy(Player p_41680_, int p_41681_) {
      p_41680_.awardStat(Stats.ITEM_CRAFTED.get(this.getItem()), p_41681_);
      this.getItem().onCraftedBy(this, p_41680_);
   }

   public void onCraftedBySystem(Level p_307669_) {
      this.getItem().onCraftedPostProcess(this, p_307669_);
   }

   public int getUseDuration(LivingEntity p_345724_) {
      return this.getItem().getUseDuration(this, p_345724_);
   }

   public ItemUseAnimation getUseAnimation() {
      return this.getItem().getUseAnimation(this);
   }

   public void releaseUsing(Level p_41675_, LivingEntity p_41676_, int p_41677_) {
      ItemStack itemstack = this.copy();
      if (this.getItem().releaseUsing(this, p_41675_, p_41676_, p_41677_)) {
         ItemStack itemstack1 = this.applyAfterUseComponentSideEffects(p_41676_, itemstack);
         if (itemstack1 != this) {
            p_41676_.setItemInHand(p_41676_.getUsedItemHand(), itemstack1);
         }
      }
   }

   public void causeUseVibration(Entity p_470645_, Holder.Reference<GameEvent> p_470639_) {
      UseEffects useeffects = this.get(DataComponents.USE_EFFECTS);
      if (useeffects != null && useeffects.interactVibrations()) {
         p_470645_.gameEvent(p_470639_);
      }
   }

   public boolean useOnRelease() {
      return this.getItem().useOnRelease(this);
   }

   @Nullable
   @Override
   public <T> T set(DataComponentType<T> p_331064_, @Nullable T p_330775_) {
      return this.components.set(p_331064_, p_330775_);
   }

   @Nullable
   public <T> T set(TypedDataComponent<T> p_451140_) {
      return this.components.set(p_451140_);
   }

   @Override
   public <T> void copyFrom(DataComponentType<T> p_397950_, DataComponentGetter p_397631_) {
      this.set(p_397950_, p_397631_.get(p_397950_));
   }

   @Nullable
   @Override
   public <T, U> T update(DataComponentType<T> p_331083_, T p_331443_, U p_331049_, BiFunction<T, U, T> p_331846_) {
      return this.set(p_331083_, p_331846_.apply(this.getOrDefault(p_331083_, p_331443_), p_331049_));
   }

   @Nullable
   @Override
   public <T> T update(DataComponentType<T> p_330921_, T p_331257_, UnaryOperator<T> p_331701_) {
      T t = this.getOrDefault(p_330921_, p_331257_);
      return this.set(p_330921_, p_331701_.apply(t));
   }

   @Nullable
   @Override
   public <T> T remove(DataComponentType<? extends T> p_332139_) {
      return this.components.remove(p_332139_);
   }

   public void applyComponentsAndValidate(DataComponentPatch p_341407_) {
      DataComponentPatch datacomponentpatch = this.components.asPatch();
      this.components.applyPatch(p_341407_);
      Optional<Error<ItemStack>> optional = validateStrict(this).error();
      if (optional.isPresent()) {
         LOGGER.error("Failed to apply component patch '{}' to item: '{}'", p_341407_, optional.get().message());
         this.components.restorePatch(datacomponentpatch);
      }
   }

   @Override
   public void applyComponents(DataComponentPatch p_332097_) {
      this.components.applyPatch(p_332097_);
   }

   @Override
   public void applyComponents(DataComponentMap p_330402_) {
      this.components.setAll(p_330402_);
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
         WrittenBookContent writtenbookcontent = this.get(DataComponents.WRITTEN_BOOK_CONTENT);
         if (writtenbookcontent != null) {
            String s = writtenbookcontent.title().raw();
            if (!StringUtil.isBlank(s)) {
               return Component.literal(s);
            }
         }

         return null;
      }
   }

   public Component getItemName() {
      return this.getItem().getName(this);
   }

   public Component getStyledHoverName() {
      MutableComponent mutablecomponent = Component.empty().append(this.getHoverName()).withStyle(this.getRarity().getStyleModifier());
      if (this.has(DataComponents.CUSTOM_NAME)) {
         mutablecomponent.withStyle(ChatFormatting.ITALIC);
      }

      return mutablecomponent;
   }

   @Override
   public <T extends TooltipProvider> void addToTooltip(
      DataComponentType<T> p_331344_, Item.TooltipContext p_341231_, TooltipDisplay p_399720_, Consumer<Component> p_331885_, TooltipFlag p_331177_
   ) {
      T t = (T)this.get(p_331344_);
      if (t != null && p_399720_.shows(p_331344_)) {
         t.addToTooltip(p_341231_, p_331885_, p_331177_, this.components);
      }
   }

   public List<Component> getTooltipLines(Item.TooltipContext p_339637_, @Nullable Player p_41652_, TooltipFlag p_41653_) {
      TooltipDisplay tooltipdisplay = this.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
      if (!p_41653_.isCreative() && tooltipdisplay.hideTooltip()) {
         boolean flag = this.getItem().shouldPrintOpWarning(this, p_41652_);
         return flag ? OP_NBT_WARNING : List.of();
      } else {
         List<Component> list = Lists.newArrayList();
         list.add(this.getStyledHoverName());
         this.addDetailsToTooltip(p_339637_, tooltipdisplay, p_41652_, p_41653_, list::add);
         EventHooks.onItemTooltip(this, p_41652_, list, p_41653_, p_339637_);
         return list;
      }
   }

   public void addDetailsToTooltip(
      Item.TooltipContext p_400057_, TooltipDisplay p_400252_, @Nullable Player p_399628_, TooltipFlag p_400174_, Consumer<Component> p_399603_
   ) {
      this.getItem().appendHoverText(this, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.TROPICAL_FISH_PATTERN, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.INSTRUMENT, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.MAP_ID, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.BEES, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.CONTAINER_LOOT, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.CONTAINER, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.BANNER_PATTERNS, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.POT_DECORATIONS, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.WRITTEN_BOOK_CONTENT, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.CHARGED_PROJECTILES, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.FIREWORKS, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.FIREWORK_EXPLOSION, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.POTION_CONTENTS, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.JUKEBOX_PLAYABLE, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.TRIM, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.STORED_ENCHANTMENTS, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.ENCHANTMENTS, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.DYED_COLOR, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.PROFILE, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.LORE, p_400057_, p_400252_, p_399603_, p_400174_);
      AttributeUtil.addAttributeTooltips(this, p_399603_, p_400252_, AttributeTooltipContext.of(p_399628_, p_400057_, p_400252_, p_400174_));
      this.addUnitComponentToTooltip(DataComponents.INTANGIBLE_PROJECTILE, INTANGIBLE_TOOLTIP, p_400252_, p_399603_);
      this.addUnitComponentToTooltip(DataComponents.UNBREAKABLE, UNBREAKABLE_TOOLTIP, p_400252_, p_399603_);
      this.addToTooltip(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.SUSPICIOUS_STEW_EFFECTS, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.BLOCK_STATE, p_400057_, p_400252_, p_399603_, p_400174_);
      this.addToTooltip(DataComponents.ENTITY_DATA, p_400057_, p_400252_, p_399603_, p_400174_);
      if ((this.is(Items.SPAWNER) || this.is(Items.TRIAL_SPAWNER)) && p_400252_.shows(DataComponents.BLOCK_ENTITY_DATA)) {
         TypedEntityData<BlockEntityType<?>> typedentitydata = this.get(DataComponents.BLOCK_ENTITY_DATA);
         Spawner.appendHoverText(typedentitydata, p_399603_, "SpawnData");
      }

      AdventureModePredicate adventuremodepredicate1 = this.get(DataComponents.CAN_BREAK);
      if (adventuremodepredicate1 != null && p_400252_.shows(DataComponents.CAN_BREAK)) {
         p_399603_.accept(CommonComponents.EMPTY);
         p_399603_.accept(AdventureModePredicate.CAN_BREAK_HEADER);
         adventuremodepredicate1.addToTooltip(p_399603_);
      }

      AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_PLACE_ON);
      if (adventuremodepredicate != null && p_400252_.shows(DataComponents.CAN_PLACE_ON)) {
         p_399603_.accept(CommonComponents.EMPTY);
         p_399603_.accept(AdventureModePredicate.CAN_PLACE_HEADER);
         adventuremodepredicate.addToTooltip(p_399603_);
      }

      if (p_400174_.isAdvanced()) {
         if (this.isDamaged() && p_400252_.shows(DataComponents.DAMAGE)) {
            p_399603_.accept(Component.translatable("item.durability", this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()));
         }

         p_399603_.accept(Component.literal(BuiltInRegistries.ITEM.getKey(this.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
         int i = this.components.size();
         if (i > 0) {
            p_399603_.accept(Component.translatable("item.components", i).withStyle(ChatFormatting.DARK_GRAY));
         }
      }

      if (p_399628_ != null && !this.getItem().isEnabled(p_399628_.level().enabledFeatures())) {
         p_399603_.accept(DISABLED_ITEM_TOOLTIP);
      }

      boolean flag = this.getItem().shouldPrintOpWarning(this, p_399628_);
      if (flag) {
         OP_NBT_WARNING.forEach(p_399603_);
      }
   }

   private void addUnitComponentToTooltip(DataComponentType<?> p_454755_, Component p_455107_, TooltipDisplay p_456114_, Consumer<Component> p_455248_) {
      if (this.has(p_454755_) && p_456114_.shows(p_454755_)) {
         p_455248_.accept(p_455107_);
      }
   }

   @Deprecated
   private void addAttributeTooltips(Consumer<Component> p_330796_, TooltipDisplay p_400175_, @Nullable Player p_330530_) {
      if (p_400175_.shows(DataComponents.ATTRIBUTE_MODIFIERS)) {
         for (EquipmentSlotGroup equipmentslotgroup : EquipmentSlotGroup.values()) {
            MutableBoolean mutableboolean = new MutableBoolean(true);
            this.forEachModifier(equipmentslotgroup, (p_415402_, p_415403_, p_415404_) -> {
               if (p_415404_ != ItemAttributeModifiers.Display.hidden()) {
                  if (mutableboolean.isTrue()) {
                     p_330796_.accept(CommonComponents.EMPTY);
                     p_330796_.accept(Component.translatable("item.modifiers." + equipmentslotgroup.getSerializedName()).withStyle(ChatFormatting.GRAY));
                     mutableboolean.setFalse();
                  }

                  p_415404_.apply(p_330796_, p_330530_, p_415402_, p_415403_);
               }
            });
         }
      }
   }

   public boolean hasFoil() {
      Boolean obool = this.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
      return obool != null ? obool : this.getItem().isFoil(this);
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
         ItemEnchantments itemenchantments = this.get(DataComponents.ENCHANTMENTS);
         return itemenchantments != null && itemenchantments.isEmpty();
      }
   }

   public void enchant(Holder<Enchantment> p_344751_, int p_41665_) {
      EnchantmentHelper.updateEnchantments(this, p_344404_ -> p_344404_.upgrade(p_344751_, p_41665_));
   }

   public boolean isEnchanted() {
      return !this.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
   }

   public ItemEnchantments getTagEnchantments() {
      return this.getEnchantments();
   }

   @Deprecated
   public ItemEnchantments getEnchantments() {
      return this.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
   }

   public boolean isFramed() {
      return this.entityRepresentation instanceof ItemFrame;
   }

   public void setEntityRepresentation(@Nullable Entity p_41637_) {
      if (!this.isEmpty()) {
         this.entityRepresentation = p_41637_;
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

   public void forEachModifier(EquipmentSlotGroup p_348610_, TriConsumer<Holder<Attribute>, AttributeModifier, ItemAttributeModifiers.Display> p_415655_) {
      this.getAttributeModifiers().forEach(p_348610_, p_415655_);
      EnchantmentHelper.forEachModifier(
         this, p_348610_, (p_415406_, p_415407_) -> p_415655_.accept(p_415406_, p_415407_, ItemAttributeModifiers.Display.attributeModifiers())
      );
   }

   public void forEachModifier(EquipmentSlot p_332001_, BiConsumer<Holder<Attribute>, AttributeModifier> p_330882_) {
      this.getAttributeModifiers().forEach(p_332001_, p_330882_);
      EnchantmentHelper.forEachModifier(this, p_332001_, p_330882_);
   }

   public Component getDisplayName() {
      MutableComponent mutablecomponent = Component.empty().append(this.getHoverName());
      if (this.has(DataComponents.CUSTOM_NAME)) {
         mutablecomponent.withStyle(ChatFormatting.ITALIC);
      }

      MutableComponent mutablecomponent1 = ComponentUtils.wrapInSquareBrackets(mutablecomponent);
      if (!this.isEmpty()) {
         mutablecomponent1.withStyle(this.getRarity().getStyleModifier()).withStyle(p_393272_ -> p_393272_.withHoverEvent(new HoverEvent.ShowItem(this)));
      }

      return mutablecomponent1;
   }

   public SwingAnimation getSwingAnimation() {
      return this.getOrDefault(DataComponents.SWING_ANIMATION, SwingAnimation.DEFAULT);
   }

   public boolean canPlaceOnBlockInAdventureMode(BlockInWorld p_331419_) {
      AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_PLACE_ON);
      return adventuremodepredicate != null && adventuremodepredicate.test(p_331419_);
   }

   public boolean canBreakBlockInAdventureMode(BlockInWorld p_331592_) {
      AdventureModePredicate adventuremodepredicate = this.get(DataComponents.CAN_BREAK);
      return adventuremodepredicate != null && adventuremodepredicate.test(p_331592_);
   }

   public int getPopTime() {
      return this.popTime;
   }

   public void setPopTime(int p_41755_) {
      this.popTime = p_41755_;
   }

   public int getCount() {
      return this.isEmpty() ? 0 : this.count;
   }

   public void setCount(int p_41765_) {
      this.count = p_41765_;
   }

   public void limitSize(int p_335437_) {
      if (!this.isEmpty() && this.getCount() > p_335437_) {
         this.setCount(p_335437_);
      }
   }

   public void grow(int p_41770_) {
      this.setCount(this.getCount() + p_41770_);
   }

   public void shrink(int p_41775_) {
      this.grow(-p_41775_);
   }

   public void consume(int p_326311_, @Nullable LivingEntity p_326200_) {
      if (p_326200_ == null || !p_326200_.hasInfiniteMaterials()) {
         this.shrink(p_326311_);
      }
   }

   public ItemStack consumeAndReturn(int p_347624_, @Nullable LivingEntity p_347710_) {
      ItemStack itemstack = this.copyWithCount(p_347624_);
      this.consume(p_347624_, p_347710_);
      return itemstack;
   }

   public void onUseTick(Level p_41732_, LivingEntity p_41733_, int p_41734_) {
      Consumable consumable = this.get(DataComponents.CONSUMABLE);
      if (consumable != null && consumable.shouldEmitParticlesAndSounds(p_41734_)) {
         consumable.emitParticlesAndSounds(p_41733_.getRandom(), p_41733_, this, 5);
      }

      KineticWeapon kineticweapon = this.get(DataComponents.KINETIC_WEAPON);
      if (kineticweapon != null && !p_41732_.isClientSide()) {
         kineticweapon.damageEntities(this, p_41734_, p_41733_, p_41733_.getUsedItemHand().asEquipmentSlot());
      } else {
         this.getItem().onUseTick(p_41732_, p_41733_, this, p_41734_);
      }
   }

   @Deprecated
   public void onDestroyed(ItemEntity p_150925_) {
      this.getItem().onDestroyed(p_150925_);
   }

   public boolean canBeHurtBy(DamageSource p_335431_) {
      if (!this.getItem().canBeHurtBy(this, p_335431_)) {
         return false;
      } else {
         DamageResistant damageresistant = this.get(DataComponents.DAMAGE_RESISTANT);
         return damageresistant == null || !damageresistant.isResistantTo(p_335431_);
      }
   }

   public boolean isValidRepairItem(ItemStack p_362836_) {
      Repairable repairable = this.get(DataComponents.REPAIRABLE);
      return repairable != null && repairable.isValidRepairItem(p_362836_);
   }

   public boolean canDestroyBlock(BlockState p_394270_, Level p_393831_, BlockPos p_394075_, Player p_393908_) {
      return this.getItem().canDestroyBlock(this, p_394270_, p_393831_, p_394075_, p_393908_);
   }

   public DamageSource getDamageSource(LivingEntity p_455284_, Supplier<DamageSource> p_455387_) {
      return Optional.ofNullable(this.get(DataComponents.DAMAGE_TYPE))
         .flatMap(p_454609_ -> p_454609_.unwrap(p_455284_.registryAccess()))
         .map(p_454612_ -> new DamageSource((Holder<DamageType>)p_454612_, p_455284_))
         .or(() -> Optional.ofNullable(this.getItem().getItemDamageSource(p_455284_)))
         .orElseGet(p_455387_);
   }
}

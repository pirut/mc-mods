package net.neoforged.neoforge.common;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class DeferredSpawnEggItem extends SpawnEggItem {
   private static final List<DeferredSpawnEggItem> MOD_EGGS = new ArrayList<>();
   private static final Map<EntityType<? extends Mob>, DeferredSpawnEggItem> TYPE_MAP = new IdentityHashMap<>();
   private final Supplier<? extends EntityType<? extends Mob>> typeSupplier;
   private static final DispenseItemBehavior DEFAULT_DISPENSE_BEHAVIOR = (source, stack) -> {
      Direction face = source.state().getValue(DispenserBlock.FACING);
      EntityType<?> type = ((SpawnEggItem)stack.getItem()).getType(stack);

      try {
         type.spawn(source.level(), stack, null, source.pos().relative(face), MobSpawnType.DISPENSER, face != Direction.UP, false);
      } catch (Exception var5) {
         DispenseItemBehavior.LOGGER.error("Error while dispensing spawn egg from dispenser at {}", source.pos(), var5);
         return ItemStack.EMPTY;
      }

      stack.shrink(1);
      source.level().gameEvent(GameEvent.ENTITY_PLACE, source.pos(), GameEvent.Context.of(source.state()));
      return stack;
   };

   public DeferredSpawnEggItem(Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor, Item.Properties props) {
      super((EntityType<? extends Mob>)null, backgroundColor, highlightColor, props);
      this.typeSupplier = type;
      MOD_EGGS.add(this);
   }

   @Nullable
   protected DispenseItemBehavior createDispenseBehavior() {
      return DEFAULT_DISPENSE_BEHAVIOR;
   }

   @Internal
   @Nullable
   public static SpawnEggItem deferredOnlyById(@Nullable EntityType<?> type) {
      return TYPE_MAP.get(type);
   }

   @Override
   protected EntityType<?> getDefaultType() {
      return (EntityType<?>)this.typeSupplier.get();
   }

   @EventBusSubscriber(
      value = {Dist.CLIENT},
      modid = "neoforge"
   )
   private static class ColorRegisterHandler {
      @SubscribeEvent(
         priority = EventPriority.HIGHEST
      )
      public static void registerSpawnEggColors(RegisterColorHandlersEvent.Item event) {
         DeferredSpawnEggItem.MOD_EGGS.forEach(egg -> event.register((stack, layer) -> FastColor.ARGB32.opaque(egg.getColor(layer)), egg));
      }
   }

   @EventBusSubscriber(
      modid = "neoforge"
   )
   private static class CommonHandler {
      @SubscribeEvent
      public static void onCommonSetup(FMLCommonSetupEvent event) {
         event.enqueueWork(() -> DeferredSpawnEggItem.MOD_EGGS.forEach(egg -> {
            DispenseItemBehavior dispenseBehavior = egg.createDispenseBehavior();
            if (dispenseBehavior != null) {
               DispenserBlock.registerBehavior(egg, dispenseBehavior);
            }

            DeferredSpawnEggItem.TYPE_MAP.put((EntityType<? extends Mob>)egg.typeSupplier.get(), egg);
         }));
      }
   }
}

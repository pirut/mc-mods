package net.neoforged.neoforge.client;

import com.mojang.datafixers.util.Pair;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterDimensionTransitionScreenEvent;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public class DimensionTransitionScreenManager {
   private static final Map<Pair<ResourceKey<Level>, ResourceKey<Level>>, DimensionTransitionScreenManager.ReceivingLevelScreenFactory> conditionalDimensionEffects = new HashMap<>();
   private static final Map<ResourceKey<Level>, DimensionTransitionScreenManager.ReceivingLevelScreenFactory> toDimensionTransitions = new HashMap<>();
   private static final Map<ResourceKey<Level>, DimensionTransitionScreenManager.ReceivingLevelScreenFactory> fromDimensionTransitions = new HashMap<>();

   @Internal
   static void init() {
      ModLoader.postEventWrapContainerInModOrder(
         new RegisterDimensionTransitionScreenEvent(conditionalDimensionEffects, toDimensionTransitions, fromDimensionTransitions)
      );
   }

   public static DimensionTransitionScreenManager.ReceivingLevelScreenFactory getScreenFromLevel(@Nullable Level target, @Nullable Level source) {
      if (source == null) {
         return getScreen(null, null);
      } else {
         return target == null ? getScreen(null, source.dimension()) : getScreen(target.dimension(), source.dimension());
      }
   }

   public static DimensionTransitionScreenManager.ReceivingLevelScreenFactory getScreen(
      @Nullable ResourceKey<Level> toDimension, @Nullable ResourceKey<Level> fromDimension
   ) {
      DimensionTransitionScreenManager.ReceivingLevelScreenFactory conditionalScreen = conditionalDimensionEffects.get(Pair.of(toDimension, fromDimension));
      if (conditionalScreen != null) {
         return conditionalScreen;
      } else {
         DimensionTransitionScreenManager.ReceivingLevelScreenFactory toDim = toDimensionTransitions.get(toDimension);
         if (toDim != null) {
            return toDim;
         } else {
            DimensionTransitionScreenManager.ReceivingLevelScreenFactory fromDim = fromDimensionTransitions.get(fromDimension);
            return fromDim != null ? fromDim : ReceivingLevelScreen::new;
         }
      }
   }

   public interface ReceivingLevelScreenFactory {
      ReceivingLevelScreen create(BooleanSupplier var1, ReceivingLevelScreen.Reason var2);
   }
}

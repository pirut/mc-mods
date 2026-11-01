package net.neoforged.neoforge.resource;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.ServerFunctionLibrary;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.util.VanillaClassToKey;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public class VanillaServerListeners {
   private static final Map<Class<?>, Identifier> KNOWN_CLASSES = new LinkedHashMap<>();
   public static final Identifier RECIPES = key(RecipeManager.class);
   public static final Identifier FUNCTIONS = key(ServerFunctionLibrary.class);
   public static final Identifier ADVANCEMENTS = key(ServerAdvancementManager.class);
   public static final Identifier FIRST = RECIPES;
   public static final Identifier LAST = ADVANCEMENTS;

   private static Identifier key(Class<? extends PreparableReloadListener> cls) {
      if (KNOWN_CLASSES.containsKey(cls)) {
         throw new UnsupportedOperationException("Attempted to create two keys for the same class");
      } else {
         Identifier key = VanillaClassToKey.convert(cls);
         KNOWN_CLASSES.put(cls, key);
         return key;
      }
   }

   @Internal
   @Nullable
   public static Identifier getNameForClass(Class<? extends PreparableReloadListener> cls) {
      return KNOWN_CLASSES.get(cls);
   }
}

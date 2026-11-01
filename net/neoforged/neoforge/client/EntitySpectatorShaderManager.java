package net.neoforged.neoforge.client;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.event.RegisterEntitySpectatorShadersEvent;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jspecify.annotations.Nullable;

public final class EntitySpectatorShaderManager {
   private static Map<EntityType<?>, Identifier> SHADERS;

   @Nullable
   public static Identifier get(EntityType<?> entityType) {
      return SHADERS.get(entityType);
   }

   @Internal
   public static void init() {
      HashMap<EntityType<?>, Identifier> shaders = new HashMap<>();
      RegisterEntitySpectatorShadersEvent event = new RegisterEntitySpectatorShadersEvent(shaders);
      ModLoader.postEventWrapContainerInModOrder(event);
      SHADERS = ImmutableMap.copyOf(shaders);
   }

   private EntitySpectatorShaderManager() {
   }
}

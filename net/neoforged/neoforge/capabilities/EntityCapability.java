package net.neoforged.neoforge.capabilities;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

public final class EntityCapability<T, C> extends BaseCapability<T, C> {
   private static final CapabilityRegistry<EntityCapability<?, ?>> registry = new CapabilityRegistry<>(EntityCapability::new);
   final Map<EntityType<?>, List<ICapabilityProvider<Entity, C, T>>> providers = new IdentityHashMap<>();

   public static <T, C> EntityCapability<T, C> create(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
      return (EntityCapability<T, C>)registry.create(name, typeClass, contextClass);
   }

   public static <T> EntityCapability<T, Void> createVoid(ResourceLocation name, Class<T> typeClass) {
      return create(name, typeClass, void.class);
   }

   public static <T> EntityCapability<T, Direction> createSided(ResourceLocation name, Class<T> typeClass) {
      return create(name, typeClass, Direction.class);
   }

   public static synchronized List<EntityCapability<?, ?>> getAll() {
      return registry.getAll();
   }

   private EntityCapability(ResourceLocation name, Class<T> typeClass, Class<C> contextClass) {
      super(name, typeClass, contextClass);
   }

   @Internal
   @Nullable
   public T getCapability(Entity entity, C context) {
      for (ICapabilityProvider<Entity, C, T> provider : this.providers.getOrDefault(entity.getType(), List.of())) {
         T ret = provider.getCapability(entity, context);
         if (ret != null) {
            return ret;
         }
      }

      return null;
   }
}

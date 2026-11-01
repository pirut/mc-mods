package net.neoforged.neoforge.client;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.particle.FireworkParticles;
import net.minecraft.world.item.component.FireworkExplosion;
import org.jspecify.annotations.Nullable;

public class FireworkShapeFactoryRegistry {
   private static final Map<FireworkExplosion.Shape, FireworkShapeFactoryRegistry.Factory> factories = new HashMap<>();

   public static void register(FireworkExplosion.Shape shape, FireworkShapeFactoryRegistry.Factory factory) {
      factories.put(shape, factory);
   }

   @Nullable
   public static FireworkShapeFactoryRegistry.Factory get(FireworkExplosion.Shape shape) {
      return factories.get(shape);
   }

   public interface Factory {
      void build(FireworkParticles.Starter var1, boolean var2, boolean var3, int[] var4, int[] var5);
   }
}

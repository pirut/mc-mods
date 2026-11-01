package net.neoforged.neoforge.common;

import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemAbility {
   private static final Map<String, ItemAbility> actions = new ConcurrentHashMap<>();
   public static Codec<ItemAbility> CODEC = Codec.STRING.xmap(ItemAbility::get, ItemAbility::name);
   private final String name;

   public static Collection<ItemAbility> getActions() {
      return Collections.unmodifiableCollection(actions.values());
   }

   public static ItemAbility get(String name) {
      return actions.computeIfAbsent(name, ItemAbility::new);
   }

   public String name() {
      return this.name;
   }

   @Override
   public String toString() {
      return "ItemAbility[" + this.name + "]";
   }

   private ItemAbility(String name) {
      this.name = name;
   }
}

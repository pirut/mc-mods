package net.neoforged.neoforge.client.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.ApiStatus.Internal;

public class RegisterKeyMappingsEvent extends Event implements IModBusEvent {
   private final Options options;
   private final Map<Identifier, KeyMapping.Category> moddedCategories = new HashMap<>();

   @Internal
   public RegisterKeyMappingsEvent(Options options) {
      this.options = options;
   }

   public void register(KeyMapping key) {
      this.options.keyMappings = (KeyMapping[])ArrayUtils.add(this.options.keyMappings, key);
   }

   public void registerCategory(KeyMapping.Category category) {
      if (this.moddedCategories.putIfAbsent(category.id(), category) != null) {
         throw new IllegalArgumentException(String.format(Locale.ROOT, "KeyMapping.Category '%s' is already registered.", category.id()));
      }
   }

   @Internal
   public void sortAndStoreCategories(List<KeyMapping.Category> categories) {
      List<KeyMapping.Category> custom = new ArrayList<>(this.moddedCategories.values());
      custom.sort((c1, c2) -> c1.id().compareNamespaced(c2.id()));
      categories.addAll(custom);
   }
}

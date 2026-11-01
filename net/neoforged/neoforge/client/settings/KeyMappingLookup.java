package net.neoforged.neoforge.client.settings;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Util;

public class KeyMappingLookup {
   private final EnumMap<KeyModifier, Map<InputConstants.Key, Collection<KeyMapping>>> map = Util.make(new EnumMap<>(KeyModifier.class), map -> {
      for (KeyModifier modifier : KeyModifier.values()) {
         map.put(modifier, Maps.newConcurrentMap());
      }
   });

   public List<KeyMapping> getAll(InputConstants.Key keyCode) {
      return this.getAll(keyCode, false);
   }

   public List<KeyMapping> getAll(InputConstants.Key keyCode, boolean releasing) {
      List<KeyMapping> matchingBindings = new ArrayList<>();
      List<KeyModifier> activeModifiers = KeyModifier.getActiveModifiers();
      KeyModifier keyCodeModifier = KeyModifier.getKeyModifier(keyCode);

      for (KeyModifier modifier : activeModifiers) {
         if (modifier.matches(keyCode)) {
            for (KeyModifier otherModifier : activeModifiers) {
               if (otherModifier != keyCodeModifier) {
                  for (InputConstants.Key otherModifierCode : otherModifier.codes()) {
                     if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), otherModifierCode.getValue())) {
                        matchingBindings.addAll(this.findKeybinds(otherModifierCode, modifier));
                     }
                  }
               }
            }
         } else {
            matchingBindings.addAll(this.findKeybinds(keyCode, modifier));
         }
      }

      if (releasing && keyCodeModifier != KeyModifier.NONE) {
         matchingBindings.addAll(
            this.map
               .get(keyCodeModifier)
               .entrySet()
               .stream()
               .filter(entry -> entry.getKey() != InputConstants.UNKNOWN)
               .flatMap(entry -> entry.getValue().stream())
               .filter(mapping -> mapping.getKeyConflictContext().isActive())
               .toList()
         );
      }

      if (releasing || matchingBindings.isEmpty()) {
         matchingBindings.addAll(this.findKeybinds(keyCode, KeyModifier.NONE));
      }

      return matchingBindings;
   }

   private List<KeyMapping> findKeybinds(InputConstants.Key keyCode, KeyModifier modifier) {
      Collection<KeyMapping> modifierBindings = this.map.get(modifier).get(keyCode);
      return modifierBindings != null ? modifierBindings.stream().filter(binding -> binding.isActiveAndMatches(keyCode)).toList() : List.of();
   }

   public void put(InputConstants.Key keyCode, KeyMapping keyBinding) {
      KeyModifier keyModifier = keyBinding.getKeyModifier();
      Map<InputConstants.Key, Collection<KeyMapping>> bindingsMap = this.map.get(keyModifier);
      Collection<KeyMapping> bindingsForKey = bindingsMap.computeIfAbsent(keyCode, k -> Lists.newCopyOnWriteArrayList());
      bindingsForKey.add(keyBinding);
   }

   public void remove(KeyMapping keyBinding) {
      KeyModifier keyModifier = keyBinding.getKeyModifier();
      InputConstants.Key keyCode = keyBinding.getKey();
      Map<InputConstants.Key, Collection<KeyMapping>> bindingsMap = this.map.get(keyModifier);
      Collection<KeyMapping> bindingsForKey = bindingsMap.get(keyCode);
      if (bindingsForKey != null) {
         bindingsForKey.remove(keyBinding);
         if (bindingsForKey.isEmpty()) {
            bindingsMap.remove(keyCode);
         }
      }
   }

   public void clear() {
      for (Map<InputConstants.Key, Collection<KeyMapping>> bindings : this.map.values()) {
         bindings.clear();
      }
   }
}

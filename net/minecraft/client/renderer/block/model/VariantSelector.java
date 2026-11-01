package net.minecraft.client.renderer.block.model;

import com.google.common.base.Splitter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class VariantSelector {
   private static final Splitter COMMA_SPLITTER = Splitter.on(',');
   private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2);

   public static <O, S extends StateHolder<O, S>> Predicate<StateHolder<O, S>> predicate(StateDefinition<O, S> p_363406_, String p_363089_) {
      Map<Property<?>, Comparable<?>> map = new HashMap<>();

      for (String s : COMMA_SPLITTER.split(p_363089_)) {
         Iterator<String> iterator = EQUAL_SPLITTER.split(s).iterator();
         if (iterator.hasNext()) {
            String s1 = iterator.next();
            Property<?> property = p_363406_.getProperty(s1);
            if (property != null && iterator.hasNext()) {
               String s2 = iterator.next();
               Comparable<?> comparable = getValueHelper((Property<Comparable<?>>)property, s2);
               if (comparable == null) {
                  throw new RuntimeException("Unknown value: '" + s2 + "' for blockstate property: '" + s1 + "' " + property.getPossibleValues());
               }

               map.put(property, comparable);
            } else if (!s1.isEmpty()) {
               throw new RuntimeException("Unknown blockstate property: '" + s1 + "'");
            }
         }
      }

      return p_363712_ -> {
         for (Entry<Property<?>, Comparable<?>> entry : map.entrySet()) {
            if (!Objects.equals(p_363712_.getValue(entry.getKey()), entry.getValue())) {
               return false;
            }
         }

         return true;
      };
   }

   @Nullable
   private static <T extends Comparable<T>> T getValueHelper(Property<T> p_362078_, String p_363669_) {
      return p_362078_.getValue(p_363669_).orElse(null);
   }
}

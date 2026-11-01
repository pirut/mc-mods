package net.neoforged.neoforge.common.util;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.spongepowered.include.com.google.common.base.Preconditions;

@Internal
public class VanillaClassToKey {
   public static Identifier convert(Class<?> cls) {
      Preconditions.checkArgument(
         cls.getPackageName().startsWith("net.minecraft"), "Automatic name conversion can only be applied to net.minecraft classes. Provided: " + cls.getName()
      );
      Preconditions.checkArgument(
         !cls.getSimpleName().isEmpty(),
         "Automatic name conversion can only happen for identifiable classes (per Class#getSimpleName()). Provided: " + cls.getName()
      );
      StringBuilder sb = new StringBuilder();
      cls.getSimpleName().codePoints().forEachOrdered(value -> {
         if (Character.isUpperCase(value)) {
            sb.append('_');
            sb.append(Character.toString(Character.toLowerCase(value)));
         } else {
            sb.append(Character.toString(value));
         }
      });
      return Identifier.withDefaultNamespace(sb.substring(1));
   }
}

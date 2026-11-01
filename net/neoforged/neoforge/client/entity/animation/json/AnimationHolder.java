package net.neoforged.neoforge.client.entity.animation.json;

import com.mojang.logging.LogUtils;
import java.util.Map;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class AnimationHolder {
   public static final AnimationDefinition EMPTY_ANIMATION = new AnimationDefinition(0.0F, false, Map.of());
   private static final Logger LOGGER = LogUtils.getLogger();
   private final Identifier key;
   @Nullable
   private AnimationDefinition value;
   private boolean absentWarned;

   AnimationHolder(Identifier key) {
      this.key = key;
   }

   void unbind() {
      this.value = null;
      this.absentWarned = false;
   }

   void bind(AnimationDefinition value) {
      this.value = value;
   }

   public Identifier key() {
      return this.key;
   }

   public AnimationDefinition get() {
      AnimationDefinition result = this.value;
      if (result == null) {
         if (!this.absentWarned) {
            this.absentWarned = true;
            LOGGER.warn("Missing entity animation {}", this.key);
         }

         return EMPTY_ANIMATION;
      } else {
         return result;
      }
   }

   @Nullable
   public AnimationDefinition getOrNull() {
      return this.value;
   }

   public boolean isBound() {
      return this.value != null;
   }
}

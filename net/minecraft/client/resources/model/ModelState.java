package net.minecraft.client.resources.model;

import com.mojang.math.Transformation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.ModelStateExtension;

@OnlyIn(Dist.CLIENT)
public interface ModelState extends ModelStateExtension {
   default Transformation getRotation() {
      return Transformation.identity();
   }

   default boolean isUvLocked() {
      return false;
   }
}

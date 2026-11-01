package net.minecraft.world.entity;

import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.common.IShearable;

@Deprecated
public interface Shearable extends IShearable {
   @Deprecated
   void shear(SoundSource var1);

   @Deprecated
   boolean readyForShearing();
}

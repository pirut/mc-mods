package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface RealmsConfigurationTab {
   void updateData(RealmsServer var1);

   default void onSelected(RealmsServer p_419724_) {
   }

   default void onDeselected(RealmsServer p_420048_) {
   }
}

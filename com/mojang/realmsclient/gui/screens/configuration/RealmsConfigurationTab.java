package com.mojang.realmsclient.gui.screens.configuration;

import com.mojang.realmsclient.dto.RealmsServer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public interface RealmsConfigurationTab {
   void updateData(RealmsServer server);

   default void onSelected(RealmsServer server) {
   }

   default void onDeselected(RealmsServer server) {
   }
}

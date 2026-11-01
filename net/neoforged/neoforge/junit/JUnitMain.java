package net.neoforged.neoforge.junit;

import cpw.mods.modlauncher.Launcher;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.common.asm.RuntimeDistCleaner;
import net.neoforged.neoforge.server.loading.ServerModLoader;

public class JUnitMain {
   public static void main(String[] args) {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
      ServerModLoader.load();
      RuntimeDistCleaner distCleaner = (RuntimeDistCleaner)Launcher.INSTANCE.environment().findLaunchPlugin("runtimedistcleaner").orElseThrow();
      distCleaner.setDistribution(null);
   }
}

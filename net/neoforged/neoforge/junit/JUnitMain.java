package net.neoforged.neoforge.junit;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.startup.JUnitGameBootstrapper;
import net.neoforged.neoforge.server.loading.ServerModLoader;

public class JUnitMain implements JUnitGameBootstrapper {
   public void bootstrap(FMLLoader fmlLoader) {
      SharedConstants.tryDetectVersion();
      Bootstrap.bootStrap();
      ServerModLoader.load(false);
   }
}

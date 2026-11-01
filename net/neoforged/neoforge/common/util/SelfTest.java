package net.neoforged.neoforge.common.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Internal
public final class SelfTest {
   private static final Logger LOGGER = LoggerFactory.getLogger(SelfTest.class);

   private SelfTest() {
   }

   public static void initCommon() {
      String serverSelfTestDestination = System.getenv("NEOFORGE_DEDICATED_SERVER_SELFTEST");
      if (serverSelfTestDestination != null) {
         if (FMLLoader.getDist() != Dist.DEDICATED_SERVER) {
            LOGGER.error("The server self-test ran with a dist of {} instead of dedicated server!", FMLLoader.getDist());
            System.exit(1);
         }

         NeoForge.EVENT_BUS.addListener(e -> {
            if (e.getServer().isRunning()) {
               writeSelfTestReport(serverSelfTestDestination);
               e.getServer().halt(false);
            }
         });
      }
   }

   public static void writeSelfTestReport(String path) {
      try {
         Files.createFile(Paths.get(path));
         LOGGER.info("Wrote self-test report to '{}'", path);
      } catch (IOException var2) {
         LOGGER.error("Failed to write self-test to '{}'", path, var2);
         System.exit(1);
      }
   }
}

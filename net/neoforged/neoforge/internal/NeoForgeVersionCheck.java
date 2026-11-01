package net.neoforged.neoforge.internal;

import net.neoforged.fml.ModList;
import net.neoforged.fml.VersionChecker;
import net.neoforged.fml.VersionChecker.CheckResult;
import net.neoforged.fml.VersionChecker.Status;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jspecify.annotations.Nullable;

public class NeoForgeVersionCheck {
   public static Status getStatus() {
      return VersionChecker.getResult((IModInfo)ModList.get().getModFileById("neoforge").getMods().get(0)).status();
   }

   @Nullable
   public static String getTarget() {
      CheckResult res = VersionChecker.getResult((IModInfo)ModList.get().getModFileById("neoforge").getMods().get(0));
      return res.target() == null ? "" : res.target().toString();
   }
}

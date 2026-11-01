package net.minecraft.data;

import com.google.common.hash.HashCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.FileUtil;

public interface CachedOutput {
   CachedOutput NO_CACHE = (p_309209_, p_309210_, p_309211_) -> {
      FileUtil.createDirectoriesSafe(p_309209_.getParent());
      Files.write(p_309209_, p_309210_);
   };

   void writeIfNeeded(Path var1, byte[] var2, HashCode var3) throws IOException;
}

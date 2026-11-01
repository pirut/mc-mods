package net.minecraft.server.packs.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;

public abstract class PackDetector<T> {
   private final DirectoryValidator validator;

   protected PackDetector(DirectoryValidator p_295530_) {
      this.validator = p_295530_;
   }

   @Nullable
   public T detectPackResources(Path p_294493_, List<ForbiddenSymlinkInfo> p_295548_) throws IOException {
      Path $$2 = p_294493_;

      BasicFileAttributes $$3;
      try {
         $$3 = Files.readAttributes(p_294493_, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      } catch (NoSuchFileException var6) {
         return null;
      }

      if ($$3.isSymbolicLink()) {
         this.validator.validateSymlink(p_294493_, p_295548_);
         if (!p_295548_.isEmpty()) {
            return null;
         }

         $$2 = Files.readSymbolicLink(p_294493_);
         $$3 = Files.readAttributes($$2, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      }

      if ($$3.isDirectory()) {
         this.validator.validateKnownDirectory($$2, p_295548_);
         if (!p_295548_.isEmpty()) {
            return null;
         } else {
            return !Files.isRegularFile($$2.resolve("pack.mcmeta")) ? null : this.createDirectoryPack($$2);
         }
      } else {
         return $$3.isRegularFile() && $$2.getFileName().toString().endsWith(".zip") ? this.createZipPack($$2) : null;
      }
   }

   @Nullable
   protected abstract T createZipPack(Path var1) throws IOException;

   @Nullable
   protected abstract T createDirectoryPack(Path var1) throws IOException;
}

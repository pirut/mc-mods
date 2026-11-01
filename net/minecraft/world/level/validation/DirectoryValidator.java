package net.minecraft.world.level.validation;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class DirectoryValidator {
   private final PathMatcher symlinkTargetAllowList;

   public DirectoryValidator(PathMatcher p_294971_) {
      this.symlinkTargetAllowList = p_294971_;
   }

   public void validateSymlink(Path p_289934_, List<ForbiddenSymlinkInfo> p_289972_) throws IOException {
      Path $$2 = Files.readSymbolicLink(p_289934_);
      if (!this.symlinkTargetAllowList.matches($$2)) {
         p_289972_.add(new ForbiddenSymlinkInfo(p_289934_, $$2));
      }
   }

   public List<ForbiddenSymlinkInfo> validateSymlink(Path p_295438_) throws IOException {
      List<ForbiddenSymlinkInfo> $$1 = new ArrayList<>();
      this.validateSymlink(p_295438_, $$1);
      return $$1;
   }

   public List<ForbiddenSymlinkInfo> validateDirectory(Path p_294195_, boolean p_295763_) throws IOException {
      List<ForbiddenSymlinkInfo> $$2 = new ArrayList<>();

      BasicFileAttributes $$3;
      try {
         $$3 = Files.readAttributes(p_294195_, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      } catch (NoSuchFileException var6) {
         return $$2;
      }

      if ($$3.isRegularFile()) {
         throw new IOException("Path " + p_294195_ + " is not a directory");
      } else {
         if ($$3.isSymbolicLink()) {
            if (!p_295763_) {
               this.validateSymlink(p_294195_, $$2);
               return $$2;
            }

            p_294195_ = Files.readSymbolicLink(p_294195_);
         }

         this.validateKnownDirectory(p_294195_, $$2);
         return $$2;
      }
   }

   public void validateKnownDirectory(Path p_294739_, final List<ForbiddenSymlinkInfo> p_295266_) throws IOException {
      Files.walkFileTree(p_294739_, new SimpleFileVisitor<Path>() {
         private void validateSymlink(Path p_289935_, BasicFileAttributes p_289941_) throws IOException {
            if (p_289941_.isSymbolicLink()) {
               DirectoryValidator.this.validateSymlink(p_289935_, p_295266_);
            }
         }

         public FileVisitResult preVisitDirectory(Path p_289946_, BasicFileAttributes p_289950_) throws IOException {
            this.validateSymlink(p_289946_, p_289950_);
            return super.preVisitDirectory(p_289946_, p_289950_);
         }

         public FileVisitResult visitFile(Path p_289986_, BasicFileAttributes p_289991_) throws IOException {
            this.validateSymlink(p_289986_, p_289991_);
            return super.visitFile(p_289986_, p_289991_);
         }
      });
   }
}

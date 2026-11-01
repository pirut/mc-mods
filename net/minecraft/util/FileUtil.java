package net.minecraft.util;

import com.mojang.serialization.DataResult;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.SharedConstants;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.apache.commons.io.FilenameUtils;

public class FileUtil {
   private static final Pattern COPY_COUNTER_PATTERN = Pattern.compile("(<name>.*) \\((<count>\\d*)\\)", 66);
   private static final int MAX_FILE_NAME = 255;
   private static final Pattern RESERVED_WINDOWS_FILENAMES = Pattern.compile(".*\\.|(?:COM|CLOCK\\$|CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?", 2);
   private static final Pattern RESERVED_WINDOWS_FILENAMES_NEOFORGE = Pattern.compile(
      ".*\\.|(?:CON|PRN|AUX|NUL|CLOCK\\$|CONIN\\$|CONOUT\\$|(?:COM|LPT)[¹²³0-9])(?:\\..*)?", 2
   );
   private static final Pattern STRICT_PATH_SEGMENT_CHECK = Pattern.compile("[-._a-z0-9]+");

   public static String sanitizeName(String p_469500_) {
      for (char c0 : SharedConstants.ILLEGAL_FILE_CHARACTERS) {
         p_469500_ = p_469500_.replace(c0, '_');
      }

      return p_469500_.replaceAll("[./\"]", "_");
   }

   public static String findAvailableName(Path p_468509_, String p_467313_, String p_468158_) throws IOException {
      p_467313_ = sanitizeName(p_467313_);
      if (!isPathPartPortable(p_467313_)) {
         p_467313_ = "_" + p_467313_ + "_";
      }

      Matcher matcher = COPY_COUNTER_PATTERN.matcher(p_467313_);
      int i = 0;
      if (matcher.matches()) {
         p_467313_ = matcher.group("name");
         i = Integer.parseInt(matcher.group("count"));
      }

      if (p_467313_.length() > 255 - p_468158_.length()) {
         p_467313_ = p_467313_.substring(0, 255 - p_468158_.length());
      }

      while (true) {
         String s = p_467313_;
         if (i != 0) {
            String s1 = " (" + i + ")";
            int j = 255 - s1.length();
            if (p_467313_.length() > j) {
               s = p_467313_.substring(0, j);
            }

            s = s + s1;
         }

         s = s + p_468158_;
         Path path = p_468509_.resolve(s);

         try {
            Path path1 = Files.createDirectory(path);
            Files.deleteIfExists(path1);
            return p_468509_.relativize(path1).toString();
         } catch (FileAlreadyExistsException var8) {
            i++;
         }
      }
   }

   public static boolean isPathNormalized(Path p_467464_) {
      Path path = p_467464_.normalize();
      return path.equals(p_467464_);
   }

   public static boolean isPathPortable(Path p_466888_) {
      for (Path path : p_466888_) {
         if (!isPathPartPortable(path.toString())) {
            return false;
         }
      }

      return true;
   }

   public static boolean isPathPartPortable(String p_428321_) {
      return !(NeoForgeMod.getProperFilenameValidation() ? RESERVED_WINDOWS_FILENAMES_NEOFORGE : RESERVED_WINDOWS_FILENAMES).matcher(p_428321_).matches();
   }

   public static Path createPathToResource(Path p_469027_, String p_468845_, String p_467618_) {
      String s = p_468845_ + p_467618_;
      Path path = Paths.get(s);
      if (path.endsWith(p_467618_)) {
         throw new InvalidPathException(s, "empty resource name");
      } else {
         return p_469027_.resolve(path);
      }
   }

   public static String getFullResourcePath(String p_468565_) {
      return FilenameUtils.getFullPath(p_468565_).replace(File.separator, "/");
   }

   public static String normalizeResourcePath(String p_467778_) {
      return FilenameUtils.normalize(p_467778_).replace(File.separator, "/");
   }

   public static DataResult<List<String>> decomposePath(String p_467771_) {
      int i = p_467771_.indexOf(47);
      if (i == -1) {
         return switch (p_467771_) {
            case "", ".", ".." -> DataResult.error(() -> "Invalid path '" + p_467771_ + "'");
            default -> !containsAllowedCharactersOnly(p_467771_)
               ? DataResult.error(() -> "Invalid path '" + p_467771_ + "'")
               : DataResult.success(List.of(p_467771_));
         };
      } else {
         List<String> list = new ArrayList<>();
         int j = 0;
         boolean flag = false;

         while (true) {
            String s = p_467771_.substring(j, i);
            switch (s) {
               case "":
               case ".":
               case "..":
                  return DataResult.error(() -> "Invalid segment '" + s + "' in path '" + p_467771_ + "'");
            }

            if (!containsAllowedCharactersOnly(s)) {
               return DataResult.error(() -> "Invalid segment '" + s + "' in path '" + p_467771_ + "'");
            }

            list.add(s);
            if (flag) {
               return DataResult.success(list);
            }

            j = i + 1;
            i = p_467771_.indexOf(47, j);
            if (i == -1) {
               i = p_467771_.length();
               flag = true;
            }
         }
      }
   }

   public static Path resolvePath(Path p_467623_, List<String> p_469629_) {
      int i = p_469629_.size();

      return switch (i) {
         case 0 -> p_467623_;
         case 1 -> p_467623_.resolve(p_469629_.get(0));
         default -> {
            String[] astring = new String[i - 1];

            for (int j = 1; j < i; j++) {
               astring[j - 1] = p_469629_.get(j);
            }

            yield p_467623_.resolve(p_467623_.getFileSystem().getPath(p_469629_.get(0), astring));
         }
      };
   }

   private static boolean containsAllowedCharactersOnly(String p_469574_) {
      return STRICT_PATH_SEGMENT_CHECK.matcher(p_469574_).matches();
   }

   public static boolean isValidPathSegment(String p_469125_) {
      return !p_469125_.equals("..") && !p_469125_.equals(".") && containsAllowedCharactersOnly(p_469125_);
   }

   public static void validatePath(String... p_468562_) {
      if (p_468562_.length == 0) {
         throw new IllegalArgumentException("Path must have at least one element");
      } else {
         for (String s : p_468562_) {
            if (!isValidPathSegment(s)) {
               throw new IllegalArgumentException("Illegal segment " + s + " in path " + Arrays.toString(p_468562_));
            }
         }
      }
   }

   public static void createDirectoriesSafe(Path p_468971_) throws IOException {
      Files.createDirectories(Files.exists(p_468971_) ? p_468971_.toRealPath() : p_468971_);
   }
}

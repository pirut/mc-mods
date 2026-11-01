package com.mojang.blaze3d.preprocessor;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.util.StringUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class GlslPreprocessor {
   private static final String C_COMMENT = "/\\*(?:[^*]|\\*+[^*/])*\\*+/";
   private static final String LINE_COMMENT = "//[^\\v]*";
   private static final Pattern REGEX_MOJ_IMPORT = Pattern.compile(
      "(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*moj_import(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*(?:\"(.*)\"|<(.*)>))"
   );
   private static final Pattern REGEX_VERSION = Pattern.compile("(#(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*version(?:/\\*(?:[^*]|\\*+[^*/])*\\*+/|\\h)*(\\d+))\\b");
   private static final Pattern REGEX_ENDS_WITH_WHITESPACE = Pattern.compile("(?:^|\\v)(?:\\s|/\\*(?:[^*]|\\*+[^*/])*\\*+/|(//[^\\v]*))*\\z");

   public List<String> process(String p_166462_) {
      GlslPreprocessor.Context $$1 = new GlslPreprocessor.Context();
      List<String> $$2 = this.processImports(p_166462_, $$1, "");
      $$2.set(0, this.setVersion($$2.get(0), $$1.glslVersion));
      return $$2;
   }

   private List<String> processImports(String p_166470_, GlslPreprocessor.Context p_166471_, String p_166472_) {
      int $$3 = p_166471_.sourceId;
      int $$4 = 0;
      String $$5 = "";
      List<String> $$6 = Lists.newArrayList();
      Matcher $$7 = REGEX_MOJ_IMPORT.matcher(p_166470_);

      while ($$7.find()) {
         if (!isDirectiveDisabled(p_166470_, $$7, $$4)) {
            String $$8 = $$7.group(2);
            boolean $$9 = $$8 != null;
            if (!$$9) {
               $$8 = $$7.group(3);
            }

            if ($$8 != null) {
               String $$10 = p_166470_.substring($$4, $$7.start(1));
               String $$11 = p_166472_ + $$8;
               String $$12 = this.applyImport($$9, $$11);
               if (!Strings.isNullOrEmpty($$12)) {
                  if (!StringUtil.endsWithNewLine($$12)) {
                     $$12 = $$12 + System.lineSeparator();
                  }

                  p_166471_.sourceId++;
                  int $$13 = p_166471_.sourceId;
                  List<String> $$14 = this.processImports($$12, p_166471_, $$9 ? FileUtil.getFullResourcePath($$11) : "");
                  $$14.set(0, String.format(Locale.ROOT, "#line %d %d\n%s", 0, $$13, this.processVersions($$14.get(0), p_166471_)));
                  if (!StringUtil.isBlank($$10)) {
                     $$6.add($$10);
                  }

                  $$6.addAll($$14);
               } else {
                  String $$15 = $$9 ? String.format(Locale.ROOT, "/*#moj_import \"%s\"*/", $$8) : String.format(Locale.ROOT, "/*#moj_import <%s>*/", $$8);
                  $$6.add($$5 + $$10 + $$15);
               }

               int $$16 = StringUtil.lineCount(p_166470_.substring(0, $$7.end(1)));
               $$5 = String.format(Locale.ROOT, "#line %d %d", $$16, $$3);
               $$4 = $$7.end(1);
            }
         }
      }

      String $$17 = p_166470_.substring($$4);
      if (!StringUtil.isBlank($$17)) {
         $$6.add($$5 + $$17);
      }

      return $$6;
   }

   private String processVersions(String p_166467_, GlslPreprocessor.Context p_166468_) {
      Matcher $$2 = REGEX_VERSION.matcher(p_166467_);
      if ($$2.find() && isDirectiveEnabled(p_166467_, $$2)) {
         p_166468_.glslVersion = Math.max(p_166468_.glslVersion, Integer.parseInt($$2.group(2)));
         return p_166467_.substring(0, $$2.start(1)) + "/*" + p_166467_.substring($$2.start(1), $$2.end(1)) + "*/" + p_166467_.substring($$2.end(1));
      } else {
         return p_166467_;
      }
   }

   private String setVersion(String p_166464_, int p_166465_) {
      Matcher $$2 = REGEX_VERSION.matcher(p_166464_);
      return $$2.find() && isDirectiveEnabled(p_166464_, $$2)
         ? p_166464_.substring(0, $$2.start(2)) + Math.max(p_166465_, Integer.parseInt($$2.group(2))) + p_166464_.substring($$2.end(2))
         : p_166464_;
   }

   private static boolean isDirectiveEnabled(String p_166474_, Matcher p_166475_) {
      return !isDirectiveDisabled(p_166474_, p_166475_, 0);
   }

   private static boolean isDirectiveDisabled(String p_166477_, Matcher p_166478_, int p_166479_) {
      int $$3 = p_166478_.start() - p_166479_;
      if ($$3 == 0) {
         return false;
      } else {
         Matcher $$4 = REGEX_ENDS_WITH_WHITESPACE.matcher(p_166477_.substring(p_166479_, p_166478_.start()));
         if (!$$4.find()) {
            return true;
         } else {
            int $$5 = $$4.end(1);
            return $$5 == p_166478_.start();
         }
      }
   }

   @Nullable
   public abstract String applyImport(boolean var1, String var2);

   @OnlyIn(Dist.CLIENT)
   static final class Context {
      int glslVersion;
      int sourceId;
   }
}

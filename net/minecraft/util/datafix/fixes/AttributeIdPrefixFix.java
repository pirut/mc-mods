package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import java.util.List;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class AttributeIdPrefixFix extends AttributesRenameFix {
   private static final List<String> PREFIXES = List.of("generic.", "horse.", "player.", "zombie.");

   public AttributeIdPrefixFix(Schema outputSchema) {
      super(outputSchema, "AttributeIdPrefixFix", AttributeIdPrefixFix::replaceId);
   }

   private static String replaceId(String id) {
      String string = NamespacedSchema.ensureNamespaced(id);

      for (String string2 : PREFIXES) {
         String string3 = NamespacedSchema.ensureNamespaced(string2);
         if (string.startsWith(string3)) {
            return "minecraft:" + string.substring(string3.length());
         }
      }

      return id;
   }
}

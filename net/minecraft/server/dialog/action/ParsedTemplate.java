package net.minecraft.server.dialog.action;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.functions.StringTemplate;

public class ParsedTemplate {
   public static final Codec<ParsedTemplate> CODEC = Codec.STRING.comapFlatMap(ParsedTemplate::parse, p_428259_ -> p_428259_.raw);
   public static final Codec<String> VARIABLE_CODEC = Codec.STRING
      .validate(
         p_428384_ -> StringTemplate.isValidVariableName(p_428384_)
            ? DataResult.success(p_428384_)
            : DataResult.error(() -> p_428384_ + " is not a valid input name")
      );
   private final String raw;
   private final StringTemplate parsed;

   private ParsedTemplate(String p_428265_, StringTemplate p_428400_) {
      this.raw = p_428265_;
      this.parsed = p_428400_;
   }

   private static DataResult<ParsedTemplate> parse(String p_428254_) {
      StringTemplate stringtemplate;
      try {
         stringtemplate = StringTemplate.fromString(p_428254_);
      } catch (Exception var3) {
         return DataResult.error(() -> "Failed to parse template " + p_428254_ + ": " + var3.getMessage());
      }

      return DataResult.success(new ParsedTemplate(p_428254_, stringtemplate));
   }

   public String instantiate(Map<String, String> p_428392_) {
      List<String> list = this.parsed.variables().stream().map(p_428551_ -> p_428392_.getOrDefault(p_428551_, "")).toList();
      return this.parsed.substitute(list);
   }
}

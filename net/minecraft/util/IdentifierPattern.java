package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import net.minecraft.resources.Identifier;

public class IdentifierPattern {
   public static final Codec<IdentifierPattern> CODEC = RecordCodecBuilder.create(
      p_469020_ -> p_469020_.group(
            ExtraCodecs.PATTERN.optionalFieldOf("namespace").forGetter(p_467950_ -> p_467950_.namespacePattern),
            ExtraCodecs.PATTERN.optionalFieldOf("path").forGetter(p_469878_ -> p_469878_.pathPattern)
         )
         .apply(p_469020_, IdentifierPattern::new)
   );
   private final Optional<Pattern> namespacePattern;
   private final Predicate<String> namespacePredicate;
   private final Optional<Pattern> pathPattern;
   private final Predicate<String> pathPredicate;
   private final Predicate<Identifier> locationPredicate;

   private IdentifierPattern(Optional<Pattern> p_469724_, Optional<Pattern> p_469717_) {
      this.namespacePattern = p_469724_;
      this.namespacePredicate = p_469724_.map(Pattern::asPredicate).orElse(p_467927_ -> true);
      this.pathPattern = p_469717_;
      this.pathPredicate = p_469717_.map(Pattern::asPredicate).orElse(p_467386_ -> true);
      this.locationPredicate = p_467582_ -> this.namespacePredicate.test(p_467582_.getNamespace()) && this.pathPredicate.test(p_467582_.getPath());
   }

   public Predicate<String> namespacePredicate() {
      return this.namespacePredicate;
   }

   public Predicate<String> pathPredicate() {
      return this.pathPredicate;
   }

   public Predicate<Identifier> locationPredicate() {
      return this.locationPredicate;
   }
}

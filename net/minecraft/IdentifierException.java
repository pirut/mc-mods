package net.minecraft;

import org.apache.commons.lang3.StringEscapeUtils;

public class IdentifierException extends RuntimeException {
   public IdentifierException(String p_467500_) {
      super(StringEscapeUtils.escapeJava(p_467500_));
   }

   public IdentifierException(String p_467057_, Throwable p_469911_) {
      super(StringEscapeUtils.escapeJava(p_467057_), p_469911_);
   }
}

package net.neoforged.neoforge.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.jetbrains.annotations.ApiStatus.Internal;

@Target({ElementType.TYPE})
@Internal
public @interface NonExhaustiveEnum {
   String reason();
}

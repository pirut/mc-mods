package net.neoforged.neoforge.common;

import java.util.Set;

public class EffectCures {
   public static final EffectCure MILK = EffectCure.get("milk");
   public static final EffectCure HONEY = EffectCure.get("honey");
   public static final EffectCure PROTECTED_BY_TOTEM = EffectCure.get("protected_by_totem");
   public static final Set<EffectCure> DEFAULT_CURES = Set.of(MILK, PROTECTED_BY_TOTEM);
}

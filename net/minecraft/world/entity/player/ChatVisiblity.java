package net.minecraft.world.entity.player;

import com.mojang.serialization.Codec;
import java.util.function.IntFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ByIdMap;

public enum ChatVisiblity {
   FULL(0, "options.chat.visibility.full"),
   SYSTEM(1, "options.chat.visibility.system"),
   HIDDEN(2, "options.chat.visibility.hidden");

   private static final IntFunction<ChatVisiblity> BY_ID = ByIdMap.continuous(p_467344_ -> p_467344_.id, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
   public static final Codec<ChatVisiblity> LEGACY_CODEC = Codec.INT.xmap(BY_ID::apply, p_468491_ -> p_468491_.id);
   private final int id;
   private final Component caption;

   private ChatVisiblity(int p_35963_, String p_35964_) {
      this.id = p_35963_;
      this.caption = Component.translatable(p_35964_);
   }

   public Component caption() {
      return this.caption;
   }
}

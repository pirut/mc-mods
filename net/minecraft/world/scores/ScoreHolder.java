package net.minecraft.world.scores;

import com.mojang.authlib.GameProfile;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public interface ScoreHolder {
   String WILDCARD_NAME = "*";
   ScoreHolder WILDCARD = new ScoreHolder() {
      @Override
      public String getScoreboardName() {
         return "*";
      }
   };

   String getScoreboardName();

   @Nullable
   default Component getDisplayName() {
      return null;
   }

   default Component getFeedbackDisplayName() {
      Component $$0 = this.getDisplayName();
      return $$0 != null
         ? $$0.copy()
            .withStyle(p_313907_ -> p_313907_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(this.getScoreboardName()))))
         : Component.literal(this.getScoreboardName());
   }

   static ScoreHolder forNameOnly(final String p_313803_) {
      if (p_313803_.equals("*")) {
         return WILDCARD;
      } else {
         final Component $$1 = Component.literal(p_313803_);
         return new ScoreHolder() {
            @Override
            public String getScoreboardName() {
               return p_313803_;
            }

            @Override
            public Component getFeedbackDisplayName() {
               return $$1;
            }
         };
      }
   }

   static ScoreHolder fromGameProfile(GameProfile p_313755_) {
      final String $$1 = p_313755_.getName();
      return new ScoreHolder() {
         @Override
         public String getScoreboardName() {
            return $$1;
         }
      };
   }
}

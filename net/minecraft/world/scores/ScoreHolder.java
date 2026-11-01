package net.minecraft.world.scores;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.jspecify.annotations.Nullable;

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
      Component component = this.getDisplayName();
      return component != null
         ? component.copy().withStyle(p_393460_ -> p_393460_.withHoverEvent(new HoverEvent.ShowText(Component.literal(this.getScoreboardName()))))
         : Component.literal(this.getScoreboardName());
   }

   static ScoreHolder forNameOnly(final String p_313803_) {
      if (p_313803_.equals("*")) {
         return WILDCARD;
      } else {
         final Component component = Component.literal(p_313803_);
         return new ScoreHolder() {
            @Override
            public String getScoreboardName() {
               return p_313803_;
            }

            @Override
            public Component getFeedbackDisplayName() {
               return component;
            }
         };
      }
   }

   static ScoreHolder fromGameProfile(GameProfile p_313755_) {
      final String s = p_313755_.name();
      return new ScoreHolder() {
         @Override
         public String getScoreboardName() {
            return s;
         }
      };
   }
}

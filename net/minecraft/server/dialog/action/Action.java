package net.minecraft.server.dialog.action;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;

public interface Action {
   Codec<Action> CODEC = BuiltInRegistries.DIALOG_ACTION_TYPE.byNameCodec().dispatch(Action::codec, p_428320_ -> p_428320_);

   MapCodec<? extends Action> codec();

   Optional<ClickEvent> createAction(Map<String, Action.ValueGetter> var1);

   public interface ValueGetter {
      String asTemplateSubstitution();

      Tag asTag();

      static Map<String, String> getAsTemplateSubstitutions(Map<String, Action.ValueGetter> p_428445_) {
         return Maps.transformValues(p_428445_, Action.ValueGetter::asTemplateSubstitution);
      }

      static Action.ValueGetter of(final String p_428500_) {
         return new Action.ValueGetter() {
            @Override
            public String asTemplateSubstitution() {
               return p_428500_;
            }

            @Override
            public Tag asTag() {
               return StringTag.valueOf(p_428500_);
            }
         };
      }

      static Action.ValueGetter of(final Supplier<String> p_428364_) {
         return new Action.ValueGetter() {
            @Override
            public String asTemplateSubstitution() {
               return p_428364_.get();
            }

            @Override
            public Tag asTag() {
               return StringTag.valueOf(p_428364_.get());
            }
         };
      }
   }
}

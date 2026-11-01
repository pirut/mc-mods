package net.minecraft.commands.arguments;

import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class EntityAnchorArgument implements ArgumentType<EntityAnchorArgument.Anchor> {
   private static final Collection<String> EXAMPLES = Arrays.asList("eyes", "feet");
   private static final DynamicCommandExceptionType ERROR_INVALID = new DynamicCommandExceptionType(
      p_304085_ -> Component.translatableEscape("argument.anchor.invalid", p_304085_)
   );

   public static EntityAnchorArgument.Anchor getAnchor(CommandContext<CommandSourceStack> p_90354_, String p_90355_) {
      return (EntityAnchorArgument.Anchor)p_90354_.getArgument(p_90355_, EntityAnchorArgument.Anchor.class);
   }

   public static EntityAnchorArgument anchor() {
      return new EntityAnchorArgument();
   }

   public EntityAnchorArgument.Anchor parse(StringReader p_90352_) throws CommandSyntaxException {
      int $$1 = p_90352_.getCursor();
      String $$2 = p_90352_.readUnquotedString();
      EntityAnchorArgument.Anchor $$3 = EntityAnchorArgument.Anchor.getByName($$2);
      if ($$3 == null) {
         p_90352_.setCursor($$1);
         throw ERROR_INVALID.createWithContext(p_90352_, $$2);
      } else {
         return $$3;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> p_90360_, SuggestionsBuilder p_90361_) {
      return SharedSuggestionProvider.suggest(EntityAnchorArgument.Anchor.BY_NAME.keySet(), p_90361_);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static enum Anchor {
      FEET("feet", (p_90389_, p_90390_) -> p_90389_),
      EYES("eyes", (p_90382_, p_90383_) -> new Vec3(p_90382_.x, p_90382_.y + p_90383_.getEyeHeight(), p_90382_.z));

      static final Map<String, EntityAnchorArgument.Anchor> BY_NAME = Util.make(Maps.newHashMap(), p_90387_ -> {
         for (EntityAnchorArgument.Anchor $$1 : values()) {
            p_90387_.put($$1.name, $$1);
         }
      });
      private final String name;
      private final BiFunction<Vec3, Entity, Vec3> transform;

      private Anchor(String p_90374_, BiFunction<Vec3, Entity, Vec3> p_90375_) {
         this.name = p_90374_;
         this.transform = p_90375_;
      }

      @Nullable
      public static EntityAnchorArgument.Anchor getByName(String p_90385_) {
         return BY_NAME.get(p_90385_);
      }

      public Vec3 apply(Entity p_90378_) {
         return this.transform.apply(p_90378_.position(), p_90378_);
      }

      public Vec3 apply(CommandSourceStack p_90380_) {
         Entity $$1 = p_90380_.getEntity();
         return $$1 == null ? p_90380_.getPosition() : this.transform.apply(p_90380_.getPosition(), $$1);
      }
   }
}

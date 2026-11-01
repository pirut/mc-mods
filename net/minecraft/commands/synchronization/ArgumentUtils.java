package net.minecraft.commands.synchronization;

import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;

public class ArgumentUtils {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final byte NUMBER_FLAG_MIN = 1;
   private static final byte NUMBER_FLAG_MAX = 2;

   public static int createNumberFlags(boolean p_235428_, boolean p_235429_) {
      int $$2 = 0;
      if (p_235428_) {
         $$2 |= 1;
      }

      if (p_235429_) {
         $$2 |= 2;
      }

      return $$2;
   }

   public static boolean numberHasMin(byte p_235403_) {
      return (p_235403_ & 1) != 0;
   }

   public static boolean numberHasMax(byte p_235431_) {
      return (p_235431_ & 2) != 0;
   }

   private static <A extends ArgumentType<?>> void serializeCap(JsonObject p_235408_, ArgumentTypeInfo.Template<A> p_235409_) {
      serializeCap(p_235408_, p_235409_.type(), p_235409_);
   }

   private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void serializeCap(
      JsonObject p_235411_, ArgumentTypeInfo<A, T> p_235412_, ArgumentTypeInfo.Template<A> p_235413_
   ) {
      p_235412_.serializeToJson((T)p_235413_, p_235411_);
   }

   private static <T extends ArgumentType<?>> void serializeArgumentToJson(JsonObject p_235405_, T p_235406_) {
      ArgumentTypeInfo.Template<T> $$2 = ArgumentTypeInfos.unpack(p_235406_);
      p_235405_.addProperty("type", "argument");
      p_235405_.addProperty("parser", BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey($$2.type()).toString());
      JsonObject $$3 = new JsonObject();
      serializeCap($$3, $$2);
      if ($$3.size() > 0) {
         p_235405_.add("properties", $$3);
      }
   }

   public static <S> JsonObject serializeNodeToJson(CommandDispatcher<S> p_235415_, CommandNode<S> p_235416_) {
      JsonObject $$2 = new JsonObject();
      if (p_235416_ instanceof RootCommandNode) {
         $$2.addProperty("type", "root");
      } else if (p_235416_ instanceof LiteralCommandNode) {
         $$2.addProperty("type", "literal");
      } else if (p_235416_ instanceof ArgumentCommandNode<?, ?> $$3) {
         serializeArgumentToJson($$2, $$3.getType());
      } else {
         LOGGER.error("Could not serialize node {} ({})!", p_235416_, p_235416_.getClass());
         $$2.addProperty("type", "unknown");
      }

      JsonObject $$4 = new JsonObject();

      for (CommandNode<S> $$5 : p_235416_.getChildren()) {
         $$4.add($$5.getName(), serializeNodeToJson(p_235415_, $$5));
      }

      if ($$4.size() > 0) {
         $$2.add("children", $$4);
      }

      if (p_235416_.getCommand() != null) {
         $$2.addProperty("executable", true);
      }

      if (p_235416_.getRedirect() != null) {
         Collection<String> $$6 = p_235415_.getPath(p_235416_.getRedirect());
         if (!$$6.isEmpty()) {
            JsonArray $$7 = new JsonArray();

            for (String $$8 : $$6) {
               $$7.add($$8);
            }

            $$2.add("redirect", $$7);
         }
      }

      return $$2;
   }

   public static <T> Set<ArgumentType<?>> findUsedArgumentTypes(CommandNode<T> p_235418_) {
      Set<CommandNode<T>> $$1 = Sets.newIdentityHashSet();
      Set<ArgumentType<?>> $$2 = Sets.newHashSet();
      findUsedArgumentTypes(p_235418_, $$2, $$1);
      return $$2;
   }

   private static <T> void findUsedArgumentTypes(CommandNode<T> p_235420_, Set<ArgumentType<?>> p_235421_, Set<CommandNode<T>> p_235422_) {
      if (p_235422_.add(p_235420_)) {
         if (p_235420_ instanceof ArgumentCommandNode<?, ?> $$3) {
            p_235421_.add($$3.getType());
         }

         p_235420_.getChildren().forEach(p_235426_ -> findUsedArgumentTypes(p_235426_, p_235421_, p_235422_));
         CommandNode<T> $$4 = p_235420_.getRedirect();
         if ($$4 != null) {
            findUsedArgumentTypes($$4, p_235421_, p_235422_);
         }
      }
   }
}

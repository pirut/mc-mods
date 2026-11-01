package net.minecraft.util.context;

import com.google.common.collect.Sets;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public class ContextMap {
   public static final ContextMap EMPTY = new ContextMap.Builder().create(ContextKeySet.EMPTY);
   private final Map<ContextKey<?>, Object> params;

   ContextMap(Map<ContextKey<?>, Object> p_381157_) {
      this.params = p_381157_;
   }

   public boolean has(ContextKey<?> p_380966_) {
      return this.params.containsKey(p_380966_);
   }

   public <T> T getOrThrow(ContextKey<T> p_380981_) {
      T t = (T)this.params.get(p_380981_);
      if (t == null) {
         throw new NoSuchElementException(p_380981_.name().toString());
      } else {
         return t;
      }
   }

   @Nullable
   public <T> T getOptional(ContextKey<T> p_381151_) {
      return (T)this.params.get(p_381151_);
   }

   @Contract("_,!null->!null; _,_->_")
   @Nullable
   public <T> T getOrDefault(ContextKey<T> p_381114_, @Nullable T p_380947_) {
      return (T)this.params.getOrDefault(p_381114_, p_380947_);
   }

   public static class Builder {
      private final Map<ContextKey<?>, Object> params = new IdentityHashMap<>();

      public <T> ContextMap.Builder withParameter(ContextKey<T> p_380968_, T p_381024_) {
         this.params.put(p_380968_, p_381024_);
         return this;
      }

      public <T> ContextMap.Builder withOptionalParameter(ContextKey<T> p_381089_, @Nullable T p_381019_) {
         if (p_381019_ == null) {
            this.params.remove(p_381089_);
         } else {
            this.params.put(p_381089_, p_381019_);
         }

         return this;
      }

      public <T> T getParameter(ContextKey<T> p_380972_) {
         T t = (T)this.params.get(p_380972_);
         if (t == null) {
            throw new NoSuchElementException(p_380972_.name().toString());
         } else {
            return t;
         }
      }

      @Nullable
      public <T> T getOptionalParameter(ContextKey<T> p_381048_) {
         return (T)this.params.get(p_381048_);
      }

      public ContextMap create(ContextKeySet p_381168_) {
         Set<ContextKey<?>> set = Sets.difference(this.params.keySet(), p_381168_.allowed());
         Set<ContextKey<?>> set1 = Sets.difference(p_381168_.required(), this.params.keySet());
         if (!set1.isEmpty()) {
            throw new IllegalArgumentException("Missing required parameters: " + set1);
         } else {
            return new ContextMap(this.params);
         }
      }
   }
}

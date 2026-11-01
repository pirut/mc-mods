package net.minecraft.util.parsing.packrat;

import net.minecraft.util.Util;
import org.jspecify.annotations.Nullable;

public abstract class CachedParseState<S> implements ParseState<S> {
   private CachedParseState.PositionCache[] positionCache = new CachedParseState.PositionCache[256];
   private final ErrorCollector<S> errorCollector;
   private final Scope scope = new Scope();
   private CachedParseState.SimpleControl[] controlCache = new CachedParseState.SimpleControl[16];
   private int nextControlToReturn;
   private final CachedParseState<S>.Silent silent = new CachedParseState.Silent();

   protected CachedParseState(ErrorCollector<S> p_410265_) {
      this.errorCollector = p_410265_;
   }

   @Override
   public Scope scope() {
      return this.scope;
   }

   @Override
   public ErrorCollector<S> errorCollector() {
      return this.errorCollector;
   }

   @Nullable
   @Override
   public <T> T parse(NamedRule<S, T> p_410365_) {
      int i = this.mark();
      CachedParseState.PositionCache cachedparsestate$positioncache = this.getCacheForPosition(i);
      int j = cachedparsestate$positioncache.findKeyIndex(p_410365_.name());
      if (j != -1) {
         CachedParseState.CacheEntry<T> cacheentry = cachedparsestate$positioncache.getValue(j);
         if (cacheentry != null) {
            if (cacheentry == CachedParseState.CacheEntry.NEGATIVE) {
               return null;
            }

            this.restore(cacheentry.markAfterParse);
            return cacheentry.value;
         }
      } else {
         j = cachedparsestate$positioncache.allocateNewEntry(p_410365_.name());
      }

      T t = p_410365_.value().parse(this);
      CachedParseState.CacheEntry<T> cacheentry1;
      if (t == null) {
         cacheentry1 = CachedParseState.CacheEntry.negativeEntry();
      } else {
         int k = this.mark();
         cacheentry1 = new CachedParseState.CacheEntry<>(t, k);
      }

      cachedparsestate$positioncache.setValue(j, cacheentry1);
      return t;
   }

   private CachedParseState.PositionCache getCacheForPosition(int p_410075_) {
      int i = this.positionCache.length;
      if (p_410075_ >= i) {
         int j = Util.growByHalf(i, p_410075_ + 1);
         CachedParseState.PositionCache[] acachedparsestate$positioncache = new CachedParseState.PositionCache[j];
         System.arraycopy(this.positionCache, 0, acachedparsestate$positioncache, 0, i);
         this.positionCache = acachedparsestate$positioncache;
      }

      CachedParseState.PositionCache cachedparsestate$positioncache = this.positionCache[p_410075_];
      if (cachedparsestate$positioncache == null) {
         cachedparsestate$positioncache = new CachedParseState.PositionCache();
         this.positionCache[p_410075_] = cachedparsestate$positioncache;
      }

      return cachedparsestate$positioncache;
   }

   @Override
   public Control acquireControl() {
      int i = this.controlCache.length;
      if (this.nextControlToReturn >= i) {
         int j = Util.growByHalf(i, this.nextControlToReturn + 1);
         CachedParseState.SimpleControl[] acachedparsestate$simplecontrol = new CachedParseState.SimpleControl[j];
         System.arraycopy(this.controlCache, 0, acachedparsestate$simplecontrol, 0, i);
         this.controlCache = acachedparsestate$simplecontrol;
      }

      int k = this.nextControlToReturn++;
      CachedParseState.SimpleControl cachedparsestate$simplecontrol = this.controlCache[k];
      if (cachedparsestate$simplecontrol == null) {
         cachedparsestate$simplecontrol = new CachedParseState.SimpleControl();
         this.controlCache[k] = cachedparsestate$simplecontrol;
      } else {
         cachedparsestate$simplecontrol.reset();
      }

      return cachedparsestate$simplecontrol;
   }

   @Override
   public void releaseControl() {
      this.nextControlToReturn--;
   }

   @Override
   public ParseState<S> silent() {
      return this.silent;
   }

   record CacheEntry<T>(@Nullable T value, int markAfterParse) {
      public static final CachedParseState.CacheEntry<?> NEGATIVE = new CachedParseState.CacheEntry(null, -1);

      public static <T> CachedParseState.CacheEntry<T> negativeEntry() {
         return (CachedParseState.CacheEntry<T>)NEGATIVE;
      }
   }

   static class PositionCache {
      public static final int ENTRY_STRIDE = 2;
      private static final int NOT_FOUND = -1;
      private Object[] atomCache = new Object[16];
      private int nextKey;

      public int findKeyIndex(Atom<?> p_410592_) {
         for (int i = 0; i < this.nextKey; i += 2) {
            if (this.atomCache[i] == p_410592_) {
               return i;
            }
         }

         return -1;
      }

      public int allocateNewEntry(Atom<?> p_410029_) {
         int i = this.nextKey;
         this.nextKey += 2;
         int j = i + 1;
         int k = this.atomCache.length;
         if (j >= k) {
            int l = Util.growByHalf(k, j + 1);
            Object[] aobject = new Object[l];
            System.arraycopy(this.atomCache, 0, aobject, 0, k);
            this.atomCache = aobject;
         }

         this.atomCache[i] = p_410029_;
         return i;
      }

      @Nullable
      public <T> CachedParseState.CacheEntry<T> getValue(int p_410518_) {
         return (CachedParseState.CacheEntry<T>)this.atomCache[p_410518_ + 1];
      }

      public void setValue(int p_409757_, CachedParseState.CacheEntry<?> p_410617_) {
         this.atomCache[p_409757_ + 1] = p_410617_;
      }
   }

   class Silent implements ParseState<S> {
      private final ErrorCollector<S> silentCollector = new ErrorCollector.Nop<>();

      @Override
      public ErrorCollector<S> errorCollector() {
         return this.silentCollector;
      }

      @Override
      public Scope scope() {
         return CachedParseState.this.scope();
      }

      @Nullable
      @Override
      public <T> T parse(NamedRule<S, T> p_410584_) {
         return CachedParseState.this.parse(p_410584_);
      }

      @Override
      public S input() {
         return CachedParseState.this.input();
      }

      @Override
      public int mark() {
         return CachedParseState.this.mark();
      }

      @Override
      public void restore(int p_410357_) {
         CachedParseState.this.restore(p_410357_);
      }

      @Override
      public Control acquireControl() {
         return CachedParseState.this.acquireControl();
      }

      @Override
      public void releaseControl() {
         CachedParseState.this.releaseControl();
      }

      @Override
      public ParseState<S> silent() {
         return this;
      }
   }

   static class SimpleControl implements Control {
      private boolean hasCut;

      @Override
      public void cut() {
         this.hasCut = true;
      }

      @Override
      public boolean hasCut() {
         return this.hasCut;
      }

      public void reset() {
         this.hasCut = false;
      }
   }
}

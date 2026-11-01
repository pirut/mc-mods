package net.minecraft.util.profiling;

import java.util.function.Supplier;
import net.minecraft.util.profiling.metrics.MetricCategory;

public interface ProfilerFiller {
   String ROOT = "root";

   void startTick();

   void endTick();

   void push(String var1);

   void push(Supplier<String> var1);

   void pop();

   void popPush(String var1);

   void popPush(Supplier<String> var1);

   default void addZoneText(String p_372857_) {
   }

   default void addZoneValue(long p_372840_) {
   }

   default void setZoneColor(int p_372844_) {
   }

   default Zone zone(String p_372946_) {
      this.push(p_372946_);
      return new Zone(this);
   }

   default Zone zone(Supplier<String> p_373095_) {
      this.push(p_373095_);
      return new Zone(this);
   }

   void markForCharting(MetricCategory var1);

   default void incrementCounter(String p_18585_) {
      this.incrementCounter(p_18585_, 1);
   }

   void incrementCounter(String var1, int var2);

   default void incrementCounter(Supplier<String> p_18586_) {
      this.incrementCounter(p_18586_, 1);
   }

   void incrementCounter(Supplier<String> var1, int var2);

   static ProfilerFiller combine(ProfilerFiller p_373012_, ProfilerFiller p_373110_) {
      if (p_373012_ == InactiveProfiler.INSTANCE) {
         return p_373110_;
      } else {
         return (ProfilerFiller)(p_373110_ == InactiveProfiler.INSTANCE ? p_373012_ : new ProfilerFiller.CombinedProfileFiller(p_373012_, p_373110_));
      }
   }

   public static class CombinedProfileFiller implements ProfilerFiller {
      private final ProfilerFiller first;
      private final ProfilerFiller second;

      public CombinedProfileFiller(ProfilerFiller p_373115_, ProfilerFiller p_372882_) {
         this.first = p_373115_;
         this.second = p_372882_;
      }

      @Override
      public void startTick() {
         this.first.startTick();
         this.second.startTick();
      }

      @Override
      public void endTick() {
         this.first.endTick();
         this.second.endTick();
      }

      @Override
      public void push(String p_372926_) {
         this.first.push(p_372926_);
         this.second.push(p_372926_);
      }

      @Override
      public void push(Supplier<String> p_372910_) {
         this.first.push(p_372910_);
         this.second.push(p_372910_);
      }

      @Override
      public void markForCharting(MetricCategory p_373064_) {
         this.first.markForCharting(p_373064_);
         this.second.markForCharting(p_373064_);
      }

      @Override
      public void pop() {
         this.first.pop();
         this.second.pop();
      }

      @Override
      public void popPush(String p_372955_) {
         this.first.popPush(p_372955_);
         this.second.popPush(p_372955_);
      }

      @Override
      public void popPush(Supplier<String> p_372989_) {
         this.first.popPush(p_372989_);
         this.second.popPush(p_372989_);
      }

      @Override
      public void incrementCounter(String p_372854_, int p_372995_) {
         this.first.incrementCounter(p_372854_, p_372995_);
         this.second.incrementCounter(p_372854_, p_372995_);
      }

      @Override
      public void incrementCounter(Supplier<String> p_373119_, int p_372889_) {
         this.first.incrementCounter(p_373119_, p_372889_);
         this.second.incrementCounter(p_373119_, p_372889_);
      }

      @Override
      public void addZoneText(String p_373071_) {
         this.first.addZoneText(p_373071_);
         this.second.addZoneText(p_373071_);
      }

      @Override
      public void addZoneValue(long p_373118_) {
         this.first.addZoneValue(p_373118_);
         this.second.addZoneValue(p_373118_);
      }

      @Override
      public void setZoneColor(int p_373025_) {
         this.first.setZoneColor(p_373025_);
         this.second.setZoneColor(p_373025_);
      }
   }
}

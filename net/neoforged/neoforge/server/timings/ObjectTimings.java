package net.neoforged.neoforge.server.timings;

import java.lang.ref.WeakReference;

public class ObjectTimings<T> {
   private WeakReference<T> object;
   private int[] rawTimingData;

   public ObjectTimings(T object, int[] rawTimingData) {
      this.object = new WeakReference<>(object);
      this.rawTimingData = rawTimingData;
   }

   public WeakReference<T> getObject() {
      return this.object;
   }

   public double getAverageTimings() {
      double sum = 0.0;

      for (int data : this.rawTimingData) {
         sum += data;
      }

      return sum / this.rawTimingData.length;
   }
}

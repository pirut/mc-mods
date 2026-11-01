package net.neoforged.neoforge.registries.datamaps;

import org.jspecify.annotations.Nullable;

public interface IWithData<R> {
   @Nullable
   default <T> T getData(DataMapType<R, T> type) {
      return null;
   }
}

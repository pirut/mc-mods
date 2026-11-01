package net.neoforged.neoforge.registries.datamaps;

import org.jetbrains.annotations.Nullable;

public interface IWithData<R> {
   @Nullable
   default <T> T getData(DataMapType<R, T> type) {
      return null;
   }
}

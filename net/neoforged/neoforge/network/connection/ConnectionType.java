package net.neoforged.neoforge.network.connection;

public enum ConnectionType {
   NEOFORGE,
   OTHER;

   public boolean isOther() {
      return this == OTHER;
   }

   public boolean isNeoForge() {
      return this == NEOFORGE;
   }
}

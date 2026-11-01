package net.neoforged.neoforge.transfer;

import net.neoforged.neoforge.transfer.resource.Resource;

@FunctionalInterface
public interface IndexModifier<T extends Resource> {
   void set(int var1, T var2, int var3);
}

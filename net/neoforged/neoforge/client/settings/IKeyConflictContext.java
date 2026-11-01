package net.neoforged.neoforge.client.settings;

public interface IKeyConflictContext {
   boolean isActive();

   boolean conflicts(IKeyConflictContext var1);
}

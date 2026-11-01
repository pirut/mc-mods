package net.neoforged.neoforge.transfer.transaction;

public sealed interface TransactionContext permits Transaction {
   int depth();
}

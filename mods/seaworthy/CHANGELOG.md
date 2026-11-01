# Changelog

## Unreleased

- Floodwater is saved with the world, so a flooded ship is still flooded after a restart.
- The sea only refills under a ship the first time it is seen, and big ships are done over several ticks.
- `/seaworthy status` shows how flooded each compartment is and how many leaks it has.
- Floodwater lies flat in the world when a ship leans, and shifts to the low side, so a part-flooded ship lists further.
- Holes taking on water show spray or bubbles, and you can hear them.
- A running Create mechanical pump empties the compartment it sits in.
- Wearing Create's Engineer's Goggles and looking at a ship shows how flooded it is.

## 0.1.0-alpha.1

- First version. Sealed hulls keep the sea out: no water, no swimming and no underwater fog inside, on any Sable
  ship (Create Aeronautics included).
- Open-topped hulls stay dry as long as their rim is above the water.
- Dry air below the waterline lifts the ship, so big ships float.
- Holes below the waterline flood the compartment behind them over time, faster for bigger and deeper holes, and
  drain again when the ship is lifted out. Bulkheads keep the water in one compartment until it tops them.
- NeoForge and Fabric.

# Changelog

## 0.1.0-alpha.3

- `ClientHooks.livingLayer`: add a render layer to every mob renderer (players excluded), on both loaders.

## 0.1.0-alpha.2

- `ServerEvents.onUseItem`: runs on the server when a player right-clicks with an item, before the item's own use,
  on both loaders. A listener can handle the use so the item's own use is skipped.
- `Net`: small tag messages between server and client on named channels. Channels are optional, so players without
  the mod can still join; `Net.canSend` says whether a player's client listens.
- `ClientHooks.key`: a rebindable key that runs an action when pressed, on both loaders.

## 0.1.0-alpha.1

First alpha: loader info (`Loader`), server events on both loaders (ticks, start and stop, entities loading, deaths,
damage, commands), title cards (`Titles`), world save data (`Stores`) and spawn spots (`Spots`).

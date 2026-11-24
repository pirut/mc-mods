# Changelog

## 0.1.0-beta.2

- **Camps get built whether or not you stay near.** Up to beta.1 a settlement was only built once every player was
  past a standoff distance (about 270 blocks at the default view distance), so a pack whose home you stayed around
  never got its camp or stronghold, though the Ledger said it had one.
- **You can watch them build.** While you can see the site, a crew of the pack's own mobs puts its new camp, upgrade or
  repair up block by block, a few blocks a second. While nobody can, it is built off screen, quickly, on ground loaded
  just for the build and let go after. It's one saved build either way: it carries on from the same step when you come
  into view, leave, or the server restarts.
- On the Earth, a site nobody can see has its cubes asked for, and with Earth Platform build 78 or later kept loaded,
  just for its build. Nothing is ever written to ground that isn't loaded, or to the Earth's void air.
- Killing a pack's builders stalls its build until the pack sends a new crew, about five minutes later. Its Brutes
  remember who killed their builders and say so.
- The Ledger, `/bloodfeud packs` and the maps say how far along a settlement is: "Camp, being built (40%)", or "Camp
  (not built yet)" before a pack has picked its site.
- New setting `campBlocksPerMinute`: each builder's pace (default 60).
- Builders never replace a block a player put in the site, and stashes are filled as they are placed.

## 0.1.0-beta.1

- Blood Feud is now in beta. This release has the same code as 0.1.0-alpha.6; see its notes below.

## 0.1.0-alpha.6

- **Works in Earth Platform worlds.** Packs find ground and build their camps, outposts, strongholds and towns at
  real heights, from below sea level to the high mountains, with or without Sable. Earth Platform stays optional.
- Fixed Earth servers freezing, and being stopped by the watchdog, while a pack far from any player looked for a camp
  site: it waited for map downloads on the server thread.
- On the Earth, player builds and containers are protected at every height and while no one is near to keep them
  loaded. A settlement only counts as built once its blocks are really in the world.
- Settlements that older versions recorded on the Earth are let go, because some were never built: those packs find
  a new site, and whatever stands at the old one is left alone.
- On the Earth, bells scare Brutes, the spawn safe radius follows where new players arrive, and new packs are named
  after the biome they stand in.
- If Blood Feud can't work with the installed Earth Platform, it says so once in the log and builds nothing there.
- Fixed camps writing over player blocks that stand in the camp's own places, in every dimension: bricks where a
  campfire was are no longer turned back into a campfire by a ruin, repair or upgrade, and the rest of the camp still
  ruins and repairs around them.
- Fixed camp stash chests always being empty: their loot was wiped as they were named. A repair still never refills a
  stash that was emptied.

## 0.1.0-alpha.5

- Automatic settlement changes (new camps, tier upgrades, ruinations, repairs, abandoned-site crumbling, and faction banner swaps) now only happen off-screen while players are outside view/standoff range, preventing structures from popping in front of players.
- In-flight automatic settlement builds immediately pause if a player approaches and seamlessly finish once the player leaves.
- Off-screen settlement updates use brief, auto-expiring, concurrency-capped chunk tickets that are released immediately upon completion, pause, cancellation, or shutdown.
- Manual operator commands (`/bloodfeud camp ...`, `upgrade`, `ruin`) continue to apply immediately on demand.

## 0.1.0-alpha.4

- **Growing gang camps and towns.** Packs now build physical settlements at their home and expand them as they grow: **Camp -> Outpost -> Stronghold -> Town** (crowning a King unlocks a Town). Each of the six mob families builds in its own vanilla block palette, with tents, watchtowers, boss halls, fortified walls, gatehouses, tavern, jail, war forge, smuggler stalls, loot stashes, and garrison patrols. Settlements build incrementally only in loaded chunks and protect player builds and containers.
- **Ruined settlements and recovery.** All four tiers have ruined forms (**Ruined Camp / Outpost / Stronghold / Town**) when a pack's numbers are beaten down or when a pack is wiped out, and packs automatically repair their home if they rebuild.
- **Toggleable.** Set `buildCamps=false` in `config/bloodfeud.properties` or run `/bloodfeud camps off` in-game to return to classic Blood Feud without physical settlements.
- **Operator camp commands.** Spawn, upgrade, or ruin camps on demand with `/bloodfeud camp <family> [camp|outpost|stronghold|town] [ruined]`, `/bloodfeud camp upgrade`, `/bloodfeud camp ruin`, and `/bloodfeud camps [on|off]`.
- Needs Pirut Lib 0.1.0-alpha.3.

## 0.1.0-alpha.3

- **Scars you can see on the body.** With Blood Feud on your client, a Brute that came back from death wears its
  scars: a charred face and chest (burned), a stitched gash (cut), arrow holes (pierced), claw marks (mauled), soot
  (blasted), algae (drowned) and bandages (broken). Painted for zombies, husks, drowned, skeletons, strays, wither
  skeletons, piglins, illagers, witches and spiders, mirrored on some Brutes so the same scar doesn't look the same
  on everyone. Players without the mod still see the particles.
- Needs Pirut Lib 0.1.0-alpha.3.

## 0.1.0-alpha.2

- **Ledger and Map screens.** With Blood Feud on your client, press J (or use the ledger) to open a full Ledger:
  Overview, Packs, Brutes and a Map tab. Pack pages show the banner, Boss and Brutes, grunts, rivals, your grudge
  and the pack's story so far. Brute pages show a 3D portrait wearing what it wears, traits you've found out (hover
  to read them), scars, what it carries and what it remembers about you. The map draws the land around you with
  every pack home you know, lines from Kings to their vassals, and a red X on each Brute hunting you; drag to pan,
  scroll to zoom, click to open a page. Players without the mod still get the book and the map item.
- **The Feud Ledger book**, given the first time a Brute finds you (or with `/bloodfeud book`), rewrites itself every
  time you open it. **Map of the Feuds** item (`/bloodfeud mapitem`) marks each pack home with a banner in its color.
- **They take your gear.** The Brute that kills you takes your best weapon and the armor you wore (never your
  helmet), wears it, and fights you with it. Kill it to get everything back. If it dies away from you, its pack's Boss
  takes the loot. Mobs that can't show gear (spiders, witches) take only the weapon. Only real weapons are taken
  (swords, axes, tridents, maces, and bows for skeletons). `takeGear` setting to turn it off.
- **Scars do something now**, and you can see them: Burned Brutes smoke and can set you on fire; Blasted ones shed
  ash, take half damage from explosions and hold their ground; Drowned ones drip water and can't drown; Cut ones
  bleed and hit 10% harder; Mauled ones bleed and shrug off wolves; Pierced ones take 25% less from arrows; Broken
  ones walk slower but are hard to knock back.
- **No two Brutes alike.** No two living Brutes share an epithet (new scar epithets, and made-up ones when the list
  runs dry). Each has its own size, and Bosses and Kings are bigger. Each wears its own armor trim pattern, in a
  material that matches its pack's color.
- Balance: a little less health and damage per level (a level 20 King has about 6.5 times a normal mob's health,
  was 7.5), spider and witch hides capped lower.
- Packs keep a short history (formed, risen, raided, challenged, crowned), saved with the world.
- `/bloodfeud scar <scar> <name>` for operators. `giveLedger` setting to turn the free ledger off.
- Needs Pirut Lib 0.1.0-alpha.2.

## 0.1.0-alpha.1

First alpha.

- Mobs that see you kill three of their kind and live, or that kill you, rise as named Brutes with a level, an
  epithet, traits you have to find out, and a memory of you.
- Brutes band together into packs of their own family (Rotten, Bones, Illagers, Nether), with a home named after
  where they formed, a color (dyed caps, and a banner on the Boss) and a Boss.
- Packs grow every night, raise new Brutes, change Bosses through challenges, raid rival packs, and swallow packs of
  their own kind to crown a King. Kill a King and its packs split.
- Brutes meet you with a title card, a boss bar and a line that remembers what happened between you. Traits change
  fights: immunities, instant-kill weaknesses, venom, theft, blinking, fears of fire, wolves, cats and bells.
- Killed Brutes can come back scarred unless the kill was final (a critical hit with a sword or axe, or lava).
- Far-away packs sleep and catch up later; grudges follow you across the world; at most 64 packs per world.
- Six families: Rotten, Bones, Illagers, Nether, Spiders (Nests) and Witches (Covens).
- Relics: Bosses, Kings and Brutes of level 10+ drop a named weapon stronger than any enchanting table makes,
  shaped by its owner's traits (poison, slowness, Looting, Fire Aspect, speed, health).
- Over 400 lines and 160 epithets that bring up your shared history (the place, the days, the stolen item, the
  scar, the King), with no repeats until a Brute runs out, plus a word or two mid-fight. Names built from syllables.
- A short cooldown between rises, and nothing rises on Peaceful.
- `/bloodfeud`, `/bloodfeud brute <name>` and `/bloodfeud nemesis`, plus operator commands. Server only: players
  don't need the mod.

![Blood Feud](brand/bloodfeud-banner-1920x480.png)

# Blood Feud

Mobs that remember you. Kill a few zombies while their friends watch and one of them walks away with a name, a
grudge and a level. Leave it alone and it gathers others of its kind into a pack, with a home, a color and a Boss.
Packs grow every night, fight the packs next to them, and the strongest swallow their own kind and crown a King.

Everything grows out of your fights. Nothing is planned ahead: every part of a big world grows its own packs and
rivalries around the places where players fight.

## How a nobody becomes somebody

- **It saw you kill its friends.** A mob that watches you kill three of its kind (within 16 blocks) and lives
  through the fight walks off as a named **Brute**: "You killed my brothers. I'm still here."
- **It killed you.** The mob that kills you becomes a Brute on the spot, and you are its **Nemesis**. A Brute that
  kills you again gets stronger.
- **It got away.** A Brute you run from gets stronger for driving you off.

Brutes have a level, a name and an epithet, and a few traits: things it can't be hurt by (Arrowproof, Fireproof,
Unshakable, Watchful), things it is good at (Venom Blade, Bonebreaker, Thief, Blink, Berserker, Regenerating,
Packcaller, Shieldbearer, Night Stalker), ways to kill it fast (Headshot, Unguarded, Beast-Bait, Hallowed Bane, Glass
Jaw) and things it runs from (fire, wolves, cats, bells). You don't know any of them at first. You find them out by
running into them.

## Packs

Mobs only band with their own family: the **Rotten** (zombies, husks, drowned, zombie villagers), the **Bones**
(skeletons, strays, bogged), the **Illagers** (pillagers, vindicators) and the **Nether** (piglin brutes, wither
skeletons), the **Spiders** (spiders and cave spiders, who live in Nests) and the **Witches** (who gather in
Covens). Add modded mobs to a family with the entity tags `bloodfeud:rotten`, `bloodfeud:bones`,
`bloodfeud:illagers`, `bloodfeud:nether`, `bloodfeud:spiders` and `bloodfeud:witches`. Spiders and witches can't
show gear, so they get a tougher hide instead of armor and caps.

- At night a lone Brute pulls in plain mobs of its kind. When two Brutes of the same family meet, the stronger one
  becomes the **Boss** and the other its first Brute. That's a pack.
- A pack is named after the place it formed (the Mossy Bog Pack, the Cold Ridge Pack) and has a color. Its members
  wear leather caps in that color and the Boss wears the pack's banner.
- Packs grow every night, and faster for every player they kill. New Brutes rise inside the pack. A Brute that gets
  far stronger than its Boss challenges it.
- Packs with homes within about 1,500 blocks are rivals and raid each other. A pack that beats a rival of its own
  family swallows it, and its Boss becomes a **King**. Kill a King and its packs split apart again.
- Go near a pack's home and it comes out to meet you: the Boss, a couple of its Brutes and some grunts.

## Relics

Kill a Boss, a King or a Brute of level 10 or more and it drops its **relic**: a weapon named after it ("Vosk's
Blade", "Grak's Crown Cleaver") that is stronger than anything an enchanting table makes. Sharpness up to VIII,
Unbreaking V, Mending, extra damage and attack speed, and more for what the owner was: poison from a Venom Blade,
slowness from a Bonebreaker, Looting from a Thief, Fire Aspect from a burned one, extra speed or health from others.
Skeletons and pillagers drop bows and crossbows; spiders drop a poisoned Fang; witches drop a Hexblade that weakens
what it cuts.

## They talk

Brutes talk about what happened between you: the time it killed you, the item it stole, how many days you've been
running, the scar you gave it, the King it serves. Over 400 lines and 160 epithets, and a Brute never repeats itself
until it has run through what it has to say. Names are built from syllables, so you'll meet tens of thousands of
different ones. All of it lives in `bloodfeud/lines.txt` inside the jar.

## Dying, coming back, and scars

A Brute you kill can come back a day or so later, scarred by how it died (burned, broken, blasted, pierced, drowned,
cut or mauled), sometimes with a new trait or fear from it and a new epithet, and it comes looking for you. A kill is
**final** if the killing blow was a critical hit with a sword or axe, or it died in lava. A Brute that has come back
three times stays dead.

Sleeping and dying move the feud along: each night, packs grow, fight and change hands.

## Big worlds

Packs stay within about 500 blocks of home and only feud with packs nearby, so each region grows its own. A pack
with no player within 2,000 blocks sleeps: it's a small record on disk, with no mobs and no chunks loaded. When
someone comes back it catches up on the nights it missed. A Brute that swore revenge follows you across the world at
about 2,000 blocks a day and turns up when it catches up. A world holds at most 64 packs; past that, the oldest
far-away pack no one has met fades away.

## Commands

- `/bloodfeud` (or `/bloodfeud packs`): the packs you've met around you, who leads them, and which way their home is.
- `/bloodfeud brute <name>`: a Brute's page: rank, level, what you know about it, scars, and your history together.
- `/bloodfeud nemesis`: who's after you.
- Operators: `/bloodfeud rise <family>`, `/bloodfeud night`, `/bloodfeud all`, `/bloodfeud forget everything`.

## Server only

Blood Feud runs on the server. Players don't need it installed: names, title cards, boss bars and lines all use
vanilla. Settings are in `config/bloodfeud.properties`.

NeoForge and Fabric, Minecraft 1.21.1. Blood Feud is inspired by the nemesis-style rivals of other games; its names,
lines and rules are its own.

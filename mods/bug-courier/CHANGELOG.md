# Changelog

## 0.2.0

- **Now on every main Minecraft version from 1.21.1 to 26.3**, on NeoForge and Fabric: 1.21.1, 1.21.4, 1.21.5,
  1.21.8, 1.21.10 (the Fabric jar also takes 1.21.9), 1.21.11, 26.1.2 (the Fabric jar also takes 26.1 and
  26.1.1), 26.2 and 26.3. Each version has its own jar.
- From 1.21.9 on, the "Open Bug Courier" key is under Miscellaneous in the controls, since Minecraft now needs
  key categories registered.
- The GPU line in reports names the graphics backend and driver, as newer Minecraft versions report them.

## 0.1.0

First full release, for Minecraft 1.21.1 on NeoForge 21.1.256 or later and on Fabric (with Fabric API).

- Four-part mod versions such as `1.0.0.0` no longer show up as `<ip>` in the mod list.
- Hiding the player's name only hides it as a whole word, so a player called "Dev" no longer blanks out every
  "device" in the log.
- The zip is saved even when a mod attaches a file with the same name as another one (such as its own
  `latest.log`); the second copy gets a number.
- `/bugreport <modid>` says so when that mod doesn't take reports, instead of opening the screen on another mod.
- Marked client-side only on NeoForge, so the server list no longer flags servers that don't have it.
- The preview's "more lines" note gives the right count and can be translated.

## 0.1.0-alpha.2

- **Now on Fabric too** (needs Fabric API). It reads each mod's `contact.issues` link, and `bugcourier_*`
  keys from the `"custom"` block of `fabric.mod.json`. Fabric API's own modules are reported as Fabric API.
- The environment table no longer hides four-part Java versions (such as 21.0.12.1) as IP addresses.
- Its small state file is now written whole, so a crash mid-write can't leave half a file.

## 0.1.0-alpha.1

- First version. Players report a bug to any installed mod from the pause menu (**Report a mod bug**),
  `/bugreport [modid]`, a key (unbound by default), or the prompt after a crash.
- The preview shows exactly what will be sent, with a checkbox for each part. The mod's issue page opens
  with the report filled in, the full logs go to the clipboard, and a zip of them is saved.
- Works with GitHub, GitLab, Codeberg, Gitea and Forgejo. Launcher tokens, home folder names, emails and IP
  addresses are hidden; the player's name and UUID only go in when they tick the box.
- Mods opt in with nothing (their `issueTrackerURL`), a few `modproperties` keys, or the Java API.
- Every screen fits on screen at any window size.

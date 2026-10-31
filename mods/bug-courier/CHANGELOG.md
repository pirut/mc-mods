# Changelog

## 0.1.0

First full release, for NeoForge 21.1.256 or later on Minecraft 1.21.1.

- Four-part mod versions such as `1.0.0.0` no longer show up as `<ip>` in the mod list and environment table.
- Hiding the player's name only hides it as a whole word, so a player called "Dev" no longer blanks out every
  "device" in the log.
- The zip is saved even when a mod attaches a file with the same name as another one (such as its own
  `latest.log`); the second copy gets a number.
- `/bugreport <modid>` says so when that mod doesn't take reports, instead of opening the screen on another mod.
- Marked client-side only, so the server list no longer flags servers that don't have it.
- The preview's "more lines" note gives the right count and can be translated.

## 0.1.0-alpha.1

- First version. Players report a bug to any installed mod from the pause menu (**Report a mod bug**),
  `/bugreport [modid]`, a key (unbound by default), or the prompt after a crash.
- The preview shows exactly what will be sent, with a checkbox for each part. The mod's issue page opens
  with the report filled in, the full logs go to the clipboard, and a zip of them is saved.
- Works with GitHub, GitLab, Codeberg, Gitea and Forgejo. Launcher tokens, home folder names, emails and IP
  addresses are hidden; the player's name and UUID only go in when they tick the box.
- Mods opt in with nothing (their `issueTrackerURL`), a few `modproperties` keys, or the Java API.
- Every screen fits on screen at any window size.

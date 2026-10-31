# Changelog

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

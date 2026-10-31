# Changelog

## 0.1.0-alpha.1

- First version. Players report a bug to any installed mod from the pause menu (**Report a mod bug**),
  `/bugreport [modid]`, a key (unbound by default), or the prompt after a crash.
- The preview shows exactly what will be sent, with a checkbox for each part. The mod's issue page opens
  with the report filled in, the full logs go to the clipboard, and a zip of them is saved.
- Works with GitHub, GitLab, Codeberg, Gitea and Forgejo. Launcher tokens, home folder names, emails and IP
  addresses are hidden; the player's name and UUID only go in when they tick the box.
- Mods opt in with nothing (their `issueTrackerURL`), a few `modproperties` keys, or the Java API.
- Every screen fits on screen at any window size.

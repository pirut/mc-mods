# Bug Courier

A small NeoForge library (Fabric to follow) that gives players a clear in-game way to report bugs to the mods they play.

The player picks the mod, writes what happened, and sees exactly what will be sent. Then the mod's issue page
opens in their browser with the report filled in, the full logs are copied to the clipboard, and a zip of the
logs is saved for them to drag into the issue. Nothing is sent without the player pressing Submit on the
tracker's own page, and no token or server is involved.

Works with GitHub, GitLab, Codeberg, Gitea and Forgejo. Any other link opens as is, with the report on the
clipboard to paste.

## Where players find it

- **Report a mod bug** in the pause menu
- `/bugreport` or `/bugreport <modid>`
- A key, unbound until the player picks one in Controls
- After a crash, the next start asks "The game crashed last time. Report it?" and picks the mod the crash
  report blames

## For mod authors

### No code

If your `neoforge.mods.toml` has an `issueTrackerURL`, Bug Courier already offers reports for your mod. That
covers every installed mod, even ones that don't know Bug Courier exists.

### A few lines of config

Add labels, or point reports somewhere other than `issueTrackerURL`:

```toml
[modproperties.yourmodid]
bugcourier_tracker = "github:you/your-mod"   # or gitlab:group/project, codeberg:you/mod, or a link
bugcourier_labels = "bug,player-report"
# bugcourier_disabled = true                 # leave your mod out of the list
```

### Java API

For your own "Report a bug" button, or extra context and files. Keep the dependency optional:

```java
if (ModList.get().isLoaded("bugcourier")) {
    BugReports.register("yourmodid", cfg -> cfg
            .tracker("github:you/your-mod")
            .labels("bug", "player-report")
            .context("Render distance", () -> Integer.toString(Minecraft.getInstance().options.renderDistance().get()))
            .attach("yourmod.toml", "Config", FMLPaths.CONFIGDIR.get().resolve("yourmod.toml")));
}

// From a settings screen button:
BugReports.open("yourmodid");
```

## What a report holds

Each part has its own checkbox on the preview screen:

- the player's title and description
- the game log (the last 256 KB of `latest.log`)
- the newest crash report: ticked when the player came from the crash prompt, unticked otherwise
- the mod list with versions
- versions of the game, NeoForge, Java and the OS, the CPU and GPU, and memory
- whatever the mod adds through the API

Before the preview, Bug Courier hides launcher access tokens and session ids, the user name in home folder
paths, email addresses and IP addresses. The player's Minecraft name and UUID are hidden unless they tick
"Include my player name".

A mod gets at most one report every 5 minutes, and the description needs at least 20 characters. The issue
title carries a short crash tag such as `(crash:3f9a1c)` that stays the same for the same crash, so duplicates
are easy to search for.

## Building

```
./gradlew :bug-courier:build                 # from the mc-mods root; the core package's tests run on plain JUnit
./gradlew :bug-courier:neoforge:runClient
```

## License

LGPL-3.0-only. See [`COPYING`](../../COPYING) and [`COPYING.LESSER`](../../COPYING.LESSER).

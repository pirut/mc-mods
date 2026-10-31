# Bug Courier

**Players report bugs from inside the game, straight to the mod that has them.**

Bug Courier adds a **Report a mod bug** button to the pause menu. The player picks the mod, writes what
happened, and sees exactly what will be sent. Then that mod's issue page opens in their browser with the report
already filled in: versions, hardware, log excerpts and a crash tag. The player reads it over and presses Submit
on the tracker's own page.

You need no account, token or server for any of this, and nothing leaves the computer until the player
submits it themselves.

- **Minecraft** 1.21.1
- **Loader** NeoForge 21.1.256 or later (Fabric is planned)
- **Side** client only. Servers don't need it.
- **Needs** nothing else

[Download](https://github.com/pirut/mc-mods/releases?q=bug-courier) ·
[Changelog](CHANGELOG.md) ·
[Report a problem with Bug Courier](https://github.com/pirut/mc-mods/issues)

---

## Why

Most bug reports a mod author gets say "it crashed" and nothing else. Most players don't know where logs live,
which mod to blame, or where that mod takes issues. Bug Courier fixes both sides of that:

- **Players** get one button that works for every mod they have installed, and they see what they're sharing
  before they share it.
- **Mod authors** get reports with the mod version in the title, the environment in a table, the end of the log
  and the top of the crash report. The full logs come as a zip the player drags in. You don't have to write any
  code for this.

## For players

### Install

Put the jar in your `mods` folder. That's all. Bug Courier runs only on your own computer, so it works on any
server, whether or not the server has it.

### Report a bug

You can open the report screen four ways:

| Where | How |
| --- | --- |
| Pause menu | **Report a mod bug** button, in a free corner of the screen |
| Chat | `/bugreport`, or `/bugreport <modid>` to go straight to one mod (Tab completes the id) |
| Key | **Report a bug** under *Controls > Bug Courier*. It has no key until you pick one. |
| After a crash | The next time the game starts, it asks *"The game crashed last time. Report it?"* and picks the mod the crash report blames |

Then:

1. **Write it up.** Pick the mod, give the report a short title, and say what you did, what you expected, and
   what happened instead (at least 20 characters). Under the box it shows where the report will go.
2. **Check the preview.** You see the whole report exactly as it will be sent. Each file has a checkbox, so
   untick anything you'd rather keep to yourself.
3. **Open the issue page.** Your browser opens the mod's tracker with the report filled in. The full report is
   on your clipboard, and a zip of the logs is saved in the `bug-reports` folder of your game directory. Drag
   the zip into the issue if you like, then press **Submit**.

If the mod's tracker can't be filled in through a link (a Discord invite or a forum, say), the page just opens.
The report is on your clipboard, so paste it there.

### Your privacy

Before you see the preview, Bug Courier hides:

- launcher access tokens, session ids, passwords, API keys and other secrets that end up in logs
- your user name in folder paths (`C:\Users\<user>\...`, `/home/<user>/...`)
- email addresses
- IP addresses, except `127.0.0.1` and `0.0.0.0`. Mod versions that only look like addresses, such as
  `1.0.0.0`, stay as they are.
- your Minecraft name and UUID, unless you tick **Include my player name**

Nothing is uploaded by Bug Courier. Your browser only gets a link to the tracker's new-issue page, and the issue
exists only once you press Submit there.

### Good to know

- Each crash is offered once. To never be asked after a crash, set `neverAskAboutCrashes=true` in
  `config/bugcourier-state.properties`.
- You can send one report per mod every 5 minutes, so a stuck key can't flood a tracker.
- Minecraft and NeoForge themselves aren't in the list. Their bugs belong with Mojang and NeoForge.
- A mod only shows up if it says where it takes issues. If yours is missing, the mod hasn't set an issue tracker.

## What a report holds

The text you write and a table of facts always go in:

- the mod and its version, Minecraft, NeoForge, Bug Courier and how many mods are installed
- OS, Java, CPU, GPU and OpenGL version, and how much memory Java may use
- any short facts the mod adds itself, such as a render setting

These files each have a checkbox in the preview:

| File | What | Ticked by default |
| --- | --- | --- |
| `latest.log` | the last 256 KB of the game log | yes |
| `crash-report.txt` | the newest crash report from the last 3 days | only when you came from the crash prompt |
| `mods.txt` | every installed mod with its version and jar name | yes |
| (the mod's own) | config or other files the mod asks for, up to 128 KB each | yes |

The issue page gets the description, the table, the first lines of the crash report and the last lines of the
log. That's as much as a link can hold, about 8 KB. Everything you ticked is in the clipboard copy and the zip.

The issue title starts with the mod's version and, for crashes, ends with a short tag such as `(crash:3f9a1c)`.
The tag is the same every time the same crash happens, so the same crash is easy to find on the tracker.

## For mod authors

You can do nothing, add two lines of config, or use the Java API. Bug Courier looks at them in this order:
what you registered in code, then your `modproperties`, then your `issueTrackerURL`.

### Nothing: `issueTrackerURL`

If your `neoforge.mods.toml` has an `issueTrackerURL`, players can already report to you. That works for every
installed mod, including ones that have never heard of Bug Courier.

### A few lines of config

Add these to `neoforge.mods.toml` to set labels, send reports somewhere other than `issueTrackerURL`, or keep
your mod out of the list:

```toml
[modproperties.yourmodid]
bugcourier_tracker = "github:you/your-mod"
bugcourier_labels = "bug,player-report"
# bugcourier_disabled = true
```

### Where reports can go

`bugcourier_tracker`, `issueTrackerURL` and `tracker(...)` in the API all accept:

| Tracker | Short form | Link that's recognized | Filled in |
| --- | --- | --- | --- |
| GitHub | `github:owner/repo` | `https://github.com/owner/repo/...` | title, body, labels |
| GitLab | `gitlab:group/project`, or `gitlab:host/group/project` for a self-hosted one | `https://gitlab.*/group/sub/project/...` | title, body |
| Codeberg | `codeberg:owner/repo` | `https://codeberg.org/owner/repo/...` | title, body |
| Gitea, Forgejo | `gitea:host/owner/repo`, `forgejo:host/owner/repo` | `https://gitea.*/...`, `https://forgejo.*/...`, `https://git.*/...` | title, body |
| Anything else | | any other `http(s)` link | nothing: the page opens and the player pastes |

GitHub quietly drops labels that the person filing the issue isn't allowed to set, so asking for labels is
always safe.

### Java API

Use the API when you want extra facts or files in the report, or your own **Report a bug** button. Compile against the jar from the
[releases](https://github.com/pirut/mc-mods/releases?q=bug-courier) as `compileOnly`, and only call it when
Bug Courier is installed:

```java
if (ModList.get().isLoaded("bugcourier")) {
    BugReports.register("yourmodid", cfg -> cfg
            .tracker("github:you/your-mod")
            .labels("bug", "player-report")
            .context("Render distance", () -> Integer.toString(Minecraft.getInstance().options.renderDistance().get()))
            .attach("yourmod.toml", "Config", FMLPaths.CONFIGDIR.get().resolve("yourmod.toml")));
}

// From a button on your settings screen:
BugReports.open("yourmodid");
```

| Method | What it does |
| --- | --- |
| `BugReports.register(modId, cfg -> ...)` | Sets how your mod takes reports. Safe to call from your mod's constructor, on either side. |
| `BugReports.open(modId)` | Opens the report screen with your mod picked. Does nothing on a dedicated server. |
| `.tracker(String)` / `.tracker(Tracker)` | Where reports go. Throws if it can't read the string. |
| `.labels(String...)` | Labels to ask for on new issues (GitHub only). |
| `.context(label, Supplier<String>)` | A row in the report's table. It's called on the render thread when the preview opens, so keep it quick and leave anything personal out. |
| `.attach(fileName, label, Path)` | A file the player can choose to send. Bug Courier reads up to the last 128 KB of it and redacts it like the logs. |

For any other tracker whose new-issue page can be filled in through its link, use
`Trackers.template("My board", "https://example.com/new?title={title}&body={body}")`. `{title}`, `{body}` and
`{labels}` are replaced with URL-encoded text.

The stable API is the `io.bugcourier.api` package plus `Tracker` and `Trackers` in `io.bugcourier.core`.
Anything else may change between versions.

## Building

From the root of this repo, with Java 21:

```sh
./gradlew :bug-courier:build                # jar in mods/bug-courier/neoforge/build/libs
./gradlew :bug-courier:neoforge:runClient   # a dev game with the mod loaded
```

The `io.bugcourier.core` package has no Minecraft classes, so its tests run on plain JUnit.

## License

LGPL-3.0-only. See [`COPYING`](../../COPYING) and [`COPYING.LESSER`](../../COPYING.LESSER).

# IceBoatTimeTrial

Bukkit plugin for ice boat time trials with WorldEdit-powered track editing and SQLite persistence.

## Features
- Requires WorldEdit and FancyHolograms (`depend` in plugin metadata)
- UUID-based track identity (safe rename without reference rewrites)
- Boat-only timing with three sectors
- Start/finish + exactly two checkpoints
- Direction checks, overlap policy, and anti-trigger debounce
- Live bossbar with sector delta
- Holograms: top times/players, sector variants, recent PBs, track info, improved/consistent
- Player settings persisted in SQLite (bossbar, message verbosity)
- In-game settings GUI (`/ibt settings`) and track browser/details GUI (`/ibt tracks`)
- Dedicated track editor mode (`/icetrackeditor`, alias `/ite`)
- Editor lock model: one editor per track, one active track per player
- Editor preview particles (edges only + direction arrows, player-only visibility)
- Reusable GUI framework foundations for future menu features

## Storage
- Static settings: `src/main/resources/config.yml`
- Runtime/domain data: SQLite at `plugins/IceBoatTimeTrial/iceboat.db`
- Track schema migrates legacy numeric IDs to UUIDs automatically on startup

## Commands
### Player / runtime commands (`/ibt`)
- `/ibt best [track]`
- `/ibt top <times|players|improved|consistent> <track> [day|week|month|all] [limit]`
- `/ibt top active [day|week|month|all] [limit]`
- `/ibt stats [player] [day|week|month|all]`
- `/ibt settings`
- `/ibt tracks`
- `/ibt hologram <place|remove> <track> <type>`
- `/ibt reload`

### Track editor commands (`/ite`)
- `/ite create <name>`
- `/ite edit <trackname>`
- `/ite stopedit [trackname]`
- `/ite setstart`
- `/ite setcp1`
- `/ite setcp2`
- `/ite setdir <start|cp1|cp2> <x> <y> <z> [threshold]`
- `/ite rename <newname>`
- `/ite status`
- `/ite validate`
- `/ite forceedit <trackname>` (admin)
- `/ite forcestop <trackname>` (admin)

## Build and test
```bash
mvn clean test
mvn clean package
```

Build output jar is under `target/`.

## GUI framework (phase 1)
- Core classes under `src/main/java/com/hegi64/iceBoatTimeTrial/gui/`
- Includes:
  - menu abstraction (`GuiMenu`)
  - button/action model (`GuiButton`, `GuiAction`)
  - session tracking (`GuiSessionService`)
  - listener routing (`GuiListener`)
  - registry (`GuiRegistry`)
  - pagination utility (`Pagination`)

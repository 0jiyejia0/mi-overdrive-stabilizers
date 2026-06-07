# MI Overdrive Stabilizers

MI Overdrive Stabilizers is a NeoForge addon for
[Modern Industrialization](https://modrinth.com/mod/modern-industrialization).
It adds overdrive-slot modules that keep electric machines from losing efficiency
when they idle or switch recipes.

## Features

- Adds three overdrive stabilizers for electric machines:
  - Analog Overdrive Stabilizer: keeps half of the stored efficiency after recipe changes.
  - Electronic Overdrive Stabilizer: keeps all stored efficiency after recipe changes.
  - Digital Overdrive Stabilizer: keeps the machine at maximum efficiency while powered.
- Uses the existing Modern Industrialization overdrive module slot.
- Matches MI-style item tooltips: hold Shift to show details.
- Adds configurable retention values in `run/config/mi_overclock_addon-common.toml`
  during development runs.

## Versions

- Minecraft: 1.21.1
- Loader: NeoForge 21.1.233
- Java: 21
- Modern Industrialization: 2.4.3
- GuideME: 21.1.16

## Development

Import this folder as a Gradle project in your IDE.

Useful tasks:

```powershell
.\gradlew build
.\gradlew runClient
.\gradlew runServer
```

The first Gradle sync may download Minecraft, NeoForge, mappings, and mod dependencies.

## Build

```powershell
.\gradlew build
```

The built jar will be written under `build/libs/`.

## Repository

Recommended GitHub repository name:

```text
mi-overdrive-stabilizers
```

The internal mod id is still `mi_overclock_addon` to avoid changing registry IDs.

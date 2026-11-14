# Crosstalk

**Proximity voice chat, sound physics and working radios for Minecraft Java Edition.**

Talk to the people near you, hear them muffled through walls and echoing in caves, and grab a handheld radio
when they wander too far.

> [!WARNING]
> **Crosstalk is in early development.** Nothing here is released yet. The features below are the plan, built
> in three phases. Ticked items are done and tested. Everything else is still to come.

---

## Features

### Phase 1: Voice chat
- [ ] Proximity voice: you only hear players within range, and they get quieter the further away they are
- [ ] Proper 3D audio, with left/right panning that follows where you're facing
- [ ] Voice activation (adjustable threshold) or push-to-talk on a key you can rebind
- [ ] Noise gate to cut background hiss
- [ ] Opus audio compression, so it's light on bandwidth
- [ ] Runs over UDP, and falls back to the normal game connection automatically if UDP is blocked
- [ ] Whisper and shout ranges
- [ ] Settings screen: mic picker, input/output volume, threshold, push-to-talk key, mute, per-player volume and a mic test

### Phase 2: Sound physics
- [ ] Walls muffle sound depending on the material: stone and other thick blocks a lot, doors less, glass only lightly
- [ ] Reverb that fits the space you're in: tight in a small room, big and echoey in a cave, dry out in an open field
- [ ] Works on voices **and** normal game sounds, with a separate toggle for each
- [ ] Built to be light: capped raycasts, cached results, and the heavy work kept off the render thread

### Phase 3: Radios
- [ ] **Handheld radio:** tune it to a frequency (1–99) and hold to transmit. Anyone tuned to the same frequency hears you, up to a configurable range
- [ ] Radio audio sounds like a radio: narrow band, a bit of static, and more static the further away you are
- [ ] **Speaker block:** plays a frequency out loud into the world, so nearby players hear it in 3D (with sound physics)
- [ ] Per-radio power and volume, plus a GUI for tuning
- [ ] Crafting recipes and its own creative tab
- [ ] An extension API so new devices (megaphone, intercom and so on) can be added without changing the core code

---

## Requirements

| | |
|---|---|
| Minecraft | Java Edition **1.20.1** |
| Mod loader | **Forge 47.x** |
| Java | **17** |
| Where it's installed | **Both** the client and the server |

---

## Installing

### Players
1. Install Forge 1.20.1.
2. Put the `Crosstalk-x.x.x.jar` file in your `mods` folder.
3. Start the game, open **Options → Voice Chat**, choose your mic and run the mic test.

> On macOS, Java needs microphone permission. If nobody can hear you, check
> **System Settings → Privacy & Security → Microphone**.

### Server owners
1. Put the same jar in the server's `mods` folder.
2. Start the server once so it generates `config/crosstalk-server.toml`.
3. **Open the voice UDP port** (see below) if you can. If you can't, Crosstalk still works over the normal game
   connection, just with a little more delay.

---

## Server config

Every value is set in `config/crosstalk-server.toml`. Nothing is hardcoded. The defaults below are the
planned ones and could change before release.

| Setting | What it does | Planned default |
|---|---|---|
| `voice_port` | UDP port for voice traffic | `24600` |
| `max_distance` | How far normal speech carries (blocks) | `48` |
| `whisper_distance` | Whisper range (blocks) | `8` |
| `shout_distance` | Shout range (blocks) | `96` |
| `disabled_dimensions` | Dimensions where voice is turned off | `[]` |
| `radio_max_range` | Maximum radio range (blocks) | `2000` |
| `require_mod` | Kick players who join without the mod, with a clear message | `false` |

---

## How it works

```
 Mic ─► noise gate / VAD ─► Opus encode ─► UDP (or game-connection fallback) ─► Server
                                                                                   │
                                     server works out who's in range and routes the audio
                                                                                   │
 Speakers ◄─ OpenAL 3D source ◄─ occlusion + reverb (EFX) ◄─ Opus decode ◄─────────┘
```

- **Capture, encoding and networking** each run on their own threads, never on the game's main thread.
- **The server decides who hears what.** Clients never send audio straight to each other.
- **Sound physics** casts rays between the sound and the listener, works out how much gets through based on
  the blocks in the way, and estimates the size of the space for reverb.

---

## Compatibility

- **A Crosstalk client on a server without Crosstalk:** voice simply switches off. Everything else works as normal.
- **A player without Crosstalk on a Crosstalk server:** Crosstalk adds blocks and items, so Forge requires the
  mod on the client. Players without it get a clear message telling them what to install.

---

## Building from source

```bash
git clone https://github.com/<your-username>/crosstalk.git
```

```bash
cd crosstalk
```

```bash
./gradlew build
```

The built jar ends up in `build/libs/`.

To test locally with two players, run the dev server and two clients, each with its own run folder and username.
A step-by-step guide will go here once Phase 1 is done.

---

## Third-party libraries

| Library | Used for | Licence |
|---|---|---|
| [Concentus](https://github.com/lostromb/concentus) | Opus encoding/decoding in pure Java | *To be confirmed before release* |

Crosstalk's own code is written from scratch.

---

## Licence

*To be decided.*

## Credits

Made by Phoenix.

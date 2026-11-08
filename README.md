# Sebastian Hili Horror

A Minecraft **Java Edition 1.20.1** mod for **Forge 47.x** starring Sebastian Hili, a polite, awkward,
unblinking boy in a green school blazer who would really, *really* like a penny.

You mine **zinc**, craft **pennies**, and try to live alongside him. He watches you from a distance. He
begs. He searches your chests. He asks you questions in chat, and he knows when you lie. And if anyone ever
burns a penny where he can see it, nobody in the world is safe.

Works in single-player and on multiplayer servers. All of his behaviour runs on the server.

---

## What's in the zip

| File | What it is |
|---|---|
| `SebastianHiliHorror-1.20.1-1.0.0.jar` | **The mod.** This is the only file you put in your `mods` folder. |
| `SebastianHili_skin.png` | Sebastian's 64×64 skin (classic/"wide" arms) in case you want to wear it yourself. |
| `SebastianHiliHorror_demo.mp4` | A video of the mod running. |
| `SebastianHili_BossFight.mp4` | A video of the whole boss fight. |
| `README.md` | This file. |
| `source/` | The full source project (Gradle). You don't need it to play. |

---

## 1. Getting the .jar out of the zip

**On Windows**
1. Find `SebastianHiliHorror.zip` (usually in your **Downloads** folder).
2. **Right-click** it and choose **Extract All…**
3. Click **Extract**. A new folder opens with the same name.
4. Inside it is `SebastianHiliHorror-1.20.1-1.0.0.jar`. That's the mod.

**On Mac**
1. Find `SebastianHiliHorror.zip` in **Finder** (usually in **Downloads**).
2. **Double-click** it. macOS unzips it into a folder next to it.
3. Open that folder. `SebastianHiliHorror-1.20.1-1.0.0.jar` is the mod.

> Don't double-click the `.jar` itself. It's not a program. It just gets copied into the mods folder (step 3).

## 2. Installing Forge (once)

1. Go to **https://files.minecraftforge.net**, pick **1.20.1** on the left, and download the **Installer**
   (the recommended version, 47.4.x. Any 47.x works).
2. Open the downloaded `forge-1.20.1-…-installer.jar`.
   - If it won't open, install Java from **https://adoptium.net** (the "Temurin 21" download is fine) and try again.
     On a Mac you may need to right-click it, choose **Open**, then confirm.
3. Leave **Install client** selected and click **OK**. Wait for "Successfully installed".
4. Open the normal **Minecraft Launcher**. You'll find a **forge** option in the version picker next to the
   Play button (or under **Installations**). Choose it and press **Play** once, then quit. This creates the
   folders you need.

## 3. Where the mods folder is

| System | Mods folder | How to get there |
|---|---|---|
| Windows | `%appdata%\.minecraft\mods` | Press **Windows key + R**, type `%appdata%\.minecraft` and press Enter. Open (or create) the **mods** folder. |
| Mac | `~/Library/Application Support/minecraft/mods` | In Finder use **Go → Go to Folder…**, paste `~/Library/Application Support/minecraft` and press Enter. Open (or create) **mods**. |

Copy `SebastianHiliHorror-1.20.1-1.0.0.jar` into **mods**, start the launcher, choose the **forge** profile and press **Play**.
In-game, the **Mods** button on the title screen should list *Sebastian Hili Horror*.

## 4. Playing with friends

**Everyone** who joins needs the mod (and Forge 1.20.1) installed, exactly as above. The server needs it too.

*Easiest: Open to LAN / one person hosts.* The host starts a single-player world, presses **Esc → Open to LAN**,
and friends on the same network join from **Multiplayer**. The host's computer runs Sebastian.

*Dedicated server:*
1. Run the same Forge installer, choose **Install server**, and pick an empty folder.
2. In that folder run `run.bat` (Windows) or `run.sh` (Mac/Linux) once. It stops and asks you to accept the EULA:
   open `eula.txt`, change `eula=false` to `eula=true`, and save.
3. Put the mod `.jar` into the server's `mods` folder and start it again.
4. Friends connect with **Multiplayer → Add Server** and your address.
5. Settings live in the server's `config/sebastianhilihorror-common.toml`. Only the server's copy matters.
   To use the `/seb` debug commands you must be an operator (`op YourName` in the server console).

---

## 5. Items, blocks and world generation

| Thing | How you get it |
|---|---|
| **Zinc Ore / Deepslate Zinc Ore** | Generates in every Overworld biome from Y −16 to Y 112 (most common around Y 48), about as often as copper. Needs a **stone pickaxe or better**. |
| **Raw Zinc** | Dropped by zinc ore (1–3 each, **Fortune** adds more, **Silk Touch** gives the ore block). |
| **Zinc Ingot** | Smelt Raw Zinc (or the ore) in a **Furnace** or, twice as fast, a **Blast Furnace**. |
| **Block of Raw Zinc / Block of Zinc** | 9 Raw Zinc / 9 Zinc Ingots in a crafting grid. Put a block back in the grid to get the 9 back. |
| **Penny** | Shapeless: **3 Zinc Ingots + 1 Copper Ingot = 4 Pennies.** (Real pennies are 97.5% zinc with a copper coat.) |
| **Sebastian Hili Spawn Egg** | Creative inventory, *Sebastian Hili Horror* tab. |

The ore blocks draw Minecraft's own stone and deepslate underneath, with the zinc nuggets
(styled after vanilla iron ore) layered on top. That way they blend into the world, even with a resource pack.
Screenshots: `docs/screenshots/` in the source folder.

Zinc also works with other mods (`forge:ingots/zinc`, `forge:ores/zinc` and so on) and as **beacon payment**.

---

## 6. Sebastian Hili

A player-shaped mob with his own skin and a **"Sebastian Hili"** nameplate. Like a real player's, it shows through walls
unless he's sneaking. He talks in normal chat as `<Sebastian Hili> message`, so it looks like another player is typing.

**Toughness (default):** 300 health (15× a player), 12 armor, immune to knockback, fire, lava, falling, drowning and
lightning, regenerates 0.5 health per second, and sometimes blinks away when hit. Hit him 5 times within 15 seconds and
he charges you. Beatable with friends, good gear and teamwork. When he dies he drops every penny he ever collected.

### The states

| State | What happens |
|---|---|
| **WATCHING** (default) | He keeps about **12–24 blocks** away and stares at the nearest player, head locked on, body snapping to face you. If you walk toward him, he walks *backwards*, still staring. When you **look away**, he silently moves somewhere else (just out of view) so he's not where you left him. He's silent: no footsteps, no ambient sounds. |
| **BEGGING** | Hold a **Penny** (main hand or off-hand) within **16 blocks** where he can see you. He walks up and begs in chat. **Right-click him while holding the penny** to give it. He takes one, thanks you, and goes back to watching. You have **10 seconds**, with a countdown at the end. |
| **CHARGING** | If you don't give it in time, **put it away**, or **run** (more than 22 blocks), he sprints straight *through* you, takes a penny from your inventory and kills you: *"Player was run through by Sebastian Hili over a penny"*. If you outrun him or hide, he blinks right behind you. |
| **RAGE** | If a penny item is destroyed by **fire, lava or fire damage** within **32 blocks** where he can see it, he screams *"WHO BURNED IT"*. Everyone in the dimension gets a **"HE SAW THAT"** title, a scream sound and a pulse of Darkness. The sky turns to a thunderstorm, he starts **glowing** (visible through walls) with **red eyes**, shaking, smoking and burning. Then he runs through **every player in that dimension, one by one** (closest first, blinking to anyone far away). Afterwards he calms down: *"...okay. i feel better now."* |
| **RUMMAGING** | Every 1–3 minutes he walks to a nearby chest, barrel, shulker box or other container (within 20 blocks), opens it (lid animation and sound), rummages, and takes **only Pennies**. Never anything else. He leaves unopened dungeon/loot chests alone. |
| **INVENTORY SEARCH** | Every **5–10 minutes** he walks up to a player and asks *"do you have a penny?"*. Answer by typing **`yes`** or **`no`** in chat (capitals and punctuation don't matter) within **30 seconds**:<br>• **yes**, and you have one: he takes exactly one penny.<br>• **no**, and you really have none: nothing happens.<br>• **no**, but you DO have one: **real lightning** strikes and kills you, and he takes every penny you had.<br>• **yes** with no penny: he's hurt you'd joke about that (Darkness + Slowness, no death).<br>• No answer: he searches you himself. With a penny, he charges you. Without one: *"...rude."*<br>He even finds pennies hidden inside **shulker boxes and bundles** you're carrying. |

### Where he comes from

* **Natural arrival (Overworld):** after 5 minutes of world time, each minute there's a 25% chance he arrives if he
  isn't already around. He appears out of sight near a random player with a yellow
  *"Sebastian Hili joined the game"* message. There is only ever **one** natural Sebastian. If he gets stranded in
  unloaded chunks, a new one takes his place and the old copy quietly disappears when its chunk loads. After he's
  killed he stays away for 20 minutes.
* **Spawn egg** and **`/seb spawn`** make extra ones (not limited).

### Debug commands (operators / cheats on)

| Command | Does |
|---|---|
| `/seb spawn` | Spawns him 4 blocks in front of you, facing you |
| `/seb state <watching\|begging\|charging\|rage\|rummaging\|searching> [player]` | Forces a state on the nearest Sebastian (default player: you) |
| `/seb ask [player]` | He asks "do you have a penny?" immediately |
| `/seb freeze <true\|false>` | `true` = he stays put (no wandering, relocating, scheduled searches or rummages), but still reacts to pennies, burns and answers. Handy for screenshots. |
| `/seb info` | State, target, pennies collected, health, time until his next search |
| `/seb remove` | Removes every Sebastian in your dimension |

### Config (`config/sebastianhilihorror-common.toml`)

Every major feature can be switched off, and all timers, ranges and difficulty values can be tuned:

* **features:** `begging`, `charging`, `rage`, `rummaging`, `inventorySearch`, `naturalSpawning`, `relocation`,
  `fakeJoinMessages`, `rageThunder`, `hints`, `retaliation`, `whispers`, `wakeUpScare`, `mimicry`, `spareCreativePlayers`
* **timers:** beg timeout (10 s), beg cooldown (45 s), search interval (5–10 min), answer timeout (30 s),
  rummage interval (60–180 s), relocation interval (15–40 s), rage wind-up (3 s), charge blink (12 s)
* **ranges:** watch distance (12–24), beg range (16), run-away distance (22), rage range (32), rummage radius (20),
  chat radius (0 = everyone sees his chat, like a real player)
* **difficulty:** max health (300), armor (12), walk speed, charge speed, hits before he retaliates (5),
  regeneration, teleport-on-hit chance
* **spawning:** first arrival delay, return-after-death delay, arrival chance per minute, max natural Sebastians (1)

Creative and spectator players are never killed (unless you turn off `spareCreativePlayers`). He still watches them.

---

## 6½. The boss fight: *The World Looks Red*

**How it starts.** Refuse him a penny **5 times in a row** while he begs: let the timer run out, put the penny away, or run.
Giving him a penny resets the count, and the count survives death. The first four times he charges you as usual. The
fifth time he doesn't. He says *"...five. keep it, then. i hope it was worth it."* and slips **Exhibit A** into your pocket: a map showing a
burned, melting penny. **Right-click him with it** to show it to him. He takes it, and **everyone online** is pulled
into **The Red**.

**The Red** is a pocket dimension: a crimson island floating on a lava sea in thick red fog, with crimson spores in the
air. There are pillars to hide behind, a few small lava pools (not everywhere) and lava pouring off distant rocks.

**The fight** is timed to the song (6:39):

| Song | What happens |
|---|---|
| arrival | You're **frozen**, and he stands a little way in front of you. *"you."* |
| 0:00 | The song starts. He talks for 42 seconds: five refusals, years of being laughed at, and nobody coming to save you. It ends quietly: *"i don't want your penny anymore. ... i want you."* |
| **0:42** | The climax hits. A red **"Sebastian Hili ❤ 50,000"** boss bar appears and you can move. **You can't hurt him.** Survive. |
| 0:42–1:50 | First climax: penny fans, dashes, eruptions, blink strikes |
| 1:50–2:28 | Bass groove: shockwaves and blink strikes |
| 2:28–2:50 | Build-up: red lightning, more and more of it |
| 2:50–3:12 | **The Dark** (the quiet part): darkness, he's invisible except for his red eyes, and a heartbeat tells you how close he is. A little Regeneration so you can breathe. |
| 3:12–3:32 | The bass returns: double dashes |
| 3:32–4:58 | Second climax: everything. 4:16–4:38 is the peak, faster and under a lightning storm |
| 4:58–5:40 | Breather: Regeneration, slow shockwaves, and him talking |
| 5:40–6:02 | The drop: triple dashes and double shockwaves |
| 6:02–6:24 | Final climax: penny rain, lightning, dashes, everything |
| **6:24** | He's tired. He kneels and slowly **fades away** as the song ends. |
| 6:39 | Everyone still alive goes home: *"YOU SURVIVED. Sebastian Hili is gone. For good."* |

**His attacks:** every one has a warning, so none of them is unavoidable.

| Attack | Warning | How to survive it |
|---|---|---|
| Penny fan | he raises his arm, a clink | strafe sideways, or get behind a pillar |
| Dash | a red line on the ground pointing at you | step off the line |
| Eruption | a ring of fire under your feet | get out of the ring within a second and a half |
| Blink strike | a sting sound and "behind you." | keep moving; he slashes where you *were* |
| Shockwave | he leaps into the air | jump as the red ring reaches you |
| Red lightning | sparks mark the ground (aimed where you're running) | change direction |
| Penny rain | pennies falling from above | keep moving |
| The Dark | heartbeat gets faster as he gets closer | keep away from the red eyes |

Armour, Protection, Fire Resistance and potions all help. If it's too hard or too easy, change `[boss] damageMultiplier`.

**If you die** in The Red you respawn at home **with all your items and XP**. Once nobody is left alive, the fight ends:
*"next time."* He goes back to normal and turns up again, and you'll have to earn a new map (5 more refusals) for
another try. **If anyone survives to the end, everyone wins**, and he never arrives on his own again. Only the spawn egg
or `/seb spawn` can bring him back (and `/seb gone false` lets him return naturally). There's no escaping through portals mid-fight.

**Boss debug commands:** `/seb map [player]` (give Exhibit A), `/seb refusals <player> <n>`, `/seb boss start`,
`/seb boss skip <seconds>` (jumps the fight's timeline; the music can't skip, so it keeps its own time), `/seb boss stop`,
`/seb gone <true|false>`.

**The song** is used with permission for this personal mod. It is packed into the jar and included in the zip, but
it is deliberately **not** in the GitHub repository (it's in `.gitignore`). To build from GitHub, put an OGG of it at
`src/main/resources/assets/sebastianhilihorror/sounds/music/the_world_looks_red.ogg`.

## 7. My creative additions

Things I added on top of the brief:

* **Fake join/leave messages:** *"Sebastian Hili joined the game"* in yellow when he arrives, and *"left the game"* when he dies.
* **Mimicry:** while watching, he holds a copy of whatever you're holding and **sneaks when you sneak** (his nameplate
  hides like a real sneaking player's).
* **Walking backwards:** step toward him and he backs away without breaking eye contact.
* **Heartbeat:** a heartbeat only you can hear while you look at him, faster the closer he is.
* **Whispers:** stare at him too long and he whispers to you privately (`Sebastian Hili whispers to you: you blinked.`).
  Sometimes you hear a whisper from directly behind you.
* **Glowing eyes:** his eyes glow faintly in the dark, and turn **red** in a rage.
* **The wake-up scare:** sleep through the night and he may be standing at your bedside when you wake up. *"good morning."*
* **He opens doors** and closes them behind him.
* **Lightning is his:** he's immune to it. Liars lose **all** their pennies, not just one, and the bolt strikes a moment
  before the kill so the liar's dropped items survive.
* **Lie detection sees inside shulker boxes and bundles.**
* **Rage extras:** "HE SAW THAT" title for every player, screaming, thunderstorm, Darkness pulse, glowing outline,
  shaking body, flame and smoke particles, and an apology afterwards.
* **Retaliation:** he puts up with a few hits ("ow.", "why would you do that?") before charging you. He also blinks away when hit.
* **He keeps your pennies:** everything he collects (gifts, searches, chests, charges) drops when he dies.
* **Five original sound effects** (heartbeat, whisper, scream, penny clink, sting), synthesized for this mod.
* **Lots of dialogue:** over 80 lines, always lowercase and awkward, except when he's screaming.
* **Advancements** (their own tab): *Sebastian Hili Horror* (get raw zinc), *Mint Condition* (craft a penny),
  *Spare Change* (give him a penny), *Honest Answer* (truthful "no"), *Pants on Fire* (lie and get struck),
  *Over a Penny* (get run through), *Money to Burn* (burn a penny in front of him), *Good Morning* (the wake-up scare),
  *Debt Collector* (kill him).
* **Original pixel art** for every texture, plus the skin.
* **Boss fight extras:** he's 20% bigger and his eyes glow red the whole time; in The Dark only his eyes show. He
  tells you not to touch him if you try to hit him ("don't touch me."), he talks between the song's sections, and he
  counts each player who falls ("one." "two."). Each attack has its own death message (e.g. *"forgot to jump"*, *"turned around too late"*,
  *"was found in the dark"*). He fades away by actually becoming see-through. There are two new advancements:
  *Exhibit A* and *The World Looks Red*.
* Zinc counts as **beacon payment**, and gets standard Forge tags so other mods can use it.

---

## 8. The skin

`SebastianHili_skin.png` is a standard 64×64 skin with **classic (wide) arms**: messy dark wavy hair with a curl on his
forehead, rectangular dark glasses, a slightly awkward smile, a dark green school blazer with a white shirt and dark tie,
dark trousers and black shoes. To wear it: **minecraft.net → Profile → Skin → Classic → upload the PNG.**

---

## 9. The demo video

**Boss fight video:** (recorded before his dialogue was rewritten, so his lines in the video are the old ones.)
`SebastianHili_BossFight.mp4` shows refusing him (one refusal live, then a command skips to the
fourth), the fifth refusal and Exhibit A, showing him the map, the frozen intro, the whole fight with the song, him
fading, and the trip home. The demo player has Resistance V so the full fight can be filmed. A scripted player can't
dodge like a person, and without it the video would end early. Two stand-in "friends" are pulled in too; they can't
move, so you can see what happens to people who stand still. The video also hides the "on fire" screen overlay
(the scripted player runs through fire a lot and it covered half the picture); in the real game it shows as normal.

`SebastianHiliHorror_demo.mp4` (2:10, 1280×720, with game sound) was recorded from the real dev client running
on a headless Linux machine (software rendering). Nothing is edited in. A script plays the scenes using
ordinary commands, chat and inputs:

| Time | Scene |
|---|---|
| 0:00 | Title card |
| 0:06 | The skin up close: the camera circles him, then he turns to look at you |
| 0:22 | Watching: he stares from a distance, moves when you look away, copies your sword and sneaks with you |
| 0:39 | Mining zinc ore and deepslate zinc ore with a stone pickaxe, then crafting 3 zinc + 1 copper into 4 pennies |
| 0:56 | Begging, then giving him a penny by right-clicking |
| 1:05 | Putting the penny away: he sprints over and runs you through |
| 1:15 | "do you have a penny?" answered with a truthful "no" |
| 1:22 | The same question answered with a lying "no": lightning |
| 1:30 | Rummaging: the chest before, him searching it, and the chest after (pennies gone, everything else still there) |
| 1:44 | A penny dropped into lava: RAGE, and he goes through all three players one by one |
| 2:04 | End card |

## 10. For programmers: building and testing

Everything is in `source/` (also on GitHub). Needs JDK 17.

```bash
cd source
./gradlew build                 # -> build/libs/SebastianHiliHorror-1.20.1-1.0.0.jar
./gradlew test                  # JUnit: chat answer parsing, lie detector truth table, burn rules, watch distances
./gradlew runGameTestServer     # 23 Forge GameTests (see below), exits non-zero on failure
./gradlew runClient             # dev client
```

**GameTests** (`src/main/java/.../gametest/SebGameTests.java`): penny recipe (incl. wrong ingredient counts), storage block
recipes both ways, furnace and blast furnace smelting, ore harvest levels (wood/gold no, stone+ yes), ore drops (raw zinc 1–3,
Fortune III gives clearly more, Silk Touch gives the ore), storage blocks drop themselves, lying "no" → lightning + death +
pennies taken, honest "no" → safe and nothing else taken, honest "yes" → exactly one penny taken, pennies hidden in a shulker
box still count, penny burned in lava → RAGE, penny burned in fire → RAGE, penny burned behind a wall → no rage, penny
destroyed by cactus → no rage, chest rummaging takes only pennies (diamonds, dirt and zinc untouched), barrel penny removal,
giving a penny while begging, putting the penny away → charge, kill and one penny taken, spawn stats and nameplate,
the 5th refusal gives Exhibit A (with the picture painted on the map), giving a penny resets the refusal count,
showing him the map → The Red (arena built, map taken, boss invulnerable) → surviving sends you home and makes him
gone for good, and dying in The Red keeps your items.

**Project layout**

* `src/main/java/com/sebastianhili/horror/entity/SebastianEntity.java`: the state machine (`customServerAiStep` dispatches
  to `tickWatching`, `tickBegging`, `tickCharging`, `tickRage`, `tickRummaging`, `tickSearching`)
* `logic/`: pure, unit-tested rules (`ChatAnswer`, `LieDetector`, `BurnRules`, `WatchRules`) and `PennyLogic`
* `item/PennyItem.java`: detects a penny destroyed by fire/lava (`onDestroyed`)
* `entity/SebSpawner.java`: natural arrival and the one-Sebastian rule (saved in the world)
* `command/SebCommand.java`: `/seb` debug commands
* `tools/generate_textures.py`, `tools/generate_data.py`, `tools/generate_sounds.sh`: regenerate all art, JSON data and sounds
* `src/demo/`: the scripted demo director used to record the video (dev client only, **not** in the jar)
* `tools/record_demo.sh`: records the demo (Xvfb + ffmpeg x11grab + PulseAudio)
* `tools/screenshot_ores.sh`: takes the in-game ore screenshots in `docs/screenshots/` (`SEB_DEMO=ores`)

### Recording the demo yourself

On Linux with Xvfb, ffmpeg and (optionally) PulseAudio installed:

```bash
cd source
tools/record_demo.sh demo/SebastianHiliHorror_demo.mp4
```

This starts a virtual display, launches the dev client with `SEB_DEMO=true`, and the director creates a fresh world and plays
every scene with real commands, chat and input. Then the script trims the capture to the demo. On a normal desktop you can
just run `SEB_DEMO=true ./gradlew runClient` and record the window with OBS.

The demo uses a tuned config (short cooldowns, faster relocation) so everything fits in about two minutes. In the rage
scene, two extra "friends" are stand-in players created by a demo-only command (`/sebdemo friends`), so you can see him
go through several players one by one.

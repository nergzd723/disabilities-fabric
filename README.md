# Disabilities (Fabric)

A Fabric mod for **Minecraft 26.3** that gives you a random disability on a timer.

By default, **every time the timer runs out all of your current disabilities are cured and you develop one
brand new one**, so a run is a series of different challenges instead of an ever-growing pile. The original
stacking behaviour is still one command away (`/disability timer mode stack`).

This is an independent re-implementation of the
[Disabilities](https://modrinth.com/mod/disabilities) mod by OddSense, with all 30 of its disabilities.

## The timer

* One shared countdown for the whole server, 10 minutes by default. It only runs while somebody is online and
  survives restarts.
* **Replace mode** (default): when it runs out, each online player is cured of everything and rolls one new
  disability. You never re-roll something you were just cured of (unless you had all 30).
* **Stack mode** (the original behaviour): when it runs out, each online player gets one more disability on
  top of the ones they have, until they have all of them.
* `/disability next` does exactly what the timer would do, right now.

## Commands

All commands need operator permissions (level 2). `/dis` is an alias for `/disability`.

| Command | Description |
|:---|:---|
| `/disability give <players> <disability/all/random>` | Give players a specific, a random, or every disability |
| `/disability remove <players> <disability/all/random>` | Remove a specific, a random, or every disability |
| `/disability list [players]` | Show a player's disabilities |
| `/disability next [players]` | Skip the timer: run the next timer step for everyone (or the given players) now |
| `/disability reset` | Cure everyone and restart the countdown |
| `/disability timer` | Show the timer's state, length, mode and time left |
| `/disability timer <minutes>` | Set the timer length (0.05-720 minutes), restart the countdown and turn the timer on |
| `/disability timer off` / `on` | Pause / resume the timer |
| `/disability timer mode <replace/stack>` | Cure-and-reroll (default) or add-on-top when the timer runs out |

Disability names in commands are the ids from the table below (`color_blind`, `parkinsons`, ...). Tab completion
lists them.

## Keybinds

* **Toggle Disability List** (default: <kbd>`</kbd>): show or hide the list in the top-left corner. In the
  inventory, hover an entry to read what it does.

## All disabilities

| # | Id | Disability | What it does |
|---:|---|---|---|
| 1 | `blind` | **Blind** | Permanent Blindness (but, unlike the potion, you can still sprint) |
| 2 | `color_blind` | **Color Blind** | You see in black and white |
| 3 | `deaf` | **Deaf** | You cannot hear anything (except voices...) |
| 4 | `diabetes` | **Diabetes** | Sugary food hurts: from half a heart (potatoes) up to 10 hearts (cookies, pie, honey, cake) |
| 5 | `schizophrenia` | **Schizophrenia** | Phantom footsteps, mobs and hissing creepers, and a dark figure at the edge of your vision that vanishes when you look at it |
| 6 | `mute` | **Mute** | You cannot speak in chat (commands still work) |
| 7 | `seizures` | **Seizures** | Big hits (50%), hunger or negative effects can trigger a 4 second seizure: you can't move, your view convulses and it hurts |
| 8 | `cancer` | **Cancer** | Your maximum health slowly wastes away, down to 3 hearts |
| 9 | `ocd` | **OCD** | Every 1-3 minutes: place, mine, jump or crouch an *exact* number of times within 10 seconds. Too few hurts every second, too many hurts a lot |
| 10 | `amputated_offhand` | **Amputated Offhand** | Your offhand is gone; anything put there falls on the floor |
| 11 | `amputated_leg` | **Amputated Leg** | You can't walk; jumping is the only way to get around |
| 12 | `dyslexia` | **Dyslexia** | Almost all text is scrambled (your own disability list, menus and what you type stay readable) |
| 13 | `short` | **Short** | Half a block shorter, with half a block less reach |
| 14 | `dementia` | **Dementia** | Items randomly swap places in your inventory and look empty until you click them |
| 15 | `asthma` | **Asthma** | Sprinting for 10 seconds leaves you out of breath and suffocating |
| 16 | `cerebral_palsy` | **Cerebral Palsy** | Random clicks, jumps, crouches and steps |
| 17 | `narcolepsy` | **Narcolepsy** | You randomly fall asleep for a few seconds |
| 18 | `parkinsons` | **Parkinson's** | Your crosshair (and so your aim) shakes constantly, sometimes badly |
| 19 | `pollen_allergy` | **Pollen Allergy** | Being within 2 blocks of flowers or pollen-carrying bees poisons you |
| 20 | `insomnia` | **Insomnia** | You can't sleep (beds still set your spawn), and you can't use beds where they would explode |
| 21 | `arthritis` | **Arthritis** | Attacks and mining are 40% slower |
| 22 | `brittle_bones` | **Brittle Bones** | Double fall damage, starting from 1.5 blocks |
| 23 | `adhd` | **ADHD** | Your selected hotbar slot randomly changes |
| 24 | `tourettes` | **Tourette's** | Your camera jerks and you blurt out random words in chat |
| 25 | `vertigo` | **Vertigo** | Every so often the world tilts, spins and wobbles for 8 seconds |
| 26 | `prosopagnosia` | **Prosopagnosia** | Every mob looks like Steve (no shadows or hitboxes to tell them apart either) |
| 27 | `scoliosis` | **Scoliosis** | Your view is permanently tilted |
| 28 | `kleptomania` | **Kleptomania** | You cannot drop items |
| 29 | `lactose_intolerance` | **Lactose Intolerance** | Milk and cake kill you instantly, totem or not |
| 30 | `anosognosia` | **Anosognosia** | You cannot see your own disabilities, in the list or in chat |

Missing limbs are visible to other players too.

## Datapack customisation

Some of the rules are tags, so a datapack can change them:

| Tag | Used by |
|---|---|
| `#disabilities:sugar/high`, `sugar/moderate`, `sugar/low`, `sugar/trace` (items) | Diabetes damage: 10 hearts, 5 hearts, 2 hearts, half a heart |
| `#disabilities:contains_milk` (items) | Lactose Intolerance (cake is always included) |
| `#disabilities:pollen_sources` (blocks) | Pollen Allergy (defaults to `#minecraft:flowers`) |
| `#disabilities:ocd_mineable` (blocks) | What OCD can ask you to mine |

## Multiplayer

The mod is needed on both the server and every client (most disabilities happen client-side), and the server
turns away clients that don't have it. Run state is stored in the world as `data/disabilities/run.dat`.

## Building

Requires **Java 25**.

```sh
./gradlew build                 # builds build/libs/disabilities-<version>.jar, runs unit and server game tests
./gradlew runClientGameTest     # end-to-end client test with screenshots, needs a display
./gradlew runClient             # play it
```

The client game test plays through the mod in a real singleplayer world and saves screenshots to
`build/run/clientGameTest/screenshots`. To run it headless with Mesa's software renderer:

```sh
SDL_VIDEO_DRIVER=x11 SDL_VIDEO_FORCE_EGL=1 xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClientGameTest
```

## License

MIT. See [LICENSE](LICENSE). The original mod's idea and design belong to its author, OddSense; this project
shares no code or assets with it.

# GUI Browser Foundation Design

## Goal

Build `GUI Browser` as a compatibility-first Minecraft 1.12.2 client mod that hosts GUI-capable interactions inside one logical browser shell instead of rewriting individual screens.

## Product Direction

The mod should feel like a single browser window that can be reopened, minimized, and eventually hold multiple GUI sessions. The implementation strategy is to host original `GuiScreen` instances inside a browser root screen instead of duplicating container logic or reimplementing vanilla/modded GUIs.

## Constraints

- Target platform: Cleanroom/Forge `1.12.2`
- Client architecture must remain compatible with `Minecraft.displayGuiScreen(...)`
- First release does not need persistent container snapshots, background session simulation, multiple browser windows, or server-side extensions
- Compatibility is more important than polish; invasive hooks should be delayed until needed

## Guiding Decisions

- Build one logical browser manager for the entire client session
- Keep `BrowserRootGui` as the only visible browser shell
- Host original GUI instances later; do not rewrite GUI behavior screen-by-screen
- Delay `Mixin` and compatibility shims until the browser shell and single-child hosting work
- Avoid scaffolding empty future packages before they are needed

## Phase Roadmap

### Phase 1: Browser Shell Foundation

This phase proves that the browser itself is a stable client-side GUI, independent from child GUI hosting. It includes:

- A draggable, fixed-size browser shell
- Title bar, tab-strip placeholder, content area, minimize button, close button
- A single `BrowserManager` that owns state and opens/closes the shell
- A minimal config file for `escAction` and browser hotkey
- Browser hotkey and client command support
- Empty-browser behavior with no content interaction

This phase explicitly excludes:

- Capturing existing GUIs
- Real tab sessions
- Compatibility rules, blacklists, whitelists, and debug overlay
- Mixins and access transformers

### Phase 2: Single-Tab GUI Hosting

This phase adds the first real hosted session:

- Intercept one GUI opening path
- Store a single hosted `GuiScreen`
- Forward render, update, and input calls into the child GUI
- Use `setWorldAndResolution(...)` for child initialization and resize updates
- Treat child-opened replacement screens as "replace current content", not "new tab"

The goal of this phase is to make one vanilla-style container GUI work inside the shell before adding tabs.

### Phase 3: Multi-Tab and Compatibility Layer

This phase expands the browser into the final architecture:

- Add `BrowserTab` and selection/closing behavior
- Restore minimized tabbed sessions
- Introduce GUI blacklists/whitelists and behavior hints
- Add `currentScreen` masquerading, child lifecycle bookkeeping, and debug overlay
- Add small `MixinMinecraft`-level fallbacks only where Forge events are insufficient

The goal of this phase is broad compatibility, not perfect compatibility.

## Phase 1 Detailed Design

### Scope

Phase 1 should ship a shell that feels intentional and stable, but it should not pretend to host content yet. The content area is a passive placeholder that tells the user the browser currently has no tabs.

### Minimal Package Layout

Only create files that are used in Phase 1:

- `src/main/java/com/zzhalex233/guibrowser/GuiBrowserMod.java`
- `src/main/java/com/zzhalex233/guibrowser/proxy/ClientProxy.java`
- `src/main/java/com/zzhalex233/guibrowser/proxy/CommonProxy.java`
- `src/main/java/com/zzhalex233/guibrowser/proxy/IProxy.java`
- `src/main/java/com/zzhalex233/guibrowser/config/EscAction.java`
- `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfig.java`
- `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfigLoader.java`
- `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserState.java`
- `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserWindowState.java`
- `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayout.java`
- `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserRootGui.java`
- `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserKeybinds.java`
- `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java`
- `src/main/java/com/zzhalex233/guibrowser/client/command/CommandGuiBrowser.java`
- `src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java`

Everything else stays out of the repository until a later phase needs it.

### Runtime Model

`BrowserManager` is the persistent logical singleton. It owns browser state, window geometry, and config, but it does not keep a long-lived `GuiScreen` instance alive. Whenever the browser must be shown, the manager opens a fresh `BrowserRootGui` bound to the same manager state.

This keeps the project aligned with Minecraft's normal GUI lifecycle while still preserving one logical browser per client session.

### State Model

The long-term enum should be defined now so later phases do not need to rename public concepts:

- `CLOSED`
- `OPEN_EMPTY`
- `OPEN_WITH_TABS`
- `MINIMIZED_WITH_TABS`

In Phase 1, only `CLOSED` and `OPEN_EMPTY` are reachable. The other states are reserved for future phases.

### Window Model

`BrowserWindowState` stores:

- `x`
- `y`
- `width`
- `height`
- `dragging`
- `dragOffsetX`
- `dragOffsetY`

Window size is fixed in Phase 1. Position is draggable and should be clamped so the title bar remains reachable on screen after dragging or resolution changes.

### Config Model

Phase 1 config is intentionally small:

- `EscAction escAction`
- `int openBrowserKeyCode`

`escAction` is stored now for forward compatibility, but in Phase 1 the empty browser always closes because there are no tabbed sessions to minimize.

### Client Wiring

`ClientProxy.preInit()` should:

- Load config
- Create or initialize the manager singleton
- Register browser keybindings
- Register Forge event listeners
- Register the client command

The common proxy remains minimal and client-only wiring stays out of shared code.

### UI Layout

`BrowserRootGui` should render:

- Title bar
- Minimize button
- Close button
- Tab strip placeholder
- Content area placeholder

`BrowserChromeLayout` should compute all rectangles from the current `BrowserWindowState` so rendering and hit-testing do not hardcode geometry in multiple places.

### Interaction Flow

Open browser:

1. Player presses the browser hotkey or runs `/guibrowser open`
2. `BrowserManager` checks that client state is suitable for opening
3. Manager transitions `CLOSED -> OPEN_EMPTY`
4. Manager opens a fresh `BrowserRootGui`

Toggle browser:

1. If state is `CLOSED`, open empty browser
2. If current screen is browser root, close it
3. If state says browser is open but another GUI is active, reopen the browser root

Close browser:

1. Close button, `ESC`, or `/guibrowser close`
2. Manager transitions to `CLOSED`
3. Current root screen closes without preserving content

Drag browser:

1. Mouse down in draggable title bar
2. Root GUI enters drag mode using `BrowserWindowState`
3. Mouse move updates `x/y`
4. Mouse release ends drag mode

### Commands

Phase 1 commands should stay small:

- `/guibrowser open`
- `/guibrowser close`
- `/guibrowser toggle`
- `/guibrowser state`

Do not add placeholder `capture` or `clear` commands yet.

### Error Handling

- If config loading fails, log a warning and use defaults
- If the client is not in a valid in-world state, open/toggle commands should no-op with a user-facing chat message
- If the current screen closes unexpectedly, manager state should be reset on the next client tick if needed

### Testing Strategy

Automated tests should focus on pure Java logic:

- `BrowserManager` state transitions
- `BrowserWindowState` drag/clamp behavior
- `BrowserChromeLayout` rectangle math

Manual testing in `runClient` should verify:

- Browser opens from hotkey and command
- Empty browser closes with `ESC`
- Dragging works at multiple resolutions
- Window stays reachable after resize
- Minimize and close buttons behave consistently for empty state

## Deferred Decisions

These are intentionally postponed to avoid contaminating Phase 1:

- How GUI capture is armed and intercepted
- Whether `GuiOpenEvent` alone is enough for Phase 2
- Whether child GUI forwarding should use `handleInput()` or more granular calls
- How tab titles are derived
- Which GUI classes belong in blacklist/whitelist defaults
- Which Mixin hooks are required once internal child navigation is supported

## Success Criteria

Phase 1 is successful when:

- The project has a stable browser shell with no child GUI hosting
- The shell opens and closes predictably from keybinds and command
- The shell can be dragged and remains visually coherent
- The codebase has a clean path into Phase 2 without throwing away the shell layer

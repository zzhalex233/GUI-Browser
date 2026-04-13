# GUI Browser Phase 1 Browser Shell Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first usable GUI Browser shell: a draggable fixed-size browser window with empty-state content, browser state management, a minimal config, a keybind, and a client command.

**Architecture:** Keep one persistent `BrowserManager` singleton that owns state, config, and window geometry, while opening a fresh `BrowserRootGui` whenever the shell is shown. Phase 1 only supports `CLOSED` and `OPEN_EMPTY`, and it deliberately excludes GUI capture, tab sessions, and Mixins so the shell can be verified in isolation.

**Tech Stack:** Java 21, Cleanroom/Forge 1.12.2, Gradle, JUnit 6, Minecraft `GuiScreen`, Forge client events and command registration

---

Spec reference: `docs/superpowers/specs/2026-04-09-gui-browser-foundation-design.md`

## Planned File Structure

- Modify: `src/main/java/com/zzhalex233/guibrowser/GuiBrowserMod.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/proxy/IProxy.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/proxy/CommonProxy.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/proxy/ClientProxy.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/config/EscAction.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfig.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfigLoader.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserState.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserWindowState.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayout.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserRootGui.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserKeybinds.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/command/CommandGuiBrowser.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java`
- Create: `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserManagerTest.java`
- Create: `src/test/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayoutTest.java`

## Chunk 1: Core Browser State and Config

### Task 1: Add Browser State Model

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserState.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserWindowState.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- Test: `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserManagerTest.java`

- [x] **Step 1: Write the failing state tests**

```java
class BrowserManagerTest {
    @Test
    void openEmptyBrowserTransitionsFromClosed() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());

        manager.openEmptyBrowser();

        assertEquals(BrowserState.OPEN_EMPTY, manager.getState());
    }

    @Test
    void closeBrowserReturnsToClosedState() {
        BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
        manager.openEmptyBrowser();

        manager.closeBrowser();

        assertEquals(BrowserState.CLOSED, manager.getState());
    }

    @Test
    void draggingWindowClampsTitleBarInsideViewport() {
        BrowserWindowState window = BrowserWindowState.defaultWindow();

        window.beginDrag(20, 10, 50, 30);
        window.dragTo(-200, -200, 320, 240);

        assertTrue(window.getY() >= 0);
    }
}
```

- [x] **Step 2: Run the focused test target and confirm it fails**

Run: `.\gradlew.bat test --tests com.zzhalex233.guibrowser.client.browser.BrowserManagerTest`

Expected: `BrowserManagerTest` fails because browser classes do not exist yet.

- [x] **Step 3: Implement the minimal browser state classes**

```java
public enum BrowserState {
    CLOSED,
    OPEN_EMPTY,
    OPEN_WITH_TABS,
    MINIMIZED_WITH_TABS
}

public final class BrowserManager {
    private BrowserState state;
    private final BrowserWindowState windowState;
    private final BrowserConfig config;

    public void openEmptyBrowser() {
        state = BrowserState.OPEN_EMPTY;
    }

    public void closeBrowser() {
        state = BrowserState.CLOSED;
    }
}
```

- [x] **Step 4: Re-run the focused test target**

Run: `.\gradlew.bat test --tests com.zzhalex233.guibrowser.client.browser.BrowserManagerTest`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit the core state model**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/browser src/test/java/com/zzhalex233/guibrowser/client/browser
git commit -m "feat: add browser state foundation"
```

### Task 2: Add Minimal Config Support

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/config/EscAction.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfig.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfigLoader.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- Test: `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserManagerTest.java`

- [x] **Step 1: Add a failing defaults test**

```java
@Test
void managerStartsWithDefaultEscActionAndKeybind() {
    BrowserConfig config = BrowserConfig.defaults();

    assertEquals(EscAction.MINIMIZE, config.getEscAction());
    assertTrue(config.getOpenBrowserKeyCode() > 0);
}
```

- [x] **Step 2: Run the focused test target and confirm it fails**

Run: `.\gradlew.bat test --tests com.zzhalex233.guibrowser.client.browser.BrowserManagerTest`

Expected: failure because the config model is incomplete.

- [x] **Step 3: Implement the config classes and wire them into the manager**

```java
public enum EscAction {
    MINIMIZE,
    CLOSE
}

public final class BrowserConfig {
    public static BrowserConfig defaults() {
        return new BrowserConfig(EscAction.MINIMIZE, Keyboard.KEY_B);
    }
}
```

`BrowserConfigLoader` should read/write a Forge config file and fall back to `BrowserConfig.defaults()` on error.

- [x] **Step 4: Re-run the focused test target**

Run: `.\gradlew.bat test --tests com.zzhalex233.guibrowser.client.browser.BrowserManagerTest`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit the config layer**

```bash
git add src/main/java/com/zzhalex233/guibrowser/config src/main/java/com/zzhalex233/guibrowser/client/browser src/test/java/com/zzhalex233/guibrowser/client/browser
git commit -m "feat: add browser config support"
```

## Chunk 2: Client Wiring, Layout Math, and UI Shell

### Task 3: Wire Client Initialization, Keybinds, and Command

**Files:**
- Modify: `src/main/java/com/zzhalex233/guibrowser/GuiBrowserMod.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/proxy/IProxy.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/proxy/CommonProxy.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/proxy/ClientProxy.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserKeybinds.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/command/CommandGuiBrowser.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java`

- [x] **Step 1: Add a manual verification checklist to the task notes**

```text
- Browser hotkey opens the shell while in-world
- /guibrowser open opens the shell
- /guibrowser close closes the shell
- /guibrowser state prints CLOSED or OPEN_EMPTY
```

- [x] **Step 2: Implement the client bootstrap**

`ClientProxy.preInit()` should:

```java
public void preInit() {
    BrowserConfig config = BrowserConfigLoader.load(...);
    BrowserManager.initialize(config);
    BrowserKeybinds.register();
    MinecraftForge.EVENT_BUS.register(new BrowserHotkeyHandler());
    MinecraftForge.EVENT_BUS.register(new ClientForgeEventHandler());
    ClientCommandHandler.instance.registerCommand(new CommandGuiBrowser());
}
```

- [x] **Step 3: Implement the keybind and command behavior**

```java
if (BrowserKeybinds.OPEN_BROWSER.isPressed()) {
    BrowserManager.getInstance().toggleBrowser();
}
```

`CommandGuiBrowser` should support only `open`, `close`, `toggle`, and `state`.

- [x] **Step 4: Run the full automated test suite**

Run: `.\gradlew.bat test`

Expected: `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit the client wiring**

```bash
git add src/main/java/com/zzhalex233/guibrowser
git commit -m "feat: wire browser client bootstrap"
```

### Task 4: Add Chrome Layout Math and Empty-Shell GUI

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayout.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserRootGui.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- Test: `src/test/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayoutTest.java`

- [x] **Step 1: Write failing layout tests**

```java
class BrowserChromeLayoutTest {
    @Test
    void titleBarContainsPointInsideWindow() {
        BrowserWindowState window = new BrowserWindowState(40, 30, 360, 240);

        assertTrue(BrowserChromeLayout.titleBar(window).contains(60, 40));
    }

    @Test
    void contentAreaStaysBelowTabStrip() {
        BrowserWindowState window = new BrowserWindowState(40, 30, 360, 240);

        assertTrue(BrowserChromeLayout.contentArea(window).getY()
            > BrowserChromeLayout.tabStrip(window).getBottom());
    }
}
```

- [x] **Step 2: Run the focused layout test target and confirm it fails**

Run: `.\gradlew.bat test --tests com.zzhalex233.guibrowser.client.gui.BrowserChromeLayoutTest`

Expected: failure because layout helpers do not exist yet.

- [x] **Step 3: Implement the layout helper and browser root GUI**

`BrowserRootGui` should:

- render title bar, buttons, tab-strip placeholder, and empty content copy
- start drag when the title bar is clicked
- end drag on mouse release
- call `BrowserManager.handleEscFromRoot()` on `ESC`
- route close/minimize button clicks back to the manager

Use a helper like:

```java
public final class BrowserChromeLayout {
    public static Rect titleBar(BrowserWindowState window) { ... }
    public static Rect tabStrip(BrowserWindowState window) { ... }
    public static Rect contentArea(BrowserWindowState window) { ... }
}
```

- [ ] **Step 4: Run automated tests and then manual client verification**

Run automated tests: `.\gradlew.bat test`

Run client: `.\gradlew.bat runClient`

Manual checks:
- press the browser hotkey in-world and confirm the shell opens
- drag the title bar and confirm the window moves smoothly
- press `ESC` and confirm the empty browser closes
- click minimize and close buttons and confirm both close the empty shell
- resize the game window and confirm the browser remains reachable

- [x] **Step 5: Commit the shell GUI**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/gui src/main/java/com/zzhalex233/guibrowser/client/browser src/test/java/com/zzhalex233/guibrowser/client/gui
git commit -m "feat: add browser shell gui"
```

## Chunk 3: Final Verification and Handoff

### Task 5: Verify Phase 1 Against the Spec

**Files:**
- Modify: `docs/superpowers/specs/2026-04-09-gui-browser-foundation-design.md` (only if implementation reveals a necessary clarification)
- Modify: `docs/superpowers/plans/2026-04-09-gui-browser-phase-1-browser-shell.md` (check off completed steps during execution)

- [x] **Step 1: Re-read the spec and confirm implemented scope matches Phase 1 only**

Checklist:
- no GUI capture
- no real tabs
- no Mixins
- browser shell is draggable and fixed-size
- keybind and command support exist

- [x] **Step 2: Run the full verification build**

Run: `.\gradlew.bat build`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run one final manual smoke test**

Run: `.\gradlew.bat runClient`

Manual checks:
- open with hotkey
- open with command
- print state with command
- close with `ESC`
- close with button

- [x] **Step 4: Update this plan file by checking off completed steps**

Update only the steps actually completed during implementation.

- [ ] **Step 5: Commit the verified Phase 1 slice**

```bash
git add docs/superpowers/specs/2026-04-09-gui-browser-foundation-design.md docs/superpowers/plans/2026-04-09-gui-browser-phase-1-browser-shell.md src
git commit -m "feat: implement gui browser phase 1 shell"
```

Plan complete and saved to `docs/superpowers/plans/2026-04-09-gui-browser-phase-1-browser-shell.md`. Ready to execute?



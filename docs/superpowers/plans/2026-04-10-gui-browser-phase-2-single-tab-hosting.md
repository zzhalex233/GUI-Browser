# GUI Browser Phase 2 Single-Tab Hosting Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement true Phase 2 single-tab hosting: fix `/guibrowser open` so the browser visibly opens, render browser chrome from `browser.png`, and make the hotkey trigger one original GUI-opening interaction that gets captured into the browser without changing normal right-click behavior.

**Architecture:** Keep `BrowserManager` as the persistent browser-session owner, but upgrade it from an empty shell state machine to a single-hosted-session model with minimize/restore semantics. `/guibrowser open` must become a deferred browser-show request so chat close events cannot erase it, while the hotkey becomes a one-shot capture request that invokes the original right-click interaction chain and only intercepts the resulting `GuiOpenEvent` when that request is active. `BrowserRootGui` draws its chrome from `browser.png`, hosts exactly one child `GuiScreen` in the `176x166` page viewport, and treats child-opened replacement screens as replacing the current tab content.

**Tech Stack:** Java 21, Cleanroom/Forge 1.12.2, Gradle, JUnit 6, Minecraft `GuiScreen`, Forge `GuiOpenEvent`, Forge client commands/keybinds, reflection for private Minecraft interaction entry points where needed

---

Spec reference: `docs/superpowers/specs/2026-04-09-gui-browser-foundation-design.md`

Phase 2 constraints approved after the original spec:
- `/guibrowser open` opens the browser itself: `CLOSED -> OPEN_EMPTY`, `MINIMIZED_WITH_TABS -> OPEN_WITH_TABS`, already-open browser -> focus the current browser.
- The hotkey no longer opens the browser by itself.
- The hotkey only does “quick capture”: it should trigger one original interaction and capture the GUI that opens from that interaction.
- Ordinary vanilla/modded right-click behavior must remain unchanged.
- `browser.png` is the only browser chrome atlas for this phase.
- `browser.png` layout:
  - browser frame/page region: `x=0..175`, `y=24..189`
  - tab sprite: `x=0..27`, `y=190..212`
  - top-right button strip: `x=148..172`, `y=13..26`
- Tabs use width `28`, height `23`, horizontal step `30`, and align by their lower-left edge to browser-local `y=27`.
- Top-right button hover uses a semi-transparent gray overlay instead of a separate hover sprite.

## Planned File Structure

- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellController.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellHost.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/MinecraftBrowserShellHost.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserCaptureRequest.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/browser/MinecraftInteractionInvoker.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayout.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeTexture.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserRootGui.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserKeybinds.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/command/CommandGuiBrowser.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/proxy/ClientProxy.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfig.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfigLoader.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserManagerTest.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserShellControllerTest.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayoutTest.java`

## Chunk 1: Repair Browser Open/Restore Semantics Before New Capture Logic

### Task 1: Upgrade BrowserManager to real Phase 2 session state

**Files:**
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserManagerTest.java`

- [ ] **Step 1: Write the failing session-state tests**

```java
@Test
void minimizePreservesHostedContentForRestore() {
    BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
    Object hosted = new Object();

    manager.captureHostedContent(hosted, "Chest");
    manager.minimizeBrowser();

    assertEquals(BrowserState.MINIMIZED_WITH_TABS, manager.getState());
    assertSame(hosted, manager.getHostedContent());

    manager.restoreBrowser();

    assertEquals(BrowserState.OPEN_WITH_TABS, manager.getState());
    assertSame(hosted, manager.getHostedContent());
}

@Test
void closeBrowserClearsHostedContentAndRestoreData() {
    BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());

    manager.captureHostedContent(new Object(), "Chest");
    manager.minimizeBrowser();
    manager.closeBrowser();

    assertEquals(BrowserState.CLOSED, manager.getState());
    assertFalse(manager.hasHostedContent());
}
```

- [ ] **Step 2: Run the focused state test target and confirm it fails**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.browser.BrowserManagerTest`

Expected: `BrowserManagerTest` fails because `minimizeBrowser()` / `restoreBrowser()` semantics are incomplete.

- [ ] **Step 3: Implement the minimal Phase 2 state model**

`BrowserManager` should own:

```java
private BrowserState state;
private Object hostedContent;
private String activeTabTitle;
private Object lastMinimizedHostedContent;
private String lastMinimizedTabTitle;
private BrowserCaptureRequest captureRequest;
```

Behavior to implement now:
- `openEmptyBrowser()` clears current hosted content and enters `OPEN_EMPTY`
- `captureHostedContent(...)` stores one hosted child and enters `OPEN_WITH_TABS`
- `minimizeBrowser()` stores the current hosted session and enters `MINIMIZED_WITH_TABS`
- `restoreBrowser()` reopens the minimized hosted session when one exists
- `closeBrowser()` clears both current and minimized session state

Do not add multi-tab structures in this task.

- [ ] **Step 4: Re-run the focused state test target**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.browser.BrowserManagerTest`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the session-state upgrade**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserManagerTest.java
git commit -m "feat: add phase 2 browser session state"
```

### Task 2: Fix `/guibrowser open` so it visibly opens after chat closes

**Files:**
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellController.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellHost.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/MinecraftBrowserShellHost.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/command/CommandGuiBrowser.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserShellControllerTest.java`

- [ ] **Step 1: Write failing controller tests for deferred browser opening**

```java
@Test
void commandOpenQueuesBrowserRootUntilFlush() {
    BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
    FakeHost host = new FakeHost(true, false);
    BrowserShellController controller = new BrowserShellController(manager, host);

    assertTrue(controller.requestBrowserOpenFromCommand());
    assertEquals(BrowserState.OPEN_EMPTY, manager.getState());
    assertEquals(0, host.openBrowserRootRequests);

    controller.flushDeferredUiActions();

    assertEquals(1, host.openBrowserRootRequests);
    assertTrue(host.browserRootActive);
}

@Test
void commandOpenRestoresMinimizedBrowserInsteadOfOpeningEmpty() {
    BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
    BrowserShellController controller = new BrowserShellController(manager, new FakeHost(true, false));

    manager.captureHostedContent(new Object(), "Chest");
    manager.minimizeBrowser();

    controller.requestBrowserOpenFromCommand();
    controller.flushDeferredUiActions();

    assertEquals(BrowserState.OPEN_WITH_TABS, manager.getState());
}
```

- [ ] **Step 2: Run the focused controller test target and confirm it fails**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.browser.BrowserShellControllerTest`

Expected: failure because there is no deferred-open path yet.

- [ ] **Step 3: Implement deferred browser-open requests**

Add controller methods like:

```java
public boolean requestBrowserOpenFromCommand() {
    if (!host.isInWorld()) return false;
    if (manager.getState() == BrowserState.MINIMIZED_WITH_TABS) {
        manager.restoreBrowser();
    } else if (manager.getState() == BrowserState.CLOSED) {
        manager.openEmptyBrowser();
    }
    pendingShowRoot = true;
    return true;
}

public void flushDeferredUiActions() {
    if (pendingShowRoot) {
        host.showBrowserRoot(this);
        pendingShowRoot = false;
    }
}
```

Then wire:
- `/guibrowser open` to `requestBrowserOpenFromCommand()` instead of `openBrowser()`
- `BrowserHotkeyHandler.onClientTick(...)` to always call `controller.flushDeferredUiActions()` once per END tick

This task is specifically to repair the currently invisible `/guibrowser open` behavior.

- [ ] **Step 4: Re-run the focused controller test target**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.browser.BrowserShellControllerTest`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Do a manual command-open smoke test and commit**

Run: `./gradlew.bat runClient`

Manual checks:
- join a world
- run `/guibrowser open`
- confirm the browser actually becomes the visible current GUI
- run `/guibrowser state` and confirm the state matches what is on screen

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/browser src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java src/main/java/com/zzhalex233/guibrowser/client/command/CommandGuiBrowser.java src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserShellControllerTest.java
git commit -m "fix: make guibrowser open visibly open the browser"
```

## Chunk 2: Switch Browser Chrome to `browser.png`

### Task 3: Replace placeholder rectangle chrome with atlas-based layout math

**Files:**
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayout.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeTexture.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayoutTest.java`

- [ ] **Step 1: Write failing atlas/layout tests**

```java
@Test
void contentAreaMatchesBrowserPngPageRegion() {
    BrowserWindowState window = BrowserWindowState.defaultWindow();
    BrowserChromeLayout.Rect content = BrowserChromeLayout.contentArea(window);

    assertEquals(176, content.getWidth());
    assertEquals(166, content.getHeight());
    assertEquals(window.getY() + 24, content.getY());
}

@Test
void firstTabAnchorsToBrowserLocalBottomLeftAtY27() {
    BrowserWindowState window = BrowserWindowState.defaultWindow();
    BrowserChromeLayout.Rect tab = BrowserChromeLayout.tab(window, 0);

    assertEquals(window.getX(), tab.getX());
    assertEquals(window.getY() + 27 - 23, tab.getY());
    assertEquals(28, tab.getWidth());
    assertEquals(23, tab.getHeight());
}
```

- [ ] **Step 2: Run the focused layout test target and confirm it fails**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.gui.BrowserChromeLayoutTest`

Expected: failure because the layout still uses Phase 1 placeholder dimensions.

- [ ] **Step 3: Implement browser atlas constants and on-screen rect math**

`BrowserChromeTexture` should define the atlas coordinates:

```java
public static final ResourceLocation ATLAS = new ResourceLocation(Reference.MOD_ID, "textures/gui/browser.png");
public static final int FRAME_WIDTH = 176;
public static final int FRAME_HEIGHT = 190;
public static final int PAGE_Y = 24;
public static final int PAGE_WIDTH = 176;
public static final int PAGE_HEIGHT = 166;
public static final int TAB_WIDTH = 28;
public static final int TAB_HEIGHT = 23;
public static final int TAB_STEP = 30;
```

`BrowserChromeLayout` should expose:
- frame rect
- page/content rect
- tab rect by index
- minimize button rect
- close button rect
- drag region rect

Update `BrowserWindowState.defaultWindow()` usage as needed so the window defaults to atlas size, not Phase 1 placeholder size.

- [ ] **Step 4: Re-run the focused layout test target**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.gui.BrowserChromeLayoutTest`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the atlas/layout foundation**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayout.java src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeTexture.java src/test/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayoutTest.java src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java
git commit -m "feat: add browser png chrome layout"
```

### Task 4: Render BrowserRootGui from `browser.png` and keep hover behavior simple

**Files:**
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserRootGui.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayout.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/MinecraftBrowserShellHost.java`

- [ ] **Step 1: Add a manual verification checklist to the task notes**

```text
- browser frame is drawn from browser.png instead of flat rectangles
- first tab uses the atlas tab sprite
- minimize and close have gray hover overlay, not alternate sprites
- empty browser still drags and still closes correctly
```

- [ ] **Step 2: Replace flat-color chrome rendering with atlas drawing**

`BrowserRootGui.drawScreen(...)` should:
- bind `BrowserChromeTexture.ATLAS`
- draw the browser frame texture
- draw one active tab when hosted content exists
- draw hover overlays with `Gui.drawRect(...)` using a translucent gray color over the button/tab rects
- keep the content-page rect as the child scissor viewport

Do not reintroduce a custom button state machine. Use plain hit testing plus the overlay.

- [ ] **Step 3: Keep empty-browser rendering intact on top of the new atlas**

The empty browser should still show text hints inside the page region, but the chrome itself must come from `browser.png`.

- [ ] **Step 4: Run build + manual chrome verification**

Run: `./gradlew.bat build`

Then run: `./gradlew.bat runClient`

Manual checks:
- `/guibrowser open` shows the atlas-based browser shell
- hover on minimize and close paints a gray overlay
- empty browser drag still works
- close button still closes the shell

- [ ] **Step 5: Commit the atlas-rendered browser root**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserRootGui.java src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayout.java src/main/java/com/zzhalex233/guibrowser/client/browser/MinecraftBrowserShellHost.java
git commit -m "feat: render browser chrome from browser png"
```

## Chunk 3: Capture-Only Hotkey and One-Shot Original Interaction

### Task 5: Change the hotkey from “open browser” to “capture next original interaction”

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserCaptureRequest.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellController.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserKeybinds.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfig.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfigLoader.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserManagerTest.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserShellControllerTest.java`

- [ ] **Step 1: Write failing capture-request lifecycle tests**

```java
@Test
void armingCaptureDoesNotOpenTheBrowserByItself() {
    BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());

    manager.armCaptureRequest(BrowserCaptureRequest.hotkeyRequest());

    assertEquals(BrowserState.CLOSED, manager.getState());
    assertTrue(manager.hasPendingCaptureRequest());
}

@Test
void captureRequestExpiresAfterTimeout() {
    BrowserManager manager = BrowserManager.createForTests(BrowserConfig.defaults());
    manager.armCaptureRequest(BrowserCaptureRequest.hotkeyRequest());

    for (int i = 0; i < BrowserCaptureRequest.DEFAULT_TICKS_TO_LIVE; i++) {
        manager.tickCaptureRequest();
    }

    assertFalse(manager.hasPendingCaptureRequest());
}
```

- [ ] **Step 2: Run the focused browser-state/controller tests and confirm they fail**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.browser.BrowserManagerTest --tests com.zzhalex233.guibrowser.client.browser.BrowserShellControllerTest`

Expected: failure because capture-request state and timeout handling do not exist.

- [ ] **Step 3: Implement capture-request state and rename hotkey semantics**

`BrowserCaptureRequest` should be a tiny value object, not a subsystem:

```java
public final class BrowserCaptureRequest {
    public static final int DEFAULT_TICKS_TO_LIVE = 10;

    private final int ticksRemaining;
    private final boolean triggeredByHotkey;
}
```

Then wire:
- `BrowserKeybinds.OPEN_BROWSER` -> rename to a capture-focused binding in code and user-facing label
- `BrowserHotkeyHandler` -> no more `toggleBrowser()`
- config loader -> read a new capture-key property, but fall back to the old key property once for compatibility if it exists
- controller -> expose `requestHotkeyCapture()` and `tickCaptureRequest()` helpers

This task must preserve `/guibrowser open`; only the hotkey behavior changes.

- [ ] **Step 4: Re-run the focused browser-state/controller tests**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.browser.BrowserManagerTest --tests com.zzhalex233.guibrowser.client.browser.BrowserShellControllerTest`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit the capture-request layer**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/browser src/main/java/com/zzhalex233/guibrowser/client/input src/main/java/com/zzhalex233/guibrowser/config src/test/java/com/zzhalex233/guibrowser/client/browser
git commit -m "feat: add capture-only hotkey state"
```

### Task 6: Trigger the original interaction and intercept only that resulting GUI

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/browser/MinecraftInteractionInvoker.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserRootGui.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellController.java`

- [ ] **Step 1: Add a manual verification checklist to the task notes**

```text
- pressing the hotkey with no GUI-capable target does nothing visible
- pressing the hotkey on a chest opens the chest inside the browser
- ordinary right-click on the same chest still opens vanilla GUI directly
- child GUI internal navigation replaces the current hosted tab
- child close returns to empty browser instead of closing the whole browser
```

- [ ] **Step 2: Implement the original interaction invoker**

`MinecraftInteractionInvoker` should be a minimal reflection bridge around `Minecraft.rightClickMouse()` (or the equivalent original right-click entry point if local source inspection shows a better one).

```java
public void triggerPrimaryGuiInteraction() {
    rightClickMouseMethod.invoke(Minecraft.getMinecraft());
}
```

Do not fake GUI classes here. The whole point is to reuse the original game/mod interaction path.

- [ ] **Step 3: Gate `GuiOpenEvent` interception behind the active capture request**

`ClientForgeEventHandler` should only rewrite an incoming GUI when either:
- a hotkey capture request is currently active, or
- the browser is currently delegating to an already-hosted child GUI and the child opened a replacement GUI

Everything else must pass through untouched.

Core shape:

```java
if (controller.hasPendingCaptureRequest() && shouldCapture(incomingGui)) {
    controller.consumeCaptureRequest();
    controller.captureHostedContent(incomingGui, deriveTabTitle(incomingGui));
    return new BrowserRootGui(controller);
}
```

The handler must *not* auto-capture ordinary vanilla right-click opens anymore.

- [ ] **Step 4: Keep child-hosted lifecycle behavior correct**

`BrowserRootGui` must continue to:
- call `setWorldAndResolution(...)` for the hosted child when the page size changes
- forward render/update/input into the child with `currentScreen` masquerading
- treat child-opened new GUIs as replacing the current hosted content
- treat child closing itself as returning to `OPEN_EMPTY`

This task is where the current half-finished Phase 2 code should be cleaned up so it matches the new capture gating instead of always-on interception.

- [ ] **Step 5: Run automated tests, then do end-to-end manual capture verification, then commit**

Run automated tests: `./gradlew.bat test`

Then run: `./gradlew.bat runClient`

Manual checks:
- `/guibrowser open` opens the empty browser
- hotkey alone does not open the browser
- hotkey on a chest opens the chest in the browser
- plain right-click on a chest still opens the vanilla chest GUI directly
- hotkey on a GUI-capable held item opens that GUI in the browser
- closing the child GUI returns to an empty browser shell

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/browser/MinecraftInteractionInvoker.java src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserRootGui.java src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellController.java
git commit -m "feat: capture original gui opens into the browser"
```

## Chunk 4: Final Verification and Hand-off

### Task 7: Verify Phase 2 behavior against the approved constraints

**Files:**
- Modify: `docs/superpowers/plans/2026-04-10-gui-browser-phase-2-single-tab-hosting.md` (check off completed steps during implementation)
- Modify: `docs/superpowers/specs/2026-04-09-gui-browser-foundation-design.md` (only if implementation reveals a necessary clarification)

- [ ] **Step 1: Re-read the approved Phase 2 constraints and confirm implementation scope stayed tight**

Checklist:
- `/guibrowser open` visibly opens the browser
- hotkey no longer opens empty browser by itself
- hotkey capture invokes original interaction instead of constructing GUIs manually
- ordinary right-click remains unchanged
- chrome is drawn from `browser.png`
- still single-tab only
- still no multi-browser windows

- [ ] **Step 2: Run the full verification build**

Run: `./gradlew.bat build`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Run a final Phase 2 manual smoke test**

Run: `./gradlew.bat runClient`

Manual checks:
- `/guibrowser open`
- `/guibrowser close`
- `/guibrowser state`
- hotkey capture on a chest
- ordinary right-click on the same chest
- browser minimize then `/guibrowser open` restore
- browser close then `/guibrowser open` empty shell

- [ ] **Step 4: Update this plan file by checking off completed steps**

Update only the steps actually completed during implementation.

- [ ] **Step 5: Commit the verified Phase 2 slice**

```bash
git add docs/superpowers/plans/2026-04-10-gui-browser-phase-2-single-tab-hosting.md docs/superpowers/specs/2026-04-09-gui-browser-foundation-design.md src
git commit -m "feat: implement gui browser phase 2 single-tab hosting"
```

Plan complete and saved to `docs/superpowers/plans/2026-04-10-gui-browser-phase-2-single-tab-hosting.md`. Ready to execute?
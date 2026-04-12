# GUI Browser Global Tab Overlay Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the standalone browser-shell GUI with a global browser-style tab strip injected into tracked Minecraft GUIs, where GUI sessions are hidden into cache on close, can be restored as live tabs, and expose bookmark/history controls.

**Architecture:** The new implementation treats every tracked `GuiScreen` as a live session owned by a global session manager instead of embedding child GUIs inside `BrowserRootGui`. A Mixin-driven lifecycle bridge intercepts `Minecraft.displayGuiScreen(...)` and `GuiScreen` render/input/close paths so the current GUI can be hidden without destruction, while a shared chrome overlay draws tabs, bookmark controls, and history on top of the active screen. The old browser-shell path is retired after the overlay path is complete; only reusable state, rendering, and compatibility helpers survive.

**Tech Stack:** Java 21, Cleanroom Forge 1.12.2, Mixin/coremod bootstrap, Minecraft `GuiScreen`/`GuiContainer`, Forge client lifecycle hooks, JUnit 6

---

Supersedes:
- `docs/superpowers/specs/2026-04-09-gui-browser-foundation-design.md`
- `docs/superpowers/plans/2026-04-09-gui-browser-phase-1-browser-shell.md`
- `docs/superpowers/plans/2026-04-10-gui-browser-phase-2-single-tab-hosting.md`

Assumptions locked by this plan:
- Any GUI leaving the foreground is hidden into cache by default instead of being destroyed.
- Only explicit tab-close actions (middle-click tab or tab close affordance) destroy a cached GUI session, plus unavoidable world/disconnect teardown.
- Tabs are restored as the original `GuiScreen` instances whenever the underlying session is still valid.
- Bookmarks and history are runtime-scoped to the current client world session for this implementation; disk persistence is out of scope until live-session semantics are stable.
- Tracking is broad but not literally every `GuiScreen`; hard exclusions include main menu, world-selection/loading/connecting/disconnect flows, crash/death flows, and the mod's own popup/panel screens.

## Planned File Structure

### Create
- `src/main/java/com/zzhalex233/guibrowser/core/GuiBrowserLoadingPlugin.java`
- `src/main/resources/mixins.guibrowser.json`
- `src/main/java/com/zzhalex233/guibrowser/mixin/MixinMinecraft.java`
- `src/main/java/com/zzhalex233/guibrowser/mixin/MixinGuiScreen.java`
- `src/main/java/com/zzhalex233/guibrowser/mixin/MixinGuiContainer.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSession.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionId.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionManager.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionTitleResolver.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiTrackingPolicy.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridge.java`
- `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeLayout.java`
- `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeRenderer.java`
- `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeOverlayController.java`
- `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeTarget.java`
- `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutPolicy.java`
- `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutState.java`
- `src/main/java/com/zzhalex233/guibrowser/client/history/GuiBookmarkEntry.java`
- `src/main/java/com/zzhalex233/guibrowser/client/history/GuiBookmarkStore.java`
- `src/main/java/com/zzhalex233/guibrowser/client/history/GuiHistoryEntry.java`
- `src/main/java/com/zzhalex233/guibrowser/client/history/GuiHistoryStore.java`
- `src/test/java/com/zzhalex233/guibrowser/client/session/GuiSessionManagerTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/session/GuiTrackingPolicyTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridgeTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeLayoutTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeOverlayControllerTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutPolicyTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/history/GuiBookmarkStoreTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/history/GuiHistoryStoreTest.java`

### Modify
- `gradle.properties`
- `src/main/java/com/zzhalex233/guibrowser/proxy/ClientProxy.java`
- `src/main/java/com/zzhalex233/guibrowser/client/command/CommandGuiBrowser.java`
- `src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java`
- `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserKeybinds.java`
- `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java`
- `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfig.java`
- `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfigLoader.java`
- `src/test/java/com/zzhalex233/guibrowser/config/BrowserConfigLoaderTest.java`

### Delete / Retire
- `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserCaptureRequest.java`
- `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellController.java`
- `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellHost.java`
- `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserState.java`
- `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserWindowState.java`
- `src/main/java/com/zzhalex233/guibrowser/client/browser/MinecraftBrowserShellHost.java`
- `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayout.java`
- `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeTexture.java`
- `src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserRootGui.java`
- `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserManagerTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserShellControllerTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandlerTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayoutTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandlerTest.java`

## Chunk 1: Replace the Shell-Centric State Model with a Global Session Core

### Task 1: Introduce trackable GUI session primitives

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionId.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSession.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionTitleResolver.java`
- Create: `src/test/java/com/zzhalex233/guibrowser/client/session/GuiSessionManagerTest.java`

- [x] **Step 1: Write the first failing session test**

```java
@Test
void registerForegroundSessionCreatesLiveTabForOriginalGuiInstance() {
    GuiSessionManager manager = new GuiSessionManager();
    GuiScreen gui = new GuiChest(null, null);

    GuiSession session = manager.registerOpenedSession(gui, "Chest");

    assertSame(gui, session.getScreen());
    assertTrue(session.isForeground());
    assertFalse(session.isHidden());
}
```

- [x] **Step 2: Run the focused test target and verify it fails**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.session.GuiSessionManagerTest`
Expected: FAIL because the session classes do not exist yet.

- [x] **Step 3: Implement minimal session primitives**

```java
public final class GuiSession {
    private final GuiSessionId id;
    private final GuiScreen screen;
    private final String title;
    private boolean foreground;
    private boolean hidden;
}
```

Keep `GuiSession` small: identity, original `GuiScreen`, title, timestamps, bookmark flag, hidden/destroyed markers.

- [x] **Step 4: Re-run the focused session test target**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.session.GuiSessionManagerTest`
Expected: PASS for the new test, with later tests still absent.

- [x] **Step 5: Commit the session primitive foundation**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/session src/test/java/com/zzhalex233/guibrowser/client/session/GuiSessionManagerTest.java
git commit -m "feat: add gui session primitives"
```

### Task 2: Replace `BrowserManager` with `GuiSessionManager`

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionManager.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/client/session/GuiSessionManagerTest.java`
- Delete: `src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java`
- Delete: `src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserManagerTest.java`

- [x] **Step 1: Add failing lifecycle tests for hide, activate, and destroy**

```java
@Test
void hidingForegroundSessionKeepsOriginalInstanceCached() {
    GuiSessionManager manager = new GuiSessionManager();
    GuiScreen gui = new GuiChest(null, null);
    GuiSession session = manager.registerOpenedSession(gui, "Chest");

    manager.hideSession(session.getId());

    assertTrue(manager.getSession(session.getId()).isHidden());
    assertSame(gui, manager.getSession(session.getId()).getScreen());
}

@Test
void explicitTabCloseDestroysOnlyThatSession() {
    GuiSessionManager manager = new GuiSessionManager();
    GuiSession first = manager.registerOpenedSession(new GuiChat(), "Chat");
    GuiSession second = manager.registerOpenedSession(new GuiChat(), "Chat 2");

    manager.destroySession(first.getId());

    assertNull(manager.findSession(first.getId()));
    assertNotNull(manager.findSession(second.getId()));
}
```

- [x] **Step 2: Run the focused session target and verify it fails for missing manager behavior**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.session.GuiSessionManagerTest`
Expected: FAIL because hide/destroy/lookup behavior is incomplete.

- [x] **Step 3: Implement the manager around live-session semantics**

`GuiSessionManager` should own:

```java
private final LinkedHashMap<GuiSessionId, GuiSession> sessions;
private GuiSessionId foregroundSessionId;
private GuiSessionId lastActivatedSessionId;
```

Required operations:
- `registerOpenedSession(GuiScreen screen, String title)`
- `hideSession(GuiSessionId id)`
- `activateSession(GuiSessionId id)`
- `destroySession(GuiSessionId id)`
- `listVisibleTabs()`
- `clearForWorldUnload()`

Do not add bookmark/history persistence in this task.

- [x] **Step 4: Re-run the session target and make it green**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.session.GuiSessionManagerTest`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit the global session manager**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/session src/test/java/com/zzhalex233/guibrowser/client/session/GuiSessionManagerTest.java
git rm src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserManager.java src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserManagerTest.java
git commit -m "feat: replace browser manager with gui session manager"
```

### Task 3: Add a hard tracking policy for which GUIs are eligible

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/session/GuiTrackingPolicy.java`
- Create: `src/test/java/com/zzhalex233/guibrowser/client/session/GuiTrackingPolicyTest.java`

- [x] **Step 1: Write failing policy tests**

```java
@Test
void mainMenuAndConnectionScreensAreNeverTracked() {
    assertFalse(GuiTrackingPolicy.shouldTrack(new GuiMainMenu()));
    assertFalse(GuiTrackingPolicy.shouldTrack(new GuiConnecting(null, null, null)));
}

@Test
void regularContainerAndRegularGuiScreensAreTracked() {
    assertTrue(GuiTrackingPolicy.shouldTrack(new GuiChat()));
}
```

- [x] **Step 2: Run the focused policy test target and verify it fails**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.session.GuiTrackingPolicyTest`
Expected: FAIL because the policy class does not exist yet.

- [x] **Step 3: Implement the minimal policy**

The policy should return `false` for:
- main menu / multiplayer / world select / loading / connecting / disconnect
- game over / crash / progress / download terrain
- any mod-owned popup screens used for bookmark/history panels

Everything else is tracked by default.

- [x] **Step 4: Re-run the focused policy test target**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.session.GuiTrackingPolicyTest`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit the tracking policy**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/session/GuiTrackingPolicy.java src/test/java/com/zzhalex233/guibrowser/client/session/GuiTrackingPolicyTest.java
git commit -m "feat: add gui tracking policy"
```
## Chunk 2: Intercept GUI Open/Close Transitions Without Destroying Sessions

### Task 4: Enable mixin/coremod bootstrap for lifecycle interception

**Files:**
- Modify: `gradle.properties`
- Create: `src/main/java/com/zzhalex233/guibrowser/core/GuiBrowserLoadingPlugin.java`
- Create: `src/main/resources/mixins.guibrowser.json`
- Modify: `src/main/java/com/zzhalex233/guibrowser/proxy/ClientProxy.java`

- [x] **Step 1: Add a small bootstrap verification test target**

Create a cheap config assertion inside `BrowserConfigLoaderTest` or a new bootstrap test that checks the declared mixin config resource exists on the classpath.

```java
@Test
void mixinConfigResourceExists() {
    assertNotNull(getClass().getClassLoader().getResource("mixins.guibrowser.json"));
}
```

- [x] **Step 2: Run the focused bootstrap test and verify it fails**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.config.BrowserConfigLoaderTest`
Expected: FAIL because the mixin config resource is absent.

- [x] **Step 3: Enable the coremod bootstrap**

Set in `gradle.properties`:

```properties
is_coremod = true
coremod_includes_mod = true
coremod_plugin_class_name = com.zzhalex233.guibrowser.core.GuiBrowserLoadingPlugin
```

Add a loading plugin that registers `mixins.guibrowser.json` during startup.

- [x] **Step 4: Re-run the focused bootstrap test and then compile Java**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.config.BrowserConfigLoaderTest`
Run: `./gradlew.bat compileJava`
Expected: both commands succeed.

- [x] **Step 5: Commit the mixin bootstrap**

```bash
git add gradle.properties src/main/java/com/zzhalex233/guibrowser/core/GuiBrowserLoadingPlugin.java src/main/resources/mixins.guibrowser.json src/main/java/com/zzhalex233/guibrowser/proxy/ClientProxy.java src/test/java/com/zzhalex233/guibrowser/config/BrowserConfigLoaderTest.java
git commit -m "feat: enable mixin bootstrap for gui overlay"
```

### Task 5: Build a lifecycle bridge around `Minecraft.displayGuiScreen(...)`

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridge.java`
- Create: `src/test/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridgeTest.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/mixin/MixinMinecraft.java`

- [x] **Step 1: Write failing bridge tests for open, replace, and hide-on-close**

```java
@Test
void closingTrackedScreenHidesSessionInsteadOfDestroyingIt() {
    GuiSessionManager manager = new GuiSessionManager();
    GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);
    GuiScreen current = new GuiChat();

    bridge.onBeforeDisplay(current, null, false);

    assertEquals(1, manager.listVisibleTabs().size());
    assertTrue(manager.listVisibleTabs().get(0).isHidden());
}

@Test
void openingNewTrackedScreenKeepsPreviousSessionCached() {
    GuiSessionManager manager = new GuiSessionManager();
    GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);
    GuiScreen first = new GuiChat();
    GuiScreen second = new GuiChat();

    bridge.onBeforeDisplay(null, first, false);
    bridge.onBeforeDisplay(first, second, false);

    assertEquals(2, manager.listAllSessions().size());
    assertSame(second, manager.getForegroundSession().getScreen());
}
```

- [x] **Step 2: Run the focused lifecycle bridge tests and verify they fail**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.session.GuiLifecycleBridgeTest`
Expected: FAIL because the bridge and mixin hook do not exist.

- [x] **Step 3: Implement the bridge before wiring the mixin**

`GuiLifecycleBridge` should expose methods like:

```java
public TransitionDecision onBeforeDisplay(GuiScreen current, GuiScreen incoming, boolean explicitDestroy);
public void onAfterDisplay(GuiScreen nowVisible);
public void onWorldUnload();
```

The bridge decides whether to hide the current session, reuse an existing session, or allow destruction.

- [x] **Step 4: Wire `MixinMinecraft` to call the bridge**

Inject at `displayGuiScreen` head so the mod can:
- register newly opened trackable screens
- prevent old tracked screens from receiving full destroy-on-close unless the close is explicit
- keep world-disconnect flows unmodified

- [x] **Step 5: Re-run the lifecycle bridge tests and then `compileJava`**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.session.GuiLifecycleBridgeTest`
Run: `./gradlew.bat compileJava`
Expected: both commands succeed.

- [x] **Step 6: Commit the lifecycle bridge and mixin**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridge.java src/main/java/com/zzhalex233/guibrowser/mixin/MixinMinecraft.java src/test/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridgeTest.java
git commit -m "feat: intercept gui display transitions for session caching"
```

### Task 6: Add explicit-destroy paths for tab close and world teardown

**Files:**
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionManager.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridge.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridgeTest.java`

- [x] **Step 1: Add failing tests for explicit destroy vs implicit hide**

```java
@Test
void explicitTabDestroySkipsHideAndRemovesSession() {
    GuiSessionManager manager = new GuiSessionManager();
    GuiLifecycleBridge bridge = new GuiLifecycleBridge(manager);
    GuiSession session = manager.registerOpenedSession(new GuiChat(), "Chat");

    bridge.destroySessionFromTab(session.getId());

    assertNull(manager.findSession(session.getId()));
}
```

- [x] **Step 2: Run the focused lifecycle tests and verify they fail**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.session.GuiLifecycleBridgeTest`
Expected: FAIL because explicit destroy semantics are missing.

- [x] **Step 3: Implement explicit destroy state and world cleanup hooks**

Add bridge methods for:
- `destroySessionFromTab(GuiSessionId id)`
- `destroyForegroundSession()`
- `clearForWorldUnload()`

`ClientForgeEventHandler` should now focus on non-mixin lifecycle events like world unload, disconnect, and debug cleanup instead of hijacking `GuiOpenEvent`.

- [x] **Step 4: Re-run the focused lifecycle tests**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.session.GuiLifecycleBridgeTest`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit explicit destroy and unload cleanup**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionManager.java src/main/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridge.java src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java src/test/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridgeTest.java
git commit -m "feat: add explicit destroy and unload cleanup for gui sessions"
```

## Chunk 3: Draw a Shared Browser-Like Chrome Overlay on Top of Active GUIs

### Task 7: Build overlay layout and hit-testing without reusing the old shell layout

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeLayout.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeTarget.java`
- Create: `src/test/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeLayoutTest.java`

- [x] **Step 1: Write failing layout tests for tabs and utility buttons**

```java
@Test
void tabsLayOutLeftToRightAcrossTopBar() {
    GuiChromeLayout layout = new GuiChromeLayout(320, 240);

    Rect first = layout.tabRect(0);
    Rect second = layout.tabRect(1);

    assertTrue(second.getX() > first.getRight());
    assertEquals(layout.topBarRect().getY(), first.getY());
}

@Test
void bookmarkAndHistoryButtonsStayOnRightEdge() {
    GuiChromeLayout layout = new GuiChromeLayout(320, 240);

    assertTrue(layout.historyButtonRect().getRight() <= 320);
    assertTrue(layout.bookmarkButtonRect().getRight() < layout.historyButtonRect().getX());
}
```

- [x] **Step 2: Run the focused layout test target and verify it fails**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.chrome.GuiChromeLayoutTest`
Expected: FAIL because the new overlay layout classes do not exist.

- [x] **Step 3: Implement the top-bar layout model**

The layout should own:
- top bar height
- tab width policy (fixed width with title trimming)
- close hotspot per tab
- bookmark button rect
- history button rect
- overflow reserve if there are many tabs

Do not reuse `client/gui/BrowserChromeLayout.java`; that file is shell-window specific and should be retired later.

- [x] **Step 4: Re-run the focused layout tests**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.chrome.GuiChromeLayoutTest`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit the overlay layout model**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeLayout.java src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeTarget.java src/test/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeLayoutTest.java
git commit -m "feat: add global gui chrome layout"
```
### Task 8: Add overlay rendering and input routing on top of any active tracked GUI

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeRenderer.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeOverlayController.java`
- Create: `src/test/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeOverlayControllerTest.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/mixin/MixinGuiScreen.java`

- [x] **Step 1: Write failing controller tests for tab switch and middle-click close**

```java
@Test
void middleClickTabRequestsExplicitDestroy() {
    GuiSessionManager manager = new GuiSessionManager();
    GuiChromeOverlayController controller = new GuiChromeOverlayController(manager);
    GuiSession session = manager.registerOpenedSession(new GuiChat(), "Chat");

    controller.handleTabMiddleClick(session.getId());

    assertNull(manager.findSession(session.getId()));
}

@Test
void leftClickTabMarksThatSessionForeground() {
    GuiSessionManager manager = new GuiSessionManager();
    GuiChromeOverlayController controller = new GuiChromeOverlayController(manager);
    GuiSession first = manager.registerOpenedSession(new GuiChat(), "A");
    GuiSession second = manager.registerOpenedSession(new GuiChat(), "B");

    controller.handleTabLeftClick(first.getId());

    assertEquals(first.getId(), manager.getForegroundSession().getId());
}
```

- [x] **Step 2: Run the focused overlay controller tests and verify they fail**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.chrome.GuiChromeOverlayControllerTest`
Expected: FAIL because the overlay controller does not exist.

- [x] **Step 3: Implement the controller and renderer**

`GuiChromeOverlayController` should be pure UI logic:
- expose current tabs from `GuiSessionManager`
- resolve mouse hits to `GuiChromeTarget`
- destroy tabs on middle-click
- activate tabs on left-click
- open bookmark/history popup state

`GuiChromeRenderer` should only draw the top chrome; keep it decoupled from lifecycle logic.

- [x] **Step 4: Wire `MixinGuiScreen` to render and route input**

Inject after tracked screens draw so the overlay appears on top.
Inject mouse handling so tab hits are consumed before the underlying GUI handles them.
Do not inject the overlay into excluded screens from `GuiTrackingPolicy`.

- [x] **Step 5: Re-run the focused overlay tests and then `compileJava`**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.chrome.GuiChromeOverlayControllerTest`
Run: `./gradlew.bat compileJava`
Expected: both commands succeed.

- [x] **Step 6: Commit the global overlay rendering path**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/chrome src/main/java/com/zzhalex233/guibrowser/mixin/MixinGuiScreen.java src/test/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeOverlayControllerTest.java
git commit -m "feat: inject browser-like chrome on tracked gui screens"
```

## Chunk 4: Make the Top Bar Adaptive So It Overlays or Pushes Down Depending on Risk

### Task 9: Add a generic layout policy instead of per-mod special cases

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutPolicy.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutState.java`
- Create: `src/test/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutPolicyTest.java`

- [x] **Step 1: Write failing policy tests for overlay vs push-down**

```java
@Test
void regularGuiScreensDefaultToOverlayMode() {
    GuiLayoutState state = GuiLayoutPolicy.forScreen(new GuiChat(), 320, 240);
    assertEquals(LayoutMode.OVERLAY, state.getMode());
}

@Test
void guiContainersUsePushDownWhenTopChromeWouldOverlapInteractiveArea() {
    GuiChest chest = new GuiChest(null, null);
    GuiLayoutState state = GuiLayoutPolicy.forScreen(chest, 320, 240);
    assertEquals(LayoutMode.PUSH_DOWN, state.getMode());
}
```

- [x] **Step 2: Run the focused layout policy tests and verify they fail**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.chrome.GuiLayoutPolicyTest`
Expected: FAIL because the policy classes do not exist.

- [x] **Step 3: Implement the generic policy**

Rules for first implementation:
- default `OVERLAY` for ordinary `GuiScreen`
- `PUSH_DOWN` for `GuiContainer` when chrome would intersect the container's top interactive band
- never use per-mod class name adapters here

- [x] **Step 4: Re-run the focused policy tests**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.chrome.GuiLayoutPolicyTest`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit the layout policy**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutPolicy.java src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutState.java src/test/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutPolicyTest.java
git commit -m "feat: add adaptive gui chrome layout policy"
```

### Task 10: Apply push-down offsets to generic containers without mod-specific adapters

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/mixin/MixinGuiContainer.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeOverlayController.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutPolicy.java`

- [x] **Step 1: Add a manual verification checklist to the task notes**

```text
- chest top slots are not covered by the tab bar
- inventory recipe/book buttons still receive clicks
- generic non-container GUIs still draw with overlay mode
- no per-mod adapter classes are introduced
```

- [x] **Step 2: Implement container push-down hooks**

Use `MixinGuiContainer` to adjust `guiTop` / effective mouse translation when `GuiLayoutPolicy` says `PUSH_DOWN`.
Keep the offset localized to tracked container screens only.

- [x] **Step 3: Run the project test suite**

Run: `./gradlew.bat test`
Expected: BUILD SUCCESSFUL.

- [x] **Step 4: Run the client and manually verify chest/inventory screens**

Run: `./gradlew.bat runClient`
Manual checks:
- open inventory
- open chest
- open hopper/dispenser if available
- confirm the chrome does not block the top interactive area

- [x] **Step 5: Commit adaptive container offset handling**

```bash
git add src/main/java/com/zzhalex233/guibrowser/mixin/MixinGuiContainer.java src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeOverlayController.java src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiLayoutPolicy.java
git commit -m "feat: push down container guis when chrome would overlap"
```

## Chunk 5: Add Runtime Bookmarks and History to the Shared Chrome

### Task 11: Add history recording for opens, hides, restores, and destroys

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/history/GuiHistoryEntry.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/history/GuiHistoryStore.java`
- Create: `src/test/java/com/zzhalex233/guibrowser/client/history/GuiHistoryStoreTest.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionManager.java`

- [x] **Step 1: Write failing history tests**

```java
@Test
void historyRecordsSessionOpenHideAndDestroy() {
    GuiHistoryStore store = new GuiHistoryStore();
    GuiSessionManager manager = new GuiSessionManager(store);
    GuiSession session = manager.registerOpenedSession(new GuiChat(), "Chat");

    manager.hideSession(session.getId());
    manager.destroySession(session.getId());

    assertEquals(3, store.entries().size());
}
```

- [x] **Step 2: Run the focused history tests and verify they fail**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.history.GuiHistoryStoreTest`
Expected: FAIL because the history store does not exist.

- [x] **Step 3: Implement runtime history recording**

Record at least:
- opened
- activated
- hidden
- explicitly closed
- invalidated on world unload

Keep the history in memory only.

- [x] **Step 4: Re-run the focused history tests**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.history.GuiHistoryStoreTest`
Expected: BUILD SUCCESSFUL.

- [x] **Step 5: Commit runtime history support**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/history src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionManager.java src/test/java/com/zzhalex233/guibrowser/client/history/GuiHistoryStoreTest.java
git commit -m "feat: record gui session history"
```
### Task 12: Add bookmark toggling and expose both histories in the top chrome

**Files:**
- Create: `src/main/java/com/zzhalex233/guibrowser/client/history/GuiBookmarkEntry.java`
- Create: `src/main/java/com/zzhalex233/guibrowser/client/history/GuiBookmarkStore.java`
- Create: `src/test/java/com/zzhalex233/guibrowser/client/history/GuiBookmarkStoreTest.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeOverlayController.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeRenderer.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/chrome/GuiChromeLayout.java`

- [x] **Step 1: Write failing bookmark tests**

```java
@Test
void bookmarkingActiveSessionMarksItPinnedWithoutDestroyingInstance() {
    GuiBookmarkStore store = new GuiBookmarkStore();
    GuiSessionManager manager = new GuiSessionManager(new GuiHistoryStore(), store);
    GuiSession session = manager.registerOpenedSession(new GuiChat(), "Chat");

    manager.toggleBookmark(session.getId());

    assertTrue(store.isBookmarked(session.getId()));
    assertSame(session.getScreen(), manager.getForegroundSession().getScreen());
}
```

- [x] **Step 2: Run the focused bookmark tests and verify they fail**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.history.GuiBookmarkStoreTest`
Expected: FAIL because the bookmark store does not exist.

- [x] **Step 3: Implement bookmark state and top-bar panels**

Required behavior:
- toggle bookmark on the active tab from the chrome
- show a small history popup panel from the top bar
- show bookmarked sessions before unbookmarked sessions when both are visible
- keep bookmarks runtime-scoped; do not add disk serialization

- [x] **Step 4: Re-run the focused bookmark tests and then the full test suite**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.client.history.GuiBookmarkStoreTest`
Run: `./gradlew.bat test`
Expected: both commands succeed.

- [x] **Step 5: Commit bookmark and history UI support**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/history src/main/java/com/zzhalex233/guibrowser/client/chrome src/test/java/com/zzhalex233/guibrowser/client/history/GuiBookmarkStoreTest.java
git commit -m "feat: add runtime bookmarks and history panels"
```

## Chunk 6: Remove the Old Shell Path and Reframe Commands, Config, and Verification

### Task 13: Delete obsolete shell-only code and repurpose command/config entrypoints

**Files:**
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/command/CommandGuiBrowser.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserKeybinds.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfig.java`
- Modify: `src/main/java/com/zzhalex233/guibrowser/config/BrowserConfigLoader.java`
- Modify: `src/test/java/com/zzhalex233/guibrowser/config/BrowserConfigLoaderTest.java`
- Delete: old shell/browser classes and their tests listed in the file structure above

- [x] **Step 1: Write failing config/command tests for the new semantics**

Add tests that verify:
- shell-specific `open browser` semantics are gone
- config now stores overlay height, max session count, and optional history panel hotkey instead of browser-shell state
- `/guibrowser clear` destroys cached sessions
- `/guibrowser history` reports current runtime history size

- [x] **Step 2: Run the focused config tests and verify they fail**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.config.BrowserConfigLoaderTest`
Expected: FAIL because the old browser-shell config schema still exists.

- [x] **Step 3: Implement the new command/config surface and delete the shell path**

Recommended config fields:

```java
private final int topBarHeight;
private final int maxCachedSessions;
private final boolean enableBookmarks;
private final boolean enableHistoryPanel;
private final int historyHotkey;
```

Recommended commands:
- `/guibrowser clear`
- `/guibrowser history`
- `/guibrowser tabs`
- `/guibrowser debug`

Do not keep `/guibrowser open` as a primary user path.

- [x] **Step 4: Re-run the focused config tests and then the full test suite**

Run: `./gradlew.bat test --tests com.zzhalex233.guibrowser.config.BrowserConfigLoaderTest`
Run: `./gradlew.bat test`
Expected: both commands succeed.

- [x] **Step 5: Commit the shell removal and entrypoint rewrite**

```bash
git add src/main/java/com/zzhalex233/guibrowser/client/command/CommandGuiBrowser.java src/main/java/com/zzhalex233/guibrowser/client/input/BrowserKeybinds.java src/main/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandler.java src/main/java/com/zzhalex233/guibrowser/config src/test/java/com/zzhalex233/guibrowser/config/BrowserConfigLoaderTest.java src/main/java/com/zzhalex233/guibrowser/client src/main/java/com/zzhalex233/guibrowser/mixin src/test/java/com/zzhalex233/guibrowser/client src/test/java/com/zzhalex233/guibrowser/client/history src/test/java/com/zzhalex233/guibrowser/client/session
git rm src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserCaptureRequest.java src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellController.java src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserShellHost.java src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserState.java src/main/java/com/zzhalex233/guibrowser/client/browser/BrowserWindowState.java src/main/java/com/zzhalex233/guibrowser/client/browser/MinecraftBrowserShellHost.java src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayout.java src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeTexture.java src/main/java/com/zzhalex233/guibrowser/client/gui/BrowserRootGui.java src/test/java/com/zzhalex233/guibrowser/client/browser/BrowserShellControllerTest.java src/test/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandlerTest.java src/test/java/com/zzhalex233/guibrowser/client/gui/BrowserChromeLayoutTest.java src/test/java/com/zzhalex233/guibrowser/client/input/BrowserHotkeyHandlerTest.java
git commit -m "refactor: replace standalone browser shell with global gui tabs"
```

### Task 14: Run final verification against the new product shape

**Files:**
- Modify: `docs/superpowers/plans/2026-04-11-gui-browser-global-tab-overlay.md`

- [x] **Step 1: Re-read the locked assumptions and confirm the implementation stayed on the new architecture**

Checklist:
- no standalone browser shell remains
- all tracked GUIs show the shared top chrome
- foreground close hides to cache by default
- only explicit tab close destroys a session
- tab switching restores original `GuiScreen` instances
- bookmarks/history are runtime features inside the overlay
- no mod-specific adapters were introduced

- [x] **Step 2: Run the full automated verification build**

Run: `./gradlew.bat build`
Expected: BUILD SUCCESSFUL.

- [x] **Step 3: Run the client and execute the manual smoke matrix**

Run: `./gradlew.bat runClient`
Manual checks:
- open inventory, chest, hopper/dispenser, and at least one non-container GUI
- close each GUI via its normal close path and confirm it becomes a cached tab instead of disappearing forever
- open a different GUI and switch back via the top tabs
- middle-click a tab and confirm only that session is destroyed
- use the bookmark toggle on an active tab and confirm bookmarked tabs stay visible and sortable
- open the history panel and confirm open/hide/restore/destroy events are listed
- verify ordinary world/menu transition screens are not polluted with tabs
- disconnect / world unload and confirm cached sessions are cleared safely

- [x] **Step 4: Check off completed boxes in this plan file**

Update only the steps actually completed.

- [x] **Step 5: Commit the verified global-tab implementation**

```bash
git add docs/superpowers/plans/2026-04-11-gui-browser-global-tab-overlay.md src gradle.properties
git commit -m "feat: implement global gui tab overlay"
```

Plan complete and saved to `docs/superpowers/plans/2026-04-11-gui-browser-global-tab-overlay.md`. Ready to execute?

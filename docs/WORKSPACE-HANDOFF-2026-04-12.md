# GUI Browser Workspace Handoff

This file is a code-and-workspace handoff for the next assistant.

It is intentionally **not** a restatement of the full product/design plan.
Requirement and roadmap documents already live under `docs/`.
This file focuses on:

- what is already implemented
- what is not implemented yet
- which files matter most right now
- what in the workspace is user-owned / should not be touched
- where the next assistant should resume safely

## Current Branch And Verification

- Current branch: `feature/gui-browser-phase-1-shell`
- Important note: the branch name is outdated and still reflects the old shell-based direction. Do **not** infer current architecture from the branch name.
- Current `HEAD` at handoff time: `a6d0c7b` (`fix: align minecraft lifecycle mixin injection`)
- Automated verification status at handoff time:
  - `./gradlew.bat test` -> `BUILD SUCCESSFUL`

## Current Product Direction

The project is **no longer** building a standalone browser shell GUI.

The current direction is:

- treat tracked `GuiScreen` instances as global cached sessions
- inject browser-like chrome onto tracked GUIs later
- default close behavior should hide GUI sessions into cache instead of destroying them
- only explicit tab-close actions should destroy sessions
- no mod-specific adapters as the main strategy

At this moment, the codebase has the **session/lifecycle foundation**, but **does not yet have the visible top tab overlay**.

## What Is Implemented

### 1. Global runtime singleton now owns session state

Key file:

- `src/main/java/com/zzhalex233/guibrowser/client/runtime/GuiBrowserRuntime.java`

What it does:

- stores loaded `BrowserConfig`
- owns a single `GuiSessionManager`
- owns a single `GuiLifecycleBridge`
- exposes both through getters

Why this matters:

- `MixinMinecraft` and the Forge event handler are now expected to use the runtime-owned lifecycle/session objects
- session state should not be duplicated in ad-hoc statics elsewhere

### 2. GUI session model is implemented

Key files:

- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSession.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionId.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionTitleResolver.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionManager.java`

`GuiSessionManager` currently supports:

- register opened session
- hide session
- activate session
- destroy session
- find by id
- find by original `GuiScreen` instance
- list visible tabs
- list all sessions
- clear all sessions on world unload
- track foreground session
- track last-activated session

Important behavior:

- sessions are keyed by original `GuiScreen` instance identity
- hidden sessions remain cached instead of being destroyed
- explicit destroy removes the session from the manager

### 3. Tracking policy exists

Key file:

- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiTrackingPolicy.java`

Current hard exclusions:

- `GuiMainMenu`
- `GuiMultiplayer`
- `GuiWorldSelection`
- `GuiDisconnected`
- `GuiDownloadTerrain`
- `GuiErrorScreen`
- `GuiGameOver`
- `GuiMemoryErrorScreen`
- `GuiScreenWorking`
- `GuiConnecting`
- any class under `com.zzhalex233.guibrowser.client.popup.`

Everything else is currently treated as trackable by default.

### 4. Mixin/coremod bootstrap is enabled

Key files:

- `gradle.properties`
- `src/main/java/com/zzhalex233/guibrowser/core/GuiBrowserLoadingPlugin.java`
- `src/main/resources/mixins.guibrowser.json`

Current state:

- `is_coremod = true`
- loading plugin bootstraps Mixin
- mixin config is registered
- current client mixin list contains only `MixinMinecraft`

### 5. Minecraft GUI lifecycle interception exists

Key files:

- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridge.java`
- `src/main/java/com/zzhalex233/guibrowser/mixin/MixinMinecraft.java`
- `src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java`
- `src/main/java/com/zzhalex233/guibrowser/proxy/ClientProxy.java`

Current flow:

1. `ClientProxy.preInit()` initializes `GuiBrowserRuntime` and registers `ClientForgeEventHandler`
2. `MixinMinecraft` hooks `Minecraft.displayGuiScreen(...)`
3. before the original `onGuiClosed()` call, the mixin asks `GuiLifecycleBridge` what to do
4. if the current tracked GUI should be hidden instead of destroyed, the redirect suppresses `onGuiClosed()`
5. after display completes, `GuiLifecycleBridge.onAfterDisplay(...)` re-registers or re-activates the visible tracked session
6. Forge world unload / client disconnect clears all cached sessions

Important detail:

- `MixinMinecraft` was intentionally moved away from a naive `HEAD` injection and now decides **right before** the original `onGuiClosed()` call.
- This reduces the risk of hiding the current screen too early when `GuiOpenEvent` changes or cancels the incoming screen.

### 6. Explicit destroy paths exist

`GuiLifecycleBridge` currently exposes:

- `destroySessionFromTab(GuiSessionId id)`
- `destroyForegroundSession()`
- `clearForWorldUnload()`

These are foundation methods for future tab UI actions.
There is **no actual tab UI yet** calling them.

## What Is Tested

Key test files:

- `src/test/java/com/zzhalex233/guibrowser/client/runtime/GuiBrowserRuntimeTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/session/GuiSessionManagerTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/session/GuiTrackingPolicyTest.java`
- `src/test/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridgeTest.java`

What is covered:

- session registration / hide / activate / destroy
- last-activated tracking
- tracking policy exclusions
- lifecycle bridge hide-vs-destroy behavior
- reopening cached tracked sessions
- world unload cleanup
- explicit destroy entrypoints

There are also small test-only Minecraft GUI stubs under:

- `src/test/java/net/minecraft/client/gui/`
- `src/test/java/net/minecraft/client/multiplayer/`
- `src/test/java/com/zzhalex233/guibrowser/client/popup/`

These are intentional test harness support files.

## What Is Not Implemented Yet

The following major pieces are still missing:

- visible top tab bar on tracked GUIs
- tab hit-testing and routing
- middle-click tab close from real UI
- left-click tab switch from real UI
- bookmark UI/state
- history UI/state
- adaptive overlay vs push-down layout policy
- `MixinGuiScreen`
- `MixinGuiContainer`
- any real chrome rendering

In short:

- the **session/cache engine exists**
- the **browser-like overlay UI does not yet exist**

## Important Files For The Next Assistant

Start here first:

- `src/main/java/com/zzhalex233/guibrowser/client/runtime/GuiBrowserRuntime.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiSessionManager.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridge.java`
- `src/main/java/com/zzhalex233/guibrowser/client/session/GuiTrackingPolicy.java`
- `src/main/java/com/zzhalex233/guibrowser/mixin/MixinMinecraft.java`
- `src/main/java/com/zzhalex233/guibrowser/client/event/ClientForgeEventHandler.java`
- `src/test/java/com/zzhalex233/guibrowser/client/session/GuiLifecycleBridgeTest.java`

If product intent needs to be reloaded after that, then read the current docs under `docs/`.

## Workspace State That Should Not Be ¡°Cleaned Up¡±

These are present in the working tree and should be treated as user-owned / intentionally retained unless the user explicitly says otherwise:

- `docs/superpowers/plans/2026-04-09-gui-browser-phase-1-browser-shell.md` (modified)
- `docs/superpowers/plans/2026-04-10-gui-browser-phase-2-single-tab-hosting.md` (untracked)
- `docs/superpowers/plans/2026-04-11-gui-browser-global-tab-overlay.md` (untracked)
- untracked GUI textures under `src/main/resources/assets/guibrowser/textures/gui/`:
  - `crafting_table.png`
  - `dispenser.png`
  - `generic_54.png`
  - `hopper.png`
  - `inventory.png`
  - `shulker_box.png`
  - `tabs.png`

Do not delete or revert those as ¡°noise¡±.

## Operational Notes

- The repo has passed `./gradlew.bat test` at handoff time.
- No manual in-client smoke test has been completed yet for the new overlay architecture, because the overlay itself has not been built.
- In this environment, `apply_patch` has intermittently failed with a Windows sandbox refresh error. When that happened, safe PowerShell file writes were used as a fallback.
- Recent history contains several intermediate subagent commits from earlier iterations. Use current files and current `HEAD` as ground truth, not older abandoned directions.

## Recommended Resume Point

The safest next resume point is:

1. keep the existing session/lifecycle foundation as-is
2. build the first real chrome overlay model on top of it
3. add UI hit-testing for tab switch and middle-click destroy
4. only after that, add bookmarks/history UI

More specifically, the next assistant should probably begin with the missing chrome layer:

- introduce `client/chrome/*`
- add layout tests first
- then add `MixinGuiScreen`
- then add `MixinGuiContainer` only when layout policy actually exists

## Short Status Summary

The rewrite is past the risky ¡°state foundation¡± stage.

What exists now is a working cached-session engine for tracked GUIs.
What does not exist yet is the actual browser-like tab strip users will see and click.

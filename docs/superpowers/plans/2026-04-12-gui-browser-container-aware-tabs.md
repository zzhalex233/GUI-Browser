# Container-Aware Tab System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the tab system to track only block/entity-bound container GUIs, fix GL state corruption, add source-keyed deduplication, and support Hybrid/Visual-Only container caching modes.

**Architecture:** Sessions are now keyed by `GuiSessionSource` (BlockPos+dimension or entityId) instead of auto-increment IDs. Only `GuiContainer` subclasses with a captured interaction source enter the tab system — player inventory, trinket mods, chat, and all non-container screens are excluded. A `ContainerCacheMode` config controls whether cached containers stay live (HYBRID, suppressing close packets) or display as greyed restore-on-click tabs (VISUAL_ONLY).

**Tech Stack:** Java 21, MC 1.12.2 Forge + Mixin (Cleanroom), JUnit Jupiter 6, Gradle/Unimined

**Spec:** `docs/superpowers/specs/2026-04-12-gui-browser-container-aware-tabs.md`

---

## File Structure

### New Files

| File | Responsibility |
|------|---------------|
| `config/ContainerCacheMode.java` | Enum: `HYBRID`, `VISUAL_ONLY` |
| `client/session/GuiSessionSource.java` | Value type with `BlockSource(BlockPos, dimensionId)` and `EntitySource(entityId)` static inner classes. `equals`/`hashCode` keyed on source identity. |
| `client/session/InteractionSourceTracker.java` | Captures pending source from Forge interaction events. Short-lived: cleared after consumption or 40-tick timeout. |
| `client/session/ContainerRestoreHandler.java` | Handles stale tab clicks: range check → simulated interaction (VISUAL_ONLY) or suppress+patch flow (HYBRID). |
| `mixin/MixinEntityPlayerSP.java` | HYBRID only: suppress `CPacketCloseWindow` in `EntityPlayerSP.closeScreen()` when runtime flag is set. Safety net — main suppression is already in `MixinMinecraft` via `onGuiClosed()` redirect. |
| `mixin/MixinNetHandlerPlayClient.java` | HYBRID only: intercept `handleOpenWindow` during pending restore to patch cached `GuiContainer` with new `windowId`. |

### Modified Files

| File | Changes |
|------|---------|
| `config/BrowserConfig.java` | Add `ContainerCacheMode containerCacheMode` field (default `HYBRID`). |
| `config/BrowserConfigLoader.java` | Load/save `containerCacheMode` via Forge config + properties fallback. |
| `client/session/GuiTrackingPolicy.java` | Replace binary `shouldTrack` → `TrackingDecision decide(screen, pendingSource)` returning `TRACK_AS_TAB` or `EXCLUDE`. Add `GuiInventory` check, `instanceof GuiContainer` gate, trinket package prefix matching, pending source requirement. |
| `client/session/GuiSession.java` | Add `@Nullable GuiSessionSource source` field, `boolean stale` flag, and `void markStale()` / `void refreshWith(GuiScreen, int windowId)` mutators. |
| `client/session/GuiSessionManager.java` | Add `Map<GuiSessionSource, GuiSessionId> sourceIndex` for dedup. New method `registerOrReuseSession(screen, title, source)` that checks source index first. Update `destroySession`/`clearForWorldUnload` to clean source index. |
| `client/session/GuiLifecycleBridge.java` | Use new `TrackingDecision` in `onBeforeDisplay`/`onAfterDisplay`. Consume pending source from `InteractionSourceTracker`. Mark sessions stale on hide (HYBRID/VISUAL_ONLY). Pass `ContainerCacheMode` to `TransitionDecision`. |
| `client/chrome/GuiChromeRenderer.java` | **GL fix:** save depth/blend/blendFunc/color before rendering, restore after. **Stale tabs:** render with dimmed color + lock icon for stale sessions. |
| `client/chrome/GuiChromeOverlayController.java` | Handle stale tab clicks → delegate to `ContainerRestoreHandler`. Filter `getTabs()` to only show sessions (stale tabs included). |
| `client/chrome/GuiLayoutPolicy.java` | Only apply `PUSH_DOWN` for tracked `GuiContainer` sessions (use new tracking policy). |
| `mixin/MixinGuiScreen.java` | Replace `GuiTrackingPolicy.shouldTrack()` calls with new `decide()` method. Only render chrome overlay for `TRACK_AS_TAB` screens. |
| `mixin/MixinGuiContainer.java` | Same: use new tracking policy for push-down offset. |
| `mixin/MixinMinecraft.java` | Update `onBeforeDisplay` call to pass `ContainerCacheMode` context. For HYBRID: set runtime suppress flag before transition so `MixinEntityPlayerSP` can read it. |
| `mixins.guibrowser.json` | Add `MixinEntityPlayerSP`, `MixinNetHandlerPlayClient` to client list. |
| `client/event/ClientForgeEventHandler.java` | Add `@SubscribeEvent` handlers for `PlayerInteractEvent.RightClickBlock` (→ `BlockSource`) and `PlayerInteractEvent.EntityInteract` (→ `EntitySource`). Delegate to `InteractionSourceTracker`. |
| `client/runtime/GuiBrowserRuntime.java` | Create and wire `InteractionSourceTracker`, `ContainerRestoreHandler`. Expose new getters. Add `boolean suppressClosePacket` flag for HYBRID mixin coordination. |
| `client/command/CommandGuiBrowser.java` | `debug` subcommand: show source info, stale state, cache mode per session. |

### New Test Files

| File | What it tests |
|------|--------------|
| `test/.../session/GuiSessionSourceTest.java` | Equality, hashCode, factory methods for BlockSource and EntitySource |
| `test/.../session/InteractionSourceTrackerTest.java` | Capture, consume, timeout, overwrite behavior |
| `test/net/minecraft/client/gui/inventory/GuiInventory.java` | Stub: `extends GuiContainer` (empty) |
| `test/.../session/FakeTrinketScreen.java` | Stub under `c4.curios.client.gui` package prefix for trinket exclusion test |

### Modified Test Files

| File | Changes |
|------|---------|
| `test/.../session/GuiTrackingPolicyTest.java` | Rewrite for new `decide()` API. Add tests: GuiInventory→EXCLUDE, GuiContainer+source→TRACK_AS_TAB, GuiContainer without source→EXCLUDE, trinket→EXCLUDE, non-container→EXCLUDE. |
| `test/.../session/GuiSessionManagerTest.java` | Add dedup tests: same source reuses session, different source creates new session, stale flag propagation. |
| `test/.../session/GuiLifecycleBridgeTest.java` | Update for new API: source-aware transitions, stale marking, cache mode behavior. |

---

## Tasks

### Task 1: Fix GL state corruption in GuiChromeRenderer

**Files:** Modify `client/chrome/GuiChromeRenderer.java:36-45`

- [ ] **Step 1:** In `render()`, before the `pushMatrix` block, save current GL state: `glGetBoolean(GL_DEPTH_TEST)`, `glGetBoolean(GL_BLEND)`, `glGetInteger(GL_BLEND_SRC)`, `glGetInteger(GL_BLEND_DST)`, `glGetFloat(GL_CURRENT_COLOR)`.
- [ ] **Step 2:** After rendering (before `popMatrix`), restore all saved state exactly instead of unconditionally calling `enableDepth()`.
- [ ] **Step 3:** Run `./gradlew build` — verify BUILD SUCCESSFUL.
- [ ] **Step 4:** Commit: `fix: save/restore GL state in chrome renderer to prevent depth/blend corruption`

**Key detail:** The current bug is line 44 `GlStateManager.enableDepth()` — `GuiContainer.drawScreen` expects depth DISABLED after `super.drawScreen()` returns. The fix must use `GL11.glGetBoolean`/`GL11.glGetInteger` (not GlStateManager) to read the actual GL state, then restore with `GlStateManager` calls.

---

### Task 2: Add ContainerCacheMode and extend BrowserConfig

**Files:**
- Create: `config/ContainerCacheMode.java`
- Modify: `config/BrowserConfig.java`
- Modify: `config/BrowserConfigLoader.java`

- [ ] **Step 1:** Create `ContainerCacheMode` enum with `HYBRID` and `VISUAL_ONLY`.
- [ ] **Step 2:** Add `containerCacheMode` field to `BrowserConfig`. Update constructors (backward-compatible: old 2-arg constructor defaults to `HYBRID`). Add getter + `withContainerCacheMode` builder.
- [ ] **Step 3:** Update `BrowserConfigLoader` to load/save `containerCacheMode` (Forge config key: `"containerCacheMode"`, category `"browser"`, default `"HYBRID"`). Update both Forge and Properties paths.
- [ ] **Step 4:** Run `./gradlew test` — all existing tests pass.
- [ ] **Step 5:** Commit: `feat: add ContainerCacheMode config (HYBRID/VISUAL_ONLY)`

---

### Task 3: Create GuiSessionSource value type

**Files:**
- Create: `client/session/GuiSessionSource.java`
- Create test: `test/.../session/GuiSessionSourceTest.java`

- [ ] **Step 1:** Write tests: `BlockSource` equality by pos+dimension, `EntitySource` equality by entityId, different types never equal, `hashCode` consistent.
- [ ] **Step 2:** Run tests — FAIL (class doesn't exist).
- [ ] **Step 3:** Implement `GuiSessionSource` as abstract class with two static inner classes: `BlockSource(BlockPos pos, int dimensionId)` and `EntitySource(int entityId)`. Both implement `equals`/`hashCode`/`toString`. **Note:** `BlockPos` in test stubs needs to be created — add a minimal `BlockPos` stub to `test/net/minecraft/util/math/BlockPos.java` with `x,y,z` fields + `equals`/`hashCode`.
- [ ] **Step 4:** Run tests — PASS.
- [ ] **Step 5:** Commit: `feat: add GuiSessionSource value type for block/entity binding`

---

### Task 4: Redesign GuiTrackingPolicy

**Files:**
- Modify: `client/session/GuiTrackingPolicy.java`
- Create stub: `test/net/minecraft/client/gui/inventory/GuiInventory.java`
- Create stub: `test/c4/curios/client/gui/FakeCuriosScreen.java` (trinket package)
- Modify: `test/.../session/GuiTrackingPolicyTest.java`

- [ ] **Step 1:** Create test stubs: `GuiInventory extends GuiContainer` (empty), trinket screen under `c4.curios.client.gui` package.
- [ ] **Step 2:** Rewrite `GuiTrackingPolicyTest` for new API: `decide(screen, hasSource)` returns `TrackingDecision`. Test matrix:
  - `null` → EXCLUDE
  - Hard-excluded classes → EXCLUDE
  - `GuiInventory` → EXCLUDE
  - Trinket package screen → EXCLUDE
  - `GuiChat` (non-container) → EXCLUDE
  - `GuiContainer` subclass + `hasSource=true` → TRACK_AS_TAB
  - `GuiContainer` subclass + `hasSource=false` → EXCLUDE (safety fallback)
  - Mod popup → EXCLUDE
- [ ] **Step 3:** Run tests — FAIL.
- [ ] **Step 4:** Implement: add `TrackingDecision` enum (`TRACK_AS_TAB`, `EXCLUDE`) inside `GuiTrackingPolicy`. New method `decide(GuiScreen, boolean hasSource)` with the decision chain from the spec. Keep old `shouldTrack` as deprecated wrapper (returns `decide(screen, true) == TRACK_AS_TAB`) so existing Mixin code doesn't break until Task 9 updates it.
- [ ] **Step 5:** Run tests — PASS. Run `./gradlew test` — all pass.
- [ ] **Step 6:** Commit: `feat: redesign tracking policy with container-aware three-level decisions`

**Key detail:** Trinket exclusion matches package prefixes: `c4.curios.`, `baubles.`, `vazkii.botania.client.gui.bag.` (expand as needed). `GuiInventory` is `net.minecraft.client.gui.inventory.GuiInventory`.

---

### Task 5: Extend GuiSession with source binding and stale state

**Files:** Modify `client/session/GuiSession.java`

- [ ] **Step 1:** Add fields: `@Nullable GuiSessionSource source`, `boolean stale`. Add to package-private constructor. Add getters: `getSource()`, `isStale()`. Add mutators: `void markStale()`, `void clearStale()`, `void updateScreen(GuiScreen newScreen)` (for HYBRID restore patching).
- [ ] **Step 2:** Run `./gradlew test` — all existing tests still pass (new fields are nullable/defaulted).
- [ ] **Step 3:** Commit: `feat: add source binding and stale state to GuiSession`

---

### Task 6: Create InteractionSourceTracker

**Files:**
- Create: `client/session/InteractionSourceTracker.java`
- Create test: `test/.../session/InteractionSourceTrackerTest.java`

- [ ] **Step 1:** Write tests: `setPending` stores source, `consumePending` returns and clears it, second `consumePending` returns null, `setPending` overwrites previous, `isExpired` returns true after 40 ticks.
- [ ] **Step 2:** Run tests — FAIL.
- [ ] **Step 3:** Implement: stores `@Nullable GuiSessionSource pending` + `long setPendingAtTick`. Methods: `setPending(source, currentTick)`, `@Nullable consumePending(currentTick)` (returns source if set and not expired, then clears), `clear()`. Expiry: 40 ticks (2 seconds).
- [ ] **Step 4:** Run tests — PASS.
- [ ] **Step 5:** Commit: `feat: add InteractionSourceTracker for capturing pending block/entity sources`

---

### Task 7: Redesign GuiSessionManager with source deduplication

**Files:**
- Modify: `client/session/GuiSessionManager.java`
- Modify: `test/.../session/GuiSessionManagerTest.java`

- [ ] **Step 1:** Add new tests:
  - `registerWithSameSourceReusesExistingSession` — register two screens with same `BlockSource` → only one session exists, second call returns the existing session (activated).
  - `registerWithDifferentSourcesCreatesSeparateSessions` — different `BlockSource`es → two sessions.
  - `destroySessionCleansSourceIndex` — destroy session → same source can create a new session.
  - `registerWithNullSourceAlwaysCreatesNewSession` — backward-compat for non-container screens if any slip through.
- [ ] **Step 2:** Run tests — FAIL.
- [ ] **Step 3:** Implement: add `HashMap<GuiSessionSource, GuiSessionId> sourceIndex`. New method `registerOrReuseSession(screen, title, @Nullable source)`:
  - If `source != null` and `sourceIndex` contains it → activate existing session, update its screen reference, return it.
  - Otherwise → create new session, index by source.
  - Old `registerOpenedSession` delegates to `registerOrReuseSession(screen, title, null)` for backward compat.
  - Update `destroySession` and `clearForWorldUnload` to clean `sourceIndex`.
- [ ] **Step 4:** Run tests — PASS. Run `./gradlew test` — all pass.
- [ ] **Step 5:** Commit: `feat: add source-keyed deduplication to session manager`

---

### Task 8: Redesign GuiLifecycleBridge for container awareness

**Files:**
- Modify: `client/session/GuiLifecycleBridge.java`
- Modify: `test/.../session/GuiLifecycleBridgeTest.java`

- [ ] **Step 1:** Add constructor params: `InteractionSourceTracker sourceTracker`, `ContainerCacheMode cacheMode`. Store as fields.
- [ ] **Step 2:** Rewrite `onBeforeDisplay`:
  - Use `GuiTrackingPolicy.decide(screen, hasSource)` instead of `shouldTrack`.
  - For incoming screen: consume pending source from `sourceTracker`, call `registerOrReuseSession` with source.
  - For outgoing screen (tracked container being hidden): mark session `stale` if another container is opening (server will close the old one).
  - `TransitionDecision.suppressCurrentClose` is true only for tracked containers in HYBRID mode.
- [ ] **Step 3:** Rewrite `onAfterDisplay` to use new tracking decision.
- [ ] **Step 4:** Update tests for new constructor and behavior. Key test cases:
  - Container with source → session created with source binding.
  - Same container reopened → existing session reused (dedup).
  - Non-container screen → no session created.
  - Container without pending source → EXCLUDE (no session).
  - Hiding a container session → marked stale.
- [ ] **Step 5:** Run `./gradlew test` — all pass.
- [ ] **Step 6:** Commit: `feat: redesign lifecycle bridge for container-aware source tracking`

---

### Task 9: Update existing Mixins for new tracking policy

**Files:**
- Modify: `mixin/MixinGuiScreen.java`
- Modify: `mixin/MixinGuiContainer.java`

- [ ] **Step 1:** `MixinGuiScreen.guibrowser$drawChromeOverlay`: replace `GuiTrackingPolicy.shouldTrack(self)` with check: only render if `sessionManager.findSessionByScreen(self) != null` (meaning it's an active tracked session). This naturally excludes non-container screens since they won't have sessions.
- [ ] **Step 2:** `MixinGuiScreen.guibrowser$interceptChromeClicks`: same replacement.
- [ ] **Step 3:** `MixinGuiContainer.guibrowser$adjustGuiTopForChrome`: same — only push down if the screen has an active session.
- [ ] **Step 4:** Run `./gradlew build` — BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit: `refactor: update mixin injection guards to use session-based tracking`

**Key detail:** After this task, chrome is only visible on screens that have a session. Since only `GuiContainer` with a captured source gets a session (Tasks 4+8), non-containers and inventory are automatically excluded.

---

### Task 10: Add MixinEntityPlayerSP (HYBRID close suppression)

**Files:**
- Create: `mixin/MixinEntityPlayerSP.java`
- Modify: `mixins.guibrowser.json`

- [ ] **Step 1:** Create Mixin targeting `EntityPlayerSP`. `@Inject` at HEAD of `closeScreen()`, cancellable. Check `GuiBrowserRuntime.getInstance().isSuppressClosePacket()` — if true, cancel (don't send `CPacketCloseWindow`, don't reset `openContainer`).
- [ ] **Step 2:** Add `MixinEntityPlayerSP` to `mixins.guibrowser.json` client list.
- [ ] **Step 3:** Run `./gradlew build` — BUILD SUCCESSFUL.
- [ ] **Step 4:** Commit: `feat: add MixinEntityPlayerSP to suppress close packet in HYBRID mode`

**Key detail:** This is a safety net. The primary suppression path is `MixinMinecraft`'s `onGuiClosed()` redirect. This mixin catches edge cases where `closeScreen()` is called directly. The `suppressClosePacket` flag is set/cleared by `GuiLifecycleBridge` during transitions.

---

### Task 11: Add MixinNetHandlerPlayClient (HYBRID restore interception)

**Files:**
- Create: `mixin/MixinNetHandlerPlayClient.java`
- Modify: `mixins.guibrowser.json`

- [ ] **Step 1:** Create Mixin targeting `NetHandlerPlayClient`. `@Inject` at HEAD of `handleOpenWindow(SPacketOpenWindow)`, cancellable. Check `ContainerRestoreHandler.isPendingRestore()` — if true:
  - Extract new `windowId` from the packet.
  - Get the cached session from `ContainerRestoreHandler`.
  - Patch the cached `GuiContainer`'s container with the new `windowId`.
  - Call `displayGuiScreen` with the cached screen.
  - Clear pending restore.
  - Cancel the original handler (prevent MC from creating a new GuiContainer).
- [ ] **Step 2:** Add to `mixins.guibrowser.json`.
- [ ] **Step 3:** Run `./gradlew build` — BUILD SUCCESSFUL.
- [ ] **Step 4:** Commit: `feat: add MixinNetHandlerPlayClient to intercept window open during HYBRID restore`

**Key detail:** `SPacketOpenWindow.getWindowId()` provides the new windowId. The cached `GuiContainer`'s `inventorySlots.windowId` must be patched. Also need to accept incoming `SPacketWindowItems` to refresh slot data — the normal handler will process this correctly since the windowId now matches.

---

### Task 12: Create ContainerRestoreHandler

**Files:**
- Create: `client/session/ContainerRestoreHandler.java`

- [ ] **Step 1:** Implement with fields: `@Nullable GuiSession pendingRestoreSession`, `ContainerCacheMode cacheMode`. Methods:
  - `requestRestore(GuiSession session)` — validates source still reachable, then:
    - VISUAL_ONLY: simulates interaction via `Minecraft.playerController` methods, clears stale flag on success.
    - HYBRID: sets `pendingRestoreSession`, simulates interaction, mixin handles the rest.
  - `isPendingRestore()` / `getPendingRestoreSession()` / `clearPendingRestore()` — for mixin coordination.
  - `validateSource(GuiSessionSource)` — checks block still exists at pos (not air) and player within 6 blocks, or entity alive and in range.
- [ ] **Step 2:** Run `./gradlew build` — BUILD SUCCESSFUL.
- [ ] **Step 3:** Commit: `feat: add ContainerRestoreHandler for stale tab restoration`

**Key detail:** Simulated block interaction: `mc.playerController.processRightClickBlock(player, world, pos, EnumFacing.UP, new Vec3d(pos), EnumHand.MAIN_HAND)`. Simulated entity interaction: `mc.playerController.interactWithEntity(player, entity, EnumHand.MAIN_HAND)`. Both trigger Forge events, so `InteractionSourceTracker` naturally captures the source for the resulting GUI.

---

### Task 13: Update chrome rendering for stale tabs

**Files:**
- Modify: `client/chrome/GuiChromeRenderer.java`
- Modify: `client/chrome/GuiChromeOverlayController.java`

- [ ] **Step 1:** `GuiChromeRenderer`: add stale tab rendering — dimmed background color (`0x66333333`), grey text, small lock indicator ("~" prefix on title). Pass `isStale` flag from session to tab drawing.
- [ ] **Step 2:** `GuiChromeOverlayController`: 
  - `getTabs()` now returns all sessions (including stale).
  - `handleTabLeftClick`: if session is stale → delegate to `ContainerRestoreHandler.requestRestore(session)` instead of `displayGuiScreen`.
  - Add `ContainerRestoreHandler` as constructor param.
- [ ] **Step 3:** Run `./gradlew build` — BUILD SUCCESSFUL.
- [ ] **Step 4:** Commit: `feat: render stale tabs with dimmed visual treatment and restore-on-click`

---

### Task 14: Wire new components in runtime, events, and commands

**Files:**
- Modify: `client/runtime/GuiBrowserRuntime.java`
- Modify: `proxy/ClientProxy.java`
- Modify: `client/event/ClientForgeEventHandler.java`
- Modify: `client/command/CommandGuiBrowser.java`

- [ ] **Step 1:** `GuiBrowserRuntime`:
  - Create `InteractionSourceTracker` and `ContainerRestoreHandler` in constructor.
  - Pass `sourceTracker` and `config.getContainerCacheMode()` to `GuiLifecycleBridge`.
  - Pass `restoreHandler` to `GuiChromeOverlayController`.
  - Add `boolean suppressClosePacket` field + getter/setter for HYBRID mixin coordination.
  - Expose new getters.
- [ ] **Step 2:** `ClientForgeEventHandler`:
  - Add constructor param `InteractionSourceTracker`.
  - Add `@SubscribeEvent onRightClickBlock(PlayerInteractEvent.RightClickBlock)` → captures `BlockSource(event.getPos(), player.dimension)`.
  - Add `@SubscribeEvent onEntityInteract(PlayerInteractEvent.EntityInteract)` → captures `EntitySource(event.getTarget().getEntityId())`.
- [ ] **Step 3:** `ClientProxy.preInit`: update event handler construction to pass `sourceTracker`.
- [ ] **Step 4:** `CommandGuiBrowser.handleDebug`: add source info, stale state, and cache mode to output.
- [ ] **Step 5:** Run `./gradlew build` — BUILD SUCCESSFUL.
- [ ] **Step 6:** Commit: `feat: wire container-aware components into runtime and event bus`

---

### Task 15: Full build verification and manual test plan

- [ ] **Step 1:** Run `./gradlew clean build` — BUILD SUCCESSFUL.
- [ ] **Step 2:** Run `./gradlew test` — all tests pass.
- [ ] **Step 3:** Manual in-game test plan:
  1. Open a chest → tab appears with chrome overlay.
  2. Open a second chest at different position → second tab appears, first tab marked stale.
  3. Open the same chest (same BlockPos) again → no duplicate tab, existing session reused.
  4. Open player inventory → NO tab, NO chrome overlay.
  5. ESC from a container → container tab persists (HYBRID: stays live; VISUAL_ONLY: greyed).
  6. Click a stale tab → container re-opens at original position.
  7. Click a stale tab for a destroyed block → status message, tab removed.
  8. Check that items/text in `GuiContainer` are NOT misaligned (GL fix).
  9. Run `/guibrowser debug` → shows source info and cache mode.
  10. Run `/guibrowser tabs` → shows stale/active state per tab.
- [ ] **Step 4:** Commit: `chore: verify container-aware tab system build`

---

## Dependency Graph

```
Task 1 (GL fix)          ─── standalone
Task 2 (CacheMode+Config) ─── standalone
Task 3 (SessionSource)    ─── standalone
Task 4 (TrackingPolicy)   ─── depends on Task 3 stubs
Task 5 (Session extend)   ─── depends on Task 3
Task 6 (SourceTracker)    ─── depends on Task 3
Task 7 (Manager dedup)    ─── depends on Tasks 3, 5
Task 8 (Bridge redesign)  ─── depends on Tasks 2, 4, 5, 6, 7
Task 9 (Mixin updates)    ─── depends on Task 8
Task 10 (EntityPlayerSP)  ─── depends on Task 2
Task 12 (RestoreHandler)  ─── depends on Tasks 3, 5, 6
Task 11 (NetHandler)      ─── depends on Task 12 (execute 12 before 11)
Task 13 (Chrome stale)    ─── depends on Tasks 5, 12
Task 14 (Wiring)          ─── depends on all above
Task 15 (Verification)    ─── depends on Task 14
```

Tasks 1, 2, 3 can run in parallel. Tasks 4, 5, 6, 10 can run in parallel after their deps. The critical path is: 3 → 5 → 7 → 8 → 9 → 14 → 15.

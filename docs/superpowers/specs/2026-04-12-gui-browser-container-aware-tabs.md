# GUI Browser Redesign: Container-Aware Tab System

**Date:** 2026-04-12
**Supersedes:** `2026-04-09-gui-browser-foundation-design.md`

## Problem Statement

The current tab overlay implementation has four critical issues:

1. **GL state corruption** — `GuiChromeRenderer` corrupts depth/blend state when injected at `GuiScreen.drawScreen` RETURN, causing item/text misalignment in `GuiContainer` screens.
2. **Duplicate tabs** — Opening the same GUI (e.g., inventory) multiple times creates multiple sessions. No deduplication exists.
3. **No exclusion granularity** — Player inventory, trinket GUIs, and non-container screens should not enter the tab system at all.
4. **Cached containers are non-functional** — Server destroys the `Container` on GUI close, making cached `GuiContainer` instances dead shells where slot interactions are silently rejected.

## Scope

The tab system tracks **block-bound and entity-bound container GUIs only**. Everything else is excluded.

### Enters the tab system

| Source type | Examples | Dedup key |
|-------------|----------|-----------|
| Block container | Chest, furnace, hopper, dispenser, brewing stand | `BlockPos` |
| Entity container | Donkey chest, villager trading | `entityId` |

### Excluded (close = destroy, no chrome)

| Category | Examples |
|----------|----------|
| Already excluded | Main menu, world selection, loading, connecting, disconnect, crash, death |
| Player inventory | `GuiInventory` and subclasses |
| Trinket/accessory mods | Curios, Baubles (matched by package prefix) |
| Non-container screens | `GuiChat`, settings, any `GuiScreen` that is not a `GuiContainer` |
| Mod-own popups | Anything under `com.zzhalex233.guibrowser.client.popup.*` |

## Session Source Binding

Every tab session is bound to a `GuiSessionSource` that identifies what opened it:

```
GuiSessionSource
├── BlockSource(BlockPos pos, int dimensionId)
└── EntitySource(int entityId)
```

**Deduplication rule:** Only one session per source. If the player opens a container at the same `BlockPos` or for the same `entityId` as an existing session, the existing session is reused/replaced instead of creating a duplicate.

**Source capture:** `SPacketOpenWindow` does not contain positional data. The source must be captured from the interaction event that triggered the container open:
- `PlayerInteractEvent.RightClickBlock` → `BlockSource`
- `PlayerInteractEvent.EntityInteract` → `EntitySource`

A short-lived "pending source" is stored between the interaction event and the resulting `displayGuiScreen` call.

## Container Cache Modes

A user-facing config option controls how cached containers behave:

```java
enum ContainerCacheMode {
    HYBRID,       // Default. Foreground container stays live; background containers can be restored.
    VISUAL_ONLY   // Containers close normally. Cached tabs are greyed/locked, click to re-open.
}
```

Both modes share the same session/source binding, deduplication, and exclusion logic. The difference is only in close-time behavior and restore flow.

### HYBRID mode

**Foreground container (currently displayed):**
- When the player opens a different GUI, suppress `CPacketCloseWindow` so the server-side `Container` stays alive.
- The `windowId` remains valid.
- The cached `GuiContainer` is fully interactive if restored before the server discards it.

**Background containers (previously cached):**
- Marked as "stale" — the server has closed their `Container`.
- Store the `GuiSessionSource` (BlockPos or entityId).
- On restore: simulate the original interaction (right-click block or interact with entity), intercept the resulting `SPacketOpenWindow`, patch the cached `GuiContainer` with the new `windowId`, and accept fresh slot data from `SPacketWindowItems`.

**Limitations:**
- MC 1.12.2 only supports one `openContainer` per player. Opening a second container server-side silently abandons the first.
- Modded containers using non-standard `IGuiHandler` logic may produce different `Container` subclasses on re-open.
- Entity containers require the entity to still be alive and in range.

### VISUAL_ONLY mode

- Containers close normally (server `Container` is destroyed).
- Cached tabs display with a greyed-out/locked visual treatment.
- Clicking a stale tab attempts to re-interact with the source block/entity.
- If out of range or the block is gone, show a status message and remove the tab.
- No network-layer Mixin injection needed. Zero desync risk.

## Mixin Injection Points

### Existing (unchanged)

| Mixin | Target | Purpose |
|-------|--------|---------|
| `MixinMinecraft` | `Minecraft.displayGuiScreen` | Lifecycle bridge for session hide/activate |

### Modified

| Mixin | Target | Change |
|-------|--------|--------|
| `MixinGuiScreen` | `GuiScreen.drawScreen` | Fix GL state save/restore in chrome renderer |
| `MixinGuiContainer` | `GuiContainer.initGui` | Only apply push-down offset for tracked containers |

### New

| Mixin | Target | Purpose |
|-------|--------|---------|
| `MixinEntityPlayerSP` | `EntityPlayerSP.closeScreen()` | HYBRID only: suppress `CPacketCloseWindow` for cached containers |
| `MixinNetHandlerPlayClient` | `NetHandlerPlayClient.handleOpenWindow` | HYBRID only: intercept new `windowId` on session restore |

### Forge Event Listeners (not Mixins)

| Event | Purpose |
|-------|---------|
| `PlayerInteractEvent.RightClickBlock` | Capture `BlockPos` as pending source |
| `PlayerInteractEvent.EntityInteract` | Capture `entityId` as pending source |

## Tracking Policy Redesign

Replace the current binary `shouldTrack(GuiScreen)` with a three-level policy:

```java
enum TrackingDecision {
    TRACK_AS_TAB,  // Enters the tab system with session source
    EXCLUDE        // No chrome, no tracking, close = destroy
}
```

Decision logic:

```
if screen is in hard-exclusion list → EXCLUDE
if screen is GuiInventory or subclass → EXCLUDE
if screen matches trinket mod packages → EXCLUDE
if screen is NOT instanceof GuiContainer → EXCLUDE
if screen is instanceof GuiContainer and has a pending source → TRACK_AS_TAB
if screen is instanceof GuiContainer with no source → EXCLUDE (safety fallback)
```

## GL State Fix

The chrome renderer must not corrupt GL state for the underlying GUI pipeline.

**Current bug:** Renderer calls `GlStateManager.enableDepth()` at cleanup, but `GuiContainer.drawScreen` expects depth to remain disabled after `super.drawScreen()` returns.

**Fix:** Before rendering, save current depth test state, blend state, blend function, and color. After rendering, restore all saved state exactly. Do not unconditionally enable or disable any GL flag.

## Config Additions

New fields in `BrowserConfig`:

```java
ContainerCacheMode containerCacheMode;  // HYBRID or VISUAL_ONLY, default HYBRID
int maxCachedSessions;                  // default 16
```

Existing fields retained for backward compatibility: `escAction`, `captureHotkeyKeyCode`.

## Summary of Changes from Current Implementation

| Area | Current | New |
|------|---------|-----|
| What gets tracked | All `GuiScreen` except hard exclusions | Only `GuiContainer` with a captured source |
| Session identity | Auto-increment ID, no dedup | Source-keyed (BlockPos or entityId), dedup enforced |
| Container close | Server Container destroyed, cached GUI is dead | HYBRID: foreground stays live. VISUAL_ONLY: greyed tab |
| GL rendering | Corrupts depth/blend state | Save/restore all state |
| Chrome visibility | Shown on all tracked screens | Shown only on tracked containers |
| Player inventory | Tracked | Excluded |
| Non-container GUIs | Tracked | Excluded |

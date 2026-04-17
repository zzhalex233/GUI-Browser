# GUI Browser Mod — 工作交接文档

## 项目概述

Minecraft 1.12.2 Cleanroom/Forge+Mixin 客户端 mod。为容器 GUI（箱子、工作台、熔炉等）添加浏览器风格的标签页系统。玩家可以打开多个容器，走开后通过点击标签页远程切换/恢复访问。

- Java 21 编译，MCP stable 39-1.12 映射，Unimined 构建系统
- Mixin refmap 已禁用（`disableRefmap()`），所有 mixin target 使用 MCP 反混淆名

## 核心架构

```
玩家右键方块 → Forge事件记录source → SPacketOpenWindow → displayGuiScreen
  → MixinMinecraft.beforeDisplay → GuiLifecycleBridge.onBeforeDisplay
    → sourceTracker.consumePending() 获取source
    → GuiSessionManager.registerOrReuseSession(screen, title, source)
      → 如果source匹配已有session → updateScreen + clearStale（复用）
      → 否则 → 创建新session
  → MixinMinecraft.afterDisplay → 清理flags
```

### 关键组件

| 组件 | 文件 | 职责 |
|------|------|------|
| Runtime | `GuiBrowserRuntime.java` | 单例，持有所有组件引用和 volatile flags |
| Session管理 | `GuiSessionManager.java` | session 注册/激活/隐藏/销毁，source索引 |
| 生命周期桥接 | `GuiLifecycleBridge.java` | displayGuiScreen 前后的 session 状态转换 |
| Source追踪 | `InteractionSourceTracker.java` | 记录待匹配的交互源（方块/实体），2秒过期 |
| 容器恢复 | `ContainerRestoreHandler.java` | stale tab 恢复：设置source → 发交互包 → 等待服务端响应 |
| Chrome渲染 | `GuiChromeRenderer.java` / `GuiChromeOverlayController.java` | 标签栏UI渲染和点击处理 |
| Mixin入口 | `MixinMinecraft.java` | displayGuiScreen 的 before/after 钩子 |
| Mixin入口 | `MixinGuiScreen.java` | drawScreen 渲染chrome，mouseClicked 拦截tab点击 |

### Volatile Flags（跨线程通信，客户端↔集成服务端）

| Flag | 用途 | 设置时机 | 清除时机 |
|------|------|----------|----------|
| `suppressClosePacket` | 阻止 EntityPlayerSP.closeScreen 发关闭包 | tab切换前 | afterDisplay |
| `bypassServerDistanceCheck` | 绕过 processTryUseItemOnBlock 距离检查 | restoreBlock 发包前 | afterDisplay |
| `keepContainerOpen` | 绕过 EntityPlayerMP.onUpdate 的 canInteractWith | afterDisplay（当前screen是tracked容器时） | afterDisplay（当前screen不是tracked容器时） |

## 三个未修复的 Bug

### Bug 1: 距离绕过失效 ⚠️ 最关键

**现象**: 离开方块几格后点击 stale tab 恢复，显示 "can't reach"，5秒后超时。

**预期**: 任意距离都能恢复 stale tab。这是 mod 的核心功能。

**恢复流程**:
1. `ContainerRestoreHandler.restoreBlock()` 设置 `bypassServerDistanceCheck = true`（volatile）
2. `RemoteInteractionHelper.sendBlockInteraction()` 直接发 `CPacketPlayerTryUseItemOnBlock`（绕过客户端reach检查）
3. 服务端 `NetHandlerPlayServer.processTryUseItemOnBlock` 收到包 → 距离检查
4. `MixinNetHandlerPlayServer` 的 `@Redirect` 应该拦截 `getDistanceSq` 返回 0.0D

**已添加的调试日志**: `MixinNetHandlerPlayServer` 现在会打印 `[GuiBrowser] Distance check: bypass=..., realDist=...`。运行游戏后查看日志：
- 如果日志**从未出现** → mixin 没有 apply，target descriptor 不匹配
- 如果 `bypass=false` → volatile flag 时序问题，服务端线程读到了旧值
- 如果 `bypass=true` 但仍然 can't reach → 还有其他距离检查没拦截

**可能的根因和排查方向**:

1. **Mixin target 不匹配**: `@Redirect` target 是 `Lnet/minecraft/entity/player/EntityPlayerMP;getDistanceSq(DDD)D`。`getDistanceSq` 定义在 `Entity` 上，`EntityPlayerMP` 继承它。bytecode 显示 `invokevirtual EntityPlayerMP.getDistanceSq:(DDD)D`，理论上 Mixin 应该能匹配。但 Cleanroom 可能修改了这个方法的调用方式。**尝试改 target owner 为 `Lnet/minecraft/entity/Entity;getDistanceSq(DDD)D`**。

2. **Cleanroom 修改了距离检查逻辑**: Cleanroom 把 vanilla 的 `if (d3 < 64.0D)` 替换为 `if (distSq < (REACH_DISTANCE + 3.0)^2)`，使用 entity attribute。如果 Cleanroom 重写了整个检查逻辑，`getDistanceSq` 调用可能不在预期位置。**需要反编译 Cleanroom 的 `NetHandlerPlayServer.processTryUseItemOnBlock` 确认**。

3. **volatile 时序**: `bypassServerDistanceCheck` 在客户端线程设置，在服务端线程读取。`volatile` 保证可见性但不保证时序。如果 `sendPacket` 是异步的，服务端可能在 flag 设置前就处理了包。**可以尝试在 sendPacket 前加一个短 sleep 或用 CountDownLatch 确认**。但更好的方案是不依赖 volatile flag，而是用其他机制（见下方替代方案）。

**替代方案（如果 volatile flag 方案不可行）**:
- 在 `CPacketPlayerTryUseItemOnBlock` 发送前，临时修改玩家的 `REACH_DISTANCE` attribute 为极大值，发送后恢复
- 用 `@Inject(at=@At("HEAD"), cancellable=true)` 注入 `processTryUseItemOnBlock`，在方法开头检查 flag，如果 bypass 则手动执行交互逻辑并 cancel 原方法
- 在服务端线程上调度 flag 设置（`mc.getIntegratedServer().addScheduledTask(...)`），确保 flag 在服务端线程上设置

### Bug 2: 恢复时浏览器关闭

**现象**: 点击 stale (~) tab 恢复时，整个 GUI 有时会关闭。

**预期**: 恢复过程中 GUI 保持打开，新窗口无缝替换旧窗口。

**根因**: `MixinNetHandlerPlayClient.handleCloseWindow` 中，当服务端发 `SPacketCloseWindow` 关闭旧窗口时，如果被关闭的 session 是 foreground，代码调用 `mc.displayGuiScreen(null)`。恢复流程中服务端先关旧窗口再开新窗口，中间的 `displayGuiScreen(null)` 关闭了浏览器。

**已做的修复**: 在 `displayGuiScreen(null)` 前加了 `sourceTracker.hasPending()` 检查。如果有 pending restore，跳过关闭。

**但可能没生效的原因**:
- `hasPending()` 检查的是 `InteractionSourceTracker` 的 `pending` 字段。如果 `consumePending()` 在 `handleCloseWindow` 之前被调用了（比如 `onBeforeDisplay` 先处理了 `SPacketOpenWindow`），pending 已经被消费，`hasPending()` 返回 false
- 包的处理顺序：`SPacketCloseWindow` 和 `SPacketOpenWindow` 是两个独立的包。如果它们在同一个 tick 内到达，处理顺序取决于网络栈。如果 `SPacketOpenWindow` 先被处理，pending 被消费，然后 `SPacketCloseWindow` 处理时 `hasPending()` 为 false → GUI 关闭
- **更可靠的方案**: 不依赖 `hasPending()`，而是在 `ContainerRestoreHandler` 中维护一个 `isRestoreInFlight` boolean flag，在 `requestRestore` 时设为 true，在 restore 成功（session clearStale）或超时时设为 false。`handleCloseWindow` 检查这个 flag。

### Bug 3: 鼠标跳位

**现象**: tab 切换/恢复后鼠标瞬移到屏幕中心或最左边 tab 位置。

**预期**: 鼠标位置不变。

**根因**: 与 Bug 2 同源。`displayGuiScreen(null)` → `setIngameFocus()` → 鼠标被 grab → `SPacketOpenWindow` → `displayGuiScreen(newScreen)` → `setIngameNotInFocus()` → `ungrabMouseCursor()` → `Mouse.setCursorPosition(Display.getWidth()/2, Display.getHeight()/2)`。

修复 Bug 2 后此问题应自动消失。如果仍有跳位，可能是其他路径触发了 grab/ungrab 链。

**排查**: 在 `Minecraft.setIngameFocus()` 和 `setIngameNotInFocus()` 加断点或日志，确认是否在 tab 切换时被调用。

## 文件清单

### Mixin 文件 (`src/main/java/com/zzhalex233/guibrowser/mixin/`)

| 文件 | 目标类 | 注入方式 | 作用 |
|------|--------|----------|------|
| `MixinMinecraft.java` | `Minecraft` | @Inject + @Redirect on `displayGuiScreen` | session 生命周期钩子 |
| `MixinGuiScreen.java` | `GuiScreen` | @Inject on `drawScreen` + `mouseClicked` | chrome 渲染和点击拦截 |
| `MixinGuiContainer.java` | `GuiContainer` | 空（占位） | 无实际功能，可删除 |
| `MixinEntityPlayerSP.java` | `EntityPlayerSP` | @Inject on `closeScreen` | 抑制关闭包发送 |
| `MixinEntityPlayerMP.java` | `EntityPlayerMP` | @Redirect on `onUpdate` | 绕过 canInteractWith 距离检查 |
| `MixinNetHandlerPlayClient.java` | `NetHandlerPlayClient` | @Inject on `handleCloseWindow` | 标记 session stale |
| `MixinNetHandlerPlayServer.java` | `NetHandlerPlayServer` | @Redirect on `processTryUseItemOnBlock` | 绕过交互距离检查 |
| `AccessorSPacketCloseWindow.java` | `SPacketCloseWindow` | @Accessor | 暴露 windowId 字段 |

### Session 文件 (`src/main/java/com/zzhalex233/guibrowser/client/session/`)

`GuiSession.java`, `GuiSessionId.java`, `GuiSessionSource.java`, `GuiSessionManager.java`, `GuiLifecycleBridge.java`, `InteractionSourceTracker.java`, `ContainerRestoreHandler.java`, `RemoteInteractionHelper.java`, `GuiTrackingPolicy.java`, `StaleTabPlaceholderScreen.java`

### Chrome UI (`src/main/java/com/zzhalex233/guibrowser/client/chrome/`)

`GuiChromeRenderer.java`, `GuiChromeOverlayController.java`, `GuiChromeLayout.java`, `GuiChromeTarget.java`

### 其他

`GuiBrowserRuntime.java` — 单例 runtime
`ClientForgeEventHandler.java` — Forge 事件监听
`BrowserConfig.java` / `ContainerCacheMode.java` — 配置
`TabPersistenceManager.java` / `JsonPersistence.java` — 持久化
`GuiRestoreFailedToast.java` — toast 提示

## 已知的其他问题

1. `InteractionSourceTracker.restoreLock` 在 restore 超时后不会被清除，可能阻塞后续正常交互
2. `EntitySource` 不存储 dimension，跨维度可能匹配错误实体
3. `maxCachedSessions` 配置存在但未强制执行
4. `GuiRestoreFailedToast` 使用静态字段，多个 toast 会互相覆盖

## 构建和测试

```bash
./gradlew build          # 编译 + 测试
./gradlew runClient      # 启动游戏客户端（如果配置了的话）
```

构建产物在 `build/libs/`。当前 `./gradlew build` 通过。

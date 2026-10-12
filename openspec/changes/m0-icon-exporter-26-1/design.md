## Context

On 1.21.1 `IconExporter` (Manufacturing client, dev only) waited for the first title screen frame, bound a 64x64 `TextureTarget`, set an orthographic GUI projection over 16 units, drew each item with `GuiGraphics.renderItem`, pulled the pixels back with `NativeImage.downloadTexture` (a blocking `glGetTexImage`), flipped them and wrote a PNG. The root `iconExport` run (client, `runs/icons`) passes `servus.icons.output` and `servus.icons.gamedata`.

On 26.1.2 none of that exists. The GUI is extracted into a `GuiRenderState` first and drawn later by `GuiRenderer`; slot items are drawn into a shared item atlas (`GuiItemAtlas`), larger items through picture in picture renderers (`PictureInPictureRenderer`) that render into their own GPU texture. All GPU work goes through `GpuDevice` and `CommandEncoder`. The port added `GuiLargeItemRenderState` and `GuiLargeItemRenderer` (Storage client) for the Shuttle Workshop on that path.

## Goals / Non-Goals

**Goals:**
- `gradlew iconExport` writes the same 64x64 icons as on 1.21.1: inventory look, transparent background, block items and composed models in 3D.
- No effect on the normal client.

**Non-Goals:**
- No new icon sizes, no cropping or framing of oversized models (the 1.21.1 exporter cut them at the slot square as well).
- No removal of stale icons for items that left `gamedata.json`; the exporter only writes.

## Decisions

### D1. Draw inside the GUI render pass, through a dev only picture in picture renderer

Items in the GUI are drawn with the GUI lightmap: `GameRenderer.render` sets `useUiLightmap` (private) only around `guiRenderer.render`, and sets `Lighting.Entry.ITEMS_3D` and fog off just before. Drawing from `RenderFrameEvent.Post` would sample the world lightmap, which is never filled on the title screen. So the exporter draws where vanilla draws GUI items: inside `GuiRenderer.preparePictureInPicture`.

- `IconExportRenderState` (record implementing `PictureInPictureRenderState`) carries the list of items to export (id and an `ItemStackRenderState` filled with `ItemModelResolver.updateForTopItem(..., ItemDisplayContext.GUI, null, null, 0)`), a 1x1 GUI rectangle at the origin, no scissor.
- `IconExportRenderer extends PictureInPictureRenderer<IconExportRenderState>` is registered in `RegisterPictureInPictureRenderersEvent` (mod bus, `register(Class, Function<BufferSource, PictureInPictureRenderer<T>>)`) only when both system properties are set.
- `IconExporter` listens to `ScreenEvent.Render.Post`. On the first title screen frame with no loading overlay (`Minecraft.getOverlay() == null`) it submits the state once with NeoForge's `GuiGraphicsExtractor.submitPictureInPictureRenderState`. When the renderer reports done, the next title screen frame calls `Minecraft.stop()`.

The Shuttle Workshop's `GuiLargeItemRenderer` cannot be reused as is: a picture in picture texture is created with usage 13 (copy destination, texture binding, render attachment) and without `USAGE_COPY_SRC`, so `CommandEncoder.copyTextureToBuffer` refuses it, and the texture is private to `PictureInPictureRenderer`. The renderer therefore draws into its own textures, with the same item drawing as `GuiLargeItemRenderer` and vanilla's `GuiItemAtlas.drawToSlot`. Manufacturing also has no compile dependency on Storage.

### D2. One 64x64 texture, drawn like a GUI atlas slot

In `renderToTexture` the renderer, for each item:
- creates once a color texture `createTexture(label, USAGE_COPY_DST | USAGE_COPY_SRC | USAGE_TEXTURE_BINDING | USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, 64, 64, 1, 1)` and a depth texture with usage `COPY_DST | RENDER_ATTACHMENT` in the main target's depth format (as NeoForge's `PictureInPictureRenderer` does), plus views (`createTextureView`);
- clears both with `clearColorAndDepthTextures(color, 0, depth, 1.0)` (transparent black);
- sets `RenderSystem.outputColorTextureOverride` and `outputDepthTextureOverride` to its views and the projection to `Projection.setupOrtho(-1000, 1000, 64, 64, true)` through its own `ProjectionMatrixBuffer`;
- draws as `GuiItemAtlas.drawToSlot` does: pose translated to (32, 32), scaled (64, -64, 64), lighting `ITEMS_3D` for block light models and `ITEMS_FLAT` otherwise, `ItemStackRenderState.submit(pose, featureRenderDispatcher.getSubmitNodeStorage(), 15728880, OverlayTexture.NO_OVERLAY, 0)`, `featureRenderDispatcher.renderAllFeatures()`, `bufferSource.endBatch()`.

That is the transform of a 16 unit slot drawn at four times its size, the same as the 1.21.1 `renderItem` into a 64 pixel target over 16 units. Afterwards it restores the picture in picture overrides and projection it found (`backupProjectionMatrix` / `restoreProjectionMatrix`), so the base class blits its own empty texture as usual.

### D3. Readback: copy to a mapped buffer, wait on a fence

- `GpuDevice.createBuffer(label, GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST, 64 * 64 * 4)` (as `Screenshot.takeScreenshot`).
- `CommandEncoder.copyTextureToBuffer(color, buffer, 0, () -> {}, 0)`. On OpenGL this is a `glReadPixels` into a pixel pack buffer and queues the callback behind a fence; the callback only runs at a later frame.
- To write the file in the same call, the renderer creates its own `GpuFence` (`CommandEncoder.createFence()`) right after the copy and waits with `awaitCompletion(timeout)`, then `mapBuffer(buffer, true, false)` and reads the bytes.
- Rows come bottom up (`Projection.setupOrtho(..., invertY = true)` and the GL origin, as in `Screenshot`), so row `y` goes to image row `63 - y`. Each pixel is read byte by byte as R, G, B, A and stored with `NativeImage.setPixelABGR` (independent of the buffer's byte order); alpha is kept, unlike the screenshot code. `NativeImage(64, 64, false)` in RGBA format and `writeToFile(Path)` write the PNG.

The colour values are what blending leaves on a transparent target, the same as on 1.21.1.

### D4. Errors and quitting

An unknown item id is logged and skipped; any exception during the export is logged, the overrides and projection are restored and the game still quits, so a broken export never leaves a dev client hanging. The renderer's textures, views, buffer and projection buffer are closed in `close()` and after the export.

## Risks / Trade-offs

- Waiting on a fence per item stalls the GPU once per icon. About 100 items at 64x64 take well under a second; it is a dev run.
- Oversized models (shuttle, Harvester, Spreader) are cut at the 64 pixel square, as on 1.21.1. Fitting them would change the look of the committed icons; left for a later change if the docs need it.

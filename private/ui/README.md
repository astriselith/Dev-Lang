# Dev-Lang UI proof of concept

This directory contains a small, runnable Dev-Lang UI backed by SDL3 and Skia.
The language file calls external functions declared in `ui.lang`; `bindings.c`
converts Dev-Lang runtime values to the native API in `ui_native.h`. SDL3 owns
the window, event queue, renderer, and texture. Skia draws into a CPU raster
surface that is copied to the SDL texture each frame.

The current API is intentionally small and immediate-mode: it supports one
window, panels, text, buttons, mouse clicks, and a frame loop. It is not yet a
general-purpose retained view tree or a standard-library module.

Build and run:

```sh
make -C private/ui
make -C private/ui run
```

Run the headless smoke test:

```sh
make -C private/ui check
```

The smoke test uses SDL's dummy video driver, limits execution to three frames,
and queues a synthetic click to verify the Dev-Lang button callback path. The
build requires SDL3 and Skia development packages discoverable by `pkg-config`.
Skia loads a real typeface from a supported system font path. Set
`DEVLANG_UI_FONT` to a `.ttf` or `.otf` file if the system font is elsewhere.
The headless check also verifies that text pixels were drawn into the Skia
surface.

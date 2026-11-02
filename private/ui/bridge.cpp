#include "ui_native.h"

#include <SDL3/SDL.h>

#include <core/SkCanvas.h>
#include <core/SkFont.h>
#include <core/SkFontMgr.h>
#include <core/SkImageInfo.h>
#include <core/SkPaint.h>
#include <core/SkPixmap.h>
#include <core/SkSurface.h>
#include <core/SkTypeface.h>
#include <ports/SkFontMgr_fontconfig.h>
#include <ports/SkFontScanner_FreeType.h>

#include <algorithm>
#include <cerrno>
#include <climits>
#include <cstdio>
#include <cstdlib>

namespace {

struct UiState {
  SDL_Window *window = nullptr;
  SDL_Renderer *renderer = nullptr;
  SDL_Texture *texture = nullptr;
  sk_sp<SkSurface> surface;
  sk_sp<SkFontMgr> fontManager;
  sk_sp<SkTypeface> typeface;
  int frames = 0;
  int testFrameLimit = 0;
  bool opened = false;
  bool quit = false;
  bool clickAvailable = false;
  float mouseX = 0;
  float mouseY = 0;
  float clickX = 0;
  float clickY = 0;
};

UiState ui;
bool testModeWasOpened = false;
bool testModeButtonClicked = false;
bool testModeTextVisible = false;
int testModeFrames = 0;

SkColor color(int red, int green, int blue) {
  return SkColorSetRGB(static_cast<U8CPU>(std::clamp(red, 0, 255)),
                       static_cast<U8CPU>(std::clamp(green, 0, 255)),
                       static_cast<U8CPU>(std::clamp(blue, 0, 255)));
}

void reportSdlError(const char *operation) {
  std::fprintf(stderr, "ui: %s failed: %s\n", operation, SDL_GetError());
}

void releaseWindow() {
  ui.surface.reset();
  ui.typeface.reset();
  ui.fontManager.reset();
  if (ui.texture)
    SDL_DestroyTexture(ui.texture);
  if (ui.renderer)
    SDL_DestroyRenderer(ui.renderer);
  if (ui.window)
    SDL_DestroyWindow(ui.window);
  ui.texture = nullptr;
  ui.renderer = nullptr;
  ui.window = nullptr;
  ui.opened = false;
  SDL_Quit();
}

bool readTestFrameLimit(int *limit) {
  const char *text = std::getenv("DEVLANG_UI_TEST_FRAMES");
  if (!text)
    return true;

  errno = 0;
  char *end = nullptr;
  long value = std::strtol(text, &end, 10);
  if (errno || end == text || *end != '\0' || value < 1 || value > INT_MAX) {
    std::fprintf(stderr,
                 "ui: DEVLANG_UI_TEST_FRAMES must be a positive integer\n");
    return false;
  }
  *limit = static_cast<int>(value);
  return true;
}

bool loadTypeface() {
  ui.fontManager =
      SkFontMgr_New_FontConfig(nullptr, SkFontScanner_Make_FreeType());
  if (!ui.fontManager) {
    std::fprintf(stderr, "ui: could not initialize Skia font manager\n");
    return false;
  }

  const char *configuredPath = std::getenv("DEVLANG_UI_FONT");
  if (configuredPath) {
    ui.typeface = ui.fontManager->makeFromFile(configuredPath);
    if (!ui.typeface) {
      std::fprintf(stderr, "ui: could not load font at DEVLANG_UI_FONT=%s\n",
                   configuredPath);
      return false;
    }
    return true;
  }

  constexpr const char *fontPaths[] = {
      "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
      "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
      "/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf",
      "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
  };
  for (const char *path : fontPaths) {
    ui.typeface = ui.fontManager->makeFromFile(path);
    if (ui.typeface)
      return true;
  }

  std::fprintf(stderr,
               "ui: no usable font found; set DEVLANG_UI_FONT to a .ttf or .otf file\n");
  return false;
}

bool isInside(float x, float y, int left, int top, int width, int height) {
  return x >= left && y >= top && x < left + width && y < top + height;
}

void drawText(const char *text, int x, int baseline, int size, SkColor ink) {
  SkPaint paint;
  paint.setAntiAlias(true);
  paint.setColor(ink);
  SkFont font(ui.typeface, static_cast<SkScalar>(std::max(size, 1)));
  ui.surface->getCanvas()->drawString(text, static_cast<SkScalar>(x),
                                      static_cast<SkScalar>(baseline), font,
                                      paint);
}

} // namespace

extern "C" bool ui_native_open(const char *title, int width, int height) {
  releaseWindow();
  ui.frames = 0;
  ui.testFrameLimit = 0;
  ui.quit = false;
  ui.clickAvailable = false;
  testModeWasOpened = false;
  testModeButtonClicked = false;
  testModeTextVisible = false;
  testModeFrames = 0;

  if (width <= 0 || height <= 0) {
    std::fprintf(stderr, "ui: window dimensions must be positive\n");
    return false;
  }
  if (!readTestFrameLimit(&ui.testFrameLimit))
    return false;
  if (!loadTypeface())
    return false;
  if (!SDL_Init(SDL_INIT_VIDEO)) {
    reportSdlError("SDL_Init");
    return false;
  }

  ui.window = SDL_CreateWindow(title, width, height, 0);
  if (!ui.window) {
    reportSdlError("SDL_CreateWindow");
    releaseWindow();
    return false;
  }
  ui.renderer = SDL_CreateRenderer(ui.window, nullptr);
  if (!ui.renderer) {
    reportSdlError("SDL_CreateRenderer");
    releaseWindow();
    return false;
  }
  ui.texture = SDL_CreateTexture(ui.renderer, SDL_PIXELFORMAT_RGBA32,
                                 SDL_TEXTUREACCESS_STREAMING, width, height);
  if (!ui.texture) {
    reportSdlError("SDL_CreateTexture");
    releaseWindow();
    return false;
  }

  SkImageInfo imageInfo = SkImageInfo::Make(
      width, height, kRGBA_8888_SkColorType, kPremul_SkAlphaType);
  ui.surface = SkSurfaces::Raster(imageInfo);
  if (!ui.surface) {
    std::fprintf(stderr, "ui: Skia could not allocate the raster surface\n");
    releaseWindow();
    return false;
  }

  ui.opened = true;
  if (ui.testFrameLimit > 0)
    testModeWasOpened = true;
  if (ui.testFrameLimit > 0) {
    SDL_Event click = {};
    click.type = SDL_EVENT_MOUSE_BUTTON_DOWN;
    click.button.button = SDL_BUTTON_LEFT;
    click.button.down = true;
    click.button.x = 80;
    click.button.y = 180;
    if (!SDL_PushEvent(&click)) {
      reportSdlError("SDL_PushEvent(test click)");
      releaseWindow();
      return false;
    }
  }
  return true;
}

extern "C" bool ui_native_running(void) {
  if (!ui.opened || ui.quit)
    return false;
  if (ui.testFrameLimit > 0 && ui.frames >= ui.testFrameLimit)
    return false;

  SDL_Event event;
  while (SDL_PollEvent(&event)) {
    if (event.type == SDL_EVENT_QUIT ||
        event.type == SDL_EVENT_WINDOW_CLOSE_REQUESTED) {
      ui.quit = true;
    } else if (event.type == SDL_EVENT_MOUSE_MOTION) {
      ui.mouseX = event.motion.x;
      ui.mouseY = event.motion.y;
    } else if (event.type == SDL_EVENT_MOUSE_BUTTON_DOWN &&
               event.button.button == SDL_BUTTON_LEFT) {
      ui.mouseX = ui.clickX = event.button.x;
      ui.mouseY = ui.clickY = event.button.y;
      ui.clickAvailable = true;
    }
  }
  return !ui.quit;
}

extern "C" void ui_native_begin_frame(void) {
  if (!ui.opened) {
    std::fprintf(stderr, "ui: uiBeginFrame called before uiOpen\n");
    return;
  }
  ui.surface->getCanvas()->clear(SkColorSetRGB(20, 25, 38));
}

extern "C" void ui_native_panel(int x, int y, int width, int height,
                                int red, int green, int blue) {
  if (!ui.opened) {
    std::fprintf(stderr, "ui: uiPanel called before uiOpen\n");
    return;
  }
  SkPaint paint;
  paint.setAntiAlias(true);
  paint.setColor(color(red, green, blue));
  ui.surface->getCanvas()->drawRoundRect(
      SkRect::MakeXYWH(static_cast<SkScalar>(x), static_cast<SkScalar>(y),
                       static_cast<SkScalar>(width),
                       static_cast<SkScalar>(height)),
      18, 18, paint);
}

extern "C" bool ui_native_button(const char *label, int x, int y, int width,
                                 int height) {
  if (!ui.opened) {
    std::fprintf(stderr, "ui: uiButton called before uiOpen\n");
    return false;
  }
  bool hovered = isInside(ui.mouseX, ui.mouseY, x, y, width, height);
  SkPaint paint;
  paint.setAntiAlias(true);
  paint.setColor(hovered ? SkColorSetRGB(91, 135, 223)
                         : SkColorSetRGB(67, 105, 190));
  ui.surface->getCanvas()->drawRoundRect(
      SkRect::MakeXYWH(static_cast<SkScalar>(x), static_cast<SkScalar>(y),
                       static_cast<SkScalar>(width),
                       static_cast<SkScalar>(height)),
      10, 10, paint);
  drawText(label, x + 18, y + height / 2 + 6, 17, SK_ColorWHITE);

  bool clicked = ui.clickAvailable &&
                 isInside(ui.clickX, ui.clickY, x, y, width, height);
  if (clicked) {
    ui.clickAvailable = false;
    testModeButtonClicked = true;
  }
  return clicked;
}

extern "C" void ui_native_text(const char *text, int x, int y, int size,
                               int red, int green, int blue) {
  if (!ui.opened) {
    std::fprintf(stderr, "ui: uiText called before uiOpen\n");
    return;
  }
  drawText(text, x, y, size, color(red, green, blue));
}

extern "C" void ui_native_present(void) {
  if (!ui.opened) {
    std::fprintf(stderr, "ui: uiPresent called before uiOpen\n");
    return;
  }
  SkPixmap pixels;
  if (!ui.surface->peekPixels(&pixels)) {
    std::fprintf(stderr, "ui: Skia raster surface pixels are unavailable\n");
    ui.quit = true;
    return;
  }
  if (ui.testFrameLimit > 0) {
    for (int y = 40; y < 82 && !testModeTextVisible; ++y) {
      for (int x = 50; x < 340; ++x) {
        SkColor pixel = pixels.getColor(x, y);
        if (SkColorGetA(pixel) > 0 && SkColorGetR(pixel) > 170 &&
            SkColorGetG(pixel) > 170 && SkColorGetB(pixel) > 170) {
          testModeTextVisible = true;
          break;
        }
      }
    }
  }
  if (!SDL_UpdateTexture(ui.texture, nullptr, pixels.addr(),
                         static_cast<int>(pixels.rowBytes())) ||
      !SDL_RenderClear(ui.renderer) ||
      !SDL_RenderTexture(ui.renderer, ui.texture, nullptr, nullptr) ||
      !SDL_RenderPresent(ui.renderer)) {
    reportSdlError("present frame");
    ui.quit = true;
    return;
  }
  ++ui.frames;
  if (ui.testFrameLimit > 0)
    testModeFrames = ui.frames;
  SDL_Delay(16);
}

extern "C" void ui_native_close(void) {
  if (ui.testFrameLimit > 0)
    testModeFrames = ui.frames;
  releaseWindow();
}

extern "C" bool ui_test_was_opened(void) { return testModeWasOpened; }

extern "C" bool ui_test_button_clicked(void) {
  return testModeButtonClicked;
}

extern "C" bool ui_test_text_visible(void) { return testModeTextVisible; }

extern "C" int ui_test_frame_count(void) { return testModeFrames; }

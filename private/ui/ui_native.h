#ifndef DEV_LANG_UI_NATIVE_H
#define DEV_LANG_UI_NATIVE_H

#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

bool ui_native_open(const char *title, int width, int height);
bool ui_native_running(void);
void ui_native_begin_frame(void);
void ui_native_panel(int x, int y, int width, int height, int red, int green,
                     int blue);
bool ui_native_button(const char *label, int x, int y, int width, int height);
void ui_native_text(const char *text, int x, int y, int size, int red,
                    int green, int blue);
void ui_native_present(void);
void ui_native_close(void);

bool ui_test_was_opened(void);
bool ui_test_button_clicked(void);
bool ui_test_text_visible(void);
int ui_test_frame_count(void);

#ifdef __cplusplus
}
#endif

#endif

#include "ui.h"
#include "ui_native.h"

#include <limits.h>
#include <stdio.h>
#include <stdlib.h>

static Value *require_value(const List *args, size_t index, const char *type,
                            const char *function) {
  Value *value = List_arg(args, index);
  if (!value || !value_is(value, type)) {
    fprintf(stderr, "ui binding error: %s argument %zu must be %s\n", function,
            index + 1, type);
    abort();
  }
  return value;
}

static const char *string_argument(const List *args, size_t index,
                                   const char *function) {
  String *value = (String *)require_value(args, index, "String", function)->ptr;
  return value->value;
}

static int int_argument(const List *args, size_t index, const char *function) {
  Int *value = (Int *)require_value(args, index, "Int", function)->ptr;
  if (value->value < INT_MIN || value->value > INT_MAX) {
    fprintf(stderr,
            "ui binding error: %s argument %zu is outside the native Int range\n",
            function, index + 1);
    abort();
  }
  return (int)value->value;
}

Value *function_uiOpen(List *args) {
  return Bool_new(ui_native_open(string_argument(args, 0, "uiOpen"),
                                int_argument(args, 1, "uiOpen"),
                                int_argument(args, 2, "uiOpen")));
}

Value *function_uiRunning(List *args) {
  (void)args;
  return Bool_new(ui_native_running());
}

Value *function_uiBeginFrame(List *args) {
  (void)args;
  ui_native_begin_frame();
  return Void_new();
}

Value *function_uiPanel(List *args) {
  ui_native_panel(int_argument(args, 0, "uiPanel"),
                  int_argument(args, 1, "uiPanel"),
                  int_argument(args, 2, "uiPanel"),
                  int_argument(args, 3, "uiPanel"),
                  int_argument(args, 4, "uiPanel"),
                  int_argument(args, 5, "uiPanel"),
                  int_argument(args, 6, "uiPanel"));
  return Void_new();
}

Value *function_uiButton(List *args) {
  return Bool_new(ui_native_button(string_argument(args, 0, "uiButton"),
                                   int_argument(args, 1, "uiButton"),
                                   int_argument(args, 2, "uiButton"),
                                   int_argument(args, 3, "uiButton"),
                                   int_argument(args, 4, "uiButton")));
}

Value *function_uiText(List *args) {
  ui_native_text(string_argument(args, 0, "uiText"),
                 int_argument(args, 1, "uiText"),
                 int_argument(args, 2, "uiText"),
                 int_argument(args, 3, "uiText"),
                 int_argument(args, 4, "uiText"),
                 int_argument(args, 5, "uiText"),
                 int_argument(args, 6, "uiText"));
  return Void_new();
}

Value *function_uiPresent(List *args) {
  (void)args;
  ui_native_present();
  return Void_new();
}

Value *function_uiClose(List *args) {
  (void)args;
  ui_native_close();
  return Void_new();
}

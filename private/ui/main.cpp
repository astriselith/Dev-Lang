#include <cstdlib>

extern "C" {
#include <list.h>
struct Value;
Value *function_main(List *args);
Value *rt_destroy(Value *value);
}

extern "C" bool ui_test_was_opened();
extern "C" bool ui_test_button_clicked();
extern "C" bool ui_test_text_visible();
extern "C" int ui_test_frame_count();

int main() {
  List args = {};
  List_init(&args);
  Value *result = function_main(&args);
  rt_destroy(result);
  List_free(&args);

  if (std::getenv("DEVLANG_UI_TEST_FRAMES"))
    return ui_test_was_opened() && ui_test_button_clicked() &&
                   ui_test_text_visible() &&
                   ui_test_frame_count() >= 3
               ? 0
               : 1;
  return 0;
}

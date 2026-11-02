ROOT := ../..
BUILD := build
LANG_JAR := $(ROOT)/target/dev-lang.jar
CC := clang
CXX := clang++

SDL_CFLAGS := $(shell pkg-config --cflags sdl3)
UI_CFLAGS := $(shell pkg-config --cflags skia)
UI_LIBS := $(shell pkg-config --libs sdl3 skia)
CFLAGS := -Wall -Wextra -O0 -g3 -std=gnu17 -I$(ROOT)/c/include -I$(BUILD)
CXXFLAGS := -Wall -Wextra -O0 -g3 -std=c++20 -I$(ROOT)/c/include -I$(BUILD) $(SDL_CFLAGS) $(UI_CFLAGS)
RUNTIME_OBJECTS := $(BUILD)/list.o $(BUILD)/map.o $(BUILD)/runtime.o $(BUILD)/vtable.o
UI_OBJECTS := $(BUILD)/ui.o $(BUILD)/bindings.o $(RUNTIME_OBJECTS) \
	$(BUILD)/bridge.o $(BUILD)/main.o

.PHONY: all generate run check clean

all: $(BUILD)/ui

generate: $(LANG_JAR)
	mkdir -p $(BUILD)
	cd $(ROOT) && printf 'generate private/ui/ui.lang --stub tests/stub.lang --out private/ui/$(BUILD)\nexit\n' | java -jar target/dev-lang.jar

$(BUILD)/ui.c $(BUILD)/ui.h: generate
	@test -f $@ || { printf 'Codegen did not create %s\n' "$@"; exit 1; }

$(LANG_JAR):
	cd $(ROOT) && mvn -q -DskipTests package

$(BUILD):
	mkdir -p $(BUILD)

$(BUILD)/ui.o: $(BUILD)/ui.c
	$(CC) $(CFLAGS) -c $(BUILD)/ui.c -o $@

$(BUILD)/bindings.o: bindings.c $(BUILD)/ui.h
	$(CC) $(CFLAGS) -c bindings.c -o $@

$(BUILD)/%.o: $(ROOT)/c/src/%.c | $(BUILD)
	$(CC) $(CFLAGS) -c $< -o $@

$(BUILD)/bridge.o: bridge.cpp $(BUILD)/ui.h | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BUILD)/main.o: main.cpp $(BUILD)/ui.h | $(BUILD)
	$(CXX) $(CXXFLAGS) -c $< -o $@

$(BUILD)/ui: generate $(UI_OBJECTS)
	$(CXX) $(UI_OBJECTS) -o $@ $(UI_LIBS)

run: $(BUILD)/ui
	./$(BUILD)/ui

check: $(BUILD)/ui
	SDL_VIDEODRIVER=dummy DEVLANG_UI_TEST_FRAMES=3 ./$(BUILD)/ui

clean:
	rm -rf $(BUILD)

CC = clang
AR = llvm-ar

CFLAGS = -Wall -Wextra -O2 -std=gnu17
INCLUDES = -I./include
LIBS = -lm

TARGET = libruntime.a
TEST = test

SOURCES = $(wildcard .src/*.c) $(wildcard ./*.c)
OBJECTS = $(SOURCES:.c=.o)

all: $(TARGET) test

$(TARGET): $(OBJECTS)
	$(AR) rcs $@ $^

%.o: %.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

test: $(TARGET) ./test.c
	$(CC) $(CFLAGS) $(INCLUDES) ./test.c -L. -l:$(TARGET) $(LIBS) -o $(TEST)

clean:
	rm -f $(OBJECTS) $(TARGET) $(TEST)

.PHONY: all test clean
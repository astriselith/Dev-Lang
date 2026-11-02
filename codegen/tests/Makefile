CC = clang

CFLAGS = -Wall -Wextra -O0 -g3 -std=gnu17
INCLUDES = -I../../c/include
LIBS = -lm

TARGET = test
SOURCES = $(wildcard *.c) $(wildcard ../../c/src/*.c)
HEADERS = $(wildcard *.h) $(wildcard ../../c/include/*.h)

all: $(TARGET)

check: $(TARGET)
	@if ! grep -Fq 'List_elements(' test.c; then \
		printf 'Generated code does not use List_elements\n'; \
		exit 1; \
	fi
	@if grep -Fq 'List_init(&_args)' test.c || grep -Fq 'List_push(&_args' test.c; then \
		printf 'Generated code still initializes argument lists incrementally\n'; \
		exit 1; \
	fi
	@if ! grep -Fq 'DEF_TYPE_INIT(A, 5, A__destroy,' test.c; then \
		printf 'Generated class initialization did not use DEF_TYPE_INIT\n'; \
		exit 1; \
	fi
	@if ! grep -Fq 'VSLOT_VAR(SLOT_VAR_NAME("should"))' test.c; then \
		printf 'Generated class variable slot is missing\n'; \
		exit 1; \
	fi
	@if grep -Fq '_variables' test.h || grep -Fq '_variables' test.c; then \
		printf 'Generated class still stores variables in a struct array\n'; \
		exit 1; \
	fi
	@if ! grep -Fq 'rt_var_get(' test.c; then \
		printf 'Generated variable access does not use runtime slot lookup\n'; \
		exit 1; \
	fi
	@if grep -Fq 'C__declaredOnly' test.c || grep -Fq 'C__operator_infix_plus' test.c \
		|| grep -Fq 'operator_prefix_bang_C(List* args) {' test.c; then \
		printf 'Generated implementation includes a declaration without a body\n'; \
		exit 1; \
	fi
	@if grep -Fq 'Value* should;' test.h; then \
		printf 'Generated class still declares a named variable field\n'; \
		exit 1; \
	fi
	@if grep -Fq 'C base;' test.h; then \
		printf 'Generated subclass still embeds a parent struct for variable storage\n'; \
		exit 1; \
	fi
	@if grep -Fq 'A__super_ptrs' test.c; then \
		printf 'Generated class still uses a separate supertype array\n'; \
		exit 1; \
	fi
	@if grep -Eq '/\\* ==== (Any|Void|Null|Stringfiable|Bool|Int|Float|String) ==== \\*/' test.c; then \
		printf 'Primordial class implementation was generated\n'; \
		exit 1; \
	fi
	@if grep -Eq 'Value\* function_symbolProbe\(List\* args\) \{' test.c; then \
		printf 'External stub function implementation was generated\n'; \
		exit 1; \
	fi
	@set -e; actual="$$(./$(TARGET))"; expected="$$(printf 'Hello World!\n3\n3\n5\n2\n2\n3\n9\n7\nA question operator\nA question operator\nA question operator\nA question operator\nreference mutation visible\nA question operator\nr updated\nA question operator\nalias updated\nglobal any question operator\nA question operator\ncustom assignment preserved')"; \
	if [ "$$actual" != "$$expected" ]; then \
		printf 'Unexpected output:\n%s\nExpected:\n%s\n' "$$actual" "$$expected"; \
		exit 1; \
	fi

$(TARGET): $(SOURCES) $(HEADERS)
	$(CC) $(CFLAGS) $(INCLUDES) $(SOURCES) -o $(TARGET) $(LIBS)

compile_commands.json:
	bear -- $(CC) $(CFLAGS) $(INCLUDES) $(SOURCES) -o $(TARGET) $(LIBS)

debug: $(TARGET)
	lldb ./$(TARGET)

clean:
	rm -f $(TARGET) compile_commands.json

.PHONY: all check debug clean
# Dev-Lang — Syntax and semantics

This document describes the syntax currently accepted by the parser and the
operator and runtime behavior used by the compiler.

## Declarations

Top-level declarations are classes, functions, variables, and operators.

### Classes

A class has a constructor parameter list, optional type parameters, optional
supertypes, and a body (or a trailing semicolon when it has no members):

```text
class Box::<T>(value: T) : Stringfiable() {
    var content: T -> value;

    fun get(): T {
        return content;
    };
};
```

Type parameters use `::<...>` and may declare bounds with `<...>`:

```text
class Container::<T<Stringfiable>>(value: T);
```

Supertypes are constructor calls separated by `&`, for example
`: Base(value) & Printable()`. Members may be fields, functions, or operators.
The trailing semicolon after a class body is optional.

### Variables

`var` declares a field or top-level variable; `let` declares a local variable.
Both accept an optional type and an optional initializer, introduced by `->`:

```text
var count: Int -> 0;
let next: Int -> count + 1;
let value -> null;
```

An assignment expression uses `=` as an operator:

```text
value = next;
```

The generic assignment operator declared for `Any` updates the left-hand
`Value` cell with the result. References that share that cell observe the
updated value. A more specific applicable operator can provide custom
assignment behavior.

### Functions

Functions take named, typed parameters and may specify a return type. Generic
functions use the same `::<...>` syntax as classes:

```text
fun first::<T, U>(value: T, fallback: U): T {
    return value;
};
```

A function declaration may end with `;` instead of a body. This form is used
for declarations supplied externally; it does not define a function body in
the source.

### Operators

An operator declaration has one operand for prefix or postfix operators, and
two for infix operators. Each operand has a name and type; the return type
follows `:`:

```text
operator ! value: Bool : Bool {
    return value;
};

operator left: Int + right: Int : Int {
    return left;
};

operator value: A ? : Bool {
    return !isNull(value);
};
```

Operator symbols are composed from `+`, `-`, `*`, `/`, `%`, `=`, `!`, `<`,
`>`, `&`, `|`, `?`, and `~`. Operators can also be declared without a body by
ending the declaration with `;`; declarations without bodies do not generate
C implementations or operator slots.

Global operators are declared at top level. Class operators and their
supertypes are considered before global operators. At runtime, global operator
slots are reachable through the hidden global metadata parent of `Any`, so
they participate in the same hierarchy lookup rather than a separate fallback
rule. For a null value with a known static type, lookup first uses that static
type; this lets a typed null use an applicable class operator before a global
one.

## Statements and expressions

Blocks contain statements. Supported control-flow statements are `if`/`else`,
`while`, `break`, `continue`, and `return`:

```text
if (count > 0) {
    count = count - 1;
} else {
    return;
}

while (count < 10) {
    count = count + 1;
}
```

`if` and `while` bodies may also use `-> statement;` instead of a block.
`return;` returns without an expression; `return expression;` returns a value.

Expressions include `null`, booleans, integer, floating-point, and string
literals; names; parenthesized expressions; function calls; member access;
and prefix, infix, or postfix operators:

```text
let result: Int -> calculate::<Int>(2).value + 1;
let empty: A -> null;
if (!isNull(empty)) {
    println("not null");
}
```

Generic types use `Type::<T1, T2>` (for example `Map::<String, Int>`), and
generic calls use `function::<T1, T2>(...)`. Generic operator type arguments
can follow the operator symbol using `::<...>`.

All infix operators currently have the same precedence and associate from
left to right. Prefix operators bind before infix parsing. Operator
declarations determine the meaning of operator symbols; `?` is a postfix
operator when used after an expression, not a ternary conditional.

`isNull(value)` is declared as an external function in the example
[`tests/stub.lang`](tests/stub.lang) and implemented by the runtime. It tests
whether a value is null, including a null runtime reference. A postfix `?`
null check is not hard-coded: it must be provided by an applicable operator
declaration.

## Runtime declarations and stubs

The runtime provides primordial classes such as `Any`, `Void`, `Null`,
`Stringfiable`, `Bool`, `Int`, `Float`, and `String`. Their runtime
implementations and extension slots are not emitted from source declarations.

Pass a declarations-only file with `--stub <file.lang>` to make its classes
and global functions available as external symbols during code generation,
without emitting implementations for those declarations. For example:

```sh
printf 'generate tests/test.lang --stub tests/stub.lang --out codegen/tests\nexit\n' \
  | java -jar target/dev-lang.jar
```

Use `file <source.lang> --stub <file.lang>` to parse, register, and
semantically analyze source declarations against an external symbol stub.

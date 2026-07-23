# Dev-Lang — Minimal language reference

This is a short reference for Dev-Lang (example language parsed by Parser.java).

Syntax highlights

- Top-level: classes

```js
class Name<T> : Superclass1 & Superclass2 & Superclass3 {
 fun method(param: Type): ReturnType { }
 var field: Type;
}
```

- Functions

```js
fun add(a: Int, b: Int): Int {
 return a + b;
}

fun short(x: Int) = x * 2;        # expression-bodied
fun opt() = ?;                     # empty body
```

- Variable declarations

```js
var x: Int = 10; // only in member scope
let y = 20; // only in body scope
```

- Control flow

```js
if (cond) {
 // then
} else {
 // else
}

while (i < 10) {
 i = i + 1;
}

break; continue;                    # inside loop
return;                             # return without value
return expr;                        # return with value
```

- Expressions

```js
# arithmetic, comparisons, logicals, ternary
1 + 2 * 3
foo<T1, T2>(arg1, arg2)               # calls with optional type arguments
obj.field                             # member access
arr[index]                            # index access (needs extend Indexable<K, V>)
cond ? a : b                          # ternary 
```

- Operators support a strict form using $ before operator

```js
a $= b    # strict assignment
a $== b   # strict equality
```

- Types

Identifier or parameterized: `Type, Map<String, Int>`

Literals: null, bools, chars, ints, floats, strings

This reference covers the concrete syntax expected by the parser.

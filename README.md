# Dev-Lang — Minimal language reference

This is a short reference for Dev-Lang (example language parsed by Parser.java).

Syntax highlights

- Top-level: classes
```
class Name<T> : Superclass | Trait1 & Trait2 {
	fun method(param: Type): ReturnType { }
	var field: Type;
}
```
- Functions
```
fun add(a: Int, b: Int): Int {
	return a + b;
}
```
fun short(x: Int) = x * 2;        # expression-bodied
fun opt() = ?;                     # empty body
```
- Variable declarations
```
var x: Int = 10;
let y = 20;                        # immutable (let)
```
- Control flow
```
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
```
# arithmetic, comparisons, logicals, ternary
1 + 2 * 3
foo(bar)<T1, T2>(arg1, arg2)         # calls with optional type arguments
obj.field                             # member access
arr[index]                            # index access
cond ? a : b                          # ternary
```
- Operators support a strict form using $ before operator
```
a $= b    # strict assignment
a $== b   # strict equality
```
- Types

Identifier or parameterized: `Type, Map<String, Int>`

Literals: null, booleans, chars, ints, floats, strings

This reference covers the concrete syntax expected by the parser.

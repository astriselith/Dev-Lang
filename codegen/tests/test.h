/* auto generated */
#ifndef TEST_H
#define TEST_H

#include "runtime.h"

typedef struct A A;
typedef struct B B;
typedef struct C C;

struct A {
    const VTable* vtable;
};

struct B {
    const VTable* vtable;
};

struct C {
    const VTable* vtable;
};

Value* A_new(List* args);
VTable* A__vt(void);
Value* A__stringfy(const Value* self, List* args);
Value* A__operator_postfix_question_generic_A(List* args);
Value* A__operator_infix_equals_generic_A_generic_A(List* args);

Value* B_new(List* args);
VTable* B__vt(void);

Value* C_new(List* args);
VTable* C__vt(void);
Value* C__declaredOnly(const Value* self, List* args);
Value* C__operator_infix_plus_generic_C_generic_C(List* args);

Value* function_main(List* args);
Value* function_firstOf(List* args);
Value* function_alternating(List* args);
Value* function_symbolProbe(List* args);
Value* operator_postfix_question_Any(List* args);
Value* operator_infix_tilde_Int_Int(List* args);
Value* operator_prefix_bang_generic_C(List* args);
Value* operator_infix_tilde_generic_T_generic_T(List* args);


#endif /* TEST_H */

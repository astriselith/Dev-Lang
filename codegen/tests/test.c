/* auto generated */
#include "test.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

/* ==== A ==== */
static Value* A__stringfy(const Value* self, List* args) {
    return String_new("A");
}

static Value* A__operator_postfix_question_generic_A(List* args) {
    (void)(({ Value* _arg_0 = String_new("A question operator"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    if (value_as_bool(({ Value* _arg_0 = List_arg(args, 0); List _args = List_elements((void *[]){_arg_0}, 1); function_is_null(List_arg(&_args, 0)); }))) {
        return Bool_new(false);
    } else {
        return rt_var_get(value_require_nonnull(List_arg(args, 0), "should"), "should");
    }
}

static Value* A__operator_infix_equals_generic_A_generic_A(List* args) {
    return List_arg(args, 0);
}

static const VSlot A__slots[] = {
    VSLOT_VAR(SLOT_VAR_NAME("should")),
    VSLOT_VAR(SLOT_VAR_NAME("b")),
    VSLOT_FN(SLOT_FN_NAME("stringfy", "void"), &A__stringfy),
    VSLOT_OP_TYPED(SLOT_OP_NAME("postfix", "question", "generic_A"), SLOT_OP_KEY("postfix", "question"), &A__operator_postfix_question_generic_A, 1, "A", NULL),
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "equals", "generic_A_generic_A"), SLOT_OP_KEY("infix", "equals"), &A__operator_infix_equals_generic_A_generic_A, 2, "A", "A"),
};

static void* A__slot_ptrs[] = {
    (void*)&A__slots[0],
    (void*)&A__slots[1],
    (void*)&A__slots[2],
    (void*)&A__slots[3],
    (void*)&A__slots[4],
};

VTableInfo A_info;

DEF_BUILD(A)
DEF_VT(A)

static Value* A__create(List* args) {
    (void)args;
    A* obj = (A*)calloc(1, sizeof(A));
    if (!obj) return NULL;
    obj->vtable = A__vt();
    return Value_new(obj);
}

static Value* A__destroy(Value* self) {
    if (!self || !self->ptr) return NULL;
    free(self->ptr);
    return NULL;
}

Value* A_new(List* args) {
    Value* b = List_arg(args, 0);
    A* obj = (A*)calloc(1, sizeof(A));
    if (!obj) return NULL;
    obj->vtable = A__vt();
    Value* instance = Value_new(obj);
    if (!instance) { free(obj); return NULL; }
    rt_var_set(instance, "b", b);
    Value* self = instance;
    (void)self;
    rt_var_set(instance, "should", b);
    return instance;
}

/* ==== B ==== */
static const VSlot B__slots[] = {
    VSLOT_VAR(SLOT_VAR_NAME("data")),
};

static void* B__slot_ptrs[] = {
    (void*)&B__slots[0],
};

VTableInfo B_info;

DEF_BUILD(B)
DEF_VT(B)

static Value* B__create(List* args) {
    (void)args;
    B* obj = (B*)calloc(1, sizeof(B));
    if (!obj) return NULL;
    obj->vtable = B__vt();
    return Value_new(obj);
}

static Value* B__destroy(Value* self) {
    if (!self || !self->ptr) return NULL;
    free(self->ptr);
    return NULL;
}

Value* B_new(List* args) {
    B* obj = (B*)calloc(1, sizeof(B));
    if (!obj) return NULL;
    obj->vtable = B__vt();
    Value* instance = Value_new(obj);
    if (!instance) { free(obj); return NULL; }
    Value* self = instance;
    (void)self;
    rt_var_set(instance, "data", Int_new(0LL));
    return instance;
}

/* ==== C ==== */
static const VSlot C__slots[] = {
};

static void* C__slot_ptrs[] = {
};

VTableInfo C_info;

DEF_BUILD(C)
DEF_VT(C)

static Value* C__create(List* args) {
    (void)args;
    C* obj = (C*)calloc(1, sizeof(C));
    if (!obj) return NULL;
    obj->vtable = C__vt();
    return Value_new(obj);
}

static Value* C__destroy(Value* self) {
    if (!self || !self->ptr) return NULL;
    free(self->ptr);
    return NULL;
}

Value* C_new(List* args) {
    C* obj = (C*)calloc(1, sizeof(C));
    if (!obj) return NULL;
    obj->vtable = C__vt();
    Value* instance = Value_new(obj);
    if (!instance) { free(obj); return NULL; }
    Value* self = instance;
    (void)self;
    return instance;
}

Value* function_main(List* args) {
    (void)(({ Value* _arg_0 = String_new("Hello World!"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    (void)(({ Value* _arg_0 = ({ Value* _arg_0 = Int_new(1LL); Value* _arg_1 = Int_new(2LL); List _args = List_elements((void *[]){_arg_0, _arg_1}, 2); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("infix", "plus"), &_args, Int__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("infix", "plus")); }); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    (void)(({ Value* _arg_0 = ({ Value* _arg_0 = Float_new(1.0); Value* _arg_1 = Float_new(2.0); List _args = List_elements((void *[]){_arg_0, _arg_1}, 2); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("infix", "plus"), &_args, Float__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("infix", "plus")); }); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    (void)(({ Value* _arg_0 = ({ Value* _arg_0 = Int_new(2LL); Value* _arg_1 = Int_new(3LL); List _args = List_elements((void *[]){_arg_0, _arg_1}, 2); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("infix", "tilde"), &_args, NULL); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("infix", "tilde")); }); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    (void)(({ Value* _arg_0 = ({ Value* _arg_0 = Int_new(2LL); Value* _arg_1 = ({ Value* _arg_0 = Bool_new(false); List _args = List_elements((void *[]){_arg_0}, 1); A_new(&_args); }); List _args = List_elements((void *[]){_arg_0, _arg_1}, 2); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("infix", "tilde"), &_args, NULL); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("infix", "tilde")); }); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    (void)(({ Value* _arg_0 = ({ Value* _arg_0 = Int_new(2LL); Value* _arg_1 = ({ Value* _arg_0 = Bool_new(false); List _args = List_elements((void *[]){_arg_0}, 1); A_new(&_args); }); List _args = List_elements((void *[]){_arg_0, _arg_1}, 2); function_firstOf(&_args); }); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    (void)(({ Value* _arg_0 = ({ Value* _arg_0 = Int_new(2LL); Value* _arg_1 = ({ Value* _arg_0 = Bool_new(false); List _args = List_elements((void *[]){_arg_0}, 1); A_new(&_args); }); Value* _arg_2 = Int_new(3LL); Value* _arg_3 = ({ Value* _arg_0 = Bool_new(false); List _args = List_elements((void *[]){_arg_0}, 1); A_new(&_args); }); List _args = List_elements((void *[]){_arg_0, _arg_1, _arg_2, _arg_3}, 4); function_alternating(&_args); }); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    Value* child = ({ List _args = List_elements(NULL, 0); C_new(&_args); });
    (void)(({ Value* _object = value_require_nonnull(child, "data"); Value* _left = rt_var_get(_object, "data"); Value* _right = Int_new(9LL); List _args = List_elements((void *[]){_left, _right}, 2); SlotOp _op = rt_find_operator_for(_left, SLOT_OP_KEY("infix", "equals"), &_args); Value* _result = _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("infix", "equals")); rt_var_assign(_object, "data", _result); }));
    (void)(({ Value* _arg_0 = rt_var_get(value_require_nonnull(child, "data"), "data"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    (void)(({ Value* _arg_0 = ({ Value* _arg_0 = Int_new(7LL); List _args = List_elements((void *[]){_arg_0}, 1); function_symbolProbe(&_args); }); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    Value* a0 = ({ Value* _arg_0 = Bool_new(false); List _args = List_elements((void *[]){_arg_0}, 1); A_new(&_args); });
    Value* a1 = ({ Value* _arg_0 = Bool_new(false); List _args = List_elements((void *[]){_arg_0}, 1); A_new(&_args); });
    Value* empty = Null_new();
    if (value_as_bool(({ Value* _arg_0 = empty; List _args = List_elements((void *[]){_arg_0}, 1); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("postfix", "question"), &_args, A__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("postfix", "question")); }))) {
        (void)(({ Value* _arg_0 = String_new("C null check failed"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    }
    Value* r = Null_new();
    if (value_as_bool(({ Value* _arg_0 = ({ Value* _arg_0 = r; List _args = List_elements((void *[]){_arg_0}, 1); function_is_null(List_arg(&_args, 0)); }); List _args = List_elements((void *[]){_arg_0}, 1); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("prefix", "bang"), &_args, Bool__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("prefix", "bang")); }))) {
        (void)(({ Value* _arg_0 = String_new("null helper failed"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    }
    if (value_as_bool(({ Value* _arg_0 = r; List _args = List_elements((void *[]){_arg_0}, 1); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("postfix", "question"), &_args, A__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("postfix", "question")); }))) {
        (void)(({ Value* _arg_0 = String_new("null check failed"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    }
    Value* alias = r;
    (void)(({ Value** _target = &(r); Value* _left = *_target; Value* _right = a0; List _args = List_elements((void *[]){_left, _right}, 2); SlotOp _op = rt_find_operator_for(_left, SLOT_OP_KEY("infix", "equals"), &_args); Value* _result = _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("infix", "equals")); *_target = value_assign(_left, _result); *_target; }));
    if (value_as_bool(({ Value* _arg_0 = r; List _args = List_elements((void *[]){_arg_0}, 1); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("postfix", "question"), &_args, A__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("postfix", "question")); }))) {
        (void)(({ Value* _arg_0 = String_new("custom operator failed"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    }
    (void)(({ Value* _object = value_require_nonnull(a0, "should"); Value* _left = rt_var_get(_object, "should"); Value* _right = Bool_new(true); List _args = List_elements((void *[]){_left, _right}, 2); SlotOp _op = rt_find_operator_for(_left, SLOT_OP_KEY("infix", "equals"), &_args); Value* _result = _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("infix", "equals")); rt_var_assign(_object, "should", _result); }));
    if (value_as_bool(({ Value* _arg_0 = r; List _args = List_elements((void *[]){_arg_0}, 1); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("postfix", "question"), &_args, A__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("postfix", "question")); }))) {
        (void)(({ Value* _arg_0 = String_new("reference mutation visible"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    }
    (void)(({ Value** _target = &(r); Value* _left = *_target; Value* _right = a1; List _args = List_elements((void *[]){_left, _right}, 2); SlotOp _op = rt_find_operator_for(_left, SLOT_OP_KEY("infix", "equals"), &_args); Value* _result = _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("infix", "equals")); *_target = value_assign(_left, _result); *_target; }));
    if (value_as_bool(({ Value* _arg_0 = r; List _args = List_elements((void *[]){_arg_0}, 1); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("postfix", "question"), &_args, A__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("postfix", "question")); }))) {
        (void)(({ Value* _arg_0 = String_new("r updated"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    }
    if (value_as_bool(({ Value* _arg_0 = alias; List _args = List_elements((void *[]){_arg_0}, 1); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("postfix", "question"), &_args, A__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("postfix", "question")); }))) {
        (void)(({ Value* _arg_0 = String_new("alias updated"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    }
    (void)(({ Value** _target = &(r); Value* _left = *_target; Value* _right = a1; List _args = List_elements((void *[]){_left, _right}, 2); SlotOp _op = rt_find_operator_for(_left, SLOT_OP_KEY("infix", "equals"), &_args); Value* _result = _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("infix", "equals")); *_target = value_assign(_left, _result); *_target; }));
    Value* an = Null_new();
    if (value_as_bool(({ Value* _arg_0 = an; List _args = List_elements((void *[]){_arg_0}, 1); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("postfix", "question"), &_args, NULL); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("postfix", "question")); }))) {
    }
    if (value_as_bool(({ Value* _arg_0 = r; List _args = List_elements((void *[]){_arg_0}, 1); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("postfix", "question"), &_args, A__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("postfix", "question")); }))) {
        (void)(({ Value* _arg_0 = String_new("custom assignment preserved"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    }
    return Void_new();
}

Value* function_firstOf(List* args) {
    return List_arg(args, 0);
}

Value* function_alternating(List* args) {
    return List_arg(args, 2);
}

Value* operator_postfix_question_Any(List* args) {
    (void)(({ Value* _arg_0 = String_new("global any question operator"); List _args = List_elements((void *[]){_arg_0}, 1); function_println(List_arg(&_args, 0)); }));
    return ({ Value* _arg_0 = ({ Value* _arg_0 = List_arg(args, 0); List _args = List_elements((void *[]){_arg_0}, 1); function_is_null(List_arg(&_args, 0)); }); List _args = List_elements((void *[]){_arg_0}, 1); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("prefix", "bang"), &_args, Bool__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("prefix", "bang")); });
}

Value* operator_infix_tilde_Int_Int(List* args) {
    return ({ Value* _arg_0 = List_arg(args, 0); Value* _arg_1 = List_arg(args, 1); List _args = List_elements((void *[]){_arg_0, _arg_1}, 2); Value* _self = List_arg(&_args, 0); SlotOp _op = rt_find_operator_for_type(_self, SLOT_OP_KEY("infix", "plus"), &_args, Int__vt()); _op ? _op(&_args) : rt_missing_operator(SLOT_OP_KEY("infix", "plus")); });
}

Value* operator_infix_tilde_generic_T_generic_T(List* args) {
    return List_arg(args, 0);
}

static const VSlot global_operator_slots[] = {
    VSLOT_OP_TYPED(SLOT_OP_NAME("postfix", "question", "Any"), SLOT_OP_KEY("postfix", "question"), &operator_postfix_question_Any, 1, "Any", NULL),
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "tilde", "Int_Int"), SLOT_OP_KEY("infix", "tilde"), &operator_infix_tilde_Int_Int, 2, "Int", "Int"),
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "tilde", "generic_T_generic_T"), SLOT_OP_KEY("infix", "tilde"), &operator_infix_tilde_generic_T_generic_T, 2, "Any", "Any"),
};

static void* global_operator_slot_ptrs[] = {
    (void*)&global_operator_slots[0],
    (void*)&global_operator_slots[1],
    (void*)&global_operator_slots[2],
};

void test_init(void) {
    runtime_init();
    if (!rt_register_slots(&__global_info, global_operator_slot_ptrs, 3)) {
        fputs("runtime error: could not register global operators\n", stderr);
        abort();
    }
    DEF_TYPE_INIT(A, 5, A__destroy, (void *)(intptr_t)Stringfiable_info.index);

    DEF_TYPE_INIT(B, 1, B__destroy, (void *)(intptr_t)Any_info.index);

    DEF_TYPE_INIT(C, 0, C__destroy, (void *)(intptr_t)B_info.index);

}

__attribute__((constructor))
static void test__auto_init(void) {
    test_init();
}

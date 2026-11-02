#pragma once
#ifndef VTABLE_H
#define VTABLE_H

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>
#include "list.h"
#include "map.h"

#define SLOT_VAR_NAME(nm) "variable_" nm

struct Value;

typedef struct {
    uintptr_t address;
} VFunction;

typedef struct {
    struct Value* value;
} VVariable;

typedef struct {
    uintptr_t address;
    const char* operator_key;
    const char* parameter_types[2];
    size_t arity;
} VOperator;

enum {
    VSLOT_FUNCTION = 0,
    VSLOT_VARIABLE = 1,
    VSLOT_OPERATOR = 2,
};

typedef struct {
    const char* name;
    uint32_t    kind;
    union {
        VFunction function;
        VVariable variable;
        VOperator operator;
    } value;
} VSlot;

typedef struct VTableInfo VTableInfo;
typedef struct VTable     VTable;

struct VTable {
    int index;
    const char* name;
    size_t base_offset;
    List        supers;
    List        slots;
};

struct VTableInfo {
    int         index;
    const char* name;
    size_t      base_offset;
    List        supers;
    List        slots;
    VFunction   create;
    VFunction   destroy;
    VTable*   (*build)(Map* acc);
    void**      owned_slots;
};

#define VSLOT_FN(name, fn) \
    { (name), VSLOT_FUNCTION, { .function = { (uintptr_t)(fn) } } }
#define VSLOT_VAR(name) \
    { (name), VSLOT_VARIABLE, { .variable = { NULL } } }
#define VSLOT_OP(name, op) \
    { (name), VSLOT_OPERATOR, { .operator = { .address = (uintptr_t)(op) } } }
#define VSLOT_OP_TYPED(name, key, op, count, first, second) \
    { (name), VSLOT_OPERATOR, { .operator = { (uintptr_t)(op), (key), { (first), (second) }, (count) } } }

extern List vtables;

bool        vfunction_copy(VFunction* destination, const VFunction* source);
bool        vvariable_copy(VVariable* destination, const VVariable* source);
bool        voperator_copy(VOperator* destination, const VOperator* source);
void        voperator_free(VOperator* value);
void        vt_add(VTableInfo* info);
VTableInfo* vt_get(int index);
VSlot*      vslot_copy(const VSlot* slot);
void        vslot_free(VSlot* slot);

const VFunction* find_function(VTable* vt, const char* name);
const VOperator* find_operator(VTable* vt, const char* name);
VVariable* find_variable(VTable* vt, const char* name);

#endif
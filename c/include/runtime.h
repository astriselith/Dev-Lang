#pragma once
#ifndef RUNTIME_H
#define RUNTIME_H

#include "vtable.h"
#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>

#define SLOT_FN_NAME(nm, sg) "function_" nm "_" sg

#define SLOT_OP_KEY(tp, nm) "operator_" tp "_" nm
#define SLOT_OP_NAME(tp, nm, sg) SLOT_OP_KEY(tp, nm) "_" sg

#define DEF_TYPE_INIT(TYPE, SLOT_COUNT, DESTROY, ...)                          \
  do {                                                                         \
    TYPE##_info.name = #TYPE;                                                  \
    if (!List_copy_init(                                                       \
            &TYPE##_info.supers, (void *[]){__VA_ARGS__},                      \
            sizeof((void *[]){__VA_ARGS__}) / sizeof(void *)))                 \
      abort();                                                                 \
    TYPE##_info.slots = List_static_init(TYPE##__slot_ptrs, SLOT_COUNT);       \
    TYPE##_info.create.address = (uintptr_t)&TYPE##__create;                  \
    TYPE##_info.destroy.address = (uintptr_t)&DESTROY;                        \
    TYPE##_info.build = TYPE##__build;                                         \
    vt_add(&TYPE##_info);                                                      \
  } while (0)

#define DEF_BUILD(X)                                                           \
  static VTable *X##__build(Map *acc) {                                        \
    const void *key = (void *)(intptr_t)X##_info.index;                        \
    VTable *existing = (VTable *)Map_get(acc, key);                            \
    if (existing)                                                              \
      return existing;                                                         \
    VTable *vt = (VTable *)malloc(sizeof(VTable));                             \
    if (!vt)                                                                    \
      return NULL;                                                             \
    vt->index = X##_info.index;                                                \
    vt->name = X##_info.name;                                                  \
    vt->base_offset = X##_info.base_offset;                                    \
    List_init(&vt->supers);                                                    \
    List_init(&vt->slots);                                                     \
    for (size_t i = 0; i < X##_info.slots.count; i++) {                        \
      VSlot *slot = vslot_copy((const VSlot *)X##_info.slots.items[i]);        \
      if (!slot || !List_push(&vt->slots, slot)) {                             \
        vslot_free(slot);                                                      \
        for (size_t j = 0; j < vt->slots.count; j++)                           \
          vslot_free((VSlot *)vt->slots.items[j]);                             \
        List_free(&vt->slots);                                                 \
        free(vt);                                                              \
        return NULL;                                                           \
      }                                                                        \
    }                                                                          \
    Map_put(acc, key, vt);                                                     \
    for (size_t i = 0; i < X##_info.supers.count; i++) {                       \
      int sup_idx = (int)(intptr_t)X##_info.supers.items[i];                   \
      VTableInfo *sup = vt_get(sup_idx);                                       \
      if (sup && sup->build) {                                                 \
        VTable *parent = sup->build(acc);                                      \
        if (parent)                                                            \
          List_push(&vt->supers, parent);                                      \
      }                                                                        \
    }                                                                          \
    return vt;                                                                 \
  }

#define DEF_VT(X)                                                              \
  VTable *X##__vt(void) {                                                      \
    Map acc;                                                                   \
    Map_init(&acc);                                                            \
    VTable *vt = X##__build(&acc);                                             \
    Map_free(&acc);                                                            \
    return vt;                                                                 \
  }

typedef struct Any {
  const VTable *vtable;
} Any;

typedef struct Value {
  void *ptr;
  struct ValueStorage *storage;
} Value;

Value *Value_new(void *ptr);
Value *value_assign(Value *target, const Value *source);

static inline const VTable *value_vtable(const Value *v) {
  return (v && v->ptr) ? ((const Any *)v->ptr)->vtable : NULL;
}

static inline Value *List_arg(const List *l, size_t i) {
  return (l && i < l->count) ? (Value *)l->items[i] : NULL;
}

bool value_is(const Value *v, const char *name);
bool value_isa(const Value *v, const char *name);
bool value_is_null(const Value *v);
bool value_as_bool(const Value *v);
Value *value_require_nonnull(Value *v, const char *member);

typedef struct {
  const VTable *vtable;
} Void;
typedef struct {
  const VTable *vtable;
} Null;
typedef struct {
  const VTable *vtable;
  bool value;
} Bool;
typedef struct {
  const VTable *vtable;
  int64_t value;
} Int;
typedef struct {
  const VTable *vtable;
  double value;
} Float;
typedef struct {
  const VTable *vtable;
  char *value;
} String;

typedef Value *(*SlotFn)(const Value *self, List *args);
typedef Value *(*SlotOp)(List *args);
typedef Value *(*CreateFn)(List *args);
typedef Value *(*DestroyFn)(Value *self);

static inline SlotFn rt_find_function(VTable *vt, const char *name) {
  const VFunction *f = find_function(vt, name);
  return f ? (SlotFn)f->address : NULL;
}
static inline SlotOp rt_find_operator(VTable *vt, const char *name) {
  const VOperator *o = find_operator(vt, name);
  return o ? (SlotOp)o->address : NULL;
}
SlotOp rt_find_operator_for(const Value *receiver, const char *operator_key,
                            const List *args);
SlotOp rt_find_operator_for_type(const Value *receiver, const char *operator_key,
                                 const List *args, const VTable *static_type);
Value *rt_missing_operator(const char *operator_key);
bool rt_register_slots(VTableInfo *info, void **slots, size_t count);
extern VTableInfo __global_info;

Value *Void_new();
Value *Null_new();
Value *Bool_new(bool value);
Value *Int_new(int64_t value);
Value *Float_new(double value);
Value *String_new(const char *value);
Value *function_is_null(Value *value);

Value *rt_create(VTable *vt, List *args);
Value *rt_destroy(Value *v);

extern Value *null_value;

extern VTableInfo Any_info;
extern VTableInfo Void_info;
extern VTableInfo Null_info;
extern VTableInfo Stringfiable_info;
extern VTableInfo Bool_info;
extern VTableInfo Int_info;
extern VTableInfo Float_info;
extern VTableInfo String_info;

VTable *Any__vt(void);
VTable *Void__vt(void);
VTable *Null__vt(void);
VTable *Stringfiable__vt(void);
VTable *Bool__vt(void);
VTable *Int__vt(void);
VTable *Float__vt(void);
VTable *String__vt(void);

VTableInfo *vt_info_of(VTable *vt);

Value *vt_var_get(const Value *self, const VVariable *var);
void vt_var_set(VVariable *var, Value *value);
Value *rt_var_get(const Value *self, const char *name);
void rt_var_set(Value *self, const char *name, Value *value);
Value *rt_var_assign(Value *self, const char *name, Value *value);

void runtime_init(void);
void function_print(Value *v);
void function_println(Value *v);

#endif
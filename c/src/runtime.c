#include "runtime.h"
#include "vtable.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

Value *null_value = NULL;
VTableInfo __global_info;

#define INT_CACHE_MIN (-128)
#define INT_CACHE_MAX 127

static Value *bool_cache[2];
static Value *int_cache[INT_CACHE_MAX - INT_CACHE_MIN + 1];

typedef struct ValueStorage {
  void *ptr;
  size_t refs;
} ValueStorage;

Value *Value_new(void *ptr) {
  Value *v = (Value *)malloc(sizeof(Value));
  if (!v)
    return NULL;
  v->ptr = ptr;
  v->storage = NULL;
  if (ptr) {
    ValueStorage *storage = (ValueStorage *)malloc(sizeof(ValueStorage));
    if (!storage) {
      free(v);
      return NULL;
    }
    storage->ptr = ptr;
    storage->refs = 1;
    v->storage = storage;
  }
  return v;
}

static void value_storage_release(ValueStorage *storage) {
  if (!storage || --storage->refs > 0)
    return;

  Value view = {.ptr = storage->ptr, .storage = NULL};
  const VTable *vt = value_vtable(&view);
  VTableInfo *info = vt ? vt_get(vt->index) : NULL;
  if (info && info->destroy.address)
    ((DestroyFn)info->destroy.address)(&view);
  else
    free(storage->ptr);
  free(storage);
}

Value *value_assign(Value *target, const Value *source) {
  if (!target)
    return (Value *)source;
  if (target == source)
    return target;

  ValueStorage *next = source ? source->storage : NULL;
  if (next)
    next->refs++;

  ValueStorage *previous = target->storage;
  target->storage = next;
  target->ptr = source ? source->ptr : NULL;
  value_storage_release(previous);
  return target;
}

static Value *value_clone(const Value *source) {
  if (!source)
    return NULL;
  Value *copy = (Value *)malloc(sizeof(Value));
  if (!copy)
    return NULL;
  copy->ptr = source->ptr;
  copy->storage = source->storage;
  if (copy->storage)
    ((ValueStorage *)copy->storage)->refs++;
  return copy;
}

static bool vt_isa(const VTable *vt, const char *name) {
  if (!vt || !name)
    return false;
  if (strcmp(vt->name, name) == 0)
    return true;
  for (size_t i = 0; i < vt->supers.count; i++)
    if (vt_isa((const VTable *)vt->supers.items[i], name))
      return true;
  return false;
}

bool value_is(const Value *v, const char *name) {
  const VTable *vt = value_vtable(v);
  return vt && name && strcmp(vt->name, name) == 0;
}

bool value_isa(const Value *v, const char *name) {
  return vt_isa(value_vtable(v), name);
}

bool value_is_null(const Value *v) {
  return !v || !v->ptr || value_is(v, "Null");
}

bool value_as_bool(const Value *v) {
  if (!value_is(v, "Bool")) {
    fputs("runtime error: expected Bool condition\n", stderr);
    abort();
  }
  return ((const Bool *)v->ptr)->value;
}

Value *value_require_nonnull(Value *v, const char *member) {
  if (value_is_null(v)) {
    fprintf(stderr, "runtime error: cannot access '%s' on null\n",
            member ? member : "member");
    abort();
  }
  return v;
}

Value *rt_missing_operator(const char *operator_key) {
  fprintf(stderr, "runtime error: no matching operator '%s'\n",
          operator_key ? operator_key : "<unknown>");
  abort();
}

static size_t type_distance(const VTable *vt, const char *name, size_t depth) {
  if (!vt || !name)
    return SIZE_MAX;
  if (strcmp(vt->name, name) == 0)
    return depth;

  size_t best = SIZE_MAX;
  for (size_t i = 0; i < vt->supers.count; i++) {
    size_t distance =
        type_distance((const VTable *)vt->supers.items[i], name, depth + 1);
    if (distance < best)
      best = distance;
  }
  return best;
}

static const VTable *dispatch_vtable(const Value *value) {
  if (value_is_null(value))
    return Null__vt();
  return value_vtable(value);
}

typedef struct {
  SlotOp function;
  size_t score;
  size_t owner_depth;
} OperatorMatch;

static void find_operator_match_slots(const List *slots, const char *operator_key,
                                      const List *args, size_t owner_depth,
                                      const VTable *null_receiver_type,
                                      OperatorMatch *best) {
  for (size_t i = 0; i < slots->count; i++) {
    const VSlot *slot = (const VSlot *)slots->items[i];
    if (!slot || slot->kind != VSLOT_OPERATOR)
      continue;
    const VOperator *op = &slot->value.operator;
    if (!op->operator_key || strcmp(op->operator_key, operator_key) != 0 ||
        op->arity != args->count)
      continue;

    size_t score = 0;
    bool matches = true;
    for (size_t j = 0; j < op->arity; j++) {
      const Value *argument = (const Value *)List_get(args, j);
      const VTable *argument_type =
          j == 0 && value_is_null(argument) && null_receiver_type
              ? null_receiver_type
              : dispatch_vtable(argument);
      size_t distance = type_distance(argument_type, op->parameter_types[j], 0);
      if (distance == SIZE_MAX) {
        matches = false;
        break;
      }
      score += distance;
    }

    if (matches &&
        (score < best->score ||
         (score == best->score && owner_depth < best->owner_depth))) {
      best->function = (SlotOp)op->address;
      best->score = score;
      best->owner_depth = owner_depth;
    }
  }
}

static void find_operator_match(const VTable *vt, const char *operator_key,
                                const List *args, size_t owner_depth,
                                const VTable *null_receiver_type,
                                OperatorMatch *best) {
  if (!vt)
    return;
  VTableInfo *info = vt_get(vt->index);
  const List *slots = info ? &info->slots : &vt->slots;
  find_operator_match_slots(slots, operator_key, args, owner_depth,
                            null_receiver_type, best);

  for (size_t i = 0; i < vt->supers.count; i++)
    find_operator_match((const VTable *)vt->supers.items[i], operator_key, args,
                        owner_depth + 1, null_receiver_type, best);
}

static SlotOp find_operator_for_vtable(const VTable *vtable,
                                       const char *operator_key,
                                       const List *args,
                                       const VTable *null_receiver_type) {
  OperatorMatch best = {NULL, SIZE_MAX, SIZE_MAX};
  find_operator_match(vtable, operator_key, args, 0, null_receiver_type, &best);
  return best.function;
}

SlotOp rt_find_operator_for_type(const Value *receiver, const char *operator_key,
                                 const List *args, const VTable *static_type) {
  if (!operator_key || !args || !args->count)
    return NULL;

  const VTable *receiver_vtable = dispatch_vtable(receiver);
  bool null_receiver = value_is_null(receiver);
  if (null_receiver && static_type) {
    SlotOp function = find_operator_for_vtable(static_type, operator_key, args,
                                               static_type);
    if (function)
      return function;
  }

  SlotOp function =
      find_operator_for_vtable(receiver_vtable, operator_key, args, NULL);
  if (function)
    return function;

  if (static_type && static_type != receiver_vtable)
    return find_operator_for_vtable(static_type, operator_key, args, NULL);
  return NULL;
}

SlotOp rt_find_operator_for(const Value *receiver, const char *operator_key,
                            const List *args) {
  return rt_find_operator_for_type(receiver, operator_key, args, NULL);
}

bool rt_register_slots(VTableInfo *info, void **slots, size_t count) {
  if (!info || (!slots && count))
    return false;
  
  size_t capacity = info->slots.count + count;
  void **merged = capacity ? calloc(capacity, sizeof(void *)) : NULL;
  if (capacity && !merged)
    return false;

  size_t merged_count = 0;
  for (size_t i = 0; i < info->slots.count; i++)
    merged[merged_count++] = info->slots.items[i];

  for (size_t i = 0; i < count; i++) {
    const VSlot *extension = (const VSlot *)slots[i];
    bool replaced = false;
    for (size_t j = 0; j < merged_count; j++) {
      const VSlot *existing = (const VSlot *)merged[j];
      if (existing && extension && existing->kind == extension->kind &&
          existing->name && extension->name &&
          strcmp(existing->name, extension->name) == 0) {
        merged[j] = slots[i];
        replaced = true;
        break;
      }
    }
    if (!replaced)
      merged[merged_count++] = slots[i];
  }

  free(info->owned_slots);
  info->owned_slots = merged;
  info->slots = List_static_init(merged, merged_count);

  return true;
}

/* ==================== runtime: mapa VTable* → VTableInfo* ====================
 */

Value *rt_create(VTable *vt, List *args) {
  VTableInfo *info = vt_get(vt->index);
  if (!info || !info->create.address)
    return NULL;
  return ((CreateFn)info->create.address)(args);
}

Value *rt_destroy(Value *v) {
  if (!v)
    return NULL;
  if (v->storage)
    value_storage_release((ValueStorage *)v->storage);
  else
    free(v->ptr);
  free(v);
  return NULL;
}

/* ==================== objetos ==================== */

static void *obj_new(size_t size, const VTable *vt) {
  Any *a = (Any *)calloc(1, size);
  if (!a)
    return NULL;
  a->vtable = vt;
  return a;
}

Value *Void_new() {
  Void *v = (Void *)obj_new(sizeof(Void), Void__vt());
  if (!v)
    return NULL;
  return Value_new(v);
}

Value *Null_new() {
  Null *v = (Null *)obj_new(sizeof(Null), Null__vt());
  if (!v)
    return NULL;
  return Value_new(v);
}

static Value *bool_allocate(bool value) {
  Bool *b = (Bool *)obj_new(sizeof(Bool), Bool__vt());
  if (!b)
    return NULL;
  b->value = value;
  return Value_new(b);
}

Value *Bool_new(bool value) {
  runtime_init();
  size_t index = value ? 1 : 0;
  if (!bool_cache[index]) {
    bool_cache[index] = bool_allocate(value);
    if (!bool_cache[index])
      return NULL;
  }
  return value_clone(bool_cache[index]);
}

static Value *int_allocate(int64_t value) {
  Int *i = (Int *)obj_new(sizeof(Int), Int__vt());
  if (!i)
    return NULL;
  i->value = value;
  return Value_new(i);
}

Value *Int_new(int64_t value) {
  runtime_init();
  if (value >= INT_CACHE_MIN && value <= INT_CACHE_MAX) {
    size_t index = (size_t)(value - INT_CACHE_MIN);
    if (!int_cache[index]) {
      int_cache[index] = int_allocate(value);
      if (!int_cache[index])
        return NULL;
    }
    return value_clone(int_cache[index]);
  }
  return int_allocate(value);
}

static Value *float_allocate(double value) {
  Float *f = (Float *)obj_new(sizeof(Float), Float__vt());
  if (!f)
    return NULL;
  f->value = value;
  return Value_new(f);
}

Value *Float_new(double value) {
  runtime_init();
  return float_allocate(value);
}

Value *String_new(const char *value) {
  String *s = (String *)obj_new(sizeof(String), String__vt());
  if (!s)
    return NULL;
  s->value = value ? strdup(value) : NULL;
  return Value_new(s);
}

/* ==================== create ==================== */

static Value *Any__create(List *args) {
  (void)args;
  return Value_new(obj_new(sizeof(Any), Any__vt()));
}
static Value *Void__create(List *args) {
  (void)args;
  return Value_new(obj_new(sizeof(Void), Void__vt()));
}
static Value *Null__create(List *args) {
  (void)args;
  return Value_new(obj_new(sizeof(Null), Null__vt()));
}
static Value *Stringfiable__create(List *args) {
  (void)args;
  return Value_new(obj_new(sizeof(Any), Stringfiable__vt()));
}
static Value *Bool__create(List *args) {
  Value *src = List_arg(args, 0);
  if (!src || !src->ptr)
    return Bool_new(false);
  return Bool_new(((Bool *)src->ptr)->value);
}
static Value *Int__create(List *args) {
  Value *src = List_arg(args, 0);
  if (!src || !src->ptr)
    return Int_new(0);
  return Int_new(((Int *)src->ptr)->value);
}
static Value *Float__create(List *args) {
  Value *src = List_arg(args, 0);
  if (!src || !src->ptr)
    return Float_new(0.0);
  return Float_new(((Float *)src->ptr)->value);
}
static Value *String__create(List *args) {
  Value *src = List_arg(args, 0);
  if (!src || !src->ptr)
    return String_new("");
  return String_new(((String *)src->ptr)->value);
}

/* ==================== destroy ==================== */

static Value *void_destroy(Value *self) {
  free(self->ptr);
  return NULL;
}

static Value *string_destroy(Value *self) {
  String *s = (String *)self->ptr;
  if (s)
    free(s->value);
  free(self->ptr);
  return NULL;
}

/* ==================== stringfy ==================== */

static Value *Stringfiable__stringfy(const Value *self, List *args) {
  (void)self;
  (void)args;
  return String_new("<Stringfiable>");
}
static Value *Bool__stringfy(const Value *self, List *args) {
  (void)args;
  return String_new(((Bool *)self->ptr)->value ? "true" : "false");
}
static Value *Int__stringfy(const Value *self, List *args) {
  (void)args;
  char buf[32];
  snprintf(buf, sizeof(buf), "%lld", (long long)((Int *)self->ptr)->value);
  return String_new(buf);
}
static Value *Float__stringfy(const Value *self, List *args) {
  (void)args;
  char buf[64];
  snprintf(buf, sizeof(buf), "%g", ((Float *)self->ptr)->value);
  return String_new(buf);
}
static Value *String__stringfy(const Value *self, List *args) {
  (void)args;
  return String_new(((String *)self->ptr)->value);
}

/* ==================== operadores ==================== */

static Value *Any__equals(List *args) {
  Value *left = List_arg(args, 0);
  Value *right = List_arg(args, 1);
  return value_assign(left, right);
}

static Value *Bool__bang(List *args) {
  const Value *self = List_arg(args, 0);
  return Bool_new(!((Bool *)self->ptr)->value);
}
static Value *Bool__amp_amp(List *args) {
  const Value *self = List_arg(args, 0);
  Value *o = List_arg(args, 1);
  if (!o)
    return NULL;
  return Bool_new(((Bool *)self->ptr)->value && ((Bool *)o->ptr)->value);
}
static Value *Bool__bar_bar(List *args) {
  const Value *self = List_arg(args, 0);
  Value *o = List_arg(args, 1);
  if (!o)
    return NULL;
  return Bool_new(((Bool *)self->ptr)->value || ((Bool *)o->ptr)->value);
}
static Value *Int__plus(List *args) {
  const Value *self = List_arg(args, 0);
  Value *o = List_arg(args, 1);
  if (!o)
    return NULL;
  return Int_new(((Int *)self->ptr)->value + ((Int *)o->ptr)->value);
}
static Value *Int__minus(List *args) {
  const Value *self = List_arg(args, 0);
  Value *o = List_arg(args, 1);
  if (!o)
    return NULL;
  return Int_new(((Int *)self->ptr)->value - ((Int *)o->ptr)->value);
}
static Value *Int__star(List *args) {
  const Value *self = List_arg(args, 0);
  Value *o = List_arg(args, 1);
  if (!o)
    return NULL;
  return Int_new(((Int *)self->ptr)->value * ((Int *)o->ptr)->value);
}
static Value *Float__plus(List *args) {
  const Value *self = List_arg(args, 0);
  Value *o = List_arg(args, 1);
  if (!o)
    return NULL;
  return Float_new(((Float *)self->ptr)->value + ((Float *)o->ptr)->value);
}
static Value *String__plus(List *args) {
  const Value *self = List_arg(args, 0);
  return String_new(((String *)self->ptr)->value);
}

Value *function_is_null(Value *value) { return Bool_new(value_is_null(value)); }

/* ==================== variáveis ==================== */

Value *vt_var_get(const Value *self, const VVariable *var) {
  if (!self || !self->ptr || !var)
    return NULL;
  return var->value;
}

void vt_var_set(VVariable *var, Value *value) {
  if (!var)
    return;
  var->value = value;
}

static VVariable *require_variable(const Value *self, const char *name) {
  if (!name) {
    fputs("runtime error: variable name is null\n", stderr);
    abort();
  }
  value_require_nonnull((Value *)self, name);
  VTable *vt = (VTable *)value_vtable(self);
  VVariable *variable = find_variable(vt, name);
  if (!variable) {
    fprintf(stderr, "runtime error: variable '%s' not found on type '%s'\n", name,
            vt ? vt->name : "<unknown>");
    abort();
  }
  return variable;
}

Value *rt_var_get(const Value *self, const char *name) {
  VVariable *variable = require_variable(self, name);
  return vt_var_get(self, variable);
}

void rt_var_set(Value *self, const char *name, Value *value) {
  VVariable *variable = require_variable(self, name);
  vt_var_set(variable, value);
}

Value *rt_var_assign(Value *self, const char *name, Value *value) {
  VVariable *variable = require_variable(self, name);
  Value *current = vt_var_get(self, variable);
  Value *assigned = value_assign(current, value);
  vt_var_set(variable, assigned);
  return assigned;
}

/* ==================== slots ==================== */

static const VSlot Any__slots[] = {
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "equals", "Any_Any"),
                   SLOT_OP_KEY("infix", "equals"), &Any__equals, 2, "Any",
                   "Any"),
};
static const VSlot Void__slots[] = {};
static const VSlot Null__slots[] = {};

static const VSlot Stringfiable__slots[] = {
    VSLOT_FN(SLOT_FN_NAME("stringfy", "void"), &Stringfiable__stringfy),
};

static const VSlot Bool__slots[] = {
    VSLOT_FN(SLOT_FN_NAME("stringfy", "void"), &Bool__stringfy),
    VSLOT_OP_TYPED(SLOT_OP_NAME("prefix", "bang", "Bool"),
                   SLOT_OP_KEY("prefix", "bang"), &Bool__bang, 1, "Bool", NULL),
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "amp_amp", "Bool_Bool"),
                   SLOT_OP_KEY("infix", "amp_amp"), &Bool__amp_amp, 2, "Bool",
                   "Bool"),
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "bar_bar", "Bool_Bool"),
                   SLOT_OP_KEY("infix", "bar_bar"), &Bool__bar_bar, 2, "Bool",
                   "Bool"),
};

static const VSlot Int__slots[] = {
    VSLOT_FN(SLOT_FN_NAME("stringfy", "void"), &Int__stringfy),
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "plus", "Int_Int"),
                   SLOT_OP_KEY("infix", "plus"), &Int__plus, 2, "Int", "Int"),
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "minus", "Int_Int"),
                   SLOT_OP_KEY("infix", "minus"), &Int__minus, 2, "Int", "Int"),
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "star", "Int_Int"),
                   SLOT_OP_KEY("infix", "star"), &Int__star, 2, "Int", "Int"),
};

static const VSlot Float__slots[] = {
    VSLOT_FN(SLOT_FN_NAME("stringfy", "void"), &Float__stringfy),
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "plus", "Float_Float"),
                   SLOT_OP_KEY("infix", "plus"), &Float__plus, 2, "Float",
                   "Float"),
};

static const VSlot String__slots[] = {
    VSLOT_FN(SLOT_FN_NAME("stringfy", "void"), &String__stringfy),
    VSLOT_OP_TYPED(SLOT_OP_NAME("infix", "plus", "String_Stringfiable"),
                   SLOT_OP_KEY("infix", "plus"), &String__plus, 2, "String",
                   "Stringfiable"),
};

static void *Any__slot_ptrs[] = {(void *)&Any__slots[0]};
static void *Void__slot_ptrs[] = {};
static void *Null__slot_ptrs[] = {};

static void *Stringfiable__slot_ptrs[] = {(void *)&Stringfiable__slots[0]};

static void *Bool__slot_ptrs[] = {
    (void *)&Bool__slots[0], (void *)&Bool__slots[1], (void *)&Bool__slots[2],
    (void *)&Bool__slots[3]};

static void *Int__slot_ptrs[] = {(void *)&Int__slots[0], (void *)&Int__slots[1],
                                 (void *)&Int__slots[2],
                                 (void *)&Int__slots[3]};

static void *Float__slot_ptrs[] = {(void *)&Float__slots[0],
                                   (void *)&Float__slots[1]};

static void *String__slot_ptrs[] = {(void *)&String__slots[0],
                                    (void *)&String__slots[1]};

VTableInfo Any_info;
VTableInfo Void_info;
VTableInfo Null_info;
VTableInfo Stringfiable_info;
VTableInfo Bool_info;
VTableInfo Int_info;
VTableInfo Float_info;
VTableInfo String_info;

DEF_BUILD(Any)
DEF_BUILD(__global)
DEF_BUILD(Void)
DEF_BUILD(Null)
DEF_BUILD(Stringfiable)
DEF_BUILD(Bool)
DEF_BUILD(Int)
DEF_BUILD(Float)
DEF_BUILD(String)

DEF_VT(Any)
DEF_VT(__global)
DEF_VT(Void)
DEF_VT(Null)
DEF_VT(Stringfiable)
DEF_VT(Bool)
DEF_VT(Int)
DEF_VT(Float)
DEF_VT(String)

static bool g_init = false;

void runtime_init(void) {
  if (g_init)
    return;
  g_init = true;

  __global_info.name = "__global";
  List_init(&__global_info.supers);
  List_init(&__global_info.slots);
  __global_info.build = __global__build;
  vt_add(&__global_info);

  DEF_TYPE_INIT(Any, 1, void_destroy,
                (void *)(intptr_t)__global_info.index);

  DEF_TYPE_INIT(Void, 0, void_destroy,
                (void *)(intptr_t)Any_info.index);
  DEF_TYPE_INIT(Null, 0, void_destroy,
                (void *)(intptr_t)Any_info.index);
  DEF_TYPE_INIT(Stringfiable, 1, void_destroy,
                (void *)(intptr_t)Any_info.index);
  DEF_TYPE_INIT(Bool, 4, void_destroy,
                (void *)(intptr_t)Stringfiable_info.index);
  DEF_TYPE_INIT(Int, 4, void_destroy,
                (void *)(intptr_t)Stringfiable_info.index);
  DEF_TYPE_INIT(Float, 2, void_destroy,
                (void *)(intptr_t)Stringfiable_info.index);
  DEF_TYPE_INIT(String, 2, string_destroy,
                (void *)(intptr_t)Stringfiable_info.index);

  null_value = Value_new(obj_new(sizeof(Null), Null__vt()));
}

__attribute__((constructor)) static void runtime__auto_init(void) {
  runtime_init();
}

void function_print(Value *v) {
  if (!v || !v->ptr) {
    fputs("null", stdout);
    return;
  }
  SlotFn fn = rt_find_function((VTable *)value_vtable(v),
                               SLOT_FN_NAME("stringfy", "void"));
  if (!fn)
    return;
  Value *sv = fn(v, NULL);
  if (!sv || !sv->ptr)
    return;
  const String *s = (const String *)sv->ptr;
  if (s->value)
    fputs(s->value, stdout);
}

void function_println(Value *v) {
  function_print(v);
  fputc('\n', stdout);
}
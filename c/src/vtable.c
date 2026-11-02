#include "vtable.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

List vtables;

void vt_add(VTableInfo *info) {
  if (!info)
    return;
  info->index = (int)vtables.count;
  if (!List_push(&vtables, info))
    info->index = -1;
}

VTableInfo *vt_get(int index) {
  if (index < 0 || (size_t)index >= vtables.count)
    return NULL;
  return (VTableInfo *)vtables.items[index];
}

static char *copy_string(const char *string) {
  if (!string)
    return NULL;
  size_t length = strlen(string) + 1;
  char *copy = (char *)malloc(length);
  if (copy)
    memcpy(copy, string, length);
  return copy;
}

bool vfunction_copy(VFunction *destination, const VFunction *source) {
  if (!destination || !source)
    return false;
  memcpy(destination, source, sizeof(*destination));
  return true;
}

bool vvariable_copy(VVariable *destination, const VVariable *source) {
  if (!destination || !source)
    return false;
  memcpy(destination, source, sizeof(*destination));
  return true;
}

void voperator_free(VOperator *value) {
  if (!value)
    return;
  free((char *)value->operator_key);
  free((char *)value->parameter_types[0]);
  free((char *)value->parameter_types[1]);
  memset(value, 0, sizeof(*value));
}

bool voperator_copy(VOperator *destination, const VOperator *source) {
  if (!destination || !source)
    return false;

  memset(destination, 0, sizeof(*destination));
  destination->address = source->address;
  destination->arity = source->arity;
  destination->operator_key = copy_string(source->operator_key);
  destination->parameter_types[0] = copy_string(source->parameter_types[0]);
  destination->parameter_types[1] = copy_string(source->parameter_types[1]);

  if ((source->operator_key && !destination->operator_key) ||
      (source->parameter_types[0] && !destination->parameter_types[0]) ||
      (source->parameter_types[1] && !destination->parameter_types[1])) {
    voperator_free(destination);
    return false;
  }
  return true;
}

void vslot_free(VSlot *slot) {
  if (!slot)
    return;
  free((char *)slot->name);
  if (slot->kind == VSLOT_OPERATOR)
    voperator_free(&slot->value.operator);
  free(slot);
}

VSlot *vslot_copy(const VSlot *slot) {
  if (!slot)
    return NULL;
  VSlot *copy = (VSlot *)calloc(1, sizeof(VSlot));
  if (!copy)
    return NULL;

  copy->kind = slot->kind;
  switch (slot->kind) {
  case VSLOT_FUNCTION:
    if (!vfunction_copy(&copy->value.function, &slot->value.function))
      goto fail;
    break;
  case VSLOT_VARIABLE:
    if (!vvariable_copy(&copy->value.variable, &slot->value.variable))
      goto fail;
    break;
  case VSLOT_OPERATOR:
    if (!voperator_copy(&copy->value.operator, &slot->value.operator))
      goto fail;
    break;
  default:
    goto fail;
  }

  copy->name = copy_string(slot->name);
  if (slot->name && !copy->name) {
    goto fail;
  }
  return copy;

fail:
  vslot_free(copy);
  return NULL;
}

static VSlot *find_slot(VTable *vt, const char *name, uint32_t kind) {
  if (!vt || !name)
    return NULL;

  const size_t variable_prefix_length = sizeof("variable_") - 1;
  VTableInfo *info = kind == VSLOT_VARIABLE ? NULL : vt_get(vt->index);
  const List *slots = info ? &info->slots : &vt->slots;
  for (size_t i = 0; i < slots->count; i++) {
    VSlot *s = (VSlot *)slots->items[i];
    if (!s || !s->name)
      continue;
    bool name_matches = kind == VSLOT_VARIABLE
                            ? strncmp(s->name, SLOT_VAR_NAME(""),
                                      variable_prefix_length) == 0 &&
                                  strcmp(s->name + variable_prefix_length,
                                         name) == 0
                            : strcmp(s->name, name) == 0;
    if (s->kind == kind && name_matches)
      return s;
  }

  for (size_t i = 0; i < vt->supers.count; i++) {
    VTable *parent = (VTable *)vt->supers.items[i];
    if (!parent)
      continue;

    VSlot *s = find_slot(parent, name, kind);
    if (s)
      return s;
  }

  return NULL;
}

const VFunction *find_function(VTable *vt, const char *name) {
  const VSlot *s = find_slot(vt, name, VSLOT_FUNCTION);
  return s ? &s->value.function : NULL;
}

const VOperator *find_operator(VTable *vt, const char *name) {
  const VSlot *s = find_slot(vt, name, VSLOT_OPERATOR);
  return s ? &s->value.operator : NULL;
}

VVariable *find_variable(VTable *vt, const char *name) {
  VSlot *s = find_slot(vt, name, VSLOT_VARIABLE);
  return s ? &s->value.variable : NULL;
}
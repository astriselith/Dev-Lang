#include "list.h"
#include "test.h"
#include "vtable.h"

Value *function_symbolProbe(List *args) { return List_arg(args, 0); }

static const VSlot operator_slot =
    VSLOT_OP_TYPED("operator_probe", "key_probe", &function_symbolProbe, 2,
                   "FirstType", "SecondType");

int main(int argc, char **argv) {
  List args = {0};
  List_init(&args);

  if (__global_info.slots.count != 3 || Any_info.slots.count != 1 ||
      Any_info.supers.count != 1 ||
      find_operator(Any__vt(), SLOT_OP_NAME("infix", "tilde", "Int_Int")) == NULL)
    return 13;
  Value *null_as_a = Null_new();
  List null_as_a_args = List_elements((void *[]){null_as_a}, 1);
  const VOperator *a_question = find_operator(
      A__vt(), SLOT_OP_NAME("postfix", "question", "generic_A"));
  if (!a_question ||
      rt_find_operator_for_type(null_as_a, SLOT_OP_KEY("postfix", "question"),
                                &null_as_a_args, A__vt()) !=
          (SlotOp)a_question->address)
    return 16;
  rt_destroy(null_as_a);
  List_free(&null_as_a_args);

  Value *global_first = Int_new(2);
  Value *global_second = Int_new(3);
  List global_args = List_elements((void *[]){global_first, global_second}, 2);
  SlotOp global_operator =
      rt_find_operator_for(global_first, SLOT_OP_KEY("infix", "tilde"),
                           &global_args);
  Value *global_result = global_operator ? global_operator(&global_args) : NULL;
  if (!global_result || !value_is(global_result, "Int") ||
      ((Int *)global_result->ptr)->value != 5)
    return 14;
  rt_destroy(global_result);

  Value *null_receiver = Null_new();
  List null_args = List_elements((void *[]){null_receiver}, 1);
  SlotOp global_null_operator =
      rt_find_operator_for(null_receiver,
                           SLOT_OP_KEY("postfix", "question"), &null_args);
  if (global_null_operator != operator_postfix_question_Any)
    return 15;
  rt_destroy(null_receiver);
  List_free(&null_args);

  rt_destroy(global_first);
  rt_destroy(global_second);
  List_free(&global_args);

  Value *true_first = Bool_new(true);
  Value *true_second = Bool_new(true);
  if (!true_first || !true_second || true_first == true_second ||
      true_first->ptr != true_second->ptr)
    return 5;

  Value *int_first = Int_new(5);
  Value *int_second = Int_new(5);
  if (!int_first || !int_second || int_first == int_second ||
      int_first->ptr != int_second->ptr)
    return 6;
  Value *replacement_int = Int_new(6);
  Value *cached_int_again = Int_new(5);
  if (!replacement_int || !cached_int_again)
    return 7;
  value_assign(int_first, replacement_int);
  if (((Int *)int_second->ptr)->value != 5 ||
      ((Int *)cached_int_again->ptr)->value != 5)
    return 8;

  Value *int_min_cached = Int_new(-128);
  Value *int_min_cached_again = Int_new(-128);
  Value *int_below_cache = Int_new(-129);
  Value *int_below_cache_again = Int_new(-129);
  Value *int_max_cached = Int_new(127);
  Value *int_above_cache = Int_new(128);
  Value *int_above_cache_again = Int_new(128);
  if (!int_min_cached || !int_min_cached_again || !int_below_cache ||
      !int_below_cache_again || !int_max_cached || !int_above_cache ||
      !int_above_cache_again ||
      int_min_cached->ptr != int_min_cached_again->ptr ||
      int_below_cache->ptr == int_below_cache_again->ptr ||
      int_above_cache->ptr == int_above_cache_again->ptr)
    return 9;

  Value *float_first = Float_new(0.5);
  Value *float_second = Float_new(0.5);
  if (!float_first || !float_second || float_first == float_second ||
      float_first->ptr == float_second->ptr)
    return 10;

  Value *uncached_int_first = Int_new(129);
  Value *uncached_int_second = Int_new(129);
  if (!uncached_int_first || !uncached_int_second ||
      uncached_int_first->ptr == uncached_int_second->ptr)
    return 11;

  rt_destroy(true_first);
  rt_destroy(true_second);
  rt_destroy(int_first);
  rt_destroy(int_second);
  rt_destroy(replacement_int);
  rt_destroy(cached_int_again);
  rt_destroy(int_min_cached);
  rt_destroy(int_min_cached_again);
  rt_destroy(int_below_cache);
  rt_destroy(int_below_cache_again);
  rt_destroy(int_max_cached);
  rt_destroy(int_above_cache);
  rt_destroy(int_above_cache_again);
  rt_destroy(float_first);
  rt_destroy(float_second);
  rt_destroy(uncached_int_first);
  rt_destroy(uncached_int_second);

  VSlot *operator_slot_copy = vslot_copy(&operator_slot);
  if (!operator_slot_copy ||
      operator_slot_copy->name == operator_slot.name ||
      operator_slot_copy->value.operator.operator_key ==
          operator_slot.value.operator.operator_key ||
      operator_slot_copy->value.operator.parameter_types[0] ==
          operator_slot.value.operator.parameter_types[0] ||
      operator_slot_copy->value.operator.parameter_types[1] ==
          operator_slot.value.operator.parameter_types[1])
    return 4;
  vslot_free(operator_slot_copy);

  VTable *slot_probe = A__vt();
  VVariable *variable = find_variable(slot_probe, "should");
  if (!variable)
    return 2;
  Value *slot_value = Int_new(77);
  variable->value = slot_value;

  VTable *second_slot_probe = A__vt();
  VVariable *second_variable =
      find_variable(second_slot_probe, "should");
  if (!second_variable || second_variable->value != NULL)
    return 3;

  function_main(&args);
  Value *a = A_new(&args);
  if (!value_isa(a, "Stringfiable"))
    return 1;
  Value *first_instance = A_new(&args);
  Value *second_instance = A_new(&args);
  Value *instance_value = Int_new(88);
  rt_var_set(first_instance, "should", instance_value);
  if (rt_var_get(first_instance, "should") != instance_value ||
      rt_var_get(second_instance, "should") != NULL)
    return 12;
  return 0;
}
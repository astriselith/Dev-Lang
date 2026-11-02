#pragma once
#ifndef LIST_H
#define LIST_H

#include <stddef.h>
#include <stdbool.h>

typedef bool (*List_predicate)(const void* item, const void* key);

typedef struct List {
    void**  items;
    size_t  count;
    size_t  cap;
    bool    owns_items;
} List;

List   List_static_init(void** items, size_t count);
bool   List_copy_init(List* list, void** items, size_t count);
List   List_elements(void** items, size_t count);
void   List_init(List* l);
void   List_free(List* l);
void   List_clear(List* l);

bool   List_push(List* l, void* item);
bool   List_reserve(List* l, size_t cap);

void*  List_get(const List* l, size_t i);
void*  List_find(const List* l, const void* key, List_predicate pred);

size_t List_distinct(List* l, List_predicate pred);
bool   List_remove(List* l, size_t i, void** out);
size_t List_remove_if(List* l, const void* key, List_predicate pred);

typedef struct List_iterator {
    List*   list;
    size_t  index;
} List_iterator;

List_iterator List_iterator_begin(List* l);
bool          List_iterator_has_next(const List_iterator* it);
bool          List_iterator_next(List_iterator* it, void** out);
void*         List_iterator_peek(const List_iterator* it);
bool          List_iterator_remove(List_iterator* it, void** out);

#endif
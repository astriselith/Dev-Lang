#include <stdlib.h>
#include <string.h>
#include "list.h"

#define LIST_INIT_CAP 8

List List_static_init(void** items, size_t count) {
    List l;
    l.items = items;
    l.count = count;
    l.cap   = count;
    l.owns_items = false;
    return l;
}

bool List_copy_init(List* list, void** items, size_t count) {
    if (!list || (count > 0 && !items))
        return false;
    list->items = NULL;
    list->count = 0;
    list->cap = 0;
    list->owns_items = false;
    if (count == 0)
        return true;

    list->items = (void**)malloc(count * sizeof(void*));
    if (!list->items)
        return false;
    memcpy(list->items, items, count * sizeof(void*));
    list->count = count;
    list->cap = count;
    list->owns_items = true;
    return true;
}

List List_elements(void** items, size_t count) {
    List list;
    if (!List_copy_init(&list, items, count))
        abort();
    return list;
}

void List_init(List* l) {
    if (!l) return;
    l->items = NULL;
    l->count = 0;
    l->cap   = 0;
    l->owns_items = false;
}

void List_free(List* l) {
    if (!l) return;
    if (l->owns_items)
        free(l->items);
    l->items = NULL;
    l->count = 0;
    l->cap   = 0;
    l->owns_items = false;
}

void List_clear(List* l) { if (l) l->count = 0; }

bool List_reserve(List* l, size_t cap) {
    if (!l) return false;
    if (cap <= l->cap) return true;
    size_t newcap = l->cap ? l->cap : LIST_INIT_CAP;
    while (newcap < cap) newcap <<= 1;
    void** p;
    if (l->owns_items) {
        p = (void**)realloc(l->items, newcap * sizeof(void*));
    } else {
        p = (void**)malloc(newcap * sizeof(void*));
        if (p && l->count > 0)
            memcpy(p, l->items, l->count * sizeof(void*));
    }
    if (!p) return false;
    l->items = p;
    l->cap   = newcap;
    l->owns_items = true;
    return true;
}

bool List_push(List* l, void* item) {
    if (!l) return false;
    if (l->count == l->cap && !List_reserve(l, l->count + 1)) return false;
    l->items[l->count++] = item;
    return true;
}

void* List_get(const List* l, size_t i) {
    if (!l || i >= l->count) return NULL;
    return l->items[i];
}

void* List_find(const List* l, const void* key, List_predicate pred) {
    if (!l || !pred) return NULL;
    for (size_t i = 0; i < l->count; i++)
        if (pred(l->items[i], key)) return l->items[i];
    return NULL;
}

size_t List_distinct(List* l, List_predicate pred) {
    if (!l || !pred) return l ? l->count : 0;
    size_t out = 0;
    for (size_t i = 0; i < l->count; i++) {
        bool dup = false;
        for (size_t j = 0; j < out; j++)
            if (pred(l->items[j], l->items[i])) { dup = true; break; }
        if (!dup) l->items[out++] = l->items[i];
    }
    l->count = out;
    return out;
}

bool List_remove(List* l, size_t i, void** out) {
    if (!l || i >= l->count) return false;
    if (out) *out = l->items[i];
    if (i + 1 < l->count)
        memmove(&l->items[i], &l->items[i + 1],
                (l->count - i - 1) * sizeof(void*));
    l->count--;
    return true;
}

size_t List_remove_if(List* l, const void* key, List_predicate pred) {
    if (!l || !pred) return 0;
    size_t out = 0, removed = 0;
    for (size_t i = 0; i < l->count; i++) {
        if (pred(l->items[i], key)) { removed++; continue; }
        l->items[out++] = l->items[i];
    }
    l->count = out;
    return removed;
}

List_iterator List_iterator_begin(List* l) {
    List_iterator it = { l, 0 };
    return it;
}

bool List_iterator_has_next(const List_iterator* it) {
    return it && it->list && it->index < it->list->count;
}

bool List_iterator_next(List_iterator* it, void** out) {
    if (!List_iterator_has_next(it)) return false;
    if (out) *out = it->list->items[it->index];
    it->index++;
    return true;
}

void* List_iterator_peek(const List_iterator* it) {
    if (!List_iterator_has_next(it)) return NULL;
    return it->list->items[it->index];
}

bool List_iterator_remove(List_iterator* it, void** out) {
    if (!it || !it->list || it->index == 0) return false;
    size_t at = it->index - 1;
    if (!List_remove(it->list, at, out)) return false;
    it->index = at;
    return true;
}
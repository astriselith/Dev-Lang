#pragma once
#ifndef MAP_H
#define MAP_H

#include <stddef.h>
#include <stdbool.h>

typedef struct Entry {
    const void* key;
    void*       value;
    bool        used;
} Entry;

typedef struct Map {
    Entry*  entries;
    size_t  cap;
    size_t  count;
} Map;

size_t map_hash(const void* key);

Map    Map_static_init(Entry* entries, size_t cap, size_t count);
void   Map_init(Map* m);
void   Map_free(Map* m);
void   Map_clear(Map* m);

bool   Map_put(Map* m, const void* key, void* value);
void*  Map_get(const Map* m, const void* key);
bool   Map_has(const Map* m, const void* key);
size_t Map_count(const Map* m);

#endif
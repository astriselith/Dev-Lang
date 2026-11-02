#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "map.h"

#define MAP_INIT_CAP 8

size_t map_hash(const void* key) {
    const uint8_t* p = (const uint8_t*)&key;
    uint64_t h = 0xcbf29ce484222325ULL;
    for (size_t i = 0; i < sizeof key; i++) {
        h ^= p[i];
        h *= 0x100000001b3ULL;
    }
    h ^= h >> 30;
    h *= 0xbf58476d1ce4e5b9ULL;
    h ^= h >> 27;
    h *= 0x94d049bb133111ebULL;
    h ^= h >> 31;
    return (size_t)h;
}

Map Map_static_init(Entry* entries, size_t cap, size_t count) {
    Map m;
    m.entries = entries;
    m.cap     = cap;
    m.count   = count;
    return m;
}

void Map_init(Map* m) {
    if (!m) return;
    m->entries = NULL;
    m->cap     = 0;
    m->count   = 0;
}

void Map_free(Map* m) {
    if (!m) return;
    free(m->entries);
    m->entries = NULL;
    m->cap     = 0;
    m->count   = 0;
}

void Map_clear(Map* m) {
    if (!m || !m->entries) return;
    memset(m->entries, 0, m->cap * sizeof(Entry));
    m->count = 0;
}

static bool map_resize(Map* m, size_t newcap) {
    Entry* ne = (Entry*)calloc(newcap, sizeof(Entry));
    if (!ne) return false;
    for (size_t i = 0; i < m->cap; i++) {
        if (!m->entries[i].used) continue;
        size_t h = map_hash(m->entries[i].key);
        size_t j = h & (newcap - 1);
        while (ne[j].used) j = (j + 1) & (newcap - 1);
        ne[j] = m->entries[i];
    }
    free(m->entries);
    m->entries = ne;
    m->cap     = newcap;
    return true;
}

bool Map_put(Map* m, const void* key, void* value) {
    if (!m) return false;
    if (m->cap == 0) {
        if (!map_resize(m, MAP_INIT_CAP)) return false;
    } else if (m->count * 4 >= m->cap * 3) {
        if (!map_resize(m, m->cap * 2)) return false;
    }
    size_t h = map_hash(key);
    size_t i = h & (m->cap - 1);
    while (m->entries[i].used) {
        if (m->entries[i].key == key) {
            m->entries[i].value = value;
            return true;
        }
        i = (i + 1) & (m->cap - 1);
    }
    m->entries[i].used  = true;
    m->entries[i].key   = key;
    m->entries[i].value = value;
    m->count++;
    return true;
}

void* Map_get(const Map* m, const void* key) {
    if (!m || !m->entries) return NULL;
    size_t h = map_hash(key);
    size_t i = h & (m->cap - 1);
    while (m->entries[i].used) {
        if (m->entries[i].key == key) return m->entries[i].value;
        i = (i + 1) & (m->cap - 1);
    }
    return NULL;
}

bool Map_has(const Map* m, const void* key) {
    if (!m || !m->entries) return false;
    size_t h = map_hash(key);
    size_t i = h & (m->cap - 1);
    while (m->entries[i].used) {
        if (m->entries[i].key == key) return true;
        i = (i + 1) & (m->cap - 1);
    }
    return false;
}

size_t Map_count(const Map* m) { return m ? m->count : 0; }
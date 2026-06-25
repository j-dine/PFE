<script setup lang="ts">
import { computed } from 'vue'

type HistoryEntry = {
  id?: number | string
  action?: string
  user?: string
  date?: string
  commentaire?: string
  fromStatut?: string
  toStatut?: string
}

const props = withDefaults(defineProps<{
  title?: string
  description?: string
  entries?: HistoryEntry[]
  loading?: boolean
  chrome?: boolean
}>(), {
  title: 'Commentaires & suivi',
  description: '',
  entries: () => [],
  loading: false,
  chrome: true,
})

const list = computed(() => Array.isArray(props.entries) ? props.entries : [])
const hasDescription = computed(() => !!String(props.description || '').trim())
const hasEntries = computed(() => list.value.length > 0)

const statutChange = (entry: HistoryEntry) => {
  const from = entry.fromStatut
  const to = entry.toStatut
  if (!from && !to) return ''
  if (from && to && from !== to) return `${from} → ${to}`
  return to || from || ''
}
</script>

<template>
  <div :style="chrome ? 'border-top:1px solid var(--border);padding:14px 18px 10px' : ''">
    <div style="font-size:11px;font-weight:800;margin-bottom:10px">{{ title }}</div>

    <div v-if="loading" style="font-size:12px;color:var(--muted);padding:6px 0">
      Chargement...
    </div>

    <template v-else>
      <div
        v-if="hasDescription"
        style="background:var(--bg);border:1px solid var(--border);border-radius:10px;padding:12px 14px;margin-bottom:12px"
      >
        <div style="font-size:10px;font-weight:700;color:var(--muted);text-transform:uppercase;letter-spacing:.5px;margin-bottom:6px">
          Description / remarques
        </div>
        <div style="font-size:12px;line-height:1.55;white-space:pre-wrap;color:var(--ink)">{{ description }}</div>
      </div>

      <div v-if="!hasDescription && !hasEntries" class="empty-state" style="padding:14px">
        Aucun commentaire ou description enregistré pour ce dossier.
      </div>

      <div v-else-if="hasEntries" style="display:flex;flex-direction:column;gap:10px">
        <div
          v-for="entry in list"
          :key="entry.id ?? `${entry.action}-${entry.date}`"
          style="border:1px solid var(--border);border-radius:10px;padding:12px 14px;background:#fafafa"
        >
          <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:10px;margin-bottom:6px">
            <div style="font-size:12px;font-weight:800;color:var(--ink)">{{ entry.action || 'Action' }}</div>
            <div style="font-size:10px;color:var(--muted);white-space:nowrap">{{ entry.date || '-' }}</div>
          </div>
          <div v-if="entry.commentaire" style="font-size:12px;line-height:1.55;white-space:pre-wrap;color:var(--ink);margin-bottom:8px">
            {{ entry.commentaire }}
          </div>
          <div style="font-size:10px;color:var(--muted);display:flex;flex-wrap:wrap;gap:8px">
            <span v-if="entry.user">Par : {{ entry.user }}</span>
            <span v-if="statutChange(entry)">Statut : {{ statutChange(entry) }}</span>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

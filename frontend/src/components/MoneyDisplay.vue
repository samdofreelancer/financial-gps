<template>
  <span class="money">{{ text }} <em class="prov" :class="provenance">{{ provenance }}</em></span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatMoney } from '../api/profile'

const props = defineProps<{ amount: string; currency: string; provenance: string }>()

/** Display formatting only; the amount string comes from the server. */
const text = computed(() => formatMoney(props.amount, props.currency))
</script>

<style scoped>
.money { font-variant-numeric: tabular-nums; }
.prov {
  font-style: normal;
  font-size: 12px;
  color: var(--fg-muted);
  border: 1px solid var(--fg-border-soft);
  border-radius: var(--fg-radius-control);
  padding: 1px 6px;
  margin-left: 6px;
}
.prov.calculated {
  color: var(--fg-primary-hover);
  border-color: rgba(0, 143, 211, 0.35);
}
</style>

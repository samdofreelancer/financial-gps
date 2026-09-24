<template>
  <span class="money">
    <span class="value">{{ text }}</span>
    <em v-if="provenance" class="prov" :class="provenance">{{ provenance }}</em>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { formatMoney } from '../api/profile'

/**
 * Display formatting only; the amount string comes from the server.
 * `hideCurrency` is for cards/lists that state the currency once (e.g. in the card header), so the
 * amount itself stays free of repeated "VND" noise. The provenance chip is rendered only when a
 * provenance is supplied: an empty value means "this screen explains the numbers elsewhere".
 */
const props = defineProps<{
  amount: string
  currency: string
  provenance?: string
  hideCurrency?: boolean
}>()

const text = computed(() =>
  formatMoney(props.amount, props.hideCurrency ? '' : props.currency).trim(),
)
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

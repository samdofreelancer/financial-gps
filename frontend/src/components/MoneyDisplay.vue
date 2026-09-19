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
.money {
  font-variant-numeric: tabular-nums;
}
.prov {
  font-style: normal;
  font-size: 12px;
  color: #677b8f;
  border: 1px solid #d9e2eb;
  border-radius: 8px;
  padding: 1px 6px;
  margin-left: 6px;
}
.prov.calculated {
  color: #0b79ce;
  border-color: rgba(15, 141, 235, 0.35);
}
</style>

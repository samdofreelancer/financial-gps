<template>
  <div class="page">
    <header class="head">
      <h1 class="page-title">Quản lý nợ</h1>
      <p class="lead">Ai cho vay, còn bao nhiêu, trả tối thiểu bao nhiêu — và khi nào hết nợ.</p>
    </header>

    <div v-if="store.error" class="error-box" role="alert">{{ store.error }}</div>

    <DebtSummaryCard v-if="store.summary" :summary="store.summary" />
    <DebtBlockerAlert :summary="store.summary" :debts="store.debts" />

    <div class="actions">
      <button type="button" class="btn btn--primary" @click="formOpen = true">Thêm khoản nợ</button>
    </div>

    <DebtForm
      v-if="formOpen"
      :line="editing"
      :error="formError"
      @submit="onSubmit"
      @cancel="cancel"
    />

    <DebtList :debts="store.debts" @edit="startEdit" @remove="onRemove" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useDebtStore } from '../stores/debtStore'
import { problemMessage } from '../api/http'
import type { DebtPayload, DebtView } from '../api/debts'
import DebtBlockerAlert from '../components/debts/DebtBlockerAlert.vue'
import DebtForm from '../components/debts/DebtForm.vue'
import DebtList from '../components/debts/DebtList.vue'
import DebtSummaryCard from '../components/debts/DebtSummaryCard.vue'

const store = useDebtStore()
const formOpen = ref(false)
const editing = ref<DebtView | null>(null)
const formError = ref('')

onMounted(() => {
  void store.refresh()
})

function startEdit(debt: DebtView): void {
  editing.value = debt
  formError.value = ''
  formOpen.value = true
}

function cancel(): void {
  formOpen.value = false
  editing.value = null
  formError.value = ''
}

async function onSubmit(payload: DebtPayload): Promise<void> {
  try {
    formError.value = ''
    if (editing.value) {
      await store.updateDebt(editing.value.id, payload)
    } else {
      await store.addDebt(payload)
    }
    cancel()
  } catch (caught) {
    formError.value = problemMessage(caught, 'Could not save the debt.')
  }
}

async function onRemove(debt: DebtView): Promise<void> {
  try {
    await store.removeDebt(debt.id)
  } catch (caught) {
    formError.value = problemMessage(caught, 'Could not delete the debt.')
  }
}
</script>

<style scoped>
.page { display: grid; gap: 16px; }
.actions { display: flex; gap: 8px; }
.error-box { color: var(--fg-danger); }
</style>

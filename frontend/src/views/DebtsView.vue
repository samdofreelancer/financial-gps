<template>
  <div class="page">
    <header class="head">
      <h1 class="page-title">Quản lý nợ</h1>
      <p class="lead">Ai cho vay, còn bao nhiêu và trả mỗi tháng bao nhiêu. Mục tiêu là khi nào bạn hết nợ.</p>
    </header>

    <div v-if="store.error && !store.summary && !store.debts.length" class="card placeholder" role="alert">
      <p><strong>Không tải được khoản nợ.</strong> {{ store.error }}</p>
      <button type="button" class="btn-ghost small" :disabled="store.loading" @click="store.refresh()">
        Thử lại
      </button>
    </div>
    <div v-else-if="store.error" class="error-box" role="alert">
      Số liệu đang hiển thị có thể đã cũ — {{ store.error }}
      <button type="button" class="btn-ghost small" :disabled="store.loading" @click="store.refresh()">
        Tải lại
      </button>
    </div>

    <!-- Loading and empty are distinct states: an empty portfolio is a result, not a pause. -->
    <div v-if="store.loading && !store.summary" class="card placeholder" data-testid="debts-loading">
      Đang tải khoản nợ của bạn…
    </div>

    <template v-else>
      <div class="layout">
        <div class="layout__side">
          <DebtSummaryCard
            v-if="store.summary"
            :summary="store.summary"
            :hidden="amountsHidden"
            @toggle-amounts="amountsHidden = !amountsHidden"
          />

          <button type="button" class="btn add-debt" @click="openCreate">
            <span aria-hidden="true">+</span> Thêm khoản nợ
          </button>
        </div>

        <div class="layout__main">
          <DebtBlockerAlert
            :summary="store.summary"
            :debts="store.debts"
            @fix="startFix"
          />

          <h2 class="list-title">Khoản nợ của bạn</h2>
          <DebtList
            :debts="store.debts"
            :hidden="amountsHidden"
            :as-of="store.summary?.asOf"
            @edit="startEdit"
            @remove="askRemove"
            @schedule="openSchedule"
            @mark-paid="markPayment"
            @undo-paid="undoPaymentMark"
          />
        </div>
      </div>
    </template>

    <DebtForm v-if="formOpen" :line="editing" :error="formError" @submit="onSubmit" @cancel="cancel" />

    <ConfirmDialog
      v-if="pendingRemoval"
      title="Xóa khoản nợ này?"
      :message="`Khoản nợ với ${pendingRemoval.creditor} sẽ bị ẩn khỏi danh sách.`"
      consequence="Khoản nợ này sẽ không còn tính vào tổng dư, khoản trả bắt buộc và tỷ lệ nợ / thu nhập. Bạn vẫn có thể nhập lại sau."
      confirm-text="Xóa khoản nợ"
      :busy="removing"
      @confirm="onRemove"
      @cancel="pendingRemoval = null"
    />

    <!-- One row per remaining period: principal, interest and ending balance straight from the server. -->
    <DebtScheduleDialog
      v-if="scheduleDebt"
      :debt="scheduleDebt"
      :schedule="store.schedule"
      :loading="store.scheduleLoading"
      :error="store.scheduleError"
      @close="closeSchedule"
    />

    <ToastStack />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useDebtStore } from '../stores/debtStore'
import { useToasts } from '../stores/toastStore'
import { problemMessage } from '../api/http'
import type { DebtPayload, DebtView } from '../api/debts'
import ConfirmDialog from '../components/ConfirmDialog.vue'
import DebtBlockerAlert from '../components/debts/DebtBlockerAlert.vue'
import DebtForm from '../components/debts/DebtForm.vue'
import DebtList from '../components/debts/DebtList.vue'
import DebtScheduleDialog from '../components/debts/DebtScheduleDialog.vue'
import DebtSummaryCard from '../components/debts/DebtSummaryCard.vue'
import ToastStack from '../components/ToastStack.vue'

/**
 * Orchestrator for the debt screen: it owns the store, the API calls and the confirmation
 * vocabulary, while every section stays presentational. No financial math happens here.
 *
 * Deleting is staged through `pendingRemoval` so a stray click never archives a loan, and every
 * completed action ends in a toast — a silent re-render reads as a frozen page.
 */
const store = useDebtStore()
const toasts = useToasts()

const formOpen = ref(false)
const editing = ref<DebtView | null>(null)
const formError = ref('')
const amountsHidden = ref(false)
const pendingRemoval = ref<DebtView | null>(null)
const removing = ref(false)
/** Which debt's calendar is open; the rows themselves live in the store. */
const scheduleDebt = ref<DebtView | null>(null)

onMounted(() => {
  void store.refresh()
})

function openCreate(): void {
  editing.value = null
  formError.value = ''
  formOpen.value = true
}

function startEdit(debt: DebtView): void {
  editing.value = debt
  formError.value = ''
  formOpen.value = true
}

/** The blocker panel's CTA: jump straight into editing the debt that is stuck. */
function startFix(creditor: string): void {
  const debt = store.debts.find((d) => d.creditor === creditor)
  if (!debt) return
  startEdit(debt)
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
      toasts.success(`Đã cập nhật khoản nợ với ${payload.creditor}.`)
    } else {
      await store.addDebt(payload)
      toasts.success(`Đã thêm khoản nợ với ${payload.creditor}.`)
    }
    cancel()
  } catch (caught) {
    formError.value = problemMessage(caught, 'Không lưu được khoản nợ.')
  }
}

function askRemove(debt: DebtView): void {
  pendingRemoval.value = debt
}

/** The calendar button on a debt row: open the dialog first, fill it when the server answers. */
async function openSchedule(debt: DebtView): Promise<void> {
  scheduleDebt.value = debt
  await store.fetchSchedule(debt.id)
}

/** Record a manual completion marker only; no payment is sent and the debt balance is unchanged. */
async function markPayment(debt: DebtView): Promise<void> {
  try {
    await store.markPaid(debt.id)
    toasts.success(`Đã đánh dấu đã trả kỳ này cho ${debt.creditor}. Dư nợ không thay đổi.`)
  } catch (caught) {
    toasts.error(problemMessage(caught, 'Không ghi nhận được trạng thái thanh toán.'))
  }
}

async function undoPaymentMark(debt: DebtView): Promise<void> {
  try {
    await store.undoPaymentMark(debt.id)
    toasts.success(`Đã hoàn tác ghi nhận thanh toán của ${debt.creditor}.`)
  } catch (caught) {
    toasts.error(problemMessage(caught, 'Không hoàn tác được trạng thái thanh toán.'))
  }
}

function closeSchedule(): void {
  scheduleDebt.value = null
  store.clearSchedule()
}

async function onRemove(): Promise<void> {
  const debt = pendingRemoval.value
  if (!debt) return
  removing.value = true
  try {
    await store.removeDebt(debt.id)
    pendingRemoval.value = null
    toasts.success(`Đã xóa khoản nợ với ${debt.creditor}.`)
  } catch (caught) {
    toasts.error(problemMessage(caught, 'Không xóa được khoản nợ.'))
  } finally {
    removing.value = false
  }
}
</script>

<style scoped>
/* Page rhythm only; cards, buttons and fields come from the global tokens. */
.page { display: grid; gap: 18px; max-width: 1120px; }
.head h1 { margin: 0; }
.head .lead { max-width: 640px; }
.placeholder { padding: 28px; text-align: center; color: var(--fg-muted); }

/*
 * Journey layout: sticky summary + scrolling list. Summary reads as a
 * "debt-free ETA" card, not a spreadsheet.
 */
.layout { display: grid; grid-template-columns: minmax(0, 380px) minmax(0, 1fr); gap: 20px; align-items: start; }
.layout__side { display: grid; gap: 14px; position: sticky; top: 76px; }
.layout__main { display: grid; gap: 14px; min-width: 0; }

.list-title { margin: 6px 0 0; font-size: 16px; }

/* The global .btn is full-width inside forms; this CTA sizes to its own label. */
.add-debt { width: auto; justify-self: start; padding: 10px 20px; border-radius: 999px; box-shadow: 0 6px 16px rgba(2, 132, 199, 0.28); }

@media (max-width: 960px) {
  .layout { grid-template-columns: minmax(0, 1fr); }
  .layout__side { position: static; }
  .add-debt { width: 100%; }
}
@media (max-width: 640px) {
  .page { padding: 20px 14px; }
  .head h1 { font-size: 20px; }
}
</style>

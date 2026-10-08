<template>
  <div class="debt-backdrop" @click.self="onCancel">
    <div class="debt-dialog" role="dialog" aria-modal="true" :aria-labelledby="titleId">
      <h2 :id="titleId" class="debt-dialog__title">{{ isEdit ? 'Sửa khoản nợ' : 'Thêm khoản nợ' }}</h2>
      <form class="debt-form" @submit.prevent="onSubmit">
        <div class="grid">
          <label class="field">Chủ nợ
            <input ref="firstField" v-model="form.creditor" class="input" data-testid="creditor" required />
          </label>
          <label class="field">Loại nợ
            <select v-model="form.debtType" class="input" data-testid="debt-type">
              <option value="CREDIT_CARD">Thẻ tín dụng</option>
              <option value="MORTGAGE">Vay mua nhà</option>
              <option value="AUTO_LOAN">Vay mua xe</option>
              <option value="STUDENT_LOAN">Vay học tập</option>
              <option value="PERSONAL_LOAN">Vay cá nhân</option>
              <option value="OTHER">Khác</option>
            </select>
          </label>
        </div>
        <div class="grid">
          <MoneyInput :id="ids.balance" v-model="form.outstandingBalance" currency="VND" label="Dư nợ hiện tại" />
          <MoneyInput :id="ids.min" v-model="form.minimumPayment" currency="VND" label="Trả tối thiểu / tháng" />
        </div>
        <div class="grid">
          <MoneyInput :id="ids.planned" v-model="form.plannedPayment" currency="VND" label="Dự định trả / tháng" />
          <label class="field">Lãi suất năm (vd 0.180000, để trống nếu chưa biết)
            <input v-model="form.annualInterestRate" class="input" data-testid="rate" placeholder="0.180000" />
          </label>
        </div>
        <div class="grid">
          <label class="field">Ngày đến hạn trả trong tháng (1–31, để trống nếu chưa biết)
            <input
              v-model="form.dueDay"
              class="input"
              data-testid="due-day"
              type="number"
              min="1"
              max="31"
              step="1"
              placeholder="Vd: 15"
            />
          </label>
        </div>
        <div v-if="validationError" class="error-box" role="alert">{{ validationError }}</div>
        <div v-if="error" class="error-box" role="alert">{{ error }}</div>
        <div class="actions">
          <button type="submit" class="btn btn--primary" :disabled="busy || Boolean(validationError)">Lưu</button>
          <button type="button" class="btn" @click="onCancel">Hủy</button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import MoneyInput from '../MoneyInput.vue'
import { isDueDayValid, isPlannedValid, isRateValid, type DebtPayload, type DebtView } from '../../api/debts'

const props = defineProps<{ line?: DebtView | null; error?: string; busy?: boolean }>()
const emit = defineEmits<{ (e: 'submit', payload: DebtPayload): void; (e: 'cancel'): void }>()

const form = reactive({
  creditor: props.line?.creditor ?? '',
  debtType: props.line?.debtType ?? 'CREDIT_CARD',
  outstandingBalance: props.line?.outstandingBalance ?? '',
  annualInterestRate: props.line?.annualInterestRate ?? '',
  minimumPayment: props.line?.minimumPayment ?? '',
  plannedPayment: props.line?.plannedPayment ?? '',
  // Empty until the user picks a day: a new debt must not silently inherit day 15.
  dueDay: props.line?.dueDay != null ? String(props.line.dueDay) : '',
})

const ids = { balance: 'debt-balance', min: 'debt-min', planned: 'debt-planned' }

const titleId = 'debt-form-title'
const firstField = ref<HTMLInputElement | null>(null)
const isEdit = Boolean(props.line)

function onCancel(): void {
  emit('cancel')
}

/**
 * Escape and a backdrop click both cancel, mirroring ConfirmDialog: the dialog is a scratch pad,
 * never a place where a stray keypress can lose data by confirming.
 */
function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') onCancel()
}

onMounted(() => {
  firstField.value?.focus()
  document.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})

const validationError = computed(() => {
  if (!form.creditor.trim()) return 'Chủ nợ là bắt buộc.'
  if (!form.outstandingBalance || !form.minimumPayment || !form.plannedPayment) {
    return 'Số tiền là bắt buộc.'
  }
  if (!isPlannedValid(form.minimumPayment, form.plannedPayment)) {
    return 'Khoản dự định trả không được nhỏ hơn khoản tối thiểu.'
  }
  if (!isRateValid(form.annualInterestRate)) {
    return 'Lãi suất phải là phân số thập phân (ví dụ 0.180000).'
  }
  if (!isDueDayValid(form.dueDay)) {
    return 'Ngày đến hạn trả phải là số nguyên từ 1 đến 31.'
  }
  return ''
})

function onSubmit(): void {
  if (props.busy || validationError.value) return
  emit('submit', {
    creditor: form.creditor.trim(),
    debtType: form.debtType as DebtPayload['debtType'],
    // The form does not collect the origination amount: keep it unknown (null) rather than
    // inventing a value such as the current balance or 0.00 (spec §4.1).
    originalPrincipal: props.line?.originalPrincipal ?? null,
    outstandingBalance: form.outstandingBalance,
    annualInterestRate: form.annualInterestRate === '' ? null : form.annualInterestRate,
    minimumPayment: form.minimumPayment,
    plannedPayment: form.plannedPayment,
    // String() covers both states: '' before typing, and a number after v-model on type="number".
    dueDay: String(form.dueDay).trim() === '' ? null : Number(form.dueDay),
  })
}
</script>

<style scoped>
/* Popup shell: same backdrop language as ConfirmDialog (fixed veil, centred card, Escape cancels). */
.debt-backdrop {
  position: fixed; inset: 0; z-index: 60;
  display: flex; justify-content: center;
  padding: 20px; overflow-y: auto;
  background: rgba(17, 24, 39, 0.45);
}
/* margin: auto centres the panel and collapses to 0 on overflow, so tall forms scroll instead of clipping. */
.debt-dialog {
  width: min(720px, 100%);
  height: fit-content; margin: auto;
  padding: 22px 24px 20px;
  background: var(--fg-surface);
  border-radius: var(--fg-radius-card);
  box-shadow: var(--fg-shadow-float);
}
.debt-dialog__title { margin: 0 0 14px; font-size: 17px; }
.debt-form { display: grid; gap: 12px; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: grid; gap: 6px; font-size: 13px; }
.actions { display: flex; gap: 8px; }
.error-box { color: var(--fg-danger); }

@media (max-width: 560px) {
  .debt-backdrop { padding: 12px; }
  .debt-dialog { padding: 18px 14px; }
  .grid { grid-template-columns: 1fr; }
}
</style>

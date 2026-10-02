<template>
  <form class="debt-form" @submit.prevent="onSubmit">
    <div class="grid">
      <label class="field">Chủ nợ
        <input v-model="form.creditor" class="input" data-testid="creditor" required />
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
    <div v-if="validationError" class="error-box" role="alert">{{ validationError }}</div>
    <div v-if="error" class="error-box" role="alert">{{ error }}</div>
    <div class="actions">
      <button type="submit" class="btn btn--primary" :disabled="Boolean(validationError)">Lưu</button>
      <button type="button" class="btn" @click="$emit('cancel')">Hủy</button>
    </div>
  </form>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'
import MoneyInput from '../MoneyInput.vue'
import { isPlannedValid, isRateValid, type DebtPayload, type DebtView } from '../../api/debts'

const props = defineProps<{ line?: DebtView | null; error?: string }>()
const emit = defineEmits<{ (e: 'submit', payload: DebtPayload): void; (e: 'cancel'): void }>()

const form = reactive({
  creditor: props.line?.creditor ?? '',
  debtType: props.line?.debtType ?? 'CREDIT_CARD',
  outstandingBalance: props.line?.outstandingBalance ?? '',
  annualInterestRate: props.line?.annualInterestRate ?? '',
  minimumPayment: props.line?.minimumPayment ?? '',
  plannedPayment: props.line?.plannedPayment ?? '',
})

const ids = { balance: 'debt-balance', min: 'debt-min', planned: 'debt-planned' }

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
  return ''
})

function onSubmit(): void {
  if (validationError.value) return
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
    dueDay: props.line?.dueDay ?? 15,
  })
}
</script>

<style scoped>
.debt-form { display: grid; gap: 12px; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field { display: grid; gap: 6px; font-size: 13px; }
.actions { display: flex; gap: 8px; }
.error-box { color: var(--fg-danger); }
</style>

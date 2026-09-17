<template>
  <div class="page">
    <h1 class="page-title">Financial profile</h1>
    <p class="hint">Facts are actual; every total below is calculated by the server.</p>
    <div v-if="store.error" class="error-box">{{ store.error }}</div>

    <PositionSummary :view="store.profile" />

    <section class="card">
      <h2>Profile facts</h2>
      <MoneyInput
        id="savings"
        v-model="savings"
        label="Liquid savings"
        hint="Decimal amount, e.g. 100.00."
      />
      <MoneyInput
        id="emergency"
        v-model="emergency"
        label="Emergency fund"
        hint="Decimal amount, e.g. 50.00."
      />
      <label for="dependents">Financial dependents</label>
      <input id="dependents" v-model.number="dependents" type="number" min="0" class="input" />
      <div v-if="formError" class="error-box">{{ formError }}</div>
      <button type="button" class="btn" :disabled="saving" @click="onSave">Save profile</button>
    </section>
    <section class="card">
      <h2>Current position <small>(calculated by the server)</small></h2>
      <p class="hint">Rendered by PositionSummary from the server response.</p>
    </section>
    <section class="card">
      <h2>Income lines</h2>
      <MoneyInput id="income-amount" v-model="incomeAmount" label="Amount" hint="e.g. 74.00" />
      <label for="income-source">Source</label>
      <input id="income-source" v-model="incomeSource" class="input" placeholder="salary" />
      <button type="button" class="btn-ghost" @click="onSubmitIncome">
        {{ editingIncomeId ? 'Update income' : 'Add income' }}
      </button>
      <button v-if="editingIncomeId" type="button" class="btn-ghost small" @click="cancelIncomeEdit">
        Cancel
      </button>
      <ul>
        <li v-for="line in store.profile?.incomes ?? []" :key="line.id">
          {{ line.source }} —
          <MoneyDisplay
            :amount="line.amount"
            :currency="line.currency"
            :provenance="line.provenance"
          />
          <button type="button" class="btn-ghost small" @click="startIncomeEdit(line)">Edit</button>
          <button type="button" class="btn-ghost small" @click="store.removeIncome(line.id)">
            Remove
          </button>
        </li>
      </ul>
    </section>
    <section class="card">
      <h2>Expense lines</h2>
      <MoneyInput id="expense-amount" v-model="expenseAmount" label="Amount" hint="e.g. 30.00" />
      <label for="expense-category">Category</label>
      <input id="expense-category" v-model="expenseCategory" class="input" placeholder="rent" />
      <label for="expense-type">Type</label>
      <select id="expense-type" v-model="expenseType" class="input">
        <option value="FIXED">FIXED</option>
        <option value="VARIABLE">VARIABLE</option>
      </select>
      <button type="button" class="btn-ghost" @click="onSubmitExpense">
        {{ editingExpenseId ? 'Update expense' : 'Add expense' }}
      </button>
      <button
        v-if="editingExpenseId"
        type="button"
        class="btn-ghost small"
        @click="cancelExpenseEdit"
      >
        Cancel
      </button>
      <ul>
        <li v-for="line in store.profile?.expenses ?? []" :key="line.id">
          {{ line.category }} ({{ line.expenseType }}) —
          <MoneyDisplay
            :amount="line.amount"
            :currency="line.currency"
            :provenance="line.provenance"
          />
          <button type="button" class="btn-ghost small" @click="startExpenseEdit(line)">Edit</button>
          <button type="button" class="btn-ghost small" @click="store.removeExpense(line.id)">
            Remove
          </button>
        </li>
      </ul>
    </section>
  </div>
</template>


<script setup lang="ts">
import { onMounted, ref } from 'vue'
import MoneyDisplay from '../components/MoneyDisplay.vue'
import MoneyInput from '../components/MoneyInput.vue'
import { isDecimalAmount } from '../api/profile'
import { useProfileStore } from '../stores/profileStore'

const store = useProfileStore()
const savings = ref('0.00')
const emergency = ref('0.00')
const dependents = ref(0)
const incomeAmount = ref('')
const incomeSource = ref('')
const expenseAmount = ref('')
const expenseCategory = ref('')
const expenseType = ref<'FIXED' | 'VARIABLE'>('FIXED')
const formError = ref('')
const saving = ref(false)

onMounted(async () => {
  await store.refresh()
  savings.value = store.profile?.savingsAmount ?? '0.00'
  emergency.value = store.profile?.emergencyFundAmount ?? '0.00'
  dependents.value = store.profile?.dependentsCount ?? 0
})

async function onSave(): Promise<void> {
  formError.value = ''
  if (!isDecimalAmount(savings.value) || !isDecimalAmount(emergency.value)) {
    formError.value = 'Savings and emergency fund must be decimal amounts like 10.00.'
    return
  }
  saving.value = true
  try {
    await store.saveProfile({
      currency: 'VND',
      savingsAmount: savings.value.trim(),
      emergencyFundAmount: emergency.value.trim(),
      dependentsCount: dependents.value,
    })
    await store.refresh()
  } catch {
    formError.value = 'Could not save the profile.'
  } finally {
    saving.value = false
  }
}

async function onAddIncome(): Promise<void> {
  formError.value = ''
  if (!isDecimalAmount(incomeAmount.value) || !incomeSource.value.trim()) {
    formError.value = 'Income needs a decimal amount and a source.'
    return
  }
  await store.addIncome({ amount: incomeAmount.value.trim(), source: incomeSource.value.trim() })
  incomeAmount.value = ''
  incomeSource.value = ''
}

async function onAddExpense(): Promise<void> {
  formError.value = ''
  if (!isDecimalAmount(expenseAmount.value) || !expenseCategory.value.trim()) {
    formError.value = 'Expense needs a decimal amount and a category.'
    return
  }
  await store.addExpense({
    amount: expenseAmount.value.trim(),
    category: expenseCategory.value.trim(),
    expenseType: expenseType.value,
  })
  expenseAmount.value = ''
  expenseCategory.value = ''
}
</script>

<style scoped>
.page { max-width: 880px; margin: 0 auto; padding: 28px 20px; display: flex; flex-direction: column; gap: 16px; }
.card { padding: 20px; }
.totals { display: grid; gap: 10px; }
.totals > div { display: flex; justify-content: space-between; align-items: baseline; }
</style>

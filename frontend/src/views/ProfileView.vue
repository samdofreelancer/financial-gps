<template>
  <div class="page">
    <header class="head">
      <h1 class="page-title">Financial GPS</h1>
      <p class="lead">Where you are financially — and what to look at next.</p>
    </header>

    <div v-if="store.error" class="error-box" role="alert">{{ store.error }}</div>

    <!-- 1. Where am I? The position card is the visual focus of the page. -->
    <FinancialPositionCard
      :view="store.profile"
      :loading="store.loading"
      @add-income="openIncomeForm"
    />

    <!-- 2. The financial facts behind that position (read-only until the user edits). -->
    <FinancialBasicsCard
      ref="basicsCard"
      :view="store.profile"
      :saving="saving"
      :error="formError"
      @save="onSave"
    />
    <div v-if="formError" class="error-box" role="alert">{{ formError }}</div>

    <!-- 3. How much comes in, 4. how much goes out. -->
    <IncomeList
      :incomes="incomes"
      :currency="currency"
      :error="incomeError"
      :form-open="incomeFormOpen"
      @add="openIncomeForm"
      @edit="startIncomeEdit"
      @remove="onRemoveIncome"
    >
      <template #form>
        <IncomeForm
          v-if="incomeFormOpen"
          :line="editingIncome"
          :error="incomeError"
          :currency="currency"
          @submit="onSubmitIncome"
          @cancel="cancelIncomeEdit"
        />
      </template>
    </IncomeList>

    <ExpenseList
      :expenses="expenses"
      :currency="currency"
      :error="expenseError"
      :form-open="expenseFormOpen"
      @add="openExpenseForm"
      @edit="startExpenseEdit"
      @remove="onRemoveExpense"
    >
      <template #form>
        <ExpenseForm
          v-if="expenseFormOpen"
          :line="editingExpense"
          :error="expenseError"
          :currency="currency"
          @submit="onSubmitExpense"
          @cancel="cancelExpenseEdit"
        />
      </template>
    </ExpenseList>

    <!-- 5. What to look at next. -->
    <NextStepCard :complete="profileComplete" @complete-profile="openIncomeForm" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import ExpenseForm from '../components/ExpenseForm.vue'
import ExpenseList from '../components/ExpenseList.vue'
import FinancialBasicsCard from '../components/FinancialBasicsCard.vue'
import FinancialPositionCard from '../components/FinancialPositionCard.vue'
import IncomeForm from '../components/IncomeForm.vue'
import IncomeList from '../components/IncomeList.vue'
import NextStepCard from '../components/NextStepCard.vue'
import { isDecimalAmount, type ProfileLine } from '../api/profile'
import { problemMessage } from '../api/http'
import { useProfileStore } from '../stores/profileStore'

/**
 * The Financial GPS page is an orchestrator: it owns the store, the API calls and the error
 * vocabulary, while every section is a presentational component (position → basics → in → out →
 * next step). No totals are computed here — the server response is rendered as-is.
 */
const store = useProfileStore()

const formError = ref('')
const incomeError = ref('')
const expenseError = ref('')
const saving = ref(false)
const incomeFormOpen = ref(false)
const expenseFormOpen = ref(false)
const editingIncome = ref<ProfileLine | null>(null)
const editingExpense = ref<ProfileLine | null>(null)
const basicsCard = ref<InstanceType<typeof FinancialBasicsCard> | null>(null)

const currency = computed(() => store.profile?.currency ?? 'VND')
const incomes = computed(() => store.profile?.incomes ?? [])
const expenses = computed(() => store.profile?.expenses ?? [])
const profileComplete = computed(
  () => !!store.profile && (incomes.value.length > 0 || expenses.value.length > 0),
)

onMounted(() => {
  void store.refresh()
})

function openIncomeForm(): void {
  incomeError.value = ''
  editingIncome.value = null
  incomeFormOpen.value = true
}

function openExpenseForm(): void {
  expenseError.value = ''
  editingExpense.value = null
  expenseFormOpen.value = true
}

/** Server 404 when the account has no profile record yet (created by saving basics). */
function isMissingProfileRecord(caught: unknown): boolean {
  return (
    (caught as { response?: { data?: { code?: string } } } | null)?.response?.data?.code ===
    'RESOURCE_NOT_FOUND'
  )
}

const missingProfileHint = 'Save your financial basics first — then this line can be recorded.'

async function onSave(body: {
  currency: string
  savingsAmount: string
  emergencyFundAmount: string
  dependentsCount: number
}): Promise<void> {
  formError.value = ''
  if (!isDecimalAmount(body.savingsAmount) || !isDecimalAmount(body.emergencyFundAmount)) {
    formError.value = 'Savings and emergency fund must be decimal amounts like 10.00.'
    return
  }
  saving.value = true
  try {
    await store.saveProfile(body)
    await store.refresh()
    // The "save your basics first" block is resolved: drop the stale hint from the line sections.
    incomeError.value = ''
    expenseError.value = ''
  } catch (caught) {
    formError.value = problemMessage(caught, 'Could not save the profile.')
  } finally {
    saving.value = false
  }
}

async function onSubmitIncome(body: { amount: string; source: string }): Promise<void> {
  incomeError.value = ''
  if (!isDecimalAmount(body.amount) || !body.source) {
    incomeError.value = 'Income needs a decimal amount and a source.'
    return
  }
  try {
    if (editingIncome.value) {
      await store.updateIncome(editingIncome.value.id, body)
    } else {
      await store.addIncome(body)
    }
  } catch (caught) {
    incomeError.value = isMissingProfileRecord(caught)
      ? missingProfileHint
      : problemMessage(caught, 'Could not save the income line.')
    if (isMissingProfileRecord(caught)) basicsCard.value?.startEdit()
    return
  }
  cancelIncomeEdit()
}

async function onRemoveIncome(id: string): Promise<void> {
  incomeError.value = ''
  try {
    await store.removeIncome(id)
  } catch (caught) {
    incomeError.value = problemMessage(caught, 'Could not remove the income line.')
  }
}

function startIncomeEdit(line: ProfileLine): void {
  incomeError.value = ''
  editingIncome.value = line
  incomeFormOpen.value = true
}

function cancelIncomeEdit(): void {
  incomeFormOpen.value = false
  editingIncome.value = null
  incomeError.value = ''
}

async function onSubmitExpense(body: {
  amount: string
  category: string
  expenseType: 'FIXED' | 'VARIABLE'
}): Promise<void> {
  expenseError.value = ''
  if (!isDecimalAmount(body.amount) || !body.category) {
    expenseError.value = 'Expense needs a decimal amount and a category.'
    return
  }
  try {
    if (editingExpense.value) {
      await store.updateExpense(editingExpense.value.id, body)
    } else {
      await store.addExpense(body)
    }
  } catch (caught) {
    expenseError.value = isMissingProfileRecord(caught)
      ? missingProfileHint
      : problemMessage(caught, 'Could not save the expense line.')
    if (isMissingProfileRecord(caught)) basicsCard.value?.startEdit()
    return
  }
  cancelExpenseEdit()
}

async function onRemoveExpense(id: string): Promise<void> {
  expenseError.value = ''
  try {
    await store.removeExpense(id)
  } catch (caught) {
    expenseError.value = problemMessage(caught, 'Could not remove the expense line.')
  }
}

function startExpenseEdit(line: ProfileLine): void {
  expenseError.value = ''
  editingExpense.value = line
  expenseFormOpen.value = true
}

function cancelExpenseEdit(): void {
  expenseFormOpen.value = false
  editingExpense.value = null
  expenseError.value = ''
}
</script>

<style scoped>
/* Page rhythm only: cards, fields and buttons come from the global tokens. */
.page {
  max-width: 880px;
  margin: 0 auto;
  padding: 28px 20px 48px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}
.head h1 { margin: 0; font-size: 26px; }
.head .lead { margin: 6px 0 0; font-size: 15px; }

@media (max-width: 640px) {
  .page { padding: 20px 14px 40px; gap: 14px; }
  .head h1 { font-size: 22px; }
}
</style>


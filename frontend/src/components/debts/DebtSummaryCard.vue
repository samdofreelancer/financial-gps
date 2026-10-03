<template>
  <section class="card summary" aria-label="Tổng quan nợ">
    <header class="summary__head">
      <h2 class="summary__title">Tổng quan</h2>
      <!-- Shoulder-surfing guard: balances stay visible by default but can be masked in public. -->
      <button
        type="button"
        class="summary__toggle"
        :aria-pressed="hidden"
        :aria-label="hidden ? 'Hiện số dư' : 'Ẩn số dư'"
        :title="hidden ? 'Hiện số dư' : 'Ẩn số dư'"
        data-testid="toggle-amounts"
        @click="emit('toggle-amounts')"
      >
        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true" focusable="false">
          <path
            d="M1.8 10S4.9 4.8 10 4.8 18.2 10 18.2 10 15.1 15.2 10 15.2 1.8 10 1.8 10Z"
            stroke="currentColor"
            stroke-width="1.4"
            stroke-linejoin="round"
          />
          <circle cx="10" cy="10" r="2.4" stroke="currentColor" stroke-width="1.4" />
          <path v-if="hidden" d="M3.4 3.4l13.2 13.2" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
        </svg>
      </button>
    </header>

    <!-- The single headline figure: everything else supports it. -->
    <p class="summary__hero">
      <span class="summary__hero-label">Tổng dư nợ</span>
      <span class="summary__hero-value" data-testid="total-debt">
        {{ amount(summary.totalOutstandingDebt) }}
      </span>
    </p>

    <dl class="summary__rows">
      <div class="summary__row">
        <dt>Tối thiểu / tháng</dt>
        <dd data-testid="total-minimum">{{ amount(summary.totalMinimumMonthlyPayment) }}</dd>
      </div>
      <div class="summary__row">
        <dt>Dự định trả / tháng</dt>
        <dd>{{ amount(summary.totalPlannedMonthlyPayment) }}</dd>
      </div>
      <!-- The figure that explains a BLOCKED state; "Chưa tính được" when a rate is unknown. -->
      <div class="summary__row summary__row--interest">
        <dt>Lãi tích luỹ / tháng</dt>
        <dd data-testid="total-interest">
          {{ summary.totalMonthlyAccruedInterest ? amount(summary.totalMonthlyAccruedInterest) : 'Chưa tính được' }}
        </dd>
      </div>
    </dl>

    <!-- Payment vs next-period interest: the fill crossing the marker = the payment outgrows interest. -->
    <div v-if="summary.totalMonthlyAccruedInterest" class="compare">
      <p class="compare__title">Khoản trả có bù được lãi kỳ tới?</p>
      <div class="compare__bar">
        <span class="compare__fill" :style="{ width: `${plannedWidth}%` }"></span>
        <span class="compare__mark" :style="{ left: `${interestWidth}%` }"></span>
      </div>
      <p class="compare__legend">
        <span><i class="dot dot--planned"></i>Dự định trả (gốc + lãi) {{ amount(summary.totalPlannedMonthlyPayment) }}</span>
        <span><i class="dot dot--interest"></i>Lãi kỳ tới {{ amount(summary.totalMonthlyAccruedInterest) }}</span>
      </p>
      <!-- The split of next period's payment: how much actually reduces the debt. -->
      <p
        v-if="split"
        class="compare__split"
        :class="{ 'compare__split--short': !split.covered }"
        data-testid="payment-split"
      >
        <template v-if="split.covered">
          Trong khoản trả: lãi {{ amount(summary.totalMonthlyAccruedInterest) }} · giảm nợ
          {{ amount(split.principal) }}
        </template>
        <template v-else>
          Chưa đủ bù lãi kỳ tới — thiếu {{ amount(split.shortfall) }}, dư nợ sẽ tăng.
        </template>
      </p>
    </div>

    <!-- DTI: a ratio only means something next to its benchmark and a verdict. -->
    <div class="dti">
      <div class="dti__head">
        <span class="dti__label">Tỷ lệ nợ / thu nhập (DTI)</span>
        <span v-if="rating" class="dti__badge" :class="`dti__badge--${rating.tone}`">
          {{ rating.label }}
        </span>
      </div>
      <p class="dti__value" data-testid="dti">{{ dtiText }}</p>
      <div v-if="rating" class="dti__meter" role="img" :aria-label="`${dtiText} — ${rating.label}`">
        <span class="dti__fill" :class="`dti__fill--${rating.tone}`" :style="{ width: `${rating.fill}%` }"></span>
        <span class="dti__marker" :style="{ left: '50%' }"></span>
      </div>
      <p v-if="rating" class="dti__hint">{{ rating.hint }}</p>
    </div>

    <div class="summary__row summary__row--payoff">
      <dt>Dự kiến hết nợ</dt>
      <dd>
        <span data-testid="payoff-date">{{ payoffText }}</span>
        <!-- "Tại sao?" — the payoff date is conditional, so the condition is one click away. -->
        <button
          type="button"
          class="summary__why"
          aria-label="Tại sao chưa dự báo được ngày hết nợ?"
          data-testid="payoff-why"
          @click="whyOpen = !whyOpen"
        >
          Tại sao?
        </button>
      </dd>
    </div>
    <p v-if="whyOpen" class="summary__why-text">{{ PAYOFF_EXPLANATION }}</p>

    <p class="summary__as-of">Cập nhật {{ summary.asOf }}</p>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { formatMoney } from '../../api/profile'
import { dtiRating, PAYOFF_EXPLANATION } from '../debtText'
import type { DebtSummary } from '../../api/debts'

/**
 * The overview: what is owed, what it costs each month, and when it ends.
 *
 * Every figure is the server's string, formatted for reading only. The `Lãi tích luỹ / tháng`
 * row exists because without it a BLOCKED state looks arbitrary — the reader can see that the
 * planned payment is smaller than the interest it has to outgrow.
 */
const props = withDefaults(defineProps<{ summary: DebtSummary; hidden?: boolean }>(), { hidden: false })
const emit = defineEmits<{ (e: 'toggle-amounts'): void }>()

const whyOpen = ref(false)

/** Masking replaces the digits, not the layout, so columns never jump while toggling. */
function amount(value: string): string {
  if (props.hidden) return '••••••••'
  return formatMoney(value, props.summary.currency, { compact: true })
}

const dtiText = computed(() => {
  const ratio = props.summary.debtToIncome.ratio
  if (props.summary.debtToIncome.status !== 'AVAILABLE' || !ratio) {
    return 'Chưa tính được (thiếu thu nhập)'
  }
  return `${(Number(ratio) * 100).toFixed(2)}%`
})

const rating = computed(() => dtiRating(props.summary.debtToIncome.ratio))

const payoffText = computed(
  () => props.summary.portfolioProjection.projectedDebtFreeDate ?? 'Chưa dự báo được',
)

/**
 * Bar geometry only: the planned payment drawn against the interest threshold (the marker).
 * Both figures share one scale = 110% of the larger figure, so when the payment is short the
 * fill stops before the marker — the visual form of "BLOCKED".
 */
const barScale = computed(() => {
  const interest = Number(props.summary.totalMonthlyAccruedInterest)
  const planned = Number(props.summary.totalPlannedMonthlyPayment)
  if (!Number.isFinite(interest) || interest <= 0) return null
  return Math.max(planned, interest) * 1.1
})

const plannedWidth = computed(() => {
  const scale = barScale.value
  if (scale === null) return 100
  const planned = Number(props.summary.totalPlannedMonthlyPayment)
  return Math.min(100, Math.max(2, (planned / scale) * 100))
})

/** The marker sits at the interest figure itself, never at the end of the fill. */
const interestWidth = computed(() => {
  const scale = barScale.value
  if (scale === null) return 100
  const interest = Number(props.summary.totalMonthlyAccruedInterest)
  return Math.min(100, Math.max(2, (interest / scale) * 100))
})

/**
 * Next period's payment split in whole VND cents (integer math — never float drift):
 * principal = planned − interest; a shortfall means the balance grows instead of shrinking.
 */
const split = computed(() => {
  const planned = Number(props.summary.totalPlannedMonthlyPayment)
  const interest = Number(props.summary.totalMonthlyAccruedInterest)
  if (!Number.isFinite(planned) || !Number.isFinite(interest)) return null
  const principalCents = Math.round(planned * 100) - Math.round(interest * 100)
  return {
    covered: principalCents >= 0,
    principal: (principalCents / 100).toFixed(2),
    shortfall: (-principalCents / 100).toFixed(2),
  }
})
</script>

<style scoped>
.card { padding: 20px 22px; }
.summary__head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.summary__title { margin: 0; font-size: 16px; }
.summary__toggle {
  display: inline-flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; padding: 0;
  color: var(--fg-muted); background: none;
  border: 1px solid var(--fg-border-soft); border-radius: var(--fg-radius-control);
  cursor: pointer;
}
.summary__toggle:hover { color: var(--fg-primary); border-color: var(--fg-primary); }
.summary__toggle:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); }
.summary__toggle svg { width: 18px; height: 18px; }

/* The headline figure: the number the whole screen exists to reduce. */
.summary__hero { margin: 14px 0 0; display: flex; flex-direction: column; gap: 2px; }
.summary__hero-label { font-size: 13px; color: var(--fg-muted); }
.summary__hero-value {
  font-size: 30px; font-weight: 700; letter-spacing: -0.02em;
  color: var(--fg-ink); font-variant-numeric: tabular-nums; overflow-wrap: anywhere;
}

.summary__rows { margin: 16px 0 0; padding-top: 14px; border-top: 1px solid var(--fg-border-soft); display: grid; gap: 10px; }
.summary__row { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.summary__row dt { color: var(--fg-muted); }
.summary__row dd {
  margin: 0; font-weight: 600; color: var(--fg-ink);
  font-variant-numeric: tabular-nums; text-align: right; overflow-wrap: anywhere;
}
.summary__row--interest dd { color: var(--fg-danger); }
.summary__row--payoff { margin-top: 16px; padding-top: 14px; border-top: 1px solid var(--fg-border-soft); }
.summary__row--payoff dd { display: flex; align-items: baseline; gap: 8px; }
.summary__why {
  padding: 0; font-family: inherit; font-size: 12px; font-weight: 600;
  color: var(--fg-primary); background: none; border: 0; cursor: pointer;
}
.summary__why:hover { text-decoration: underline; }
.summary__why:focus-visible { outline: none; box-shadow: var(--fg-focus-ring); border-radius: 4px; }
.summary__why-text { margin: 8px 0 0; font-size: 13px; color: var(--fg-muted); }

/* Planned payment vs accrued interest — the gap is the BLOCKED story. */
.compare { margin-top: 16px; }
.compare__title { margin: 0 0 8px; font-size: 13px; color: var(--fg-muted); }
.compare__bar {
  position: relative; height: 10px; border-radius: 999px;
  background: var(--fg-danger-bg); border: 1px solid var(--fg-danger-border); overflow: hidden;
}
.compare__fill { position: absolute; inset: 0 auto 0 0; background: var(--fg-primary); border-radius: 999px; }
.compare__mark { position: absolute; top: -3px; bottom: -3px; width: 2px; background: var(--fg-danger); }
.compare__legend { margin: 8px 0 0; display: grid; gap: 4px; font-size: 12px; color: var(--fg-muted); }
.compare__legend span { display: flex; align-items: center; gap: 6px; }
.compare__split { margin: 8px 0 0; font-size: 12px; color: var(--fg-muted); }
.compare__split--short { color: var(--fg-danger); font-weight: 600; }
.dot { width: 8px; height: 8px; border-radius: 50%; flex: 0 0 8px; }
.dot--planned { background: var(--fg-primary); }
.dot--interest { background: var(--fg-danger); }

.dti { margin-top: 16px; padding-top: 14px; border-top: 1px solid var(--fg-border-soft); }
.dti__head { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.dti__label { font-size: 13px; color: var(--fg-muted); }
.dti__badge { padding: 2px 8px; border-radius: 999px; font-size: 12px; font-weight: 600; }
.dti__badge--good { color: var(--fg-success); background: rgba(22, 163, 74, 0.1); }
.dti__badge--warn { color: #b45309; background: rgba(180, 83, 9, 0.1); }
.dti__badge--bad { color: var(--fg-danger); background: var(--fg-danger-bg); }
.dti__value { margin: 6px 0 0; font-size: 22px; font-weight: 700; color: var(--fg-ink); font-variant-numeric: tabular-nums; }
.dti__meter {
  position: relative; height: 8px; margin-top: 8px;
  background: var(--fg-app-bg); border-radius: 999px; overflow: hidden;
}
.dti__fill { position: absolute; inset: 0 auto 0 0; border-radius: 999px; }
.dti__fill--good { background: var(--fg-success); }
.dti__fill--warn { background: #e0a13a; }
.dti__fill--bad { background: var(--fg-danger); }
/* The marker is the 36% benchmark, drawn at half the 2× scale. */
.dti__marker { position: absolute; top: -2px; bottom: -2px; width: 2px; background: var(--fg-muted); }
.dti__hint { margin: 8px 0 0; font-size: 12px; color: var(--fg-muted); }

.summary__as-of { margin: 14px 0 0; font-size: 12px; color: var(--fg-muted-soft); }

@media (max-width: 640px) {
  .card { padding: 18px 16px; }
  .summary__hero-value { font-size: 26px; }
}
</style>

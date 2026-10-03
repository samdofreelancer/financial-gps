<template>
  <section v-if="blocked.length" class="blocker" role="alert" data-testid="blocker-alert">
    <header class="blocker__head">
      <svg class="blocker__icon" viewBox="0 0 20 20" fill="none" aria-hidden="true" focusable="false">
        <path d="M10 2.6 18.4 17H1.6L10 2.6Z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round" />
        <path d="M10 7.6v4M10 14v.6" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
      </svg>
      <div>
        <h2 class="blocker__title">Chưa dự báo được ngày hết nợ</h2>
        <p class="blocker__lead">
          Có {{ blocked.length }} khoản nợ đang không thể tính được ngày hết nợ. Dưới đây là cách
          khắc phục cho từng khoản.
        </p>
      </div>
    </header>

    <ul class="blocker__list">
      <li v-for="item in blocked" :key="item.creditor + item.reasonCode" class="blocker__item">
        <div class="blocker__row">
          <span class="blocker__creditor">{{ item.creditor }}</span>
          <span class="blocker__badge" :title="item.reasonCode ?? undefined">
            {{ blockerBadge(item.reasonCode) }}
          </span>
        </div>

        <!-- Vietnamese first: the reader never has to decode an enum to know what to do. -->
        <p class="blocker__why">{{ blockerMessage(item.reasonCode) }}</p>

        <!-- The one number that turns a diagnosis into an action. -->
        <p v-if="item.target" class="blocker__target">
          Cần nâng khoản trả lên trên <strong>{{ item.target }}</strong> mỗi tháng để khoản nợ này
          bắt đầu giảm.
        </p>

        <button
          v-if="item.fixable"
          type="button"
          class="btn-ghost small blocker__action"
          :data-testid="`fix-${item.creditor}`"
          @click="emit('fix', item.creditor)"
        >
          Sửa khoản trả của {{ item.creditor }}
        </button>
      </li>
    </ul>

    <!-- Raw codes stay available for support/debugging, out of the way by default. -->
    <details class="blocker__details">
      <summary>Chi tiết kỹ thuật</summary>
      <ul>
        <li v-for="b in blocked" :key="`detail-${b.creditor}-${b.reasonCode}`">
          <strong>{{ b.creditor }}</strong> — {{ b.reasonCode }}
          <span v-if="b.explanation" class="blocker__raw"> · {{ b.explanation }}</span>
        </li>
        <li v-if="reasonCode">Mã danh mục: {{ reasonCode }}</li>
      </ul>
    </details>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { blockerBadge, blockerMessage, needsHigherPayment } from '../debtText'
import { formatMoney } from '../../api/profile'
import type { DebtSummary, DebtView } from '../../api/debts'

/**
 * Why the payoff date is missing and what to do about it.
 *
 * Presentation rule: the reader gets a plain Vietnamese sentence plus the exact payment that
 * unblocks the debt. Codes and the server's English text are not deleted — they move into a
 * collapsed block, because support conversations still need them.
 *
 * Every amount shown here is a server value (`monthlyInterest`), so the guidance can never
 * disagree with the projection it explains.
 */
const props = defineProps<{ summary?: DebtSummary | null; debts?: DebtView[] }>()
const emit = defineEmits<{ (e: 'fix', creditor: string): void }>()

interface BlockerItem {
  creditor: string
  /** Null only when the server sent BLOCKED without a code — rendered through the fallback. */
  reasonCode: string | null
  explanation: string | null
  /** The payment that clears the blocker, when the server told us the interest figure. */
  target: string | null
  fixable: boolean
}

const currency = computed(() => props.summary?.currency ?? 'VND')

const blocked = computed<BlockerItem[]>(() => {
  const byCreditor = new Map<string, BlockerItem>()
  for (const debt of props.debts ?? []) {
    if (debt.projection.status !== 'BLOCKED') continue
    // A DebtView carries its verdict under `projection`, not at the top level.
    byCreditor.set(
      debt.creditor,
      toItem(
        {
          creditor: debt.creditor,
          reasonCode: debt.projection.reasonCode,
          explanation: debt.projection.explanation,
        },
        debt.projection.monthlyInterest,
      ),
    )
  }
  // The portfolio list decides which debts block; the per-debt rows supply the numbers.
  const merged: BlockerItem[] = []
  for (const entry of props.summary?.portfolioProjection.blockedDebts ?? []) {
    merged.push(byCreditor.get(entry.creditor) ?? toItem(entry, null))
  }
  for (const item of byCreditor.values()) {
    if (!merged.some((m) => m.creditor === item.creditor)) merged.push(item)
  }
  return merged
})

function toItem(
  entry: { creditor: string; reasonCode: string | null; explanation: string | null },
  monthlyInterest: string | null,
): BlockerItem {
  return {
    creditor: entry.creditor,
    reasonCode: entry.reasonCode,
    explanation: entry.explanation,
    target:
      needsHigherPayment(entry.reasonCode) && monthlyInterest
        ? formatMoney(oneUnitAbove(monthlyInterest), currency.value, { compact: true })
        : null,
    fixable: entry.reasonCode === 'INTEREST_RATE_MISSING' || needsHigherPayment(entry.reasonCode),
  }
}

/**
 * The blocker clears when the payment is strictly greater than the monthly interest, so the
 * number we ask for sits one đồng above it — the smallest step a VND payer can actually make.
 * This only nudges a server-sent string upward — no amortization rule is re-implemented in the
 * browser.
 */
function oneUnitAbove(amount: string): string {
  const [whole = '0'] = amount.split('.')
  return `${BigInt(whole) + 1n}.00`
}

const reasonCode = computed(() => props.summary?.portfolioProjection.reasonCode ?? null)
</script>

<style scoped>
.blocker {
  padding: 16px 18px;
  background: var(--fg-danger-bg);
  border: 1px solid var(--fg-danger-border);
  border-left: 3px solid var(--fg-danger);
  border-radius: var(--fg-radius-control);
}
.blocker__head { display: flex; gap: 12px; align-items: flex-start; }
.blocker__icon { width: 22px; height: 22px; flex: 0 0 22px; margin-top: 2px; color: var(--fg-danger); }
.blocker__title { margin: 0; font-size: 16px; color: var(--fg-danger); }
.blocker__lead { margin: 4px 0 0; font-size: 13px; color: var(--fg-text); }

.blocker__list { list-style: none; margin: 14px 0 0; padding: 0; display: grid; gap: 10px; }
.blocker__item {
  padding: 12px 14px;
  background: var(--fg-surface);
  border: 1px solid var(--fg-border-soft);
  border-radius: var(--fg-radius-control);
}
.blocker__row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.blocker__creditor { font-weight: 700; font-size: 14px; color: var(--fg-ink); }
.blocker__badge {
  padding: 2px 8px; border-radius: 999px;
  font-size: 12px; font-weight: 600;
  color: var(--fg-danger);
  background: var(--fg-danger-bg);
  border: 1px solid var(--fg-danger-border);
}
.blocker__why { margin: 8px 0 0; font-size: 13px; color: var(--fg-text); }
.blocker__target { margin: 6px 0 0; font-size: 13px; color: var(--fg-text); }
.blocker__target strong { color: var(--fg-danger); font-variant-numeric: tabular-nums; }
.blocker__action { width: auto; min-height: 34px; margin-top: 10px; padding: 6px 14px; }

.blocker__details { margin-top: 12px; font-size: 12px; color: var(--fg-muted); }
.blocker__details summary { cursor: pointer; }
.blocker__details ul { margin: 8px 0 0; padding-left: 18px; display: grid; gap: 4px; }
.blocker__raw { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; }

@media (max-width: 640px) {
  .blocker { padding: 14px; }
  .blocker__action { width: 100%; }
}
</style>

export function formatCurrency(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value)
}

export function formatCurrencyPrecise(value: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

export function formatPercent(value: number, digits = 2): string {
  return `${value.toFixed(digits)}%`
}

export function formatDate(iso: string | null): string {
  if (!iso) return '—'
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
  }).format(new Date(iso))
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(iso))
}

export function formatAction(action: string): string {
  return action
    .toLowerCase()
    .split('_')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ')
}

export function newIdempotencyKey(): string {
  return crypto.randomUUID()
}

export function subscriptionProgress(remaining: number, total: number): number {
  if (total <= 0) return 0
  return Math.round(((total - remaining) / total) * 100)
}

/** Face value received at maturity minus amount invested. */
export function positionProfit(invested: number, redemptionValue: number): number {
  return toApiDecimal(redemptionValue - invested)
}

/** Match backend @Digits(fraction = 4) — avoid float noise from JS division. */
export function toApiDecimal(value: number, fractionDigits = 4): number {
  const factor = 10 ** fractionDigits
  return Math.round(value * factor) / factor
}

export function computeUnitPrice(faceValue: number, totalUnits: number): number {
  if (totalUnits <= 0) return 0
  return toApiDecimal(faceValue / totalUnits)
}

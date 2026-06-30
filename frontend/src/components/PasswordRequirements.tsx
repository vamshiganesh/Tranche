import { evaluatePassword } from '../lib/password'

export function PasswordRequirements({
  password,
  show = true,
}: {
  password: string
  show?: boolean
}) {
  if (!show) return null

  const rules = evaluatePassword(password)
  const allMet = rules.every((r) => r.met)

  return (
    <div
      className={`password-requirements${allMet && password.length > 0 ? ' password-requirements-valid' : ''}`}
      aria-live="polite"
    >
      <p className="password-requirements-title">Password must include:</p>
      <ul>
        {rules.map((rule) => (
          <li key={rule.id} className={rule.met ? 'met' : undefined}>
            <span className="password-requirements-icon" aria-hidden>
              {rule.met ? '✓' : '○'}
            </span>
            {rule.label}
          </li>
        ))}
      </ul>
    </div>
  )
}

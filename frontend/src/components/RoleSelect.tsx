import { useEffect, useId, useRef, useState } from 'react'
import type { Role } from '../api/types'

const OPTIONS: { value: Role; label: string; description: string }[] = [
  {
    value: 'INVESTOR',
    label: 'Investor',
    description: 'Browse live opportunities and commit funds',
  },
  {
    value: 'ISSUER',
    label: 'Issuer',
    description: 'List invoice receivables for platform review',
  },
]

export function RoleSelect({ value, onChange }: { value: Role; onChange: (role: Role) => void }) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const listId = useId()
  const selected = OPTIONS.find((o) => o.value === value) ?? OPTIONS[0]

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (!rootRef.current?.contains(e.target as Node)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', onDocClick)
    return () => document.removeEventListener('mousedown', onDocClick)
  }, [])

  return (
    <div className={`role-select${open ? ' open' : ''}`} ref={rootRef}>
      <button
        type="button"
        className="role-select-trigger"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen((v) => !v)}
      >
        <span className="role-select-value">
          <strong>{selected.label}</strong>
          <span>{selected.description}</span>
        </span>
        <span className="role-select-chevron" aria-hidden />
      </button>
      {open && (
        <ul className="role-select-menu" id={listId} role="listbox">
          {OPTIONS.map((opt) => (
            <li key={opt.value} role="option" aria-selected={opt.value === value}>
              <button
                type="button"
                className={`role-select-option${opt.value === value ? ' selected' : ''}`}
                onClick={() => {
                  onChange(opt.value)
                  setOpen(false)
                }}
              >
                <strong>{opt.label}</strong>
                <span>{opt.description}</span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

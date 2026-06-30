import { useEffect, useId, useRef, useState } from 'react'

export type CustomSelectOption<T extends string> = {
  value: T
  label: string
  description?: string
}

type CustomSelectProps<T extends string> = {
  value: T
  onChange: (value: T) => void
  options: readonly CustomSelectOption<T>[]
  id?: string
}

export function CustomSelect<T extends string>({
  value,
  onChange,
  options,
  id,
}: CustomSelectProps<T>) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const listId = useId()
  const selected = options.find((o) => o.value === value) ?? options[0]

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
    <div className={`custom-select${open ? ' open' : ''}`} ref={rootRef}>
      <button
        type="button"
        id={id}
        className="custom-select-trigger"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen((v) => !v)}
      >
        <span className="custom-select-value">
          <strong>{selected.label}</strong>
          {selected.description ? <span>{selected.description}</span> : null}
        </span>
        <span className="custom-select-chevron" aria-hidden />
      </button>
      {open && (
        <ul className="custom-select-menu" id={listId} role="listbox">
          {options.map((opt) => (
            <li key={opt.value} role="option" aria-selected={opt.value === value}>
              <button
                type="button"
                className={`custom-select-option${opt.value === value ? ' selected' : ''}`}
                onClick={() => {
                  onChange(opt.value)
                  setOpen(false)
                }}
              >
                <strong>{opt.label}</strong>
                {opt.description ? <span>{opt.description}</span> : null}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

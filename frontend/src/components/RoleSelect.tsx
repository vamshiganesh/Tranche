import type { Role } from '../api/types'
import { CustomSelect } from './CustomSelect'

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
  return <CustomSelect value={value} onChange={onChange} options={OPTIONS} />
}

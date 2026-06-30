import type { RiskGrade } from '../api/types'
import { CustomSelect } from './CustomSelect'

const OPTIONS: { value: RiskGrade; label: string; description: string }[] = [
  { value: 'A', label: 'Grade A', description: 'Lowest credit risk' },
  { value: 'B', label: 'Grade B', description: 'Low to moderate risk' },
  { value: 'C', label: 'Grade C', description: 'Moderate risk' },
  { value: 'D', label: 'Grade D', description: 'Higher risk' },
]

export function RiskGradeSelect({
  value,
  onChange,
  id,
}: {
  value: RiskGrade
  onChange: (grade: RiskGrade) => void
  id?: string
}) {
  return <CustomSelect id={id} value={value} onChange={onChange} options={OPTIONS} />
}

import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { createOpportunity } from '../../api/opportunities'
import type { RiskGrade } from '../../api/types'
import { ApiClientError } from '../../api/client'
import { VerificationCallout } from '../../components/VerificationCallout'
import { useAuth } from '../../context/AuthContext'
import { useToast } from '../../context/ToastContext'
import { computeUnitPrice, toApiDecimal } from '../../lib/format'

const RISK_GRADES: RiskGrade[] = ['A', 'B', 'C', 'D']

export function CreateOpportunityPage() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const { showToast } = useToast()
  const [submitting, setSubmitting] = useState(false)

  const verificationStatus = user?.issuerVerificationStatus ?? 'PENDING'
  const canCreate = verificationStatus === 'APPROVED'

  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [faceValue, setFaceValue] = useState(2_125_000)
  const [discountRate, setDiscountRate] = useState(5.5)
  const [tenureDays, setTenureDays] = useState(90)
  const [totalUnits, setTotalUnits] = useState(85)
  const [riskGrade, setRiskGrade] = useState<RiskGrade>('B')

  const unitPrice = computeUnitPrice(faceValue, totalUnits)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    try {
      const roundedUnitPrice = computeUnitPrice(faceValue, totalUnits)
      const created = await createOpportunity({
        title,
        faceValue: toApiDecimal(faceValue),
        discountRate: toApiDecimal(discountRate),
        tenureDays,
        minimumLot: roundedUnitPrice,
        riskGrade,
        totalUnits,
        unitPrice: roundedUnitPrice,
        description: description || undefined,
      })
      showToast('Opportunity created as draft', 'success')
      navigate(`/issuer/${created.id}`)
    } catch (err) {
      let msg = 'Could not create opportunity'
      if (err instanceof ApiClientError) {
        msg = err.message.includes('numeric value out of bounds')
          ? 'Amounts must use at most 4 decimal places. Adjust face value or total units so they divide evenly.'
          : err.message
      }
      showToast(msg, 'error')
    } finally {
      setSubmitting(false)
    }
  }

  if (!canCreate) {
    return (
      <>
        <header className="page-header">
          <h2>New opportunity</h2>
          <p>Company verification must be approved before you can create opportunities.</p>
        </header>
        <VerificationCallout
          status={verificationStatus}
          title="Company verification"
          action={
            verificationStatus === 'REJECTED' ? (
              <Link to="/issuer/onboarding" className="btn btn-primary btn-sm">
                Update profile
              </Link>
            ) : (
              <Link to="/issuer" className="btn btn-secondary btn-sm">
                Back to dashboard
              </Link>
            )
          }
        >
          {verificationStatus === 'REJECTED'
            ? 'Your company profile was rejected. Update and resubmit it for administrator review.'
            : 'Your company profile is awaiting administrator approval.'}
        </VerificationCallout>
      </>
    )
  }

  return (
    <>
      <header className="page-header">
        <h2>New opportunity</h2>
        <p>Define invoice terms. You can submit for review once the draft is complete.</p>
      </header>

      <form className="card form-card" onSubmit={handleSubmit}>
        <div className="card-body">
          <div className="form-grid">
            <div className="field field-span-2">
              <label htmlFor="title">Title</label>
              <input
                id="title"
                className="input"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Q2 receivables pool"
                required
                maxLength={500}
              />
            </div>
            <div className="field field-span-2">
              <label htmlFor="description">Description</label>
              <textarea
                id="description"
                className="input textarea"
                rows={3}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="Brief summary for investors"
              />
            </div>
            <div className="field">
              <label htmlFor="faceValue">Face value (USD)</label>
              <input
                id="faceValue"
                className="input mono"
                type="number"
                min={1}
                step={1000}
                value={faceValue}
                onChange={(e) => setFaceValue(Number(e.target.value))}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="discountRate">Discount rate (%)</label>
              <input
                id="discountRate"
                className="input mono"
                type="number"
                min={0}
                max={100}
                step={0.01}
                value={discountRate}
                onChange={(e) => setDiscountRate(Number(e.target.value))}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="tenureDays">Tenure (days)</label>
              <input
                id="tenureDays"
                className="input mono"
                type="number"
                min={1}
                value={tenureDays}
                onChange={(e) => setTenureDays(Number(e.target.value))}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="totalUnits">Total units</label>
              <input
                id="totalUnits"
                className="input mono"
                type="number"
                min={1}
                value={totalUnits}
                onChange={(e) => setTotalUnits(Number(e.target.value))}
                required
              />
            </div>
            <div className="field">
              <label htmlFor="riskGrade">Risk grade</label>
              <select
                id="riskGrade"
                className="input"
                value={riskGrade}
                onChange={(e) => setRiskGrade(e.target.value as RiskGrade)}
              >
                {RISK_GRADES.map((g) => (
                  <option key={g} value={g}>
                    Grade {g}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label>Unit price (computed)</label>
              <div className="computed-value mono">
                {unitPrice.toLocaleString('en-US', {
                  style: 'currency',
                  currency: 'USD',
                  minimumFractionDigits: 2,
                })}
              </div>
            </div>
          </div>
        </div>
        <div className="card-footer">
          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => navigate('/issuer')}
            disabled={submitting}
          >
            Cancel
          </button>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Creating…' : 'Save draft'}
          </button>
        </div>
      </form>
    </>
  )
}

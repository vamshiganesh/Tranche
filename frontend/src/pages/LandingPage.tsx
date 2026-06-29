import 'lenis/dist/lenis.css'
import { motion, type Variants } from 'framer-motion'
import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useLenis } from '../hooks/useLenis'
import '../styles/landing.css'

const ease = [0.25, 0.1, 0.25, 1] as const

const fadeUp: Variants = {
  hidden: { opacity: 0, y: 16 },
  visible: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.5, delay: i * 0.07, ease },
  }),
}

const stagger: Variants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.06, delayChildren: 0.08 } },
}

const FEATURES = [
  {
    index: '01',
    tag: 'Concurrency',
    title: 'Atomic allocation',
    body: 'Pessimistic locking on opportunities and wallets. Concurrent commitments never overbook a single unit.',
  },
  {
    index: '02',
    tag: 'Reliability',
    title: 'Idempotent commits',
    body: 'Every investor order carries a unique key. Retries return the original fill without double-reserving funds.',
  },
  {
    index: '03',
    tag: 'Compliance',
    title: 'Immutable audit trail',
    body: 'Status transitions and money movements append to an audit log with actor, correlation ID, and before/after state.',
  },
  {
    index: '04',
    tag: 'Fairness',
    title: 'Partial fills',
    body: 'When demand exceeds remaining units, the engine allocates fairly and reports full, partial, or rejected fills.',
  },
  {
    index: '05',
    tag: 'Governance',
    title: 'Lifecycle control',
    body: 'Draft through settled: admin review, publish, maturity, and settlement enforced by a domain state machine.',
  },
  {
    index: '06',
    tag: 'Events',
    title: 'Outbox delivery',
    body: 'Investment and settlement events land in a transactional outbox for reliable downstream notification delivery.',
  },
]

const LIFECYCLE = [
  'Draft',
  'In review',
  'Approved',
  'Live',
  'Subscribed',
  'Matured',
  'Settled',
]

const ROLES = [
  {
    tag: 'Issuer',
    title: 'Publish receivables',
    body: 'Create invoice opportunities, submit for review, and track subscription progress as investors commit.',
  },
  {
    tag: 'Investor',
    title: 'Commit with confidence',
    body: 'Browse live opportunities, place unit commitments from your wallet, and monitor positions through maturity.',
  },
  {
    tag: 'Admin',
    title: 'Operate the platform',
    body: 'Review submissions, publish to marketplace, drive lifecycle transitions, and inspect the full audit timeline.',
  },
]

function workspacePath(role: string) {
  if (role === 'ADMIN') return '/admin'
  if (role === 'ISSUER') return '/issuer'
  return '/marketplace'
}

function BtnArrow() {
  return (
    <span className="landing-btn-arrow" aria-hidden>
      →
    </span>
  )
}

export function LandingPage() {
  const { user, loading } = useAuth()
  const [navScrolled, setNavScrolled] = useState(false)
  useLenis()

  useEffect(() => {
    function onScroll() {
      setNavScrolled(window.scrollY > 12)
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  if (!loading && user) {
    return <Navigate to={workspacePath(user.role)} replace />
  }

  return (
    <div className="landing">
      <div className="landing-grain" aria-hidden />

      <div className="landing-nav-float">
        <header className={`landing-nav-pill${navScrolled ? ' scrolled' : ''}`}>
          <div className="landing-nav-inner">
            <Link to="/" className="landing-nav-brand">
              <strong>Tranche</strong>
              <span>Invoice discounting</span>
            </Link>
            <nav className="landing-nav-links">
              <a href="#platform" className="landing-nav-link">
                Platform
              </a>
              <a href="#lifecycle" className="landing-nav-link">
                Lifecycle
              </a>
              <a href="#roles" className="landing-nav-link">
                Roles
              </a>
              <span className="landing-nav-divider" aria-hidden />
              <Link to="/login" className="landing-btn landing-btn-primary">
                Sign in
              </Link>
            </nav>
          </div>
        </header>
      </div>

      <section className="landing-hero">
        <div className="landing-hero-grid" aria-hidden />
        <div className="landing-hero-glow" aria-hidden />

        <motion.div variants={stagger} initial="hidden" animate="visible">
          <motion.p className="landing-eyebrow" variants={fadeUp} custom={0}>
            Allocation-first fintech
          </motion.p>
          <motion.h1 variants={fadeUp} custom={1}>
            Fair allocation under pressure.
          </motion.h1>
          <motion.p className="landing-hero-lead" variants={fadeUp} custom={2}>
            Tranche coordinates invoice commitments when demand exceeds supply. Every unit
            reserved atomically. Every transition audited.
          </motion.p>
          <motion.div className="landing-hero-actions" variants={fadeUp} custom={3}>
            <Link to="/login" className="landing-btn landing-btn-primary">
              Launch app
              <BtnArrow />
            </Link>
            <a href="#platform" className="landing-btn landing-btn-secondary">
              See how it works
            </a>
          </motion.div>
          <motion.dl className="landing-hero-meta" variants={fadeUp} custom={4}>
            <div>
              <dt>Lock order</dt>
              <dd>Opportunity → Wallet</dd>
            </div>
            <div>
              <dt>Idempotency</dt>
              <dd>Per investor key</dd>
            </div>
            <div>
              <dt>Stack</dt>
              <dd>Java 21 · Spring Boot</dd>
            </div>
          </motion.dl>
        </motion.div>
      </section>

      <section id="platform" className="landing-section">
        <motion.div
          className="landing-section-header"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true, margin: '-60px' }}
          transition={{ duration: 0.45, ease }}
        >
          <h2>Built for the hard part</h2>
          <p>
            Listing invoices is easy. Allocating units fairly when ten investors hit submit at
            the same millisecond is not. That is what Tranche is for.
          </p>
        </motion.div>

        <div className="landing-features">
          {FEATURES.map((f, i) => (
            <motion.article
              key={f.index}
              className="landing-feature-card"
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true, margin: '-20px' }}
              transition={{ duration: 0.4, delay: i * 0.04, ease }}
            >
              <div className="landing-feature-head">
                <span className="landing-feature-tag">{f.tag}</span>
                <span className="landing-feature-index">{f.index}</span>
              </div>
              <h3>{f.title}</h3>
              <p>{f.body}</p>
            </motion.article>
          ))}
        </div>
      </section>

      <section id="lifecycle" className="landing-section">
        <motion.div
          className="landing-section-header"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true, margin: '-60px' }}
          transition={{ duration: 0.45, ease }}
        >
          <h2>Controlled opportunity lifecycle</h2>
          <p>
            Every status change passes through a single state machine. Invalid transitions are
            rejected before they reach the database.
          </p>
        </motion.div>

        <motion.div
          className="landing-lifecycle"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ duration: 0.5, ease }}
        >
          {LIFECYCLE.map((label, i) => (
            <div key={label} className="landing-lifecycle-step">
              <div className="step-num">{String(i + 1).padStart(2, '0')}</div>
              <div className="step-label">{label}</div>
            </div>
          ))}
        </motion.div>
      </section>

      <section id="roles" className="landing-section">
        <motion.div
          className="landing-section-header"
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true, margin: '-60px' }}
          transition={{ duration: 0.45, ease }}
        >
          <h2>Three workspaces, one platform</h2>
          <p>
            Issuers, investors, and operators each get a focused view of the same underlying
            engine.
          </p>
        </motion.div>

        <div className="landing-roles">
          {ROLES.map((r, i) => (
            <motion.div
              key={r.tag}
              className="landing-role-card"
              initial={{ opacity: 0 }}
              whileInView={{ opacity: 1 }}
              viewport={{ once: true, margin: '-30px' }}
              transition={{ duration: 0.4, delay: i * 0.06, ease }}
            >
              <div className="role-tag">{r.tag}</div>
              <h3>{r.title}</h3>
              <p>{r.body}</p>
            </motion.div>
          ))}
        </div>
      </section>

      <section className="landing-cta-band">
        <div>
          <h2>Ready to explore the demo?</h2>
          <p>
            Seeded accounts for admin, issuer, and two investors. Run the full lifecycle in
            minutes.
          </p>
        </div>
        <div className="landing-cta-actions">
          <Link to="/login" className="landing-btn landing-btn-on-dark">
            Get started
            <BtnArrow />
          </Link>
          <a href="#platform" className="landing-btn landing-btn-ghost-light landing-btn-sm">
            Learn more
          </a>
        </div>
      </section>

      <footer className="landing-footer">
        <span>Tranche · Invoice discounting platform</span>
        <div style={{ display: 'flex', gap: '1.5rem' }}>
          <a href="#platform">Platform</a>
          <Link to="/login">Sign in</Link>
        </div>
      </footer>
    </div>
  )
}

import 'lenis/dist/lenis.css'
import {
  motion,
  useScroll,
  useTransform,
  type Variants,
} from 'framer-motion'
import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useLenis } from '../hooks/useLenis'
import '../styles/landing.css'

const fadeUp: Variants = {
  hidden: { opacity: 0, y: 28 },
  visible: (i: number) => ({
    opacity: 1,
    y: 0,
    transition: { duration: 0.65, delay: i * 0.1, ease: [0.22, 1, 0.36, 1] },
  }),
}

const stagger: Variants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.08, delayChildren: 0.15 } },
}

const FEATURES = [
  {
    icon: '01',
    title: 'Atomic allocation',
    body: 'Pessimistic locking on opportunities and wallets. Concurrent commitments never overbook a single unit.',
  },
  {
    icon: '02',
    title: 'Idempotent commits',
    body: 'Every investor order carries a unique key. Retries return the original fill without double-reserving funds.',
  },
  {
    icon: '03',
    title: 'Immutable audit trail',
    body: 'Status transitions and money movements append to an audit log with actor, correlation ID, and before/after state.',
  },
  {
    icon: '04',
    title: 'Partial fills',
    body: 'When demand exceeds remaining units, the engine allocates fairly and reports full, partial, or rejected fills.',
  },
  {
    icon: '05',
    title: 'Lifecycle control',
    body: 'Draft through settled: admin review, publish, maturity, and settlement enforced by a domain state machine.',
  },
  {
    icon: '06',
    title: 'Outbox events',
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

export function LandingPage() {
  const { user, loading } = useAuth()
  const [navScrolled, setNavScrolled] = useState(false)
  useLenis()

  const { scrollY } = useScroll()
  const heroY = useTransform(scrollY, [0, 600], [0, 120])
  const heroOpacity = useTransform(scrollY, [0, 400], [1, 0.3])

  useEffect(() => {
    function onScroll() {
      setNavScrolled(window.scrollY > 48)
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

      <motion.header
        className={`landing-nav${navScrolled ? ' scrolled' : ''}`}
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.55, ease: [0.22, 1, 0.36, 1], delay: 0.1 }}
      >
        <Link to="/" className="landing-nav-brand">
          <strong>Tranche</strong>
          <span>Invoice discounting</span>
        </Link>
        <nav className="landing-nav-links">
          <a href="#platform">Platform</a>
          <a href="#lifecycle">Lifecycle</a>
          <a href="#roles">Roles</a>
          <Link to="/login" className="btn btn-primary btn-sm landing-nav-cta">
            Sign in
          </Link>
        </nav>
      </motion.header>

      <motion.section className="landing-hero" style={{ position: 'relative', zIndex: 1 }}>
        <div className="landing-hero-grid" aria-hidden />
        <motion.div
          className="landing-hero-orb landing-hero-orb--forest"
          animate={{ y: [0, -18, 0], x: [0, 8, 0] }}
          transition={{ duration: 14, repeat: Infinity, ease: 'easeInOut' }}
          aria-hidden
        />
        <motion.div
          className="landing-hero-orb landing-hero-orb--brass"
          animate={{ y: [0, 14, 0], x: [0, -10, 0] }}
          transition={{ duration: 11, repeat: Infinity, ease: 'easeInOut' }}
          aria-hidden
        />

        <motion.div style={{ y: heroY, opacity: heroOpacity }}>
          <motion.div
            variants={stagger}
            initial="hidden"
            animate="visible"
          >
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
              <Link to="/login" className="btn btn-primary">
                Launch app
              </Link>
              <a href="#platform" className="btn btn-secondary">
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
        </motion.div>
      </motion.section>

      <section id="platform" className="landing-section">
        <motion.div
          className="landing-section-header"
          initial={{ opacity: 0, y: 32 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
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
              key={f.icon}
              className="landing-feature-card"
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-40px' }}
              transition={{ duration: 0.5, delay: i * 0.06, ease: [0.22, 1, 0.36, 1] }}
              whileHover={{ y: -4, transition: { duration: 0.2 } }}
            >
              <div className="landing-feature-icon">{f.icon}</div>
              <h3>{f.title}</h3>
              <p>{f.body}</p>
            </motion.article>
          ))}
        </div>
      </section>

      <section id="lifecycle" className="landing-section">
        <motion.div
          className="landing-section-header"
          initial={{ opacity: 0, y: 32 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6 }}
        >
          <h2>Controlled opportunity lifecycle</h2>
          <p>
            Every status change passes through a single state machine. Invalid transitions are
            rejected before they reach the database.
          </p>
        </motion.div>

        <motion.div
          className="landing-lifecycle"
          initial={{ opacity: 0, x: -24 }}
          whileInView={{ opacity: 1, x: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
        >
          {LIFECYCLE.map((label, i) => (
            <motion.div
              key={label}
              className="landing-lifecycle-step"
              initial={{ opacity: 0, scale: 0.96 }}
              whileInView={{ opacity: 1, scale: 1 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.05, duration: 0.4 }}
            >
              <div className="step-num">{String(i + 1).padStart(2, '0')}</div>
              <div className="step-label">{label}</div>
            </motion.div>
          ))}
        </motion.div>
      </section>

      <section id="roles" className="landing-section">
        <motion.div
          className="landing-section-header"
          initial={{ opacity: 0, y: 32 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: '-80px' }}
          transition={{ duration: 0.6 }}
        >
          <h2>Three workspaces, one platform</h2>
          <p>Issuers, investors, and operators each get a focused view of the same underlying engine.</p>
        </motion.div>

        <div className="landing-roles">
          {ROLES.map((r, i) => (
            <motion.div
              key={r.tag}
              className="landing-role-card"
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, margin: '-40px' }}
              transition={{ duration: 0.5, delay: i * 0.08 }}
              whileHover={{ y: -3 }}
            >
              <div className="role-tag">{r.tag}</div>
              <h3>{r.title}</h3>
              <p>{r.body}</p>
            </motion.div>
          ))}
        </div>
      </section>

      <motion.section
        className="landing-cta-band"
        initial={{ opacity: 0, y: 40 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: '-60px' }}
        transition={{ duration: 0.65, ease: [0.22, 1, 0.36, 1] }}
      >
        <div>
          <h2>Ready to explore the demo?</h2>
          <p>
            Seeded accounts for admin, issuer, and two investors. Run the full lifecycle in
            minutes.
          </p>
        </div>
        <div className="landing-cta-actions">
          <Link to="/login" className="btn btn-primary">
            Get started
          </Link>
          <a href="#platform" className="btn landing-btn-ghost-light btn-sm">
            Learn more
          </a>
        </div>
      </motion.section>

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

import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import client from '../api/client'

const tiles = [
  { to: '/employees', label: 'Angajați', icon: 'M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 7a4 4 0 100 8 4 4 0 000-8z', color: '#6B5CE7', bg: '#F0EEFB' },
  { to: '/projects', label: 'Proiecte', icon: 'M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z', color: '#F59E0B', bg: '#FFF7ED' },
  { to: '/invoices', label: 'Facturi', icon: 'M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8zM14 2v6h6', color: '#10B981', bg: '#ECFDF5' },
  { to: '/reports', label: 'Rapoarte', icon: 'M9 17v-2m3 2v-4m3 4v-6M4 6h16M4 10h16M4 14h8', color: '#3B82F6', bg: '#EFF6FF' },
  { to: '/payroll', label: 'Salarizare', icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z', color: '#EF4444', bg: '#FFF1F2' },
  { to: '/leaves', label: 'Concedii', icon: 'M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z', color: '#8B5CF6', bg: '#FAF5FF' },
  { to: '/work-schedule', label: 'Program Lucru', icon: 'M12 8v4l3 3M21 12a9 9 0 11-18 0 9 9 0 0118 0z', color: '#06B6D4', bg: '#ECFEFF' },
  { to: '/company', label: 'Firmă', icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5', color: '#F97316', bg: '#FFF7ED' },
]

export default function Dashboard() {
  const navigate = useNavigate()
  const [stats, setStats] = useState({ employees: 0, invoicesUnpaid: 0, income: 0, payroll: 0 })
  const month = new Date().toISOString().slice(0, 7)

  useEffect(() => {
    const load = async () => {
      try {
        const [empRes, invRes, repRes] = await Promise.all([
          client.get('/angajati'),
          client.get('/facturi'),
          client.get('/rapoarte'),
        ])
        const employees = empRes.data?.length || 0
        const invoices = invRes.data || []
        const invoicesUnpaid = invoices.filter(i => i.status !== 'platita').reduce((s, i) => s + (i.total || 0), 0)
        const income = invoices.filter(i => i.status === 'platita' && (i.data_emitere || '').startsWith(month))
          .reduce((s, i) => s + (i.total || 0), 0)
        setStats({ employees, invoicesUnpaid, income, payroll: 0 })
      } catch {}
    }
    load()
  }, [])

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: 14, marginTop: 2 }}>
            {new Date().toLocaleDateString('ro-RO', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' })}
          </p>
        </div>
      </div>
      <div className="page-body">
        <div className="stat-grid">
          <div className="stat-card blue">
            <div className="stat-label">Angajați activi</div>
            <div className="stat-value">{stats.employees}</div>
          </div>
          <div className="stat-card rose">
            <div className="stat-label">Restanțe clienți</div>
            <div className="stat-value">{stats.invoicesUnpaid.toFixed(0)} RON</div>
          </div>
          <div className="stat-card green">
            <div className="stat-label">Venituri luna aceasta</div>
            <div className="stat-value">{stats.income.toFixed(0)} RON</div>
          </div>
          <div className="stat-card purple">
            <div className="stat-label">Fond salarii</div>
            <div className="stat-value">{stats.payroll.toFixed(0)} RON</div>
          </div>
        </div>

        <div className="card">
          <h2 style={{ fontSize: 16, fontWeight: 700, marginBottom: 20 }}>Meniu rapid</h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: 12 }}>
            {tiles.map(({ to, label, icon, color, bg }) => (
              <button key={to} onClick={() => navigate(to)} style={{
                background: bg, border: 'none', borderRadius: 16, padding: '18px 16px',
                cursor: 'pointer', display: 'flex', alignItems: 'center', gap: 12,
                transition: 'transform 0.15s', textAlign: 'left'
              }}
                onMouseEnter={e => e.currentTarget.style.transform = 'translateY(-2px)'}
                onMouseLeave={e => e.currentTarget.style.transform = ''}
              >
                <span style={{ background: color, borderRadius: 12, width: 40, height: 40,
                  display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                  <svg viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2"
                    strokeLinecap="round" strokeLinejoin="round" style={{ width: 20, height: 20 }}>
                    <path d={icon} />
                  </svg>
                </span>
                <span style={{ fontWeight: 600, fontSize: 13, color: '#1A1A2E' }}>{label}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </>
  )
}

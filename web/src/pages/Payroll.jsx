import React, { useEffect, useState } from 'react'
import client from '../api/client'

export default function Payroll() {
  const [employees, setEmployees] = useState([])
  const [reports, setReports] = useState([])
  const [loading, setLoading] = useState(true)
  const [month, setMonth] = useState(new Date().toISOString().slice(0, 7))

  useEffect(() => {
    const load = async () => {
      setLoading(true)
      try {
        const [er, rr] = await Promise.all([client.get('/angajati'), client.get('/rapoarte_zilnice')])
        setEmployees(er.data || [])
        setReports(rr.data || [])
      } catch {}
      setLoading(false)
    }
    load()
  }, [])

  const monthReports = reports.filter(r => (r.date || '').startsWith(month))

  const calcSalary = (emp) => {
    const empReports = monthReports.filter(r => r.employeeId === emp.id)
    const rate = emp.paymentRate || 0
    let total = 0, details = ''

    if (emp.paymentType === 'FIXED') {
      total = rate
      details = `Salariu fix ${rate.toFixed(2)} RON/lună`
    } else if (emp.paymentType === 'HOURLY') {
      const totalHours = empReports.reduce((s, r) => s + (r.hoursWorked || 0), 0)
      total = totalHours * rate
      details = `${totalHours.toFixed(1)}h × ${rate.toFixed(2)} RON/h`
    } else if (emp.paymentType === 'DAILY') {
      const dailyMap = {}
      for (const r of empReports) {
        if (r.date) dailyMap[r.date] = (dailyMap[r.date] || 0) + (r.hoursWorked || 0)
      }
      let fullDays = 0, halfDays = 0
      for (const h of Object.values(dailyMap)) {
        if (h >= 6) fullDays++; else halfDays++
      }
      total = fullDays * rate + halfDays * (rate / 2)
      if (halfDays > 0)
        details = `${fullDays} zile întregi + ${halfDays} zile scurte (<6h) × ${rate.toFixed(2)} RON/zi`
      else
        details = `${fullDays} zile × ${rate.toFixed(2)} RON/zi`
    }
    return { total, details }
  }

  const grandTotal = employees.reduce((s, e) => s + calcSalary(e).total, 0)

  return (
    <>
      <div className="page-header">
        <h1>Salarizare</h1>
        <input type="month" value={month} onChange={e => setMonth(e.target.value)}
          style={{ padding: '8px 14px', borderRadius: 8, border: '1px solid var(--border)', fontSize: 14, fontFamily: 'inherit' }} />
      </div>
      <div className="page-body">
        <div className="stat-grid" style={{ marginBottom: 20 }}>
          <div className="stat-card blue"><div className="stat-label">Total angajați</div><div className="stat-value">{employees.length}</div></div>
          <div className="stat-card purple"><div className="stat-label">Fond salarii {month}</div><div className="stat-value">{grandTotal.toFixed(0)} RON</div></div>
          <div className="stat-card green"><div className="stat-label">Rapoarte luna aceasta</div><div className="stat-value">{monthReports.length}</div></div>
        </div>
        <div className="card">
          <h2 style={{ fontSize: 15, fontWeight: 700, marginBottom: 16 }}>Detalii salarizare — {month}</h2>
          {loading ? <div className="spinner-wrap"><div className="spinner" /></div> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Angajat</th><th>Tip plată</th><th>Detalii calcul</th><th style={{ textAlign: 'right' }}>Total</th></tr></thead>
                <tbody>
                  {employees.length === 0 && <tr><td colSpan={4} style={{ textAlign: 'center', color: 'var(--text-hint)', padding: 40 }}>Niciun angajat</td></tr>}
                  {employees.map(emp => {
                    const { total, details } = calcSalary(emp)
                    return (
                      <tr key={emp.id}>
                        <td>
                          <strong>{emp.firstName} {emp.lastName}</strong>
                          {emp.position && <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 2 }}>{emp.position}</div>}
                        </td>
                        <td><span className="badge badge-blue">{emp.paymentType}</span></td>
                        <td style={{ fontSize: 13, color: 'var(--text-secondary)' }}>{details || '—'}</td>
                        <td style={{ textAlign: 'right' }}>
                          <strong style={{ fontSize: 15, color: 'var(--primary)' }}>{total.toFixed(2)} RON</strong>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
                <tfoot>
                  <tr>
                    <td colSpan={3} style={{ textAlign: 'right', fontWeight: 700, paddingTop: 12 }}>Total fond salarii:</td>
                    <td style={{ textAlign: 'right', fontWeight: 700, fontSize: 16, color: 'var(--primary)', paddingTop: 12 }}>{grandTotal.toFixed(2)} RON</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          )}
        </div>
      </div>
    </>
  )
}

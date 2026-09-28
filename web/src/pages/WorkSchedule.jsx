import React, { useEffect, useState } from 'react'
import client from '../api/client'

const dayNames = ['', 'Luni', 'Marți', 'Miercuri', 'Joi', 'Vineri', 'Sâmbătă', 'Duminică']
const empty = { employeeId: '', dayOfWeek: 1, startTime: '08:00', endTime: '17:00', isWorkDay: true }

export default function WorkSchedule() {
  const [list, setList] = useState([])
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [filterEmp, setFilterEmp] = useState('')
  const [modal, setModal] = useState(false)
  const [form, setForm] = useState(empty)
  const [saving, setSaving] = useState(false)

  const load = async () => {
    try {
      const [sr, er] = await Promise.all([client.get('/program_lucru'), client.get('/angajati')])
      setList(sr.data || [])
      setEmployees(er.data || [])
    } catch {}
    setLoading(false)
  }
  useEffect(() => { load() }, [])

  const save = async () => {
    setSaving(true)
    try {
      const payload = { ...form, employeeId: parseInt(form.employeeId), dayOfWeek: parseInt(form.dayOfWeek) }
      await client.post('/program_lucru', payload)
      setModal(false); load()
    } catch (e) {
      const d = e.response?.data?.detail
      alert(Array.isArray(d) ? d.map(x => x.msg || JSON.stringify(x)).join('\n') : (d || 'Eroare la salvare'))
    }
    setSaving(false)
  }

  const del = async (id) => {
    if (!confirm('Ștergi înregistrarea?')) return
    try { await client.delete(`/program_lucru/${id}`); load() } catch {}
  }

  const empMap = Object.fromEntries(employees.map(e => [e.id, `${e.firstName} ${e.lastName}`]))
  const filtered = list.filter(s => !filterEmp || s.employeeId === parseInt(filterEmp))

  const grouped = {}
  for (const s of filtered) {
    if (!grouped[s.employeeId]) grouped[s.employeeId] = {}
    grouped[s.employeeId][s.dayOfWeek] = s
  }

  return (
    <>
      <div className="page-header">
        <h1>Program de lucru</h1>
        <button className="btn btn-primary" onClick={() => { setForm(empty); setModal(true) }}>+ Adaugă program</button>
      </div>
      <div className="page-body">
        <div className="card">
          <div className="toolbar">
            <select value={filterEmp} onChange={e => setFilterEmp(e.target.value)}
              style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', fontSize: 13 }}>
              <option value="">Toți angajații</option>
              {employees.map(e => <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>)}
            </select>
          </div>
          {loading ? <div className="spinner-wrap"><div className="spinner" /></div> : (
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Angajat</th>
                    {dayNames.slice(1).map(d => <th key={d} style={{ textAlign: 'center', minWidth: 80 }}>{d}</th>)}
                    <th>Acțiuni</th>
                  </tr>
                </thead>
                <tbody>
                  {Object.keys(grouped).length === 0 && (
                    <tr><td colSpan={9} style={{ textAlign: 'center', color: 'var(--text-hint)', padding: 40 }}>Niciun program configurat</td></tr>
                  )}
                  {Object.entries(grouped).map(([empId, dayMap]) => (
                    <tr key={empId}>
                      <td><strong>{empMap[empId] || `#${empId}`}</strong></td>
                      {[1,2,3,4,5,6,7].map(d => {
                        const s = dayMap[d]
                        return <td key={d} style={{ textAlign: 'center', fontSize: 12 }}>
                          {!s ? <span style={{ color: 'var(--text-hint)' }}>—</span>
                            : !s.isWorkDay ? <span style={{ color: '#EF4444', fontSize: 11 }}>Liber</span>
                            : <span style={{ color: 'var(--primary)', fontSize: 11 }}>{s.startTime}–{s.endTime}</span>}
                        </td>
                      })}
                      <td>
                        <button className="btn btn-secondary btn-sm" onClick={() => setFilterEmp(empId.toString())}>Filtrează</button>
                      </td>
                    </tr>
                  ))}
                  {filterEmp && filtered.map(s => (
                    <tr key={`detail-${s.id}`} style={{ background: '#fafafa' }}>
                      <td colSpan={2} style={{ paddingLeft: 32, color: 'var(--text-secondary)', fontSize: 13 }}>
                        {dayNames[s.dayOfWeek] || `Ziua ${s.dayOfWeek}`}
                      </td>
                      <td colSpan={5} style={{ fontSize: 13 }}>
                        {s.isWorkDay ? `${s.startTime} – ${s.endTime}` : <span style={{ color: '#EF4444' }}>Zi liberă</span>}
                      </td>
                      <td colSpan={2}>
                        <button className="btn btn-danger btn-sm" onClick={() => del(s.id)}>Șterge</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {modal && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setModal(false)}>
          <div className="modal">
            <div className="modal-header">
              <h2>Adaugă program</h2>
              <button className="modal-close" onClick={() => setModal(false)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-group"><label>Angajat</label>
                <select value={form.employeeId} onChange={e => setForm(f => ({ ...f, employeeId: e.target.value }))}>
                  <option value="">— Selectează —</option>
                  {employees.map(e => <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>)}
                </select></div>
              <div className="form-group"><label>Ziua săptămânii</label>
                <select value={form.dayOfWeek} onChange={e => setForm(f => ({ ...f, dayOfWeek: parseInt(e.target.value) }))}>
                  {dayNames.slice(1).map((d, i) => <option key={i+1} value={i+1}>{d}</option>)}
                </select></div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                <input type="checkbox" id="isWorkDay" checked={form.isWorkDay}
                  onChange={e => setForm(f => ({ ...f, isWorkDay: e.target.checked }))} />
                <label htmlFor="isWorkDay" style={{ margin: 0, cursor: 'pointer' }}>Zi lucrătoare</label>
              </div>
              {form.isWorkDay && (
                <div className="form-row">
                  <div className="form-group"><label>Ora început</label>
                    <input type="time" value={form.startTime || '08:00'} onChange={e => setForm(f => ({ ...f, startTime: e.target.value }))} /></div>
                  <div className="form-group"><label>Ora final</label>
                    <input type="time" value={form.endTime || '17:00'} onChange={e => setForm(f => ({ ...f, endTime: e.target.value }))} /></div>
                </div>
              )}
              <div className="form-actions">
                <button className="btn btn-secondary" onClick={() => setModal(false)}>Anulează</button>
                <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Se salvează...' : 'Salvează'}</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

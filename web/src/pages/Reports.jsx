import React, { useEffect, useState } from 'react'
import client from '../api/client'

const empty = { employeeId: '', employeeName: '', projectId: 0, projectName: '', date: '', reportText: '', hoursWorked: '' }

export default function Reports() {
  const [list, setList] = useState([])
  const [employees, setEmployees] = useState([])
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [filterEmp, setFilterEmp] = useState('')
  const [filterMonth, setFilterMonth] = useState(new Date().toISOString().slice(0, 7))
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState(empty)
  const [saving, setSaving] = useState(false)

  const load = async () => {
    try {
      const [rr, er, pr] = await Promise.all([
        client.get('/rapoarte_zilnice'),
        client.get('/angajati'),
        client.get('/proiecte'),
      ])
      setList(rr.data || [])
      setEmployees(er.data || [])
      setProjects(pr.data || [])
    } catch {}
    setLoading(false)
  }
  useEffect(() => { load() }, [])

  const open = (r = null) => {
    setForm(r ? { ...r } : { ...empty, date: new Date().toISOString().slice(0, 10) })
    setModal(r ? 'edit' : 'add')
  }

  const pickEmployee = (id) => {
    const e = employees.find(x => x.id === parseInt(id))
    setForm(f => ({ ...f, employeeId: id, employeeName: e ? `${e.firstName} ${e.lastName}` : '' }))
  }

  const pickProject = (id) => {
    const p = projects.find(x => x.id === parseInt(id))
    setForm(f => ({ ...f, projectId: parseInt(id) || 0, projectName: p?.name || '' }))
  }

  const save = async () => {
    setSaving(true)
    try {
      const payload = {
        ...form,
        employeeId: parseInt(form.employeeId),
        projectId: parseInt(form.projectId) || 0,
        hoursWorked: parseFloat(form.hoursWorked) || 0,
      }
      if (modal === 'add') await client.post('/rapoarte_zilnice', payload)
      else await client.put(`/reports/${form.id}`, payload)
      setModal(null); load()
    } catch (e) {
      const d = e.response?.data?.detail
      alert(Array.isArray(d) ? d.map(x => x.msg || JSON.stringify(x)).join('\n') : (d || 'Eroare la salvare'))
    }
    setSaving(false)
  }

  const del = async (id) => {
    if (!confirm('Ștergi raportul?')) return
    try { await client.delete(`/reports/${id}`); load() } catch { alert('Eroare') }
  }

  const filtered = list.filter(r => {
    const matchEmp = !filterEmp || r.employeeId === parseInt(filterEmp)
    const matchMonth = !filterMonth || (r.date || '').startsWith(filterMonth)
    return matchEmp && matchMonth
  })

  const totalHours = filtered.reduce((s, r) => s + (r.hoursWorked || 0), 0)

  return (
    <>
      <div className="page-header">
        <h1>Rapoarte zilnice</h1>
        <button className="btn btn-primary" onClick={() => open()}>+ Adaugă raport</button>
      </div>
      <div className="page-body">
        <div className="card">
          <div className="toolbar" style={{ flexWrap: 'wrap', gap: 10 }}>
            <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', flex: 1 }}>
              <select value={filterEmp} onChange={e => setFilterEmp(e.target.value)}
                style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', fontSize: 13 }}>
                <option value="">Toți angajații</option>
                {employees.map(e => <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>)}
              </select>
              <input type="month" value={filterMonth} onChange={e => setFilterMonth(e.target.value)}
                style={{ padding: '8px 12px', borderRadius: 8, border: '1px solid var(--border)', fontSize: 13 }} />
            </div>
            <span style={{ color: 'var(--text-secondary)', fontSize: 13, alignSelf: 'center' }}>
              {filtered.length} înregistrări · {totalHours.toFixed(1)}h total
            </span>
          </div>
          {loading ? <div className="spinner-wrap"><div className="spinner" /></div> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Angajat</th><th>Data</th><th>Ore</th><th>Proiect</th><th>Detalii</th><th>Acțiuni</th></tr></thead>
                <tbody>
                  {filtered.length === 0 && <tr><td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-hint)', padding: 40 }}>Niciun raport</td></tr>}
                  {[...filtered].sort((a, b) => (b.date || '').localeCompare(a.date || '')).map(r => (
                    <tr key={r.id}>
                      <td><strong>{r.employeeName || `#${r.employeeId}`}</strong></td>
                      <td>{r.date}</td>
                      <td>
                        <span style={{ background: '#6B5CE7', color: '#fff', borderRadius: 20, padding: '2px 10px', fontSize: 12, fontWeight: 600 }}>
                          {Number.isInteger(r.hoursWorked) ? `${r.hoursWorked}h` : `${Number(r.hoursWorked).toFixed(1)}h`}
                        </span>
                      </td>
                      <td>{r.projectName || '—'}</td>
                      <td style={{ color: 'var(--text-secondary)', fontSize: 13, maxWidth: 200 }}>{r.reportText || '—'}</td>
                      <td style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-secondary btn-sm" onClick={() => open(r)}>Editează</button>
                        <button className="btn btn-danger btn-sm" onClick={() => del(r.id)}>Șterge</button>
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
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && setModal(null)}>
          <div className="modal">
            <div className="modal-header">
              <h2>{modal === 'add' ? 'Adaugă raport' : 'Editează raport'}</h2>
              <button className="modal-close" onClick={() => setModal(null)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-group"><label>Angajat</label>
                <select value={form.employeeId} onChange={e => pickEmployee(e.target.value)}>
                  <option value="">— Selectează —</option>
                  {employees.map(e => <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>)}
                </select></div>
              <div className="form-row">
                <div className="form-group"><label>Data</label>
                  <input type="date" value={form.date || ''} onChange={e => setForm(f => ({ ...f, date: e.target.value }))} /></div>
                <div className="form-group"><label>Ore lucrate</label>
                  <input type="number" step="0.5" min="0" max="24" value={form.hoursWorked} onChange={e => setForm(f => ({ ...f, hoursWorked: e.target.value }))} /></div>
              </div>
              <div className="form-group"><label>Proiect</label>
                <select value={form.projectId || ''} onChange={e => pickProject(e.target.value)}>
                  <option value="0">— Niciun proiect —</option>
                  {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                </select></div>
              <div className="form-group"><label>Detalii activitate</label>
                <input value={form.reportText || ''} onChange={e => setForm(f => ({ ...f, reportText: e.target.value }))} /></div>
              <div className="form-actions">
                <button className="btn btn-secondary" onClick={() => setModal(null)}>Anulează</button>
                <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Se salvează...' : 'Salvează'}</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

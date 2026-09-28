import React, { useEffect, useState } from 'react'
import client from '../api/client'

const empty = { employeeId: '', employeeName: '', type: 'concediu_odihna', startDate: '', endDate: '', reason: '', status: 'PENDING' }
const typeLabel = { concediu_odihna: 'Concediu odihnă', medical: 'Medical', fara_plata: 'Fără plată', maternitate: 'Maternitate' }
const statusBadge = { PENDING: 'badge-blue', APPROVED: 'badge-green', REJECTED: 'badge-red' }
const statusLabel = { PENDING: 'În așteptare', APPROVED: 'Aprobat', REJECTED: 'Respins' }

export default function Leaves() {
  const [list, setList] = useState([])
  const [employees, setEmployees] = useState([])
  const [loading, setLoading] = useState(true)
  const [filterStatus, setFilterStatus] = useState('all')
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState(empty)
  const [saving, setSaving] = useState(false)

  const load = async () => {
    try {
      const [lr, er] = await Promise.all([client.get('/concedii'), client.get('/angajati')])
      setList(lr.data || [])
      setEmployees(er.data || [])
    } catch {}
    setLoading(false)
  }
  useEffect(() => { load() }, [])

  const open = (l = null) => { setForm(l ? { ...l } : empty); setModal(l ? 'edit' : 'add') }

  const pickEmployee = (id) => {
    const e = employees.find(x => x.id === parseInt(id))
    setForm(f => ({ ...f, employeeId: id, employeeName: e ? `${e.firstName} ${e.lastName}` : '' }))
  }

  const save = async () => {
    setSaving(true)
    try {
      const payload = { ...form, employeeId: parseInt(form.employeeId) }
      if (modal === 'add') await client.post('/concedii', payload)
      else await client.put(`/concedii/${form.id}`, payload)
      setModal(null); load()
    } catch (e) {
      const d = e.response?.data?.detail
      alert(Array.isArray(d) ? d.map(x => x.msg || JSON.stringify(x)).join('\n') : (d || 'Eroare la salvare'))
    }
    setSaving(false)
  }

  const setStatus = async (item, status) => {
    try { await client.put(`/concedii/${item.id}`, { ...item, status }); load() } catch {}
  }

  const del = async (id) => {
    if (!confirm('Ștergi cererea?')) return
    try { await client.delete(`/concedii/${id}`); load() } catch {}
  }

  const filtered = list.filter(l => filterStatus === 'all' || l.status === filterStatus)

  return (
    <>
      <div className="page-header">
        <h1>Concedii</h1>
        <button className="btn btn-primary" onClick={() => open()}>+ Cerere concediu</button>
      </div>
      <div className="page-body">
        <div className="card">
          <div className="toolbar">
            <div style={{ display: 'flex', gap: 8 }}>
              {['all', 'PENDING', 'APPROVED', 'REJECTED'].map(s => (
                <button key={s} onClick={() => setFilterStatus(s)}
                  style={{ padding: '6px 14px', borderRadius: 20, border: 'none', cursor: 'pointer', fontSize: 13,
                    background: filterStatus === s ? 'var(--primary)' : 'var(--surface-hover)',
                    color: filterStatus === s ? '#fff' : 'var(--text-secondary)', fontWeight: 600 }}>
                  {s === 'all' ? 'Toate' : statusLabel[s]}
                </button>
              ))}
            </div>
            <span style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{filtered.length} cereri</span>
          </div>
          {loading ? <div className="spinner-wrap"><div className="spinner" /></div> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Angajat</th><th>Tip</th><th>Perioadă</th><th>Motiv</th><th>Status</th><th>Acțiuni</th></tr></thead>
                <tbody>
                  {filtered.length === 0 && <tr><td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-hint)', padding: 40 }}>Nicio cerere</td></tr>}
                  {filtered.map(l => (
                    <tr key={l.id}>
                      <td><strong>{l.employeeName || `#${l.employeeId}`}</strong></td>
                      <td>{typeLabel[l.type] || l.type}</td>
                      <td style={{ fontSize: 13 }}>{l.startDate} → {l.endDate}</td>
                      <td style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{l.reason || '—'}</td>
                      <td><span className={`badge ${statusBadge[l.status] || 'badge-blue'}`}>{statusLabel[l.status] || l.status}</span></td>
                      <td style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                        {l.status === 'PENDING' && <>
                          <button className="btn btn-success btn-sm" onClick={() => setStatus(l, 'APPROVED')}>Aprobă</button>
                          <button className="btn btn-danger btn-sm" onClick={() => setStatus(l, 'REJECTED')}>Respinge</button>
                        </>}
                        <button className="btn btn-secondary btn-sm" onClick={() => open(l)}>Editează</button>
                        <button className="btn btn-danger btn-sm" onClick={() => del(l.id)}>Șterge</button>
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
              <h2>{modal === 'add' ? 'Cerere concediu' : 'Editează cerere'}</h2>
              <button className="modal-close" onClick={() => setModal(null)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-group"><label>Angajat</label>
                <select value={form.employeeId} onChange={e => pickEmployee(e.target.value)}>
                  <option value="">— Selectează —</option>
                  {employees.map(e => <option key={e.id} value={e.id}>{e.firstName} {e.lastName}</option>)}
                </select></div>
              <div className="form-row">
                <div className="form-group"><label>Tip concediu</label>
                  <select value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value }))}>
                    {Object.entries(typeLabel).map(([v, l]) => <option key={v} value={v}>{l}</option>)}
                  </select></div>
                <div className="form-group"><label>Status</label>
                  <select value={form.status} onChange={e => setForm(f => ({ ...f, status: e.target.value }))}>
                    <option value="PENDING">În așteptare</option>
                    <option value="APPROVED">Aprobat</option>
                    <option value="REJECTED">Respins</option>
                  </select></div>
              </div>
              <div className="form-row">
                <div className="form-group"><label>Data început</label>
                  <input type="date" value={form.startDate || ''} onChange={e => setForm(f => ({ ...f, startDate: e.target.value }))} /></div>
                <div className="form-group"><label>Data final</label>
                  <input type="date" value={form.endDate || ''} onChange={e => setForm(f => ({ ...f, endDate: e.target.value }))} /></div>
              </div>
              <div className="form-group"><label>Motiv</label>
                <input value={form.reason || ''} onChange={e => setForm(f => ({ ...f, reason: e.target.value }))} /></div>
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

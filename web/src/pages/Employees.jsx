import React, { useEffect, useState } from 'react'
import client from '../api/client'

const empty = { firstName: '', lastName: '', position: '', paymentType: 'FIXED', paymentRate: '', paymentDay: 1, phone: '', email: '' }
const payLabel = { HOURLY: 'RON/h', DAILY: 'RON/zi', FIXED: 'RON/lună' }

export default function Employees() {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState(empty)
  const [saving, setSaving] = useState(false)

  const load = async () => {
    try { const r = await client.get('/angajati'); setList(r.data || []) } catch {}
    setLoading(false)
  }
  useEffect(() => { load() }, [])

  const open = (emp = null) => {
    setForm(emp ? { ...emp } : empty)
    setModal(emp ? 'edit' : 'add')
  }

  const save = async () => {
    setSaving(true)
    try {
      const payload = { ...form, paymentRate: parseFloat(form.paymentRate) || 0, paymentDay: parseInt(form.paymentDay) || 1 }
      if (modal === 'add') await client.post('/angajati', payload)
      else await client.put(`/employees/${form.id}`, payload)
      setModal(null); load()
    } catch (e) {
      const d = e.response?.data?.detail
      alert(Array.isArray(d) ? d.map(x => x.msg || JSON.stringify(x)).join('\n') : (d || 'Eroare la salvare'))
    }
    setSaving(false)
  }

  const del = async (id) => {
    if (!confirm('Ștergi angajatul?')) return
    try { await client.delete(`/employees/${id}`); load() } catch { alert('Eroare la ștergere') }
  }

  const filtered = list.filter(e =>
    `${e.firstName} ${e.lastName} ${e.email || ''}`.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <>
      <div className="page-header">
        <h1>Angajați</h1>
        <button className="btn btn-primary" onClick={() => open()}>+ Adaugă angajat</button>
      </div>
      <div className="page-body">
        <div className="card">
          <div className="toolbar">
            <div className="search-bar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
              <input placeholder="Caută angajat..." value={search} onChange={e => setSearch(e.target.value)} />
            </div>
            <span style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{filtered.length} angajați</span>
          </div>
          {loading ? <div className="spinner-wrap"><div className="spinner" /></div> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Nume</th><th>Funcție</th><th>Email</th><th>Telefon</th><th>Tip plată</th><th>Tarif</th><th>Acțiuni</th></tr></thead>
                <tbody>
                  {filtered.length === 0 && <tr><td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-hint)', padding: 40 }}>Niciun angajat</td></tr>}
                  {filtered.map(e => (
                    <tr key={e.id}>
                      <td><strong>{e.firstName} {e.lastName}</strong></td>
                      <td>{e.position || '—'}</td>
                      <td>{e.email || '—'}</td>
                      <td>{e.phone || '—'}</td>
                      <td><span className="badge badge-blue">{e.paymentType}</span></td>
                      <td>{e.paymentRate} {payLabel[e.paymentType]}</td>
                      <td style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-secondary btn-sm" onClick={() => open(e)}>Editează</button>
                        <button className="btn btn-danger btn-sm" onClick={() => del(e.id)}>Șterge</button>
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
              <h2>{modal === 'add' ? 'Adaugă angajat' : 'Editează angajat'}</h2>
              <button className="modal-close" onClick={() => setModal(null)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-row">
                <div className="form-group"><label>Prenume</label>
                  <input value={form.firstName} onChange={e => setForm(f => ({ ...f, firstName: e.target.value }))} /></div>
                <div className="form-group"><label>Nume</label>
                  <input value={form.lastName} onChange={e => setForm(f => ({ ...f, lastName: e.target.value }))} /></div>
              </div>
              <div className="form-group"><label>Funcție</label>
                <input value={form.position || ''} onChange={e => setForm(f => ({ ...f, position: e.target.value }))} /></div>
              <div className="form-row">
                <div className="form-group"><label>Email</label>
                  <input type="email" value={form.email || ''} onChange={e => setForm(f => ({ ...f, email: e.target.value }))} /></div>
                <div className="form-group"><label>Telefon</label>
                  <input value={form.phone || ''} onChange={e => setForm(f => ({ ...f, phone: e.target.value }))} /></div>
              </div>
              <div className="form-row">
                <div className="form-group"><label>Tip plată</label>
                  <select value={form.paymentType} onChange={e => setForm(f => ({ ...f, paymentType: e.target.value }))}>
                    <option value="FIXED">Fix lunar</option>
                    <option value="HOURLY">Pe oră</option>
                    <option value="DAILY">Pe zi</option>
                  </select></div>
                <div className="form-group"><label>Tarif ({payLabel[form.paymentType]})</label>
                  <input type="number" value={form.paymentRate} onChange={e => setForm(f => ({ ...f, paymentRate: e.target.value }))} /></div>
              </div>
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

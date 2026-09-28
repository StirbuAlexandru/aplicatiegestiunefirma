import React, { useEffect, useState } from 'react'
import client from '../api/client'

const empty = { name: '', clientName: '', description: '', budget: '', deadline: '', status: 'In desfasurare', notes: '' }
const statusColors = { 'In desfasurare': 'badge-blue', 'Finalizat': 'badge-green', 'Anulat': 'badge-red', 'activ': 'badge-blue', 'finalizat': 'badge-green', 'anulat': 'badge-red' }

export default function Projects() {
  const [list, setList] = useState([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState(empty)
  const [saving, setSaving] = useState(false)

  const load = async () => {
    try { const r = await client.get('/proiecte'); setList(r.data || []) } catch {}
    setLoading(false)
  }
  useEffect(() => { load() }, [])

  const open = (p = null) => { setForm(p ? { ...p } : empty); setModal(p ? 'edit' : 'add') }

  const save = async () => {
    setSaving(true)
    try {
      const payload = { ...form, budget: parseFloat(form.budget) || 0 }
      if (modal === 'add') await client.post('/proiecte', payload)
      else await client.put(`/projects/${form.id}`, payload)
      setModal(null); load()
    } catch (e) {
      const d = e.response?.data?.detail
      alert(Array.isArray(d) ? d.map(x => x.msg || JSON.stringify(x)).join('\n') : (d || 'Eroare la salvare'))
    }
    setSaving(false)
  }

  const del = async (id) => {
    if (!confirm('Ștergi proiectul?')) return
    try { await client.delete(`/projects/${id}`); load() } catch { alert('Eroare la ștergere') }
  }

  const filtered = list.filter(p =>
    `${p.name} ${p.clientName || ''}`.toLowerCase().includes(search.toLowerCase())
  )

  return (
    <>
      <div className="page-header">
        <h1>Proiecte</h1>
        <button className="btn btn-primary" onClick={() => open()}>+ Adaugă proiect</button>
      </div>
      <div className="page-body">
        <div className="card">
          <div className="toolbar">
            <div className="search-bar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
              <input placeholder="Caută proiect..." value={search} onChange={e => setSearch(e.target.value)} />
            </div>
            <span style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{filtered.length} proiecte</span>
          </div>
          {loading ? <div className="spinner-wrap"><div className="spinner" /></div> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Proiect</th><th>Client</th><th>Status</th><th>Buget</th><th>Deadline</th><th>Acțiuni</th></tr></thead>
                <tbody>
                  {filtered.length === 0 && <tr><td colSpan={6} style={{ textAlign: 'center', color: 'var(--text-hint)', padding: 40 }}>Niciun proiect</td></tr>}
                  {filtered.map(p => (
                    <tr key={p.id}>
                      <td><strong>{p.name}</strong>{p.description && <div style={{ fontSize: 12, color: 'var(--text-secondary)', marginTop: 2 }}>{p.description}</div>}</td>
                      <td>{p.clientName || '—'}</td>
                      <td><span className={`badge ${statusColors[p.status] || 'badge-blue'}`}>{p.status}</span></td>
                      <td>{p.budget ? `${Number(p.budget).toFixed(0)} RON` : '—'}</td>
                      <td style={{ fontSize: 12 }}>{p.deadline || '—'}</td>
                      <td style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-secondary btn-sm" onClick={() => open(p)}>Editează</button>
                        <button className="btn btn-danger btn-sm" onClick={() => del(p.id)}>Șterge</button>
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
              <h2>{modal === 'add' ? 'Adaugă proiect' : 'Editează proiect'}</h2>
              <button className="modal-close" onClick={() => setModal(null)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-group"><label>Nume proiect</label>
                <input value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} /></div>
              <div className="form-row">
                <div className="form-group"><label>Client</label>
                  <input value={form.clientName || ''} onChange={e => setForm(f => ({ ...f, clientName: e.target.value }))} /></div>
                <div className="form-group"><label>Status</label>
                  <select value={form.status} onChange={e => setForm(f => ({ ...f, status: e.target.value }))}>
                    <option value="In desfasurare">În desfășurare</option>
                    <option value="Finalizat">Finalizat</option>
                    <option value="Anulat">Anulat</option>
                  </select></div>
              </div>
              <div className="form-group"><label>Descriere</label>
                <input value={form.description || ''} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} /></div>
              <div className="form-row">
                <div className="form-group"><label>Buget (RON)</label>
                  <input type="number" value={form.budget} onChange={e => setForm(f => ({ ...f, budget: e.target.value }))} /></div>
                <div className="form-group"><label>Deadline</label>
                  <input type="date" value={form.deadline || ''} onChange={e => setForm(f => ({ ...f, deadline: e.target.value }))} /></div>
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

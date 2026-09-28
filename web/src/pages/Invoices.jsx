import React, { useEffect, useState } from 'react'
import client from '../api/client'

const empty = { invoiceNumber: '', projectId: '', projectName: '', amount: '', date: '', dueDate: '', isPaid: false, type: 'Servicii' }

export default function Invoices() {
  const [list, setList] = useState([])
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [filterPaid, setFilterPaid] = useState('all')
  const [modal, setModal] = useState(null)
  const [form, setForm] = useState(empty)
  const [saving, setSaving] = useState(false)

  const load = async () => {
    try {
      const [ir, pr] = await Promise.all([client.get('/facturi'), client.get('/proiecte')])
      setList(ir.data || [])
      setProjects(pr.data || [])
    } catch {}
    setLoading(false)
  }
  useEffect(() => { load() }, [])

  const open = (inv = null) => {
    const today = new Date().toISOString().slice(0, 10)
    setForm(inv ? { ...inv } : { ...empty, date: today })
    setModal(inv ? 'edit' : 'add')
  }

  const pickProject = (id) => {
    const p = projects.find(x => x.id === parseInt(id))
    setForm(f => ({ ...f, projectId: id, projectName: p?.name || '' }))
  }

  const save = async () => {
    setSaving(true)
    try {
      const payload = {
        ...form,
        projectId: parseInt(form.projectId) || 0,
        amount: parseFloat(form.amount) || 0,
        isPaid: Boolean(form.isPaid),
      }
      if (modal === 'add') await client.post('/facturi', payload)
      else await client.put(`/invoices/${form.id}`, payload)
      setModal(null); load()
    } catch (e) {
      const d = e.response?.data?.detail
      alert(Array.isArray(d) ? d.map(x => x.msg || JSON.stringify(x)).join('\n') : (d || 'Eroare la salvare'))
    }
    setSaving(false)
  }

  const del = async (id) => {
    if (!confirm('Ștergi factura?')) return
    try { await client.delete(`/invoices/${id}`); load() } catch { alert('Eroare la ștergere') }
  }

  const markPaid = async (inv) => {
    try { await client.put(`/invoices/${inv.id}`, { ...inv, isPaid: true }); load() } catch {}
  }

  const filtered = list.filter(inv => {
    if (filterPaid === 'paid') return inv.isPaid
    if (filterPaid === 'unpaid') return !inv.isPaid
    return true
  })

  const totalPaid = list.filter(i => i.isPaid).reduce((s, i) => s + (Number(i.amount) || 0), 0)
  const totalUnpaid = list.filter(i => !i.isPaid).reduce((s, i) => s + (Number(i.amount) || 0), 0)

  return (
    <>
      <div className="page-header">
        <h1>Facturi</h1>
        <button className="btn btn-primary" onClick={() => open()}>+ Factură nouă</button>
      </div>
      <div className="page-body">
        <div className="stat-grid" style={{ marginBottom: 20 }}>
          <div className="stat-card green"><div className="stat-label">Total încasat</div><div className="stat-value">{totalPaid.toFixed(0)} RON</div></div>
          <div className="stat-card rose"><div className="stat-label">Total neplatit</div><div className="stat-value">{totalUnpaid.toFixed(0)} RON</div></div>
          <div className="stat-card blue"><div className="stat-label">Total facturi</div><div className="stat-value">{list.length}</div></div>
        </div>
        <div className="card">
          <div className="toolbar">
            <div style={{ display: 'flex', gap: 8 }}>
              {[['all', 'Toate'], ['unpaid', 'Neplatite'], ['paid', 'Platite']].map(([v, l]) => (
                <button key={v} onClick={() => setFilterPaid(v)}
                  style={{ padding: '6px 14px', borderRadius: 20, border: 'none', cursor: 'pointer', fontSize: 13,
                    background: filterPaid === v ? 'var(--primary)' : 'var(--surface-hover)',
                    color: filterPaid === v ? '#fff' : 'var(--text-secondary)', fontWeight: 600 }}>
                  {l}
                </button>
              ))}
            </div>
            <span style={{ color: 'var(--text-secondary)', fontSize: 13 }}>{filtered.length} facturi</span>
          </div>
          {loading ? <div className="spinner-wrap"><div className="spinner" /></div> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Nr.</th><th>Proiect</th><th>Sumă</th><th>Tip</th><th>Emitere</th><th>Scadență</th><th>Status</th><th>Acțiuni</th></tr></thead>
                <tbody>
                  {filtered.length === 0 && <tr><td colSpan={8} style={{ textAlign: 'center', color: 'var(--text-hint)', padding: 40 }}>Nicio factură</td></tr>}
                  {filtered.map(inv => (
                    <tr key={inv.id}>
                      <td><strong>{inv.invoiceNumber || `#${inv.id}`}</strong></td>
                      <td>{inv.projectName || '—'}</td>
                      <td><strong>{Number(inv.amount || 0).toFixed(2)} RON</strong></td>
                      <td>{inv.type || '—'}</td>
                      <td>{inv.date || '—'}</td>
                      <td>{inv.dueDate || '—'}</td>
                      <td>
                        {inv.isPaid
                          ? <span className="badge badge-green">Platită</span>
                          : <span className="badge badge-rose">Neplatită</span>}
                      </td>
                      <td style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                        {!inv.isPaid && <button className="btn btn-success btn-sm" onClick={() => markPaid(inv)}>Platit</button>}
                        <button className="btn btn-secondary btn-sm" onClick={() => open(inv)}>Editează</button>
                        <button className="btn btn-danger btn-sm" onClick={() => del(inv.id)}>Șterge</button>
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
              <h2>{modal === 'add' ? 'Factură nouă' : 'Editează factură'}</h2>
              <button className="modal-close" onClick={() => setModal(null)}>✕</button>
            </div>
            <div className="modal-body">
              <div className="form-row">
                <div className="form-group"><label>Nr. factură</label>
                  <input value={form.invoiceNumber || ''} onChange={e => setForm(f => ({ ...f, invoiceNumber: e.target.value }))} /></div>
                <div className="form-group"><label>Tip</label>
                  <input value={form.type || ''} placeholder="Servicii, Produse..." onChange={e => setForm(f => ({ ...f, type: e.target.value }))} /></div>
              </div>
              <div className="form-group"><label>Proiect</label>
                <select value={form.projectId} onChange={e => pickProject(e.target.value)}>
                  <option value="">— Selectează proiect —</option>
                  {projects.map(p => <option key={p.id} value={p.id}>{p.name}</option>)}
                </select></div>
              <div className="form-group"><label>Sumă (RON)</label>
                <input type="number" value={form.amount} onChange={e => setForm(f => ({ ...f, amount: e.target.value }))} /></div>
              <div className="form-row">
                <div className="form-group"><label>Data emitere</label>
                  <input type="date" value={form.date || ''} onChange={e => setForm(f => ({ ...f, date: e.target.value }))} /></div>
                <div className="form-group"><label>Data scadență</label>
                  <input type="date" value={form.dueDate || ''} onChange={e => setForm(f => ({ ...f, dueDate: e.target.value }))} /></div>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <input type="checkbox" id="isPaid" checked={Boolean(form.isPaid)}
                  onChange={e => setForm(f => ({ ...f, isPaid: e.target.checked }))} />
                <label htmlFor="isPaid" style={{ margin: 0, cursor: 'pointer' }}>Factură platită</label>
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

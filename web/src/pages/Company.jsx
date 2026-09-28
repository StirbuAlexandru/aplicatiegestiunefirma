import React, { useEffect, useState } from 'react'
import client from '../api/client'

export default function Company() {
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [form, setForm] = useState({ name: '', address: '', taxId: '', regCom: '', iban: '', bank: '', phone: '', email: '' })

  useEffect(() => {
    client.get('/company').then(r => {
      if (r.data && Object.keys(r.data).length > 0) setForm(r.data)
    }).catch(() => {}).finally(() => setLoading(false))
  }, [])

  const set = (k, v) => setForm(f => ({ ...f, [k]: v }))

  const save = async () => {
    setSaving(true)
    try {
      await client.post('/company', form)
      setSaved(true)
      setTimeout(() => setSaved(false), 2500)
    } catch (e) {
      const d = e.response?.data?.detail
      alert(Array.isArray(d) ? d.map(x => x.msg || JSON.stringify(x)).join('\n') : (d || 'Eroare la salvare'))
    }
    setSaving(false)
  }

  if (loading) return <div className="page-body"><div className="spinner-wrap"><div className="spinner" /></div></div>

  return (
    <>
      <div className="page-header">
        <h1>Informații firmă</h1>
        <button className="btn btn-primary" onClick={save} disabled={saving}>
          {saving ? 'Se salvează...' : saved ? '✓ Salvat!' : 'Salvează'}
        </button>
      </div>
      <div className="page-body">
        <div className="card">
          <h2 style={{ fontSize: 15, fontWeight: 700, marginBottom: 20 }}>Date firmă</h2>
          <div className="form-grid">
            <div className="form-group"><label>Denumire firmă</label>
              <input value={form.name || ''} onChange={e => set('name', e.target.value)} /></div>
            <div className="form-group"><label>CUI / CIF</label>
              <input value={form.taxId || ''} onChange={e => set('taxId', e.target.value)} /></div>
            <div className="form-group"><label>Nr. Registrul Comerțului</label>
              <input value={form.regCom || ''} onChange={e => set('regCom', e.target.value)} /></div>
            <div className="form-group"><label>Adresă sediu</label>
              <input value={form.address || ''} onChange={e => set('address', e.target.value)} /></div>
            <div className="form-group"><label>Email</label>
              <input type="email" value={form.email || ''} onChange={e => set('email', e.target.value)} /></div>
            <div className="form-group"><label>Telefon</label>
              <input value={form.phone || ''} onChange={e => set('phone', e.target.value)} /></div>
            <div className="form-group"><label>IBAN</label>
              <input value={form.iban || ''} onChange={e => set('iban', e.target.value)} /></div>
            <div className="form-group"><label>Bancă</label>
              <input value={form.bank || ''} onChange={e => set('bank', e.target.value)} /></div>
          </div>
        </div>
      </div>
    </>
  )
}

import { addIncome } from '../services/incomeService'
import { useState, useEffect } from 'react'

export default function AddIncomeModal({ onClose, onAdd, categoriesList }) {
  const [amount, setAmount] = useState('')
  const [date, setDate] = useState(new Date().toISOString().split('T')[0])
  const [category, setCategory] = useState('')
  const [creditedTo, setCreditedTo] = useState('')
  const [note, setNote] = useState('')

  useEffect(() => {
    function handleKeyDown(e) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [onClose])

  async function handleSubmit(e) {
    e.preventDefault()
    const newIncome = {
      amount: Number(amount),
      date,
      category_id: Number(category),
      credited_to: creditedTo,
      note,
    }
    await addIncome(newIncome)
    onAdd()
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <span>Add Income</span>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="modal-body">
          <form onSubmit={handleSubmit}>
            <input type="number" placeholder="Amount" value={amount}
              onChange={e => setAmount(e.target.value)} required />
            <input type="date" value={date}
              onChange={e => setDate(e.target.value)} required />
            <select value={creditedTo} onChange={e => setCreditedTo(e.target.value)} required>
              <option value="">Credited To</option>
              <option value="Bank Account">Bank Account</option>
              <option value="Cash">Cash</option>
            </select>
            <select value={category} onChange={e => setCategory(e.target.value)} required>
              <option value="">Select Category</option>
              {categoriesList.map(cat => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
            <textarea placeholder="Note (optional)" value={note}
              onChange={e => setNote(e.target.value)} />
            <button type="submit" className="btn btn-primary">Add Income</button>
          </form>
        </div>
      </div>
    </div>
  )
}

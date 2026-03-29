import { getCategories } from '../services/categoriesService'
import { addExpense } from '../services/expenseService'
import { useState, useEffect } from 'react'

export default function AddExpenseModal({ onClose, onAdd }) {
  const [title, setTitle] = useState('')
  const [amount, setAmount] = useState('')
  const [date, setDate] = useState('')
  const [category, setCategory] = useState('')
  const [note, setNote] = useState('')
  const [categories, setCategories] = useState([])

  useEffect(() => {
    async function fetchCategories() {
      const data = await getCategories()
      setCategories(data)
    }
    fetchCategories()

    function handleKeyDown(e) {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [])

  async function handleSubmit(e) {
    e.preventDefault()
    const newExpense = {
      title,
      amount: Number(amount),
      date,
      category_id: Number(category),
      note,
    }
    await addExpense(newExpense)
    onAdd()
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>

        <div className="modal-header">
          <span>Add Expense</span>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="modal-body">
          <form onSubmit={handleSubmit}>
            <input type="text" placeholder="Title" value={title}
              onChange={e => setTitle(e.target.value)} required />
            <input type="number" placeholder="Amount" value={amount}
              onChange={e => setAmount(e.target.value)} required />
            <input type="date" value={date}
              onChange={e => setDate(e.target.value)} required />
            <select value={category} onChange={e => setCategory(e.target.value)} required>
              <option value="">Select Category</option>
              {categories.map(cat => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
            <textarea placeholder="Note (optional)" value={note}
              onChange={e => setNote(e.target.value)} />
            <button type="submit" className="btn btn-primary">Add Expense</button>
          </form>
        </div>

      </div>
    </div>
  )
}
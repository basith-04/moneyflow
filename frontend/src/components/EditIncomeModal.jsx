import { updateIncome } from "../services/incomeService"
import { useState } from "react"

export default function EditIncomeModal(props) {
  const [amount, setAmount] = useState(props.updatedIncome.amount)
  const [date, setDate] = useState(props.updatedIncome.date.slice(0, 10))
  const [category, setCategory] = useState(props.updatedIncome.category_id)
  const [creditedTo, setCreditedTo] = useState(props.updatedIncome.credited_to)
  const [note, setNote] = useState(props.updatedIncome.note ?? '')

  async function handleSubmit(e) {
    e.preventDefault()
    const updatedIncome = {
      amount: Number(amount),
      date,
      category_id: category,
      credited_to: creditedTo,
      note,
    }
    props.onClose()
    const result = await updateIncome(props.updatedIncome.income_id, updatedIncome)
    if (result) props.onEdit()
  }

  return (
    <div className="modal-overlay" onClick={props.onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <span>Edit Income</span>
          <button className="modal-close" onClick={props.onClose}>✕</button>
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
              {props.categories.map(cat => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
              ))}
            </select>
            <textarea placeholder="Note (optional)" value={note}
              onChange={e => setNote(e.target.value)} />
            <button type="submit" className="btn btn-primary">Save Changes</button>
          </form>
        </div>
      </div>
    </div>
  )
}

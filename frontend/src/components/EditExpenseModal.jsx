import { updateExpense } from "../services/expenseService"
import { useState,useEffect } from "react"
import { getCategories } from "../services/categoriesService";
export default function EditExpenseModal(props) {
    const [title, setTitle] = useState(props.updatedExpense.title)
    const [amount, setAmount] = useState(props.updatedExpense.amount)
    const [date, setDate] = useState(props.updatedExpense.date.slice(0,10))
    const [category, setCategory] = useState(props.updatedExpense.category_id)
    const [note, setNote] = useState(props.updatedExpense.note)
    const categories=props.categories
   

    async function handleSubmit(e) {
        e.preventDefault()
        const updatedExpense = {
            title,
            amount: Number(amount),
            date,
            category_id: category,
            note,
        }
        props.onClose()
        await updateExpense(props.updatedExpense.expense_id, updatedExpense)

    }
    return <div className="modal-overlay" onClick={props.onClose}>
  <div className="modal" onClick={e => e.stopPropagation()}>

    <div className="modal-header">
      <span>Edit Expense</span>
      <button className="modal-close" onClick={props.onClose}>✕</button>
    </div>

    <div className="modal-body">
      <form onSubmit={handleSubmit}>
        <input
          type="text"
          placeholder="Title"
          value={title}
          onChange={e => setTitle(e.target.value)}
        />

        <input
          type="number"
          placeholder="Amount"
          value={amount}
          onChange={e => setAmount(e.target.value)}
        />

        <input
          type="date"
          value={date}
          onChange={e => setDate(e.target.value)}
        />

        <select
          value={category}
          onChange={e => setCategory(e.target.value)}
          
        >
          <option value={props.updatedExpense.name}>Select Category</option>
          {categories.map(cat => (
            <option key={cat.id} value={cat.id}>{cat.name}</option>
          ))}
        </select>

        <textarea
          placeholder="Note (optional)"
          value={note}
          onChange={e => setNote(e.target.value)}
        />
        {console.log(category,props.updatedExpense.category_id)}
        <button type="submit" className="btn btn-primary">
          Save Changes
        </button>
      </form>
    </div>

  </div>
</div>
}
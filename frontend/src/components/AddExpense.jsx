import { getCategories } from "../services/categoriesService";
import { addExpense } from "../services/expenseService";
import {useState,useEffect} from "react"
export default function AddExpense({ onAdd }) {
    const [title, setTitle] = useState("");
    const [amount, setAmount] = useState("");
    const [date, setDate] = useState("");
    const [category, setCategory] = useState("");
    const [note, setNote] = useState("");
    const [categories, setCategories] = useState([]);
    useEffect(() => {
        async function fetchCategories() {
            const data = await getCategories();
            setCategories(data);
        }

        fetchCategories();
    }, []);
    async function handleSubmit(e) {
    e.preventDefault();

    const newExpense = {
      title,
      amount: Number(amount),
      date,
      category_id:Number(category),
      note,
    };

    console.log("Submitted:", newExpense);
    await addExpense(newExpense)
    onAdd()

    // reset form
    setTitle("");
    setAmount("");
    setDate("");
    setCategory("");
    setNote("");
  }

    return <div>
        <form onSubmit={handleSubmit}>
            <h2>Add Expense</h2>

            <input
                type="text"
                placeholder="Title"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
            />

            <input
                type="number"
                placeholder="Amount"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
            />

            <input
                type="date"
                value={date}
                onChange={(e) => setDate(e.target.value)}
            />

            <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
            >
                <option value="">Select Category</option>
                {categories.map((cat, index) => (
                    <option key={cat.id} value={cat.id}>
                        {cat.name}
                    </option>
                ))}
            </select>

            <textarea
                placeholder="Note (optional)"
                value={note}
                onChange={(e) => setNote(e.target.value)}
            />

            <button type="submit">Add Expense</button>
        </form>
    </div>

}
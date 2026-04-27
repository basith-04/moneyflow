import { getExpenses } from "../services/expenseService"
import { useState, useEffect } from "react"
import ExpenseList from "../components/ExpenseList";
import AddExpenseModal from "../components/AddExpenseModal";
import FilterExpenses from "../components/FilterExpenses";
import EditExpenseModal from "../components/EditExpenseModal";
import { useOutletContext } from "react-router-dom"
export default function Expenses() {
    const [isModalOpen, setIsModalOpen] = useState(false)
    const [editingExpense, setEditingExpense] = useState(false);
    const [updatedExpense, setUpdatedExpense] = useState({});
    const { expenses, setExpenses,fetchData } = useOutletContext()
    return <div><h1>Expenses</h1>
        <FilterExpenses onFilter={fetchData} />
        {isModalOpen && (
            <AddExpenseModal
                onClose={() => setIsModalOpen(false)}
                onAdd={() => { fetchData(); setIsModalOpen(false) }}
            />
        )}
        {editingExpense && (
            <EditExpenseModal
                onClose={() => setEditingExpense(false)}
                updatedExpense={updatedExpense}
            />
        )}
        {/* <AddExpense onAdd={fetchData} /> */}
        <ExpenseList setUpdatedExpense={setUpdatedExpense} setEditingExpense={setEditingExpense} onDelete={fetchData} expenses={expenses} />
        <button className="btn addExpenseBtn" onClick={() => setIsModalOpen(true)}>
            +
        </button>

    </div>
}
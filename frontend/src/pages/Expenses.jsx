import { useState } from "react"
import ExpenseList from "../components/ExpenseList";
import AddExpenseModal from "../components/AddExpenseModal";
import FilterExpenses from "../components/FilterExpenses";
import EditExpenseModal from "../components/EditExpenseModal";
import { useOutletContext } from "react-router-dom"
export default function Expenses() {
    const [isModalOpen, setIsModalOpen] = useState(false)
    const [editingExpense, setEditingExpense] = useState(false);
    const [updatedExpense, setUpdatedExpense] = useState({});
    const { expenses, fetchData, expenseFilters, filterExpenses, categories } = useOutletContext()
    const totalAmount = expenses.reduce((total, expense) => total + Number(expense.amount), 0)

    return <div><h1>Expenses</h1>
        <div className="stat-cards expense-stat-cards">
            <div className="card stat-card">
                <p className="stat-label">Total Expenses</p>
                <h3 className="stat-value">{expenses.length}</h3>
            </div>
            <div className="card stat-card">
                <p className="stat-label">Total Amount</p>
                <h3 className="stat-value">₹{totalAmount.toFixed(2)}</h3>
            </div>
        </div>
        <FilterExpenses onFilter={filterExpenses} filters={expenseFilters} categories={categories} />
        {isModalOpen && (
            <AddExpenseModal
                onClose={() => setIsModalOpen(false)}
                onAdd={() => { fetchData(); setIsModalOpen(false) }}
                categoriesList={categories}
            />
        )}
        {editingExpense && (
            <EditExpenseModal
                onClose={() => setEditingExpense(false)}
                onEdit={fetchData}
                updatedExpense={updatedExpense}
                categories={categories}
            />
        )}
        <ExpenseList setUpdatedExpense={setUpdatedExpense} setEditingExpense={setEditingExpense} onDelete={fetchData} expenses={expenses} />
        <button className="btn addExpenseBtn" onClick={() => setIsModalOpen(true)}>
            +
        </button>

    </div>
}

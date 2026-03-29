import { getExpenses } from "../services/expenseService"
import { useState, useEffect } from "react"
import ExpenseList from "../components/ExpenseList";
import AddExpenseModal from "../components/AddExpenseModal";

export default function Expenses() {
    const [expenses, setExpenses] = useState([])
    const [isModalOpen, setIsModalOpen] = useState(false)
    useEffect(() => {


        fetchData();
    }, []);
    async function fetchData() {
        const data = await getExpenses({});
        setExpenses(data);
    }
    return <div><h1>Expenses</h1>
        {isModalOpen && (
            <AddExpenseModal
                onClose={() => setIsModalOpen(false)}
                onAdd={() => { fetchData(); setIsModalOpen(false) }}
            />
        )}
        {/* <AddExpense onAdd={fetchData} /> */}
        <ExpenseList onDelete={fetchData} expenses={expenses} />
        <button className="btn addExpenseBtn" onClick={() => setIsModalOpen(true)}>
            + 
        </button>

    </div>
}
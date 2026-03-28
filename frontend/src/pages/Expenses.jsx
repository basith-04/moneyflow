import { getExpenses } from "../services/expenseService"
import { useState, useEffect } from "react"
import ExpenseList from "../components/ExpenseList";
import AddExpense from "../components/AddExpense";

export default function Expenses() {
    const [expenses, setExpenses] = useState([])
    useEffect(() => {


        fetchData();
    }, []);
    async function fetchData() {
        const data = await getExpenses({});
        setExpenses(data);
    }
    return <div><h1>Expenses</h1>
        <AddExpense onAdd={fetchData} />
        <ExpenseList onDelete={fetchData} expenses={expenses} />

    </div>
}
import { useEffect, useState } from "react"
import { getExpenses } from "../services/expenseService"

export default function Dashboard() {
    const [expenses, setExpenses] = useState([])
    useEffect(() => {
        fetchData();

    }, []);
    async function fetchData() {
        const data = await getExpenses({});
        setExpenses(data);
    }
    function thisMonthExpenses() {
    const now =new Date()
    const currentMonth = now.getMonth()
    const currentYear = now.getFullYear()
    return expenses.filter(el => {
        const d = new Date(el.date)
        return d.getMonth() === currentMonth && d.getFullYear() === currentYear
    })
}
    function totalSpend() {
        if (expenses.length === 0) return 0
        const thisMonthAmounts = thisMonthExpenses()
        if (thisMonthAmounts.length === 0) return 0;
        return thisMonthAmounts.reduce((total, currentEl) => {
            return total + Number(currentEl.amount)
        }, 0)


    }
    function biggestSingleExpense() {
        if (expenses.length === 0) return 0;

        const thisMonthAmounts = thisMonthExpenses()
            .map(el => Number(el.amount));

        if (thisMonthAmounts.length === 0) return 0;

        return Math.max(...thisMonthAmounts);
    }

    function topSpendingCategory() {
        if (expenses.length === 0) return "N/A"
        let obj = {}
        thisMonthExpenses().forEach((el) => {
            obj[el.name] = (obj[el.name] || 0) + Number(el.amount)

        })

        return Object.keys(obj).reduce((highest, currentEl) => {
            return obj[currentEl] > obj[highest] ? currentEl : highest
        }, Object.keys(obj)[0])


    }
    function noOfTransactions() {

        return thisMonthExpenses().length
    }


    return (<div className="page-content">

    <div className="page-header">
      <h2>Dashboard</h2>
    </div>

    <div className="stat-cards">

      <div className="card stat-card">
        <p className="stat-label">Total Spent</p>
        <h3 className="stat-value">₹{totalSpend().toFixed(2)}</h3>
      </div>

      <div className="card stat-card">
        <p className="stat-label">Biggest Expense</p>
        <h3 className="stat-value">₹{biggestSingleExpense().toFixed(2)}</h3>
      </div>

      <div className="card stat-card">
        <p className="stat-label">Transactions</p>
        <h3 className="stat-value">{noOfTransactions()}</h3>
      </div>

      <div className="card stat-card">
        <p className="stat-label">Top Category</p>
        <h3 className="stat-value">{topSpendingCategory() || '—'}</h3>
      </div>

    </div>
  </div>
)
}


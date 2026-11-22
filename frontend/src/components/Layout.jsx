import { Outlet } from "react-router-dom"
import SideBar from "./SideBar"
import Header from "./Header"
import { getExpenses } from "../services/expenseService"
import { getCategories } from "../services/categoriesService"
import { useState, useEffect, useRef, useCallback } from "react"
function Layout() {
  const [expenses, setExpenses] = useState([])
  const [categories, setCategories] = useState([])
  const [incomeCategories, setIncomeCategories] = useState([])
  const [expenseFilters, setExpenseFilters] = useState({})
  const expenseRequest = useRef({ filters: {} })

  const fetchData = useCallback(async (filters = expenseRequest.current.filters) => {
    const request = { filters }
    expenseRequest.current = request
    const data = await getExpenses(filters)

    if (expenseRequest.current === request && data) {
      setExpenses(data)
    }
  }, [])

  function filterExpenses(filters) {
    setExpenseFilters(filters)
    fetchData(filters)
  }

  const fetchCategories = useCallback(async () => {
    const data = await getCategories()
    setCategories(data)
  }, [])

  const fetchIncomeCategories = useCallback(async () => {
    const data = await getCategories('income')
    if (data) setIncomeCategories(data)
  }, [])

  useEffect(() => {
    fetchData()
    fetchCategories()
  }, [fetchData, fetchCategories])

  return (
    <div className="app-shell">
      <SideBar />
      <div className="main-area">
        <Header />
        <main className="page-content">
          <Outlet context={{ expenses, setExpenses, fetchData, expenseFilters, filterExpenses, categories, setCategories, fetchCategories, incomeCategories, fetchIncomeCategories }} />
        </main>
      </div>
    </div>
  )
}
export default Layout

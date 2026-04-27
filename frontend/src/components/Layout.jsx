import { Outlet } from "react-router-dom"
import SideBar from "./SideBar"
import Header from "./Header"
import { getExpenses } from "../services/expenseService"
import { getCategories } from "../services/categoriesService"
import { useState, useEffect } from "react"
function Layout() {
  const [expenses, setExpenses] = useState([])
  const [categories, setCategories] = useState([])

  useEffect(() => {


    fetchData();
  }, []);
  async function fetchData(filters = {}) {
    const data = await getExpenses(filters);
    setExpenses(data);
  }
  useEffect(() => {

    fetchCategories()
  }, [])
  async function fetchCategories() {
    const data = await getCategories()
    setCategories(data)
  }


  return (
    <div className="app-shell">
      <SideBar />
      <div className="main-area">
        <Header />
        <main className="page-content">
          <Outlet context={{ expenses, setExpenses, fetchData,categories,setCategories,fetchCategories }} />
        </main>
      </div>
    </div>
  )
}
export default Layout

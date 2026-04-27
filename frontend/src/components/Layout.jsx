import { Outlet } from "react-router-dom"
import SideBar from "./SideBar"
import Header from "./Header"
import { getExpenses } from "../services/expenseService"
import { useState,useEffect } from "react"
function Layout() {
  const [expenses, setExpenses] = useState([])

  useEffect(() => {


    fetchData();
  }, []);
  async function fetchData(filters = {}) {
    const data = await getExpenses(filters);
    setExpenses(data);
  }

  return (
    <div className="app-shell">
      <SideBar />
      <div className="main-area">
        <Header />
        <main className="page-content">
          <Outlet context={{expenses,setExpenses,fetchData}}/>
        </main>
      </div>
    </div>
  )
}
export default Layout

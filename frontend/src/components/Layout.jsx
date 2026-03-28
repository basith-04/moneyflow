import { Outlet } from "react-router-dom"
import SideBar from "./SideBar"
import Header from "./Header"
function Layout() {
  return (
    <div className="app-shell">
      <SideBar />
      <div className="main-area">
        <Header />
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
export default Layout

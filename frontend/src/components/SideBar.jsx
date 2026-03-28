import { NavLink } from "react-router-dom";

export default function SideBar(props) {
    return <aside>
        <div className="sidebar-brand">
            <h1>MoneyFlow</h1>
        </div>
        <nav>
            <ul role="list">
                <li><NavLink to="/" end>Dashboard</NavLink></li>
                <li><NavLink to="/expenses">Expenses</NavLink></li>
                <li><NavLink to="/categories">Categories</NavLink></li>
            </ul>
        </nav>
    </aside>



}
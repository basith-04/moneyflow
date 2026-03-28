import { Routes, Route } from "react-router-dom";
import Expenses from './pages/Expenses';
import Categories from './pages/Categories';
import Layout from './components/Layout.jsx'
import "./index.css"
import Dashboard from "./pages/Dashboard.jsx";
function App() {
  return <div>

    <Routes>
      <Route path='/' element={<Layout />}>
        <Route path='/' element={<Dashboard />} />
        <Route path='expenses' element={<Expenses />} />
        <Route path='categories' element={<Categories />} />
      </Route>
    </Routes>

  </div>
}

export default App

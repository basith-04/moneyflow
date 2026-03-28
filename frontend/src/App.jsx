import { useState } from 'react'
import { Routes , Route} from "react-router-dom";
import Expenses from './pages/Expenses';
import Categories from './pages/Categories';
import "./index.css"
function App() {
  return <div> 
    
    <Routes>
      <Route path='/' element ={<Expenses />}/>
      <Route path='/categories' element ={<Categories />}/>
      
    </Routes>

  </div>
}

export default App

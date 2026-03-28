import { useState } from 'react'
import { Routes , Route,BrowserRouter } from "react-router-dom";
import Expenses from './pages/Expenses';
import Categories from './pages/Categories';
import "./index.css"
function App() {
  return  <BrowserRouter> <div> 
    
    <Routes>
      <Route path='/' element ={<Expenses />}/>
      <Route path='/categories' element ={<Categories />}/>
      
    </Routes>

  </div>
  </BrowserRouter>
}

export default App

import { useState } from "react"
import AddCategory from "../components/AddCategory"
import CategoryList from "../components/CategoryList"
import { useOutletContext } from "react-router-dom"

export default function Categories(){
    const [type, setType] = useState('expense')
    const { categories, fetchCategories, incomeCategories, fetchIncomeCategories } = useOutletContext()
    const categoryList = type === 'income' ? incomeCategories : categories
    const fetchList = type === 'income' ? fetchIncomeCategories : fetchCategories
    
    return <div>
        <div className="category-tabs">
            <button type="button" aria-pressed={type === 'expense'}
                className={type === 'expense' ? 'filter-btn' : 'filter-btn filter-clear-btn'}
                onClick={() => setType('expense')}>
                Expense Categories
            </button>
            <button type="button" aria-pressed={type === 'income'}
                className={type === 'income' ? 'filter-btn' : 'filter-btn filter-clear-btn'}
                onClick={() => { setType('income'); fetchIncomeCategories() }}>
                Income Categories
            </button>
        </div>
        <AddCategory key={type} onAdd={fetchList} type={type} />
        <CategoryList categories={categoryList} onDelete={fetchList} type={type} />
    </div>

}

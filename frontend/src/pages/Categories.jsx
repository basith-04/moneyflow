import { useEffect, useState } from "react"
import AddCategory from "../components/AddCategory"
import CategoryList from "../components/CategoryList"
import { useOutletContext } from "react-router-dom"

export default function Categories(){
    const {categories,setCategories,fetchCategories}=useOutletContext()
    
    return <div>
            <AddCategory onAdd={fetchCategories} />
            <CategoryList categories={categories} onDelete={fetchCategories} />
    </div>

}
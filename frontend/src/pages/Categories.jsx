import { useEffect, useState } from "react"
import { getCategories } from "../services/categoriesService"
import AddCategory from "../components/AddCategory"
import CategoryList from "../components/CategoryList"

export default function Categories(){
    const [categories,setCategories]=useState([])
    useEffect(()=>{

        fetchCategories()
    },[])
    async function fetchCategories(){
        const data= await getCategories()
        setCategories(data)
    }
    
    return <div>
            <AddCategory onAdd={fetchCategories} />
            <CategoryList categories={categories} onDelete={fetchCategories} />
    </div>

}
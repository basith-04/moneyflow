import {useState,useEffect} from "react"
import { addCategories } from "../services/categoriesService";
export default function AddCategory({ onAdd }) {
    const [name, setName] = useState("");
    
    async function handleSubmit(e) {
    e.preventDefault();

    const newCategory = {
     name    };

    console.log("Submitted:", newCategory);
    await addCategories(newCategory)
    onAdd()

    // reset form
    setName("");

  }

    return <div> 
    <h2 > Add Expense</h2>
        <form onSubmit={handleSubmit} >
            

            <input
                type="text"
                placeholder="Title"
                value={name}
                onChange={(e) => setName(e.target.value)}
            />

            <button type="submit">Add Category</button>
        </form>
    </div>

}
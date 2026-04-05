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

    return <div className="add-expense-container"> 
    <h2 className="add-expense-title">Add Category</h2>
    <form onSubmit={handleSubmit} className="add-expense-form">
        <input
            type="text"
            placeholder="Title"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="add-expense-input" required
        />

        <button type="submit" className="add-expense-btn">Add Category</button>
    </form>
</div>

}
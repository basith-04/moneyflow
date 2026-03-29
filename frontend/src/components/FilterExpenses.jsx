import { useState, useEffect } from "react";
import { getCategories } from "../services/categoriesService";
export default function FilterExpenses(props) {
    const [minAmount, setMinAmount] = useState("");
    const [maxAmount, setMaxAmount] = useState("");
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [category, setCategory] = useState("");
    const [categories, setCategories] = useState([]);

    useEffect(() => {
        async function fetchCategories() {
            const data = await getCategories();
            setCategories(data);
        }
        fetchCategories();
    }, []);

    function handleSubmit(e) {
        e.preventDefault();

        props.onFilter({ min: minAmount, max: maxAmount, from: startDate, to: endDate, category_id: category });
    }

    return <div>
        <h1>Filter Expenses</h1>
        <form onSubmit={handleSubmit}>
            <input type="text" placeholder="min" value={minAmount}
                onChange={e => setMinAmount(e.target.value)} />

            <input type="text" placeholder="max" value={maxAmount}
                onChange={e => setMaxAmount(e.target.value)} />

            <input type="date" value={startDate}
                onChange={e => setStartDate(e.target.value)} />

            <input type="date" value={endDate}
                onChange={e => setEndDate(e.target.value)} />
                
            <select value={category} onChange={e => setCategory(e.target.value)}>
                <option value="">Select Category</option>
                {categories.map(cat => (
                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
            </select>
            <button type="submit" className="btn btn-primary">Apply Filters</button>

        </form>
    </div>
}
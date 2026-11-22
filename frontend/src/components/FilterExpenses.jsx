import { useState } from "react";
import { paymentModes } from "../paymentModes";
export default function FilterExpenses(props) {
    const { filters } = props;
    const [minAmount, setMinAmount] = useState(filters.min ?? "");
    const [maxAmount, setMaxAmount] = useState(filters.max ?? "");
    const [startDate, setStartDate] = useState(filters.from ?? "");
    const [endDate, setEndDate] = useState(filters.to ?? "");
    const [category, setCategory] = useState(filters.category_id ?? "");
    const [modeOfPayment, setModeOfPayment] = useState(filters.payment_mode ?? "");
    const categories=props.categories

    

    function handleSubmit(e) {
        e.preventDefault();

        props.onFilter({ min: minAmount, max: maxAmount, from: startDate, to: endDate, category_id: category, payment_mode: modeOfPayment });
    }

    return <div className="filter-container">
    <h1 className="filter-title">Filter Expenses</h1>
    <form onSubmit={handleSubmit} className="filter-form">
        <input 
            type="text" 
            placeholder="min" 
            value={minAmount}
            onChange={e => setMinAmount(e.target.value)} 
            className="filter-input"
        />

        <input 
            type="text" 
            placeholder="max" 
            value={maxAmount}
            onChange={e => setMaxAmount(e.target.value)} 
            className="filter-input"
        />

        <input 
            type="date" 
            value={startDate}
            onChange={e => setStartDate(e.target.value)} 
            className="filter-input"
        />

        <input 
            type="date" 
            value={endDate}
            onChange={e => setEndDate(e.target.value)} 
            className="filter-input"
        />
            
        <select 
            value={category} 
            onChange={e => setCategory(e.target.value)}
            className="filter-input"
        >
            <option value="">Select Category</option>
            {categories.map(cat => (
                <option key={cat.id} value={cat.id}>{cat.name}</option>
            ))}
        </select>

        <select
            aria-label="Mode of payment"
            value={modeOfPayment}
            onChange={e => setModeOfPayment(e.target.value)}
            className="filter-input"
        >
            <option value="">All Payment Modes</option>
            {paymentModes.map(mode => (
                <option key={mode} value={mode}>{mode}</option>
            ))}
        </select>
        
        <button type="submit" className="filter-btn">Apply Filters</button>
    </form>
</div>
}

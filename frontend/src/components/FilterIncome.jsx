import { useState } from "react";

export default function FilterIncome(props) {
    const [minAmount, setMinAmount] = useState("");
    const [maxAmount, setMaxAmount] = useState("");
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [category, setCategory] = useState("");
    const [creditedTo, setCreditedTo] = useState("");

    function handleSubmit(e) {
        e.preventDefault();
        props.onFilter({ min: minAmount, max: maxAmount, from: startDate, to: endDate, category_id: category, credited_to: creditedTo });
    }

    function handleClear() {
        setMinAmount("");
        setMaxAmount("");
        setStartDate("");
        setEndDate("");
        setCategory("");
        setCreditedTo("");
        props.onFilter({});
    }

    return <div className="filter-container">
        <h1 className="filter-title">Filter Income</h1>
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
            <select value={category} onChange={e => setCategory(e.target.value)} className="filter-input">
                <option value="">Select Category</option>
                {props.categories.map(cat => (
                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                ))}
            </select>
            <select aria-label="Credited To" value={creditedTo}
                onChange={e => setCreditedTo(e.target.value)} className="filter-input">
                <option value="">Credited To</option>
                <option value="Bank Account">Bank Account</option>
                <option value="Cash">Cash</option>
            </select>

            <button type="submit" className="filter-btn">Apply Filters</button>
            <button type="button" className="filter-btn filter-clear-btn" onClick={handleClear}>Clear</button>
        </form>
    </div>
}

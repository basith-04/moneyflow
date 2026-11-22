import { useState, useRef, useCallback, useEffect } from "react"
import { useOutletContext } from "react-router-dom"
import { getIncome } from "../services/incomeService"
import IncomeList from "../components/IncomeList"
import AddIncomeModal from "../components/AddIncomeModal"
import FilterIncome from "../components/FilterIncome"
import EditIncomeModal from "../components/EditIncomeModal"

export default function Income() {
    const [income, setIncome] = useState([])
    const [isModalOpen, setIsModalOpen] = useState(false)
    const [editingIncome, setEditingIncome] = useState(false)
    const [updatedIncome, setUpdatedIncome] = useState({})
    const { incomeCategories, fetchIncomeCategories } = useOutletContext()
    const incomeRequest = useRef({ filters: {} })

    const fetchData = useCallback(async (filters = incomeRequest.current.filters) => {
        const request = { filters }
        incomeRequest.current = request
        const data = await getIncome(filters)

        if (incomeRequest.current === request && data) {
            setIncome(data)
        }
    }, [])

    useEffect(() => {
        fetchData()
        fetchIncomeCategories()
    }, [fetchData, fetchIncomeCategories])

    return <div><h1>Income</h1>
        <FilterIncome onFilter={fetchData} categories={incomeCategories} />
        {isModalOpen && (
            <AddIncomeModal
                onClose={() => setIsModalOpen(false)}
                onAdd={() => { fetchData(); setIsModalOpen(false) }}
                categoriesList={incomeCategories}
            />
        )}
        {editingIncome && (
            <EditIncomeModal
                onClose={() => setEditingIncome(false)}
                onEdit={fetchData}
                updatedIncome={updatedIncome}
                categories={incomeCategories}
            />
        )}
        <IncomeList setUpdatedIncome={setUpdatedIncome} setEditingIncome={setEditingIncome} onDelete={fetchData} income={income} />
        <button className="btn addExpenseBtn" onClick={() => setIsModalOpen(true)}>
            +
        </button>
    </div>
}

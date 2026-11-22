import IncomeItem from "./IncomeItem"

export default function IncomeList(props) {
    return <div>
        <table border={2}>
            <thead>
                <tr>
                    <td>Id</td>
                    <td>Amount</td>
                    <td>Date</td>
                    <td>Category</td>
                    <td>Credited To</td>
                    <td>Note</td>
                    <td>Edit</td>
                    <td>Delete</td>
                </tr>
            </thead>
            <tbody>
                {props.income.map((element, index) => (
                    <IncomeItem key={element.income_id} id={index}
                        setUpdatedIncome={props.setUpdatedIncome} setEditingIncome={props.setEditingIncome}
                        onDelete={props.onDelete} item={element} />
                ))}
            </tbody>
        </table>
    </div>
}

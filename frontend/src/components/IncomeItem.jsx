import { removeIncome } from "../services/incomeService"

export default function IncomeItem(props) {
    function handleClick() {
        props.setUpdatedIncome(props.item)
        props.setEditingIncome(true)
    }

    return <tr>
        <td>{props.id + 1}</td>
        <td>{props.item.amount}</td>
        <td>{props.item.date}</td>
        <td>{props.item.name}</td>
        <td>{props.item.credited_to}</td>
        <td>{props.item.note}</td>
        <td><button onClick={handleClick}>edit</button></td>
        <td><button onClick={async () => {
            await removeIncome(props.item.income_id)
            props.onDelete()
        }}>X</button></td>
    </tr>
}

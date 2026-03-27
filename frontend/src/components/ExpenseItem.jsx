export default function ExpenseItem(props) {
        return <tr>
            <td>{props.item.expense_id}</td>
            <td>{props.item.title}</td>
            <td>{props.item.amount}</td>
            <td>{props.item.date}</td>
            <td>{props.item.name}</td>
            <td>{props.item.note}</td>
        </tr>

    }
import { removeExpense } from "../services/expenseService"
export default function ExpenseItem(props) {
    function handleClick(){
        props.setUpdatedExpense(props.item)
        props.setEditingExpense(true)
    }   
        return <tr>
            <td>{props.item.expense_id}</td>
            <td>{props.item.title}</td>
            <td>{props.item.amount}</td>
            <td>{props.item.date}</td>
            <td>{props.item.name}</td>
            <td>{props.item.note}</td>
            <td><button onClick={handleClick}>edit</button></td>
            <td><button onClick={async()=>{ 
                await removeExpense(props.item.expense_id)
                props.onDelete()

            } }>X</button></td>
        </tr>

    }
import ExpenseItem from "./ExpenseItem"
export default function ExpenseList(props) {
    return <div>
        <table border={2}>
            <thead>
                <tr>
                    <td>Id</td>
                    <td>Title</td>
                    <td>Amount</td>
                    <td>date</td>
                    <td>category</td>
                    <td> note</td>
                    <td>x</td>

                </tr>
            </thead>
            <tbody>
                {props.expenses.map((element) => {
                    return <ExpenseItem key={element.expense_id}setUpdatedExpense={props.setUpdatedExpense} setEditingExpense={props.setEditingExpense} onDelete={props.onDelete} item={element} />
                })}
            </tbody>
        </table>
    </div>
}
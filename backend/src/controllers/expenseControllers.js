import pool from '../db/db.js'
async function getExpenses(req, res) {
    let query = ' select expense_id,title,amount ,DATE_FORMAT(date, "%Y-%m-%d") as date,name,category_id,note from expenses E inner join categories C on C.id = E.category_id where E.user_id=?'
    let values = []
    const user_id = req.user.userId
    values.push(user_id)

    try {

        if (req.query.min) {
            query += ' AND amount >= ?';
            values.push(req.query.min);
        }

        if (req.query.max) {
            query += ' AND amount <= ?';
            values.push(req.query.max);
        }
        if (req.query.category_id) {
            query += ' AND category_id = ?';
            values.push(req.query.category_id);
        }
        if (req.query.from) {
            query += ' AND date >= ?';
            values.push(req.query.from);
        }

        if (req.query.to) {
            query += ' AND date <= ?';
            values.push(req.query.to);
        }

        query += ' ORDER BY expense_id DESC'
        const [rows] = await pool.query(query, values)
        return res.status(200).json(rows)

    } catch (err) {
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }
}
async function addExpense(req, res) {
    console.log(req.body)
    const { title, amount, date, category_id, group_id, note } = req.body
    const user_id = req.user.userId
    if (!title || !amount || !date || !category_id) {
        return res.status(400).json({ error: "missing required fields" })
    }
    try {
        const [result] = await pool.query(
            `INSERT INTO expenses 
       (title, amount, date, category_id, group_id, note, user_id)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
            [title, amount, date, category_id, group_id || null, note || null, user_id]
        );
        res.status(201).json({ message: "Expense added", expense_id: result.insertId })

    } catch (err) {
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }

}
async function removeExpense(req, res) {

    const { expense_id } = req.params
    const user_id = req.user.userId
    if (!expense_id) {
        return res.status(400).json({ error: "missing required fields" })
    }
    try {
        const [result] = await pool.query(
            `DELETE FROM expenses WHERE expense_id = ? AND user_id = ?`,
            [expense_id, user_id]
        );
        if (result.affectedRows > 0) {
            return res.status(200).json({ message: "Expense deleted" })

        }
        else {
            return res.status(404).json({ error: "not found" })
        }

    } catch (err) {
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }


}
async function updateExpense(req, res) {
    console.log("frrr")
    const { expense_id } = req.params
    const user_id = req.user.userId
    const allowed_fields = ['title', 'amount', 'date', 'category_id', 'group_id', 'note'];
    let query = 'UPDATE expenses SET '
    let values = []
    if (!req.body) {
        return res.status(400).json({ error: "required fields are empty" })
    }
    Object.keys(req.body).forEach((key) => {
        if (allowed_fields.includes(key)) {
            if (values.length > 0) {
                query += ','
            }
            query += `${key}=? `
            values.push(req.body[key])

        }
    })
    if (values.length == 0) return res.status(400).json({ error: "invalid fields" })
    query += ' WHERE expense_id = ? AND user_id = ?';
    values.push(expense_id);
    values.push(user_id);

    try {
        const [result] = await pool.query(query, values);
        if (result.affectedRows > 0) {
            return res.status(200).json({ message: "Expense updated" })

        }
        else {
            return res.status(404).json({ error: "not found" })
        }

    } catch (err) {
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }

}
export { getExpenses, addExpense, removeExpense, updateExpense }
import pool from '../db/db.js'

async function getExpenses(req, res) {
    let query = `SELECT expense_id, title, amount, TO_CHAR(date, 'YYYY-MM-DD') AS date, name, category_id, note
                 FROM expenses E
                 INNER JOIN categories C ON C.id = E.category_id
                 WHERE E.user_id = $1`
    let values = []
    const user_id = req.user.userId
    values.push(user_id)

    try {
        if (req.query.min) {
            query += ` AND amount >= $${values.length + 1}`;
            values.push(req.query.min);
        }

        if (req.query.max) {
            query += ` AND amount <= $${values.length + 1}`;
            values.push(req.query.max);
        }

        if (req.query.category_id) {
            query += ` AND category_id = $${values.length + 1}`;
            values.push(req.query.category_id);
        }

        if (req.query.from) {
            query += ` AND date >= $${values.length + 1}`;
            values.push(req.query.from);
        }

        if (req.query.to) {
            query += ` AND date <= $${values.length + 1}`;
            values.push(req.query.to);
        }

        query += ' ORDER BY expense_id DESC'
        const result = await pool.query(query, values)
        return res.status(200).json(result.rows)

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
        const result = await pool.query(
            `INSERT INTO expenses 
       (title, amount, date, category_id, group_id, note, user_id)
       VALUES ($1, $2, $3, $4, $5, $6, $7)
       RETURNING expense_id`,
            [title, amount, date, category_id, group_id || null, note || null, user_id]
        );
        res.status(201).json({ message: "Expense added", expense_id: result.rows[0].expense_id })

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
        const result = await pool.query(
            `DELETE FROM expenses WHERE expense_id = $1 AND user_id = $2`,
            [expense_id, user_id]
        );
        if (result.rowCount > 0) {
            return res.status(200).json({ message: "Expense deleted" })
        } else {
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
    let setClauses = []
    let values = []

    if (!req.body) {
        return res.status(400).json({ error: "required fields are empty" })
    }

    Object.keys(req.body).forEach((key) => {
        if (allowed_fields.includes(key)) {
            values.push(req.body[key])
            setClauses.push(`${key} = $${values.length}`)
        }
    })

    if (values.length === 0) return res.status(400).json({ error: "invalid fields" })

    values.push(expense_id)
    const expenseParamIdx = values.length
    values.push(user_id)
    const userParamIdx = values.length

    const query = `UPDATE expenses SET ${setClauses.join(', ')} WHERE expense_id = $${expenseParamIdx} AND user_id = $${userParamIdx}`;

    try {
        const result = await pool.query(query, values);
        if (result.rowCount > 0) {
            return res.status(200).json({ message: "Expense updated" })
        } else {
            return res.status(404).json({ error: "not found" })
        }

    } catch (err) {
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }
}

export { getExpenses, addExpense, removeExpense, updateExpense }
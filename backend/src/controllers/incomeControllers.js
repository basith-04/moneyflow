import pool from '../db/db.js'

async function getIncome(req, res) {
    let query = " select income_id,amount::float as amount,to_char(date, 'YYYY-MM-DD') as date,name,I.category_id,credited_to,note from income I inner join income_categories C on C.id = I.category_id where I.user_id=$1"
    const values = [req.user.userId]

    try {
        if (req.query.min) {
            values.push(req.query.min);
            query += ` AND amount >= $${values.length}`;
        }

        if (req.query.max) {
            values.push(req.query.max);
            query += ` AND amount <= $${values.length}`;
        }

        if (req.query.category_id) {
            values.push(req.query.category_id);
            query += ` AND I.category_id = $${values.length}`;
        }

        if (req.query.credited_to) {
            values.push(req.query.credited_to);
            query += ` AND credited_to = $${values.length}`;
        }

        if (req.query.from) {
            values.push(req.query.from);
            query += ` AND date >= $${values.length}`;
        }

        if (req.query.to) {
            values.push(req.query.to);
            query += ` AND date <= $${values.length}`;
        }

        query += ' ORDER BY I.date DESC, I.income_id DESC'
        const { rows } = await pool.query(query, values)
        return res.status(200).json(rows)

    } catch (err) {
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }
}

async function addIncome(req, res) {
    const { amount, date, category_id, credited_to, note } = req.body || {}
    const user_id = req.user.userId
    if (!amount || !date || !category_id || !credited_to) {
        return res.status(400).json({ error: 'missing required fields' })
    }
    if (credited_to !== 'Bank Account' && credited_to !== 'Cash') {
        return res.status(400).json({ error: 'invalid credited to' })
    }

    try {
        const result = await pool.query(
            `INSERT INTO income
       (amount, date, category_id, credited_to, note, user_id)
       VALUES ($1, $2, $3, $4, $5, $6)
       RETURNING income_id`,
            [amount, date, category_id, credited_to, note || null, user_id]
        );
        res.status(201).json({ message: 'Income added', income_id: result.rows[0].income_id })

    } catch (err) {
        if (err.code === '23503') {
            return res.status(400).json({ error: 'invalid income category' })
        }
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }
}

async function removeIncome(req, res) {
    const { income_id } = req.params
    const user_id = req.user.userId
    if (!income_id) {
        return res.status(400).json({ error: 'missing required fields' })
    }

    try {
        const result = await pool.query(
            `DELETE FROM income WHERE income_id = $1 AND user_id = $2`,
            [income_id, user_id]
        );
        if (result.rowCount > 0) {
            return res.status(200).json({ message: 'Income deleted' })
        }
        return res.status(404).json({ error: 'not found' })

    } catch (err) {
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }
}

async function updateIncome(req, res) {
    const { income_id } = req.params
    const user_id = req.user.userId
    const allowed_fields = ['amount', 'date', 'category_id', 'credited_to', 'note'];
    let query = 'UPDATE income SET '
    const values = []
    if (!req.body) {
        return res.status(400).json({ error: 'required fields are empty' })
    }
    const { credited_to } = req.body
    if (credited_to !== undefined && credited_to !== 'Bank Account' && credited_to !== 'Cash') {
        return res.status(400).json({ error: 'invalid credited to' })
    }

    Object.keys(req.body).forEach((key) => {
        if (allowed_fields.includes(key)) {
            if (values.length > 0) {
                query += ','
            }
            values.push(req.body[key]);
            query += `${key}=$${values.length} `
        }
    })
    if (values.length === 0) return res.status(400).json({ error: 'invalid fields' });
    values.push(income_id);
    query += ` WHERE income_id = $${values.length} AND user_id = `;
    values.push(user_id);
    query += `$${values.length}`;

    try {
        const result = await pool.query(query, values);
        if (result.rowCount > 0) {
            return res.status(200).json({ message: 'Income updated' })
        }
        return res.status(404).json({ error: 'not found' })

    } catch (err) {
        if (err.code === '23503') {
            return res.status(400).json({ error: 'invalid income category' })
        }
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }
}

export { getIncome, addIncome, removeIncome, updateIncome }

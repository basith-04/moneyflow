import bcrypt from 'bcrypt'
import pool from '../db/db.js'
import jwt from 'jsonwebtoken'

async function registerUser(req, res) {
    const { fullName, userName, password, email } = req.body
    try {
        const passwordHash = await bcrypt.hash(password, 10)
        const result = await pool.query(
            `INSERT INTO users 
       (user_name, full_name, email_address, password_hash)
       VALUES ($1, $2, $3, $4)
       RETURNING user_id`,
            [userName, fullName, email, passwordHash]
        );
        const newUserId = result.rows[0].user_id
        await defaultCategories(newUserId)
        res.status(201).json({ message: "user added", user_id: newUserId })

    } catch (err) {
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }
}

async function defaultCategories(user_id) {
    const defaultCategoryList = [
        "Food & Dining",
        "Groceries",
        "Transport",
        "Recharge & Subscriptions",
        "Education",
        "College Fees",
        "Memberships",
        "Shopping",
        "Personal Care",
        "Entertainment",
        "Investments",
        "Government & Documents",
        "Miscellaneous"
    ];

    for (const name of defaultCategoryList) {
        await pool.query(
            `INSERT INTO categories (name, user_id) VALUES ($1, $2)`,
            [name, user_id]
        )
    }
}

async function loginUser(req, res) {
    const { userName, password } = req.body
    try {
        const result = await pool.query(
            `SELECT * FROM users WHERE user_name = $1`,
            [userName]
        );
        if (result.rows.length === 0) {
            return res.status(401).json({ error: 'Invalid credentials' })
        }
        const user = result.rows[0]
        const isMatch = await bcrypt.compare(password, user.password_hash)
        if (!isMatch) {
            return res.status(401).json({ error: 'Invalid credentials' })
        }

        const token = jwt.sign({ userId: user.user_id }, process.env.JWT_SECRET, { expiresIn: '5d' })
        res.status(200).json({ token })

    } catch (err) {
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }
}

export { registerUser, loginUser }
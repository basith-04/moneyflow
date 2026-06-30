import bcrypt from 'bcrypt'
import pool from '../db/db.js'
import jwt from 'jsonwebtoken'

async function registerUser(req, res) {
    const { fullName, userName, password, email } = req.body
    console.log(req.body)
    try {
        const passwordHash = await bcrypt.hash(password, 10)
        const result = await pool.query(
            `INSERT INTO users 
       (user_name,full_name,email_address,password_hash)
       VALUES ($1,$2,$3,$4)
       RETURNING user_id`,
            [userName, fullName, email, passwordHash]
        );
        const userId = result.rows[0].user_id
        await defaultCategories(userId)
        res.status(201).json({ message: "user added", user_id: userId })

    } catch (err) {
        console.error('DB ERROR', err)
        return res.status(500).json({ error: 'Internal server error' })
    }


}
async function defaultCategories(user_id) {
    const defaultCategories = [
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
    
        const placeholders = defaultCategories.map((_, index) => `($${index * 2 + 1}, $${index * 2 + 2})`).join(', ')
        const values = defaultCategories.flatMap(item => [item, user_id])
        await pool.query(`INSERT INTO categories (name, user_id) VALUES ${placeholders}`, values)
   
}
async function loginUser(req, res) {
    const { userName, password } = req.body
    try {
        const { rows } = await pool.query(
            `SELECT * FROM users WHERE user_name = $1`,
            [userName]
        );
        if (rows.length === 0) {
            return res.status(401).json({ error: 'Invalid credentials' })
        }
        const user = rows[0]
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

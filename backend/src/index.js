import dotenv from 'dotenv'
dotenv.config()
import 'dotenv/config'
import cors from 'cors';
import express from 'express'
import {expenseRouter}  from './routes/expenseRouter.js'
import { categoryRouter } from './routes/categoryRouter.js'
import { authRouter } from './routes/authRouter.js';

const app =express()
const PORT = process.env.PORT || 8000;

app.use(cors());
app.use(express.json())
app.use('/expenses',expenseRouter)
app.use('/category',categoryRouter)
app.use('/auth',authRouter)
app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running on port ${PORT}`)
}).on('error', (err) => {
  console.error('Failed to start server:', err)
}) 
import express from 'express'
import { getIncome, addIncome, removeIncome, updateIncome } from '../controllers/incomeControllers.js'
import { authMiddleware } from '../middleware/auth.js'

export const incomeRouter = express.Router()

incomeRouter.use(authMiddleware)
incomeRouter.get('/', getIncome)
incomeRouter.post('/', addIncome)
incomeRouter.delete('/:income_id', removeIncome)
incomeRouter.patch('/:income_id', updateIncome)

import express from 'express'
import {getExpenses,addExpense,removeExpense,updateExpense} from '../controllers/expenseControllers.js'
export const expenseRouter=express.Router()

expenseRouter.get('/',getExpenses)
expenseRouter.post('/',addExpense)
expenseRouter.delete('/:expense_id',removeExpense)
expenseRouter.patch('/:expense_id',updateExpense)
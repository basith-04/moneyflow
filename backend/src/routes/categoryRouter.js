import express from 'express'
import {getCategories,addCategory,removeCategory} from '../controllers/categoryControllers.js'
import { authMiddleware } from '../middleware/auth.js'
export const categoryRouter= express.Router()

categoryRouter.use(authMiddleware)
categoryRouter.get('/',getCategories)
categoryRouter.post('/',addCategory)
categoryRouter.delete('/:category_id',removeCategory)
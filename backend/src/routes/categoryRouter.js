import express from 'express'
import {getCategories,addCategory} from '../controllers/categoryControllers.js'
export const categoryRouter= express.Router()

categoryRouter.get('/',getCategories)
categoryRouter.post('/',addCategory)
import pool from '../db/db.js'

async function getCategories(req,res){
    const user_id = req.user.userId
    try{
    const { rows }=await pool.query('select * from categories where user_id = $1 order by id', [user_id])
    res.status(200).json(rows)
     }catch(err){
        console.error('DB ERROR',err)
        return res.status(500).json({error:'Internal server error'})
    }
}
async function addCategory(req, res) {

    const {name} = req.body
    const user_id = req.user.userId
    try{
    const result = await pool.query(
        `INSERT INTO categories 
       (name, user_id)
       VALUES ($1, $2)
       RETURNING id`,
        [name, user_id]
    );
    res.status(201).json({message:"category added",category_id:result.rows[0].id})

     }catch(err){
        console.error('DB ERROR',err)
        return res.status(500).json({error:'Internal server error'})
    }
}
async function removeCategory(req,res){
    const {category_id} = req.params
    const user_id = req.user.userId
    if(!category_id){
        return res.status(400).json({error:"missing required fields"})
    }
    try{
        const result = await pool.query(
            `DELETE FROM categories WHERE id = $1 AND user_id = $2`,
            [category_id, user_id]
        );
        if(result.rowCount > 0){
            return res.status(200).json({message:"Category deleted"})
        }else{
            return res.status(404).json({error:"Category not found"})
        }
     }catch(err){
        console.error('DB ERROR',err)
        return res.status(500).json({error:'Internal server error'})
    }
}

export {getCategories,addCategory,removeCategory}

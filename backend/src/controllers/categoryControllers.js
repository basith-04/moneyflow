import pool from '../db/db.js'

async function getCategories(req,res){
    try{
    const [rows]=await pool.query('select * from categories')
    res.status(200).json(rows)
     }catch(err){
        console.error('DB ERROR',err)
        return res.status(500).json({error:'Internal server error'})
    }
}
async function addCategory(req, res) {

    const {name} = req.body
    try{
    const [result] = await pool.query(
        `INSERT INTO categories 
       (name)
       VALUES (?)`,
        [name]
    );
    res.status(201).json({message:"category added",category_id:result.insertId})

     }catch(err){
        console.error('DB ERROR',err)
        return res.status(500).json({error:'Internal server error'})
    }
}
export {getCategories,addCategory}
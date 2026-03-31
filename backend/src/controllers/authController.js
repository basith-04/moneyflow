import bcrypt from 'bcrypt'
import { pool } from '../db.js'
async function registerUser(req,res){
    const {fullName,userName,password,email,}=req.body
    try{
    const passwordHash=await bcrypt.hash(password,10)    
    const [result] = await pool.query(
        `INSERT INTO users 
       (user_name,full_name,email_address,password_hash)
       VALUES (?,?,?,?)`,
        [userName, fullName, email, passwordHash]
    );
    res.status(201).json({message:"user added",user_id:result.insertId})

     }catch(err){
        console.error('DB ERROR',err)
        return res.status(500).json({error:'Internal server error'})
    }
    
    
}
async function loginUser(req,res){
    

}
export {registerUser,loginUser}
import bcrypt from 'bcrypt'
import pool from '../db/db.js'
import jwt from 'jsonwebtoken'
async function registerUser(req,res){
    const {fullName,userName,password,email}=req.body
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
    const {userName,password}=req.body
    try{
        const [rows] = await pool.query(
            `SELECT * FROM users WHERE user_name = ?`,
            [userName]
        );
        if(rows.length === 0){
            return res.status(401).json({error:'Invalid credentials'})
        }
        const user=rows[0]
        const isMatch=await bcrypt.compare(password,user.password_hash)
        if(!isMatch){
            return res.status(401).json({error:'Invalid credentials'})
        }
        
        const token=jwt.sign({userId:user.user_id},process.env.JWT_SECRET,{expiresIn:'1h'} )
        res.status(200).json({token})

    }catch(err){
        console.error('DB ERROR',err)
        return res.status(500).json({error:'Internal server error'})
    }
}
export {registerUser,loginUser}
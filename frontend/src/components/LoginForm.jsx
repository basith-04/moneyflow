import { useNavigate } from "react-router-dom";
import { login } from "../services/authService";
import { useState } from "react";

export default function LoginForm(){
    const navigate = useNavigate();
    const [userName, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const handleSubmit = async (e) => {
        e.preventDefault();
        if(!userName||!password) {
            alert("Please fill in both fields");
            return;
        }
        await login({userName, password})
        
        navigate('/')
    }
    return <div>
        <h1>Login</h1>
        <form className="login-form" onSubmit={handleSubmit}>
            <input type="text" placeholder="Username" value={userName} onChange={(e) => setUsername(e.target.value)} />
            <input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} />
            <button type="submit">Login</button>
        </form>
    </div>
}
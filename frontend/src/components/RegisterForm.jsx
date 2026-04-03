import { useState } from 'react';
import { register } from '../services/authService';

export default function RegisterForm({ onSwitch }) {
  const [userName, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!userName || !password || !fullName || !email) {
      alert("Please fill in all fields");
      return;
    }
    try {
      await register({ userName, password, fullName, email });
      alert("Registered successfully! Please login.");
      onSwitch(); 
    } catch (err) {
      alert(err.message || "Registration failed");
    }
  };

  return (
    <div>
      <h1>Register</h1>
      <form className="login-form" onSubmit={handleSubmit}>
        <input type="text" placeholder="Full name" value={fullName} onChange={(e) => setFullName(e.target.value)} />
        <input type="text" placeholder="Username" value={userName} onChange={(e) => setUsername(e.target.value)} />
        <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)} />
        <input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button type="submit">Register</button>
      </form>
      <p onClick={onSwitch} style={{ cursor: 'pointer' }}>Already have an account? Login</p>
    </div>
  );
}
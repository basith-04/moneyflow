import LoginForm from "../components/LoginForm";
import RegisterForm from "../components/RegisterForm";
import { useState } from 'react';

export default function Auth() {
  const [showLogin, setShowLogin] = useState(true);

  return (
    <div className="auth-wrapper">
      <div className="auth-box">
        {showLogin
          ? <LoginForm onSwitch={() => setShowLogin(false)} />
          : <RegisterForm onSwitch={() => setShowLogin(true)} />
        }</div>
    </div>
  );
}
const apiUrl = import.meta.env.VITE_API_URL;

async function login(user) {
    try {
    const res = await fetch(`${apiUrl}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(user)
    })
    
    const data= await res.json()
    localStorage.setItem("token",data.token)
    if (!res.ok) throw new Error(`Server error: ${res.status}`);
    
  }catch(err){
    console.error(err)
  }

}    
export {login}
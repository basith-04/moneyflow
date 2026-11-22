const apiUrl = import.meta.env.VITE_API_URL;
import { authFetch } from "./api";
async function getCategories(type) {
  try {

    const query = type === "income" ? "?type=income" : ""
    const res = await authFetch(`${apiUrl}/category${query}`)
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const data = await res.json();

    return data

  } catch (err) {
    console.error(err);
  }

}
async function addCategories(catName, type) {
  try {
    const query = type === "income" ? "?type=income" : ""
    const res = await authFetch(`${apiUrl}/category${query}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(catName)
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`);
    
  }catch(err){
    console.error(err)
  } 

}
async function removeCategory(id, type){
  try {
    const query = type === "income" ? "?type=income" : ""
    const res = await authFetch(`${apiUrl}/category/${id}${query}`, {
      method: "DELETE",
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`);
    
  }catch(err){
    console.error(err)
  } 
}
export { getCategories,addCategories,removeCategory }

const apiUrl = import.meta.env.VITE_API_URL;
async function getCategories() {
  try {

    const res = await fetch(`${apiUrl}/category`)
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const data = await res.json();

    return data

  } catch (err) {
    console.error(err);
  }

}
async function addCategories(catName) {
  try {
    const res = await fetch(`${apiUrl}/category`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(catName)
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`);
    
  }catch(err){
    console.error(err)
  } 

}
export { getCategories,addCategories }
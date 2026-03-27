const apiUrl = import.meta.env.VITE_API_URL;
async function getCategories(){
     try {
    
    const res = await fetch(`${apiUrl}/category`)
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const data = await res.json();

    console.log(data);
    return data

  } catch (err) {
    console.error(err);
  }

}
export {getCategories}
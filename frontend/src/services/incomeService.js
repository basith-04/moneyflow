const apiUrl = import.meta.env.VITE_API_URL;
import { authFetch } from "./api";

async function getIncome(filters = {}) {
  try {
    const queryString = new URLSearchParams(filters).toString();
    const url = queryString ? `${apiUrl}/income?${queryString}` : `${apiUrl}/income`
    const res = await authFetch(url)
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const data = await res.json();
    return data
  } catch (err) {
    console.error(err);
  }
}

async function addIncome(income) {
  try {
    const res = await authFetch(`${apiUrl}/income`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(income)
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const data = await res.json()
    return data
  } catch (err) {
    console.error(err);
  }
}

async function removeIncome(id) {
  try {
    const res = await authFetch(`${apiUrl}/income/${id}`, {
      method: 'DELETE'
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const data = await res.json()
    return data
  } catch (err) {
    console.error(err);
  }
}

async function updateIncome(id, data) {
  try {
    const res = await authFetch(`${apiUrl}/income/${id}`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data)
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const result = await res.json()
    return result
  } catch (err) {
    console.error(err);
  }
}

export { getIncome, addIncome, removeIncome, updateIncome }

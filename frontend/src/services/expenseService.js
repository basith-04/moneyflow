const apiUrl = import.meta.env.VITE_API_URL;
async function getExpenses(filters={}) {
  try {
    const queryString = new URLSearchParams(filters).toString();
    const url = queryString ? `${apiUrl}/expenses?${queryString}` : `${apiUrl}/expenses`
    const res = await fetch(url)
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const data = await res.json();

    return data

  } catch (err) {
    console.error(err);
  }
}

async function addExpense(expense) {
  try {
    const res = await fetch(`${apiUrl}/expenses`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(expense)
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const data = await res.json()
    return data

  } catch (err) {
    console.error(err);

  }
}
async function removeExpense(id) {
  try {
    const res = await fetch(`${apiUrl}/expenses/${id}`, {
      method: 'DELETE'
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const data = await res.json()
    return data

  } catch (err) {
    console.error(err);

  }
}
async function updateExpense(id, data) {
  try {
    const res = await fetch(`${apiUrl}/expenses/${id}`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(data)
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`);

    const result = await res.json()
    return result
  } catch (err) {
    console.error(err);

  }
}
export { getExpenses, addExpense, removeExpense, updateExpense }
// updateExpense(124,{'title':'meal'})
// removeExpense(129)
// addExpense({
//   "title": "print",
//   "amount": 150,
//   "date": "2026-03-22",
//   "category_id": 5,
//   "group_id": null,
//   "note": null
// }
// )
// getExpenses({"min":1000})
#  MoneyFlow (Kharcha)

A personal expense tracker built to actually understand the stack — no Firebase shortcuts, no boilerplate — just raw SQL, a clean REST API, and a React dashboard that does what you need it to.

---

##  Why this exists

Most expense trackers are either bloated or use too many abstractions to learn from. MoneyFlow was built from scratch as a deliberate learning project — raw `mysql2` instead of an ORM, manually handled JWTs instead of Firebase Auth, and a clean React frontend with lifted state and a proper services layer.

It's a real working app. It's also how I learned backend fundamentals that actually stick.

---

##  Features

-  Add, edit, and delete expenses
-  Organize by categories
-  Dashboard with spending stats and visual summaries
-  Filter expenses by date range, category, and amount
-  JWT-based authentication (in progress)
-  Responsive UI

---

## 🛠 Tech Stack

| Layer      | Tech                          |
|------------|-------------------------------|
| Frontend   | React + Vite                  |
| Backend    | Node.js + Express             |
| Database   | MySQL (`mysql2` — no ORM)     |
| Auth       | JWT + bcrypt                  |
| Deployment |                               |

---

##  Project Structure

```
moneyflow/
├── client/                 # React + Vite frontend
│   ├── src/
│   │   ├── components/     # Reusable UI components
│   │   ├── pages/          # Dashboard, Expenses, Categories
│   │   └── services/       # API call abstractions
├── server/                 # Express backend
│   ├── routes/             # API routes
│   ├── controllers/        # Route logic
│   └── db/                 # MySQL connection + queries
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Node.js v18+
- MySQL running locally

### 1. Clone the repo

```bash
git clone https://github.com/your-username/moneyflow.git
cd moneyflow
```

### 2. Set up the backend

```bash
cd server
npm install
```

Create a `.env` file:

```env
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=yourpassword
DB_NAME=moneyflow
JWT_SECRET=your_secret_key
PORT=5000
```

Run the server:

```bash
npm run dev
```

### 3. Set up the frontend

```bash
cd client
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`, backend at `http://localhost:5000`.

---

##  API Endpoints

### Expenses

| Method | Endpoint              | Description            |
|--------|-----------------------|------------------------|
| GET    | `/api/expenses`       | Get all expenses       |
| POST   | `/api/expenses`       | Add new expense        |
| PUT    | `/api/expenses/:id`   | Update an expense      |
| DELETE | `/api/expenses/:id`   | Delete an expense      |

### Categories

| Method | Endpoint               | Description             |
|--------|------------------------|-------------------------|
| GET    | `/api/categories`      | Get all categories      |
| POST   | `/api/categories`      | Add new category        |
| DELETE | `/api/categories/:id`  | Delete a category       |

### Auth

| Method | Endpoint          | Description     |
|--------|-------------------|-----------------|
| POST   | `/api/auth/register` | Register user |
| POST   | `/api/auth/login`    | Login + JWT   |

---

## 🗺 Roadmap

- [x] Full CRUD for expenses and categories
- [x] Dashboard with stat cards
- [x] Filters by date, category, amount
- [ ] JWT auth + protected routes
- [ ] Deploy to Railway + Vercel
- [ ] Export expenses as CSV
- [ ] Monthly spending charts

---

##  Contributing

This is a personal learning project, but PRs and suggestions are welcome. Open an issue first if it's a big change.

---

##  License

MIT

---


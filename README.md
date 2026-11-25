#  MoneyFlow (Kharcha)

A personal finance tracker for income and expenses, with a React web client and a native macOS app backed by the same Express API and PostgreSQL database.

## MoneyFlow for macOS

The first Mac release is tagged `macos-v1.0.0`. It requires macOS 26 or later. Sign in with your existing MoneyFlow account to access the same transactions and categories you use on the web.

The Mac app includes a cash flow dashboard, searchable and sortable income and expense ledgers, category management, and Keychain-backed sign-in. Build it from source with Xcode 26 or later. [Build instructions and release notes](macapp/README.md)

---

##  Why this exists

Most expense trackers are either bloated or use too many abstractions to learn from. MoneyFlow was built from scratch as a deliberate learning project — raw PostgreSQL queries instead of an ORM, manually handled JWTs instead of Firebase Auth, and a clean React frontend with lifted state and a proper services layer.

It's a real working app. It's also how I learned backend fundamentals that actually stick.

---

##  Features

-  Add, edit, and delete income and expenses
-  Manage separate income and expense categories
-  Dashboard with spending stats and visual summaries
-  Filter expenses by date range, category, amount, and payment method
-  JWT-based authentication and protected routes
-  Responsive UI

---

## 🛠 Tech Stack

| Layer      | Tech                          |
|------------|-------------------------------|
| Web        | React + Vite                  |
| macOS      | SwiftUI + Swift Charts        |
| Backend    | Node.js + Express             |
| Database   | PostgreSQL (`pg` — no ORM)    |
| Auth       | JWT + bcrypt                  |

---

##  Project Structure

```
moneyflow/
├── frontend/               # React + Vite frontend
│   ├── src/
│   │   ├── components/     # Reusable UI components
│   │   ├── pages/          # Dashboard, Expenses, Categories
│   │   └── services/       # API call abstractions
├── backend/                # Express backend
│   ├── src/                # API routes, controllers, and PostgreSQL connection
│   └── schema.sql          # PostgreSQL schema
├── macapp/                 # Native SwiftUI app and Xcode project
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- Node.js v18+
- PostgreSQL running locally

### 1. Clone the repo

```bash
git clone https://github.com/basith-04/moneyflow.git
cd moneyflow
```

### 2. Set up the backend

```bash
cd backend
npm install
```

Create a `.env` file from `.env.example`:

```env
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=moneyflow
DB_SSL=false
JWT_SECRET=your_secret_key
PORT=5000
```

Create the database and tables:

```bash
createdb moneyflow
psql -d moneyflow -f schema.sql
```

Run the server:

```bash
npm run dev
```

### 3. Set up the frontend

```bash
cd ../frontend
npm install
cp .env.example .env
npm run dev
```

Frontend runs at `http://localhost:5173`, backend at `http://localhost:5000`.

---

##  API Endpoints

### Expenses

| Method | Endpoint        | Description       |
|--------|-----------------|-------------------|
| GET    | `/expenses`     | Get expenses      |
| POST   | `/expenses`     | Add an expense    |
| PATCH  | `/expenses/:id` | Update an expense |
| DELETE | `/expenses/:id` | Delete an expense |

### Income

| Method | Endpoint      | Description             |
|--------|---------------|-------------------------|
| GET    | `/income`     | Get income records      |
| POST   | `/income`     | Add an income record    |
| PATCH  | `/income/:id` | Update an income record |
| DELETE | `/income/:id` | Delete an income record |

### Categories

| Method | Endpoint        | Description       |
|--------|-----------------|-------------------|
| GET    | `/category`     | Get categories    |
| POST   | `/category`     | Add a category    |
| DELETE | `/category/:id` | Delete a category |

Use `?type=income` on category requests to work with income categories.

### Auth

| Method | Endpoint         | Description     |
|--------|------------------|-----------------|
| POST   | `/auth/register` | Register a user |
| POST   | `/auth/login`    | Get a JWT       |

---

## 🗺 Roadmap

- [x] Add, edit, and delete income and expenses
- [x] Create and delete categories
- [x] Dashboard with stat cards
- [x] Filters by date, category, amount
- [x] JWT auth + protected routes
- [x] Native macOS app
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

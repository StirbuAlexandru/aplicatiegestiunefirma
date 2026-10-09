# Business Management App

A full-stack system for managing a small business: a native **Android app**, a **React web dashboard** and a **FastAPI REST backend** with **PostgreSQL**, self-hosted on my own Linux server.

It covers the day-to-day operations of a company: employees, projects, invoices, timesheets, payroll, leave and internal chat, with separate views for administrators and employees.

## Features

### For administrators
- **Employees** – add, edit and delete employees, detailed employee profile, attached PDF contract, create login accounts for employees
- **Projects** – project list, project details and a project calendar
- **Invoices** – create and edit invoices, generate and share PDFs, due-date notifications and automatically generated recurring invoices
- **Daily reports / timesheets** – reports with a photo taken from the camera and GPS location, exported to PDF and CSV
- **Hours reports** per employee and full reports for a selected period
- **Payroll** – salary calculation with PDF and CSV export
- **Finance** – income and expense charts, bank reconciliation
- **Leave and work schedule** management
- **Internal chat** between users
- **Activity log** – audit trail of user actions
- **Company profile**

### For employees
- A separate employee portal where each employee sees their own reports and schedule

### Security
- JWT authentication, passwords hashed with bcrypt
- Role-based access (admin / employee)
- Multi-tenant: each company only sees its own data
- PIN lock and biometric (fingerprint) authentication on Android

### Offline support
The Android app stores data locally in a **Room (SQLite)** database, sends changes to the server and syncs on load, including deletions made on the server.

## Architecture

```
┌──────────────────┐     ┌──────────────────┐
│  Android app     │     │  Web dashboard   │
│  Java, Room      │     │  React + Vite    │
└────────┬─────────┘     └────────┬─────────┘
         │      HTTPS (ngrok)     │
         └───────────┬────────────┘
                     ▼
          ┌──────────────────────┐
          │  REST API            │
          │  Python, FastAPI     │
          │  (37 endpoints)      │
          └──────────┬───────────┘
                     ▼
          ┌──────────────────────┐
          │  PostgreSQL          │
          │  Ubuntu server       │
          └──────────────────────┘
```

The backend runs on my own Ubuntu server (an old laptop turned into a server), as systemd services. It is exposed publicly through an ngrok HTTPS tunnel, and the server is also reachable over a private Tailscale network.

## Tech stack

| Layer | Technologies |
|---|---|
| **Android** | Java, Android SDK (min 24, target 35), Material Design, RecyclerView, Room, Retrofit + OkHttp + Gson, WorkManager, MPAndroidChart, AndroidX Biometric, Play Services Location, PdfDocument |
| **Backend** | Python, FastAPI, Uvicorn, Pydantic, python-jose (JWT), bcrypt, psycopg2 |
| **Web** | React 18, Vite, React Router, Axios, Context API |
| **Database** | PostgreSQL (server), SQLite / Room (device cache) |
| **Infrastructure** | Ubuntu Linux, systemd, ngrok, Tailscale, Git / GitHub |

## Project structure

```
app/        Android application (Java)
web/        Web dashboard (React + Vite)
main.py     FastAPI backend
```

## Running locally

### Backend

```bash
pip install -r requirements.txt
python main.py
```

The backend reads its configuration from environment variables (never committed to the repository):

| Variable | Description |
|---|---|
| `JWT_SECRET_KEY` | Secret used to sign JWT tokens |
| `DATABASE_URL` | Full PostgreSQL connection string (optional) |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection, used when `DATABASE_URL` is not set |
| `PORT` | API port (default `8000`) |

### Web dashboard

```bash
cd web
npm install
npm run dev
```

The API address is set in `BASE_URL` in `web/src/api/client.js`.

### Android app

Open the project in Android Studio and run the `app` module on a device or emulator. The API address is set in `BASE_URL` in `network/RetrofitClient.java`.

## Author

**Alexandru Știrbu** – Junior Frontend Developer

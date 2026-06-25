from fastapi import FastAPI, HTTPException, Depends, Header, UploadFile, File
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
import psycopg2
from psycopg2.extras import RealDictCursor
import bcrypt
from datetime import datetime, timedelta
from jose import JWTError, jwt
from fastapi.middleware.cors import CORSMiddleware
from typing import List, Optional
import traceback
import os
import shutil
import uuid
import urllib.parse

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─── FISIERE INCARCATE (poze rapoarte, contracte PDF, facturi PDF) ────────────
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)
app.mount("/files", StaticFiles(directory=UPLOAD_DIR), name="files")

SECRET_KEY = os.environ.get("JWT_SECRET_KEY")
if not SECRET_KEY:
    raise RuntimeError(
        "JWT_SECRET_KEY nu este setat! Seteaza variabila de mediu inainte de a porni serverul "
        "(vezi set_secrets.bat)."
    )
ALGORITHM = "HS256"

def hash_password(password: str) -> str:
    salt = bcrypt.gensalt()
    return bcrypt.hashpw(password.encode('utf-8'), salt).decode('utf-8')

def verify_password(password: str, hashed_password: str) -> bool:
    try:
        return bcrypt.checkpw(password.encode('utf-8'), hashed_password.encode('utf-8'))
    except Exception:
        return False

def get_db_conn():
    # Cloud: use DATABASE_URL env var (set by Railway/Render)
    # Local: fallback to hardcoded credentials
    db_url = os.environ.get("DATABASE_URL")
    if db_url:
        # Parse the URL (format: postgres://user:pass@host:port/dbname)
        result = urllib.parse.urlparse(db_url)
        return psycopg2.connect(
            dbname=result.path[1:],
            user=result.username,
            password=result.password,
            host=result.hostname,
            port=result.port or 5432,
            connect_timeout=10
        )
    # Local fallback: PostgreSQL ruleaza pe serverul Ubuntu separat, accesibil prin Tailscale VPN
    db_password = os.environ.get("DB_PASSWORD")
    if not db_password:
        raise RuntimeError(
            "DB_PASSWORD nu este setat! Seteaza variabila de mediu inainte de a porni serverul "
            "(vezi set_secrets.bat)."
        )
    return psycopg2.connect(
        dbname=os.environ.get("DB_NAME", "gestiune"),
        user=os.environ.get("DB_USER", "gestiune_user"),
        password=db_password,
        host=os.environ.get("DB_HOST", "100.80.39.59"),
        port=os.environ.get("DB_PORT", "5432"),
        connect_timeout=10
    )

# ─── DB Migration: ensure all columns exist ──────────────────────────────────
def ensure_db_columns():
    try:
        conn = get_db_conn(); cur = conn.cursor()
        # users
        cur.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'admin'")
        cur.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS employee_id INTEGER DEFAULT NULL")
        # angajati - date personale
        cur.execute("ALTER TABLE angajati ADD COLUMN IF NOT EXISTS cnp VARCHAR(20)")
        cur.execute("ALTER TABLE angajati ADD COLUMN IF NOT EXISTS serie_id VARCHAR(10)")
        cur.execute("ALTER TABLE angajati ADD COLUMN IF NOT EXISTS numar_id VARCHAR(20)")
        cur.execute("ALTER TABLE angajati ADD COLUMN IF NOT EXISTS telefon VARCHAR(20)")
        cur.execute("ALTER TABLE angajati ADD COLUMN IF NOT EXISTS email VARCHAR(100)")
        cur.execute("ALTER TABLE angajati ADD COLUMN IF NOT EXISTS salariu_net DECIMAL(10,2)")
        cur.execute("ALTER TABLE angajati ADD COLUMN IF NOT EXISTS salariu_brut DECIMAL(10,2)")
        # proiecte
        cur.execute("ALTER TABLE proiecte ADD COLUMN IF NOT EXISTS nume_client VARCHAR(200)")
        cur.execute("ALTER TABLE proiecte ADD COLUMN IF NOT EXISTS descriere TEXT")
        cur.execute("ALTER TABLE proiecte ADD COLUMN IF NOT EXISTS buget DECIMAL(12,2)")
        cur.execute("ALTER TABLE proiecte ADD COLUMN IF NOT EXISTS data_deadline VARCHAR(20)")
        cur.execute("ALTER TABLE proiecte ADD COLUMN IF NOT EXISTS status VARCHAR(50)")
        cur.execute("ALTER TABLE proiecte ADD COLUMN IF NOT EXISTS note TEXT")
        # Convert data_deadline from date to varchar so any text is accepted
        cur.execute("""
            DO $$ BEGIN
                IF (SELECT data_type FROM information_schema.columns
                    WHERE table_name='proiecte' AND column_name='data_deadline') = 'date' THEN
                    ALTER TABLE proiecte ALTER COLUMN data_deadline TYPE VARCHAR(20) USING data_deadline::TEXT;
                END IF;
            END $$;
        """)
        # facturi
        cur.execute("ALTER TABLE facturi ADD COLUMN IF NOT EXISTS procent_tva DECIMAL(5,2) DEFAULT 19.0")
        cur.execute("ALTER TABLE facturi ADD COLUMN IF NOT EXISTS este_recurenta BOOLEAN DEFAULT FALSE")
        cur.execute("ALTER TABLE facturi ADD COLUMN IF NOT EXISTS zi_recurenta INTEGER DEFAULT 0")
        cur.execute("ALTER TABLE facturi ADD COLUMN IF NOT EXISTS nume_proiect VARCHAR(200)")
        # rapoarte_zilnice
        cur.execute("ALTER TABLE rapoarte_zilnice ADD COLUMN IF NOT EXISTS photo_uri TEXT")
        cur.execute("ALTER TABLE rapoarte_zilnice ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION")
        cur.execute("ALTER TABLE rapoarte_zilnice ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION")
        cur.execute("ALTER TABLE rapoarte_zilnice ADD COLUMN IF NOT EXISTS employee_name VARCHAR(200)")
        cur.execute("ALTER TABLE rapoarte_zilnice ADD COLUMN IF NOT EXISTS project_name VARCHAR(200)")
        # Convert data column from date to varchar so any text is accepted
        cur.execute("""
            DO $$ BEGIN
                IF (SELECT data_type FROM information_schema.columns
                    WHERE table_name='rapoarte_zilnice' AND column_name='data') = 'date' THEN
                    ALTER TABLE rapoarte_zilnice ALTER COLUMN data TYPE VARCHAR(20) USING data::TEXT;
                END IF;
            END $$;
        """)
        # company table
        cur.execute("""
            CREATE TABLE IF NOT EXISTS company (
                id SERIAL PRIMARY KEY,
                firma_id INTEGER UNIQUE,
                name VARCHAR(200),
                address TEXT,
                tax_id VARCHAR(50),
                reg_com VARCHAR(50),
                iban VARCHAR(50),
                bank VARCHAR(100),
                phone VARCHAR(20),
                email VARCHAR(100)
            )
        """)
        # mesaje (chat)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS mesaje (
                id SERIAL PRIMARY KEY,
                firma_id INTEGER,
                employee_id INTEGER,
                employee_name VARCHAR(200),
                message TEXT,
                timestamp VARCHAR(30),
                is_from_admin BOOLEAN DEFAULT FALSE,
                is_read BOOLEAN DEFAULT FALSE
            )
        """)
        # concedii (leave)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS concedii (
                id SERIAL PRIMARY KEY,
                firma_id INTEGER,
                angajat_id INTEGER,
                employee_name VARCHAR(200),
                data_start VARCHAR(20),
                data_stop VARCHAR(20),
                tip VARCHAR(20),
                motiv TEXT,
                status VARCHAR(20) DEFAULT 'PENDING',
                zile_lucratoare INTEGER DEFAULT 1
            )
        """)
        # program_lucru (work schedule)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS program_lucru (
                id SERIAL PRIMARY KEY,
                firma_id INTEGER,
                angajat_id INTEGER,
                zi_saptamana INTEGER,
                ora_start VARCHAR(10),
                ora_stop VARCHAR(10),
                zi_lucratoare BOOLEAN DEFAULT TRUE
            )
        """)
        conn.commit(); cur.close(); conn.close()
        print("DB migration OK: toate coloanele verificate")
    except Exception as e:
        print(f"DB migration note: {e}")

ensure_db_columns()
# ──────────────────────────────────────────────────────────────────────────────

# Modele date
class LoginRequest(BaseModel):
    email: str
    password: str

class RegisterRequest(BaseModel):
    email: str
    password: str
    company_name: str

class CreateEmployeeAccountRequest(BaseModel):
    email: str
    password: str

class EmployeeSchema(BaseModel):
    id: Optional[int] = None
    firstName: str
    lastName: str
    position: str
    paymentType: str
    paymentRate: float
    paymentDay: int
    contractPdfUri: Optional[str] = None
    cnp: Optional[str] = None
    idSeries: Optional[str] = None
    idNumber: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    netSalary: Optional[float] = None
    grossSalary: Optional[float] = None

class ProjectSchema(BaseModel):
    id: Optional[int] = None
    name: str
    clientName: Optional[str] = ""
    description: Optional[str] = ""
    budget: Optional[float] = 0.0
    deadline: Optional[str] = ""
    status: Optional[str] = "In desfasurare"
    notes: Optional[str] = None

class InvoiceSchema(BaseModel):
    id: Optional[int] = None
    invoiceNumber: str
    projectId: int
    projectName: str
    amount: float
    date: str
    dueDate: str
    isPaid: bool
    type: str
    pdfUri: Optional[str] = None
    vatPercent: Optional[float] = 19.0
    isRecurring: Optional[bool] = False
    recurringDay: Optional[int] = 0

class DailyReportSchema(BaseModel):
    employeeId: int
    employeeName: str
    projectId: int
    projectName: str
    date: str
    reportText: str
    hoursWorked: float
    photoUri: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None

class CompanySchema(BaseModel):
    name: Optional[str] = None
    address: Optional[str] = None
    taxId: Optional[str] = None
    regCom: Optional[str] = None
    iban: Optional[str] = None
    bank: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None

class LeaveSchema(BaseModel):
    id: Optional[int] = None
    employeeId: int
    employeeName: str
    startDate: str
    endDate: str
    type: str
    reason: Optional[str] = None
    status: Optional[str] = "PENDING"
    workingDays: Optional[int] = 1

class WorkScheduleSchema(BaseModel):
    id: Optional[int] = None
    employeeId: int
    dayOfWeek: int
    startTime: Optional[str] = None
    endTime: Optional[str] = None
    isWorkDay: Optional[bool] = True

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(days=30)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

# Returns just firma_id — used by admin-only endpoints
def get_current_user_firma(authorization: str = Header(...)):
    try:
        token = authorization.replace("Bearer ", "")
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email = payload.get("sub")
        conn = get_db_conn(); cur = conn.cursor()
        cur.execute("SELECT firma_id FROM users WHERE email=%s", (email,))
        res = cur.fetchone(); cur.close(); conn.close()
        return res[0]
    except Exception:
        raise HTTPException(status_code=401, detail="Sesiune expirata")

# Returns firma_id, role, employee_id — used for role-aware endpoints
def get_current_user_full(authorization: str = Header(...)):
    try:
        token = authorization.replace("Bearer ", "")
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email = payload.get("sub")
        conn = get_db_conn(); cur = conn.cursor()
        cur.execute("SELECT firma_id, role, employee_id FROM users WHERE email=%s", (email,))
        res = cur.fetchone(); cur.close(); conn.close()
        return {
            "firma_id": res[0],
            "role": res[1] if res[1] else "admin",
            "employee_id": res[2]
        }
    except Exception:
        raise HTTPException(status_code=401, detail="Sesiune expirata")

# ─── UPLOAD FISIERE ────────────────────────────────────────────────────────────

@app.post("/upload")
def upload_file(file: UploadFile = File(...), firma_id: int = Depends(get_current_user_firma)):
    ext = os.path.splitext(file.filename or "")[1]
    unique_name = f"{uuid.uuid4().hex}{ext}"
    dest_path = os.path.join(UPLOAD_DIR, unique_name)
    with open(dest_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)
    return {"url": f"/files/{unique_name}"}

# ─── AUTH ENDPOINTS ────────────────────────────────────────────────────────────

@app.post("/register")
def register(req: RegisterRequest):
    try:
        conn = get_db_conn(); cur = conn.cursor()
        # Verifică dacă emailul e deja folosit
        cur.execute("SELECT id FROM users WHERE email = %s", (req.email,))
        if cur.fetchone():
            raise HTTPException(status_code=400, detail="Email deja inregistrat")
        # Creează firma nouă cu ID auto-generat
        cur.execute("INSERT INTO firma (nume) VALUES (%s) RETURNING id", (req.company_name,))
        firma_id = cur.fetchone()[0]
        # Creează contul admin pentru firmă
        hashed_pwd = hash_password(req.password)
        cur.execute("INSERT INTO users (email, password, firma_id, role) VALUES (%s, %s, %s, 'admin')",
                    (req.email, hashed_pwd, firma_id))
        conn.commit(); cur.close(); conn.close()
        return {"message": "Succes", "firma_id": firma_id}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.post("/login")
def login(req: LoginRequest):
    try:
        conn = get_db_conn(); cur = conn.cursor()
        cur.execute("SELECT password, firma_id, role, employee_id FROM users WHERE email=%s", (req.email,))
        user = cur.fetchone(); cur.close(); conn.close()
        if not user or not verify_password(req.password, user[0]):
            raise HTTPException(status_code=401, detail="Email sau parola incorecta")
        token = create_access_token(data={"sub": req.email})
        return {
            "access_token": token,
            "token_type": "bearer",
            "firma_id": user[1],
            "role": user[2] if user[2] else "admin",
            "employee_id": user[3]
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# ─── EMPLOYEE ACCOUNT MANAGEMENT ──────────────────────────────────────────────

@app.post("/employees/{employee_id}/create-account")
def create_employee_account(
    employee_id: int,
    req: CreateEmployeeAccountRequest,
    current_user: dict = Depends(get_current_user_full)
):
    if current_user["role"] != "admin":
        raise HTTPException(status_code=403, detail="Acces interzis - doar adminii pot crea conturi")
    try:
        conn = get_db_conn(); cur = conn.cursor()
        # Check if this employee already has an account
        cur.execute("SELECT id FROM users WHERE employee_id=%s", (employee_id,))
        if cur.fetchone():
            cur.close(); conn.close()
            raise HTTPException(status_code=400, detail="Angajatul are deja un cont")
        hashed_pwd = hash_password(req.password)
        cur.execute(
            "INSERT INTO users (email, password, firma_id, role, employee_id) VALUES (%s, %s, %s, 'employee', %s)",
            (req.email, hashed_pwd, current_user["firma_id"], employee_id)
        )
        conn.commit(); cur.close(); conn.close()
        return {"message": "Cont angajat creat cu succes"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))

@app.get("/employees/{employee_id}/has-account")
def check_employee_account(employee_id: int, current_user: dict = Depends(get_current_user_full)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute("SELECT email FROM users WHERE employee_id=%s", (employee_id,))
    row = cur.fetchone(); cur.close(); conn.close()
    return {"hasAccount": row is not None, "email": row[0] if row else None}

# ─── ANGAJATI ─────────────────────────────────────────────────────────────────

@app.get("/angajati")
def list_angajati(firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""SELECT id, nume as "firstName", prenume as "lastName", functie as position,
                   tip_plata as "paymentType", rata_plata as "paymentRate", zi_plata as "paymentDay",
                   contract_pdf as "contractPdfUri", cnp, serie_id as "idSeries", numar_id as "idNumber",
                   telefon as phone, email, salariu_net as "netSalary", salariu_brut as "grossSalary"
                   FROM angajati WHERE firma_id=%s""", (firma_id,))
    res = cur.fetchall(); cur.close(); conn.close()
    return res

@app.post("/angajati")
def add_employee(req: EmployeeSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute(
        """INSERT INTO angajati (nume, prenume, functie, tip_plata, rata_plata, zi_plata, contract_pdf,
           cnp, serie_id, numar_id, telefon, email, salariu_net, salariu_brut, firma_id)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
        (req.firstName, req.lastName, req.position, req.paymentType, req.paymentRate, req.paymentDay,
         req.contractPdfUri, req.cnp, req.idSeries, req.idNumber, req.phone, req.email,
         req.netSalary, req.grossSalary, firma_id)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.put("/employees/{emp_id}")
def update_employee(emp_id: int, req: EmployeeSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute(
        """UPDATE angajati SET nume=%s, prenume=%s, functie=%s, tip_plata=%s, rata_plata=%s, zi_plata=%s,
           cnp=%s, serie_id=%s, numar_id=%s, telefon=%s, email=%s, salariu_net=%s, salariu_brut=%s
           WHERE id=%s AND firma_id=%s""",
        (req.firstName, req.lastName, req.position, req.paymentType, req.paymentRate, req.paymentDay,
         req.cnp, req.idSeries, req.idNumber, req.phone, req.email, req.netSalary, req.grossSalary,
         emp_id, firma_id)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.delete("/employees/{emp_id}")
def delete_employee(emp_id: int, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute("DELETE FROM angajati WHERE id=%s AND firma_id=%s", (emp_id, firma_id))
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

# ─── PROIECTE ─────────────────────────────────────────────────────────────────

@app.get("/proiecte")
def list_proiecte(firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""SELECT id, nume as name, nume_client as "clientName", descriere as description,
                   buget as budget, data_deadline as deadline, status, note as notes
                   FROM proiecte WHERE firma_id=%s""", (firma_id,))
    res = cur.fetchall(); cur.close(); conn.close()
    return res

@app.post("/proiecte")
def add_project(req: ProjectSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    deadline = req.deadline if req.deadline else None
    budget = req.budget if req.budget else 0.0
    cur.execute(
        "INSERT INTO proiecte (nume, nume_client, descriere, buget, data_deadline, status, note, firma_id) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)",
        (req.name, req.clientName or "", req.description or "", budget, deadline, req.status or "In desfasurare", req.notes, firma_id)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.put("/projects/{proj_id}")
def update_project(proj_id: int, req: ProjectSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    deadline = req.deadline if req.deadline else None
    budget = req.budget if req.budget else 0.0
    cur.execute(
        "UPDATE proiecte SET nume=%s, nume_client=%s, descriere=%s, buget=%s, data_deadline=%s, status=%s, note=%s WHERE id=%s AND firma_id=%s",
        (req.name, req.clientName or "", req.description or "", budget, deadline, req.status or "In desfasurare", req.notes, proj_id, firma_id)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.delete("/projects/{proj_id}")
def delete_project(proj_id: int, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute("DELETE FROM proiecte WHERE id=%s AND firma_id=%s", (proj_id, firma_id))
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

# ─── FACTURI ──────────────────────────────────────────────────────────────────

@app.get("/facturi")
def list_invoices(firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""SELECT id, numar_factura as "invoiceNumber", proiect_id as "projectId",
                   nume_proiect as "projectName", suma as amount, data_emitere as date,
                   data_scadenta as "dueDate", este_platita as "isPaid", tip as type,
                   pdf_path as "pdfUri", procent_tva as "vatPercent",
                   este_recurenta as "isRecurring", zi_recurenta as "recurringDay"
                   FROM facturi WHERE firma_id=%s""", (firma_id,))
    res = cur.fetchall(); cur.close(); conn.close()
    return res

@app.post("/facturi")
def add_invoice(req: InvoiceSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute(
        """INSERT INTO facturi (numar_factura, suma, data_emitere, data_scadenta, este_platita, tip,
           proiect_id, nume_proiect, firma_id, pdf_path, procent_tva, este_recurenta, zi_recurenta)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
        (req.invoiceNumber, req.amount, req.date, req.dueDate, 1 if req.isPaid else 0, req.type,
         req.projectId, req.projectName, firma_id, req.pdfUri, req.vatPercent, req.isRecurring, req.recurringDay)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.put("/invoices/{inv_id}")
def update_invoice(inv_id: int, req: InvoiceSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute(
        """UPDATE facturi SET numar_factura=%s, suma=%s, data_emitere=%s, data_scadenta=%s,
           este_platita=%s, tip=%s, procent_tva=%s, este_recurenta=%s, zi_recurenta=%s
           WHERE id=%s AND firma_id=%s""",
        (req.invoiceNumber, req.amount, req.date, req.dueDate, 1 if req.isPaid else 0, req.type,
         req.vatPercent, req.isRecurring, req.recurringDay, inv_id, firma_id)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.delete("/invoices/{inv_id}")
def delete_invoice(inv_id: int, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute("DELETE FROM facturi WHERE id=%s AND firma_id=%s", (inv_id, firma_id))
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

# ─── RAPOARTE ZILNICE ─────────────────────────────────────────────────────────

@app.get("/rapoarte_zilnice")
def get_reports(date: Optional[str] = None, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    if date:
        cur.execute("""SELECT id, angajat_id as "employeeId", employee_name as "employeeName",
                       proiect_id as "projectId", project_name as "projectName",
                       data as date, descriere as "reportText", ore as "hoursWorked",
                       photo_uri as "photoUri", latitude, longitude
                       FROM rapoarte_zilnice WHERE firma_id=%s AND data=%s""", (firma_id, date))
    else:
        cur.execute("""SELECT id, angajat_id as "employeeId", employee_name as "employeeName",
                       proiect_id as "projectId", project_name as "projectName",
                       data as date, descriere as "reportText", ore as "hoursWorked",
                       photo_uri as "photoUri", latitude, longitude
                       FROM rapoarte_zilnice WHERE firma_id=%s""", (firma_id,))
    res = cur.fetchall(); cur.close(); conn.close()
    return res

@app.post("/rapoarte_zilnice")
def add_report(req: DailyReportSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    # Avoid FK violation: use NULL if project/employee don't exist on server yet
    cur.execute("SELECT id FROM proiecte WHERE id=%s AND firma_id=%s", (req.projectId, firma_id))
    project_id = req.projectId if cur.fetchone() else None
    cur.execute("SELECT id FROM angajati WHERE id=%s AND firma_id=%s", (req.employeeId, firma_id))
    angajat_id = req.employeeId if cur.fetchone() else None
    cur.execute(
        """INSERT INTO rapoarte_zilnice (angajat_id, employee_name, proiect_id, project_name,
           data, descriere, ore, photo_uri, latitude, longitude, firma_id)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
        (angajat_id, req.employeeName, project_id, req.projectName, req.date,
         req.reportText, req.hoursWorked, req.photoUri, req.latitude, req.longitude, firma_id)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.put("/reports/{report_id}")
def update_report(report_id: int, req: DailyReportSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute(
        "UPDATE rapoarte_zilnice SET descriere=%s, ore=%s WHERE id=%s AND firma_id=%s",
        (req.reportText, req.hoursWorked, report_id, firma_id)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.delete("/reports/{report_id}")
def delete_report(report_id: int, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute("DELETE FROM rapoarte_zilnice WHERE id=%s AND firma_id=%s", (report_id, firma_id))
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

# ─── EMPLOYEE PORTAL ENDPOINTS ────────────────────────────────────────────────

@app.get("/employee/proiecte")
def get_employee_projects(current_user: dict = Depends(get_current_user_full)):
    """Employees can read projects (read-only) to select when submitting reports."""
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""SELECT id, nume as name, nume_client as "clientName", descriere as description,
                   buget, data_deadline as deadline, status FROM proiecte WHERE firma_id=%s""",
                (current_user["firma_id"],))
    res = cur.fetchall(); cur.close(); conn.close()
    return res

@app.get("/employee/rapoarte")
def get_employee_own_reports(current_user: dict = Depends(get_current_user_full)):
    """Employee sees only their own reports."""
    if current_user["role"] != "employee" or not current_user["employee_id"]:
        raise HTTPException(status_code=403, detail="Acces interzis")
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""SELECT id, angajat_id as "employeeId", employee_name as "employeeName",
                   proiect_id as "projectId", project_name as "projectName",
                   data as date, descriere as "reportText", ore as "hoursWorked",
                   photo_uri as "photoUri", latitude, longitude
                   FROM rapoarte_zilnice
                   WHERE angajat_id=%s AND firma_id=%s
                   ORDER BY data DESC LIMIT 100""",
                (current_user["employee_id"], current_user["firma_id"]))
    res = cur.fetchall(); cur.close(); conn.close()
    return res

@app.post("/employee/rapoarte")
def add_employee_own_report(req: DailyReportSchema, current_user: dict = Depends(get_current_user_full)):
    """Employee can only submit a report for themselves."""
    if current_user["role"] != "employee" or not current_user["employee_id"]:
        raise HTTPException(status_code=403, detail="Acces interzis")
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute("SELECT prenume, nume FROM angajati WHERE id=%s", (current_user["employee_id"],))
    emp = cur.fetchone()
    employee_name = f"{emp[0]} {emp[1]}" if emp else req.employeeName
    # Avoid FK violation: use NULL project_id if project doesn't exist on server yet
    cur.execute("SELECT id FROM proiecte WHERE id=%s AND firma_id=%s", (req.projectId, current_user["firma_id"]))
    project_id = req.projectId if cur.fetchone() else None
    cur.execute(
        """INSERT INTO rapoarte_zilnice (angajat_id, employee_name, proiect_id, project_name,
           data, descriere, ore, photo_uri, latitude, longitude, firma_id)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)""",
        (current_user["employee_id"], employee_name, project_id, req.projectName, req.date,
         req.reportText, req.hoursWorked, req.photoUri, req.latitude, req.longitude, current_user["firma_id"])
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.get("/employee/info")
def get_employee_info(current_user: dict = Depends(get_current_user_full)):
    """Employee fetches their own profile."""
    if current_user["role"] != "employee" or not current_user["employee_id"]:
        raise HTTPException(status_code=403, detail="Acces interzis")
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""SELECT id, nume as "firstName", prenume as "lastName", functie as position,
                   tip_plata as "paymentType", rata_plata as "paymentRate",
                   telefon as phone, email, salariu_net as "netSalary", salariu_brut as "grossSalary"
                   FROM angajati WHERE id=%s""", (current_user["employee_id"],))
    res = cur.fetchone(); cur.close(); conn.close()
    if not res:
        raise HTTPException(status_code=404, detail="Angajat negasit")
    return res

# ─── COMPANY ──────────────────────────────────────────────────────────────────

@app.get("/company")
def get_company(firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""SELECT name, address, tax_id as "taxId", reg_com as "regCom",
                   iban, bank, phone, email FROM company WHERE firma_id=%s""", (firma_id,))
    res = cur.fetchone(); cur.close(); conn.close()
    return res or {}

@app.post("/company")
def save_company(req: CompanySchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute(
        """INSERT INTO company (firma_id, name, address, tax_id, reg_com, iban, bank, phone, email)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)
           ON CONFLICT (firma_id) DO UPDATE SET
           name=EXCLUDED.name, address=EXCLUDED.address, tax_id=EXCLUDED.tax_id,
           reg_com=EXCLUDED.reg_com, iban=EXCLUDED.iban, bank=EXCLUDED.bank,
           phone=EXCLUDED.phone, email=EXCLUDED.email""",
        (firma_id, req.name, req.address, req.taxId, req.regCom, req.iban, req.bank, req.phone, req.email)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

# ─── CONCEDII (LEAVE) ─────────────────────────────────────────────────────────

@app.get("/concedii")
def list_concedii(firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""SELECT id, angajat_id as "employeeId", employee_name as "employeeName",
                   data_start as "startDate", data_stop as "endDate", tip as type,
                   motiv as reason, status, zile_lucratoare as "workingDays"
                   FROM concedii WHERE firma_id=%s ORDER BY data_start DESC""", (firma_id,))
    res = cur.fetchall(); cur.close(); conn.close()
    return res

@app.post("/concedii")
def add_concediu(req: LeaveSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute(
        """INSERT INTO concedii (angajat_id, employee_name, data_start, data_stop, tip, motiv, status, zile_lucratoare, firma_id)
           VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s)""",
        (req.employeeId, req.employeeName, req.startDate, req.endDate, req.type,
         req.reason, req.status or "PENDING", req.workingDays or 1, firma_id)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.put("/concedii/{leave_id}")
def update_concediu(leave_id: int, req: LeaveSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute(
        """UPDATE concedii SET status=%s, motiv=%s WHERE id=%s AND firma_id=%s""",
        (req.status, req.reason, leave_id, firma_id)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.delete("/concedii/{leave_id}")
def delete_concediu(leave_id: int, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute("DELETE FROM concedii WHERE id=%s AND firma_id=%s", (leave_id, firma_id))
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

# ─── PROGRAM LUCRU (WORK SCHEDULE) ────────────────────────────────────────────

@app.get("/program_lucru")
def list_program_lucru(firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""SELECT id, angajat_id as "employeeId", zi_saptamana as "dayOfWeek",
                   ora_start as "startTime", ora_stop as "endTime", zi_lucratoare as "isWorkDay"
                   FROM program_lucru WHERE firma_id=%s""", (firma_id,))
    res = cur.fetchall(); cur.close(); conn.close()
    return res

@app.post("/program_lucru")
def add_program_lucru(req: WorkScheduleSchema, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute(
        """INSERT INTO program_lucru (angajat_id, zi_saptamana, ora_start, ora_stop, zi_lucratoare, firma_id)
           VALUES (%s, %s, %s, %s, %s, %s)""",
        (req.employeeId, req.dayOfWeek, req.startTime, req.endTime, req.isWorkDay, firma_id)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.delete("/program_lucru/{schedule_id}")
def delete_program_lucru(schedule_id: int, firma_id: int = Depends(get_current_user_firma)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute("DELETE FROM program_lucru WHERE id=%s AND firma_id=%s", (schedule_id, firma_id))
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

# ─── CHAT ────────────────────────────────────────────────────────────────────

class MessageSchema(BaseModel):
    employee_id: int
    message: str
    is_from_admin: bool = False

@app.get("/messages/{employee_id}")
def get_messages(employee_id: int, current_user: dict = Depends(get_current_user_full)):
    conn = get_db_conn(); cur = conn.cursor(cursor_factory=RealDictCursor)
    cur.execute("""
        SELECT id, employee_id as "employeeId", employee_name as "employeeName",
               message, timestamp, is_from_admin as "isFromAdmin", is_read as "isRead"
        FROM mesaje WHERE employee_id=%s AND firma_id=%s ORDER BY timestamp ASC
    """, (employee_id, current_user["firma_id"]))
    res = cur.fetchall(); cur.close(); conn.close()
    return res

@app.post("/messages")
def send_message(req: MessageSchema, current_user: dict = Depends(get_current_user_full)):
    from datetime import datetime
    timestamp = datetime.utcnow().strftime("%Y-%m-%d %H:%M:%S")
    is_from_admin = current_user["role"] == "admin"
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute("SELECT prenume, nume FROM angajati WHERE id=%s AND firma_id=%s",
                (req.employee_id, current_user["firma_id"]))
    emp = cur.fetchone()
    emp_name = f"{emp[0]} {emp[1]}" if emp else "Angajat"
    cur.execute("""
        INSERT INTO mesaje (employee_id, employee_name, firma_id, message, timestamp, is_from_admin, is_read)
        VALUES (%s, %s, %s, %s, %s, %s, FALSE)""",
        (req.employee_id, emp_name, current_user["firma_id"], req.message, timestamp, is_from_admin)
    )
    conn.commit(); cur.close(); conn.close()
    return {"status": "success"}

@app.post("/messages/{employee_id}/read")
def mark_messages_read(employee_id: int, current_user: dict = Depends(get_current_user_full)):
    conn = get_db_conn(); cur = conn.cursor()
    cur.execute("UPDATE mesaje SET is_read=TRUE WHERE employee_id=%s AND firma_id=%s",
                (employee_id, current_user["firma_id"]))
    conn.commit(); cur.close(); conn.close()
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)

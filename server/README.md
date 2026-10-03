# KinGz Server — V0.1

First milestone:
- Start the FastAPI server
- Verify `/api/health`
- Keep the project ready for SQLite, files, authentication, and sync

Run on Windows:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

Health check:

`http://127.0.0.1:8000/api/health`

Swagger:

`http://127.0.0.1:8000/docs`

Android emulator will later use:

`http://10.0.2.2:8000`

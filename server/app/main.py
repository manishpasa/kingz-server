from fastapi import FastAPI

from app.config import settings
from app.database import init_db, test_database_connection


app = FastAPI(
    title=settings.APP_NAME,
    version="0.3.0",
)


@app.on_event("startup")
def startup() -> None:
    test_database_connection()
    init_db()


@app.get("/api/health")
def health():
    return {
        "status": "ok",
        "service": settings.APP_NAME,
        "version": "0.3.0",
        "database": "postgresql",
    }

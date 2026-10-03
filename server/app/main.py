from fastapi import FastAPI

from app.config import settings
from app.database import test_database_connection


app = FastAPI(
    title=settings.APP_NAME,
    version="0.2.0",
)


@app.on_event("startup")
def startup() -> None:
    test_database_connection()


@app.get("/api/health")
def health():
    return {
        "status": "ok",
        "service": settings.APP_NAME,
        "version": "0.2.0",
        "database": "postgresql",
    }
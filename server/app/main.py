from fastapi import FastAPI

from app.auth.router import router as auth_router
from app.config import settings
from app.database import init_db, test_database_connection


app = FastAPI(
    title=settings.APP_NAME,
    version="0.4.0",
)


@app.on_event("startup")
def startup() -> None:
    test_database_connection()
    init_db()


app.include_router(auth_router)


@app.get("/api/health")
def health():
    return {
        "status": "ok",
        "service": settings.APP_NAME,
        "version": "0.4.0",
        "database": "postgresql",
    }
from fastapi import Depends, FastAPI, HTTPException
from slowapi import _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded

from app.auth.router import router as auth_router
from app.auth.security import get_admin_user
from app.config import settings
from app.database import init_db, test_database_connection
from app.files.router import router as files_router
from app.limiter import limiter
from app.models import Folder, GalleryFile, Session, User


app = FastAPI(
    title=settings.APP_NAME,
    version="1.0.0",
)

app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)


@app.on_event("startup")
def startup() -> None:
    test_database_connection()
    init_db()


app.include_router(auth_router)
app.include_router(files_router)


@app.get("/api/health")
def health():
    return {
        "status": "ok",
        "service": settings.APP_NAME,
        "version": "1.0.0",
        "database": "postgresql",
    }


@app.get("/api/admin/database/health")
def admin_database_health(
    admin: User = Depends(get_admin_user),
):
    try:
        test_database_connection()
        return {
            "status": "connected",
            "database": "postgresql",
        }
    except Exception:
        raise HTTPException(
            status_code=503,
            detail="Database connection failed",
        )

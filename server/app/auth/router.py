from datetime import datetime, timezone
from uuid import uuid4

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import select
from sqlalchemy.orm import Session as DbSession

from app.auth.schemas import LoginRequest, LoginResponse, UserResponse
from app.auth.security import (
    create_access_token,
    get_current_session,
    get_current_user,
    verify_password,
)
from app.database import get_db
from app.limiter import limiter
from app.models import Session as UserSession, User


router = APIRouter(prefix="/api/auth", tags=["Authentication"])


@router.post("/login", response_model=LoginResponse)
@limiter.limit("5/minute")
def login(
    request: Request,
    credentials: LoginRequest,
    db: DbSession = Depends(get_db),
):
    user = db.scalar(
        select(User).where(User.username == credentials.username)
    )

    if (
        user is None
        or not user.is_active
        or not verify_password(credentials.password, user.password_hash)
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
        )

    token_jti = str(uuid4())
    access_token, expires_at = create_access_token(
        user_id=user.id,
        token_jti=token_jti,
    )

    session = UserSession(
        user_id=user.id,
        token_jti=token_jti,
        expires_at=expires_at,
    )

    db.add(session)
    db.commit()

    return LoginResponse(
        access_token=access_token,
        user=UserResponse(
            id=user.id,
            username=user.username,
            is_admin=user.is_admin,
        ),
    )


@router.post("/logout")
def logout(
    current_session: UserSession = Depends(get_current_session),
    db: DbSession = Depends(get_db),
):
    current_session.revoked_at = datetime.now(timezone.utc)
    db.commit()
    return {"message": "Logged out successfully"}


@router.get("/me", response_model=UserResponse)
def get_me(
    current_user: User = Depends(get_current_user),
):
    return UserResponse(
        id=current_user.id,
        username=current_user.username,
        is_admin=current_user.is_admin,
    )

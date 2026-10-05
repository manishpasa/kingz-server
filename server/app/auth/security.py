from datetime import datetime, timedelta, timezone
from uuid import uuid4

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy import select
from sqlalchemy.orm import Session as DbSession

from app.auth.schemas import LoginResponse
from app.config import settings
from app.database import get_db
from app.models import Device, Session as UserSession, User


oauth2_scheme = OAuth2PasswordBearer(
    tokenUrl="/api/auth/login"
)


def hash_password(password: str) -> str:
    from pwdlib import PasswordHash

    return PasswordHash.recommended().hash(password)


def verify_password(password: str, hashed_password: str) -> bool:
    from pwdlib import PasswordHash

    return PasswordHash.recommended().verify(
        password,
        hashed_password,
    )


def create_access_token(
    user_id: int,
    device_id: int,
    token_jti: str,
    expires_delta: timedelta | None = None,
) -> tuple[str, datetime]:

    if expires_delta is None:
        expires_delta = timedelta(
            minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES
        )

    expire = datetime.now(timezone.utc) + expires_delta

    payload = {
        "sub": str(user_id),
        "device_id": str(device_id),
        "jti": token_jti,
        "exp": expire,
    }

    token = jwt.encode(
        payload,
        settings.JWT_SECRET_KEY,
        algorithm=settings.JWT_ALGORITHM,
    )

    return token, expire


def get_current_session(
    token: str = Depends(oauth2_scheme),
    db: DbSession = Depends(get_db),
) -> UserSession:

    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid, expired, or revoked session",
        headers={"WWW-Authenticate": "Bearer"},
    )

    try:
        payload = jwt.decode(
            token,
            settings.JWT_SECRET_KEY,
            algorithms=[settings.JWT_ALGORITHM],
        )

        subject = payload.get("sub")
        device_id_value = payload.get("device_id")
        token_jti = payload.get("jti")

        if (
            subject is None
            or device_id_value is None
            or token_jti is None
        ):
            raise credentials_exception

        user_id = int(subject)
        device_id = int(device_id_value)

    except (jwt.InvalidTokenError, ValueError):
        raise credentials_exception

    current_session = db.scalar(
        select(UserSession).where(
            UserSession.token_jti == token_jti,
            UserSession.user_id == user_id,
            UserSession.device_id == device_id,
        )
    )

    if current_session is None:
        raise credentials_exception

    now = datetime.now(timezone.utc)

    if current_session.revoked_at is not None:
        raise credentials_exception

    if current_session.expires_at <= now:
        raise credentials_exception

    device = db.get(Device, device_id)

    if (
        device is None
        or device.user_id != user_id
        or not device.is_authorized
    ):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="This device is not authorized",
        )

    device.last_seen_at = now
    db.commit()

    return current_session


def get_current_user(
    current_session: UserSession = Depends(get_current_session),
    db: DbSession = Depends(get_db),
) -> User:

    user = db.get(User, current_session.user_id)

    if user is None or not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User account is unavailable",
        )

    return user
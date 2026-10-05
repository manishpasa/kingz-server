from datetime import datetime, timedelta, timezone
from uuid import uuid4

from fastapi import APIRouter, Depends, Header, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.schemas import (
    LoginResponse,
    RegisterRequest,
    UserResponse,
)
from app.auth.security import (
    create_access_token,
    get_current_session,
    get_current_user,
    hash_password,
    verify_password,
)
from app.database import get_db
from app.models import Device, Session as UserSession, User


router = APIRouter(
    prefix="/api/auth",
    tags=["Authentication"],
)


@router.post(
    "/register",
    response_model=UserResponse,
    status_code=status.HTTP_201_CREATED,
)
def register(
    request: RegisterRequest,
    db: Session = Depends(get_db),
):
    username = request.username.strip()

    if not username:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Username cannot be empty",
        )

    existing_user = db.scalar(
        select(User).where(User.username == username)
    )

    if existing_user is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Username already exists",
        )

    user = User(
        username=username,
        password_hash=hash_password(request.password),
        is_active=True,
    )

    db.add(user)
    db.commit()
    db.refresh(user)

    return user


@router.post(
    "/login",
    response_model=LoginResponse,
)
def login(
    form_data: OAuth2PasswordRequestForm = Depends(),
    x_device_key: str | None = Header(
        default=None,
        alias="X-Device-Key",
    ),
    db: Session = Depends(get_db),
):
    user = db.scalar(
        select(User).where(
            User.username == form_data.username.strip()
        )
    )

    if user is None or not verify_password(
        form_data.password,
        user.password_hash,
    ):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is inactive",
        )

    device = None

    if x_device_key:
        device = db.scalar(
            select(Device).where(
                Device.user_id == user.id,
                Device.device_key == x_device_key,
                Device.is_authorized.is_(True),
            )
        )

        if device is None:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Device is not registered or authorized",
            )

    else:
        device = db.scalar(
            select(Device)
            .where(
                Device.user_id == user.id,
                Device.is_authorized.is_(True),
            )
            .order_by(Device.created_at.asc())
        )

        if device is None:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="No authorized device exists for this user",
            )

    now = datetime.now(timezone.utc)

    token_jti = uuid4().hex

    access_token, expires_at = create_access_token(
        user_id=user.id,
        device_id=device.id,
        token_jti=token_jti,
    )

    session = UserSession(
        user_id=user.id,
        device_id=device.id,
        token_jti=token_jti,
        created_at=now,
        expires_at=expires_at,
    )

    device.last_seen_at = now

    db.add(session)
    db.commit()

    return LoginResponse(
        access_token=access_token,
        token_type="bearer",
        user=user,
    )


@router.post(
    "/logout",
)
def logout(
    current_session: UserSession = Depends(get_current_session),
    db: Session = Depends(get_db),
):
    current_session.revoked_at = datetime.now(timezone.utc)

    db.commit()

    return {
        "message": "Logged out successfully",
    }


@router.get(
    "/me",
    response_model=UserResponse,
)
def get_me(
    current_user: User = Depends(get_current_user),
):
    return current_user
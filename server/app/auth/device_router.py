from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.auth.device_schemas import (
    DeviceCreateRequest,
    DeviceResponse,
)
from app.auth.security import get_current_user
from app.database import get_db
from app.models import Device, User


router = APIRouter(
    prefix="/api/devices",
    tags=["Devices"],
)


@router.post(
    "",
    response_model=DeviceResponse,
    status_code=status.HTTP_201_CREATED,
)
def register_device(
    request: DeviceCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    existing_device = db.scalar(
        select(Device).where(
            Device.device_key == request.device_key
        )
    )

    if existing_device is not None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="This device is already registered",
        )

    device = Device(
        user_id=current_user.id,
        device_key=request.device_key,
        name=request.name.strip(),
        platform=request.platform.strip(),
        is_authorized=True,
        last_seen_at=datetime.now(timezone.utc),
    )

    db.add(device)
    db.commit()
    db.refresh(device)

    return device


@router.get(
    "",
    response_model=list[DeviceResponse],
)
def list_devices(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    devices = db.scalars(
        select(Device)
        .where(Device.user_id == current_user.id)
        .order_by(Device.created_at.desc())
    ).all()

    return list(devices)


@router.post(
    "/{device_id}/revoke",
    response_model=DeviceResponse,
)
def revoke_device(
    device_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    device = db.scalar(
        select(Device).where(
            Device.id == device_id,
            Device.user_id == current_user.id,
        )
    )

    if device is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Device not found",
        )

    device.is_authorized = False
    device.revoked_at = datetime.now(timezone.utc)

    db.commit()
    db.refresh(device)

    return device
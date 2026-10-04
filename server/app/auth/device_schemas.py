from datetime import datetime

from pydantic import BaseModel, Field


class DeviceCreateRequest(BaseModel):
    device_key: str = Field(
        min_length=16,
        max_length=128,
    )

    name: str = Field(
        min_length=1,
        max_length=100,
    )

    platform: str = Field(
        min_length=1,
        max_length=50,
    )


class DeviceResponse(BaseModel):
    id: int
    user_id: int
    device_key: str
    name: str
    platform: str
    is_authorized: bool
    created_at: datetime
    last_seen_at: datetime | None
    revoked_at: datetime | None
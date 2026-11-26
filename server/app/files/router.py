from pathlib import Path
from uuid import uuid4

from fastapi import APIRouter, Depends, File, HTTPException, UploadFile
from fastapi.responses import FileResponse
from pydantic import BaseModel, Field
from sqlalchemy import or_, select
from sqlalchemy.orm import Session as DbSession

from app.auth.security import get_current_user
from app.config import settings
from app.database import get_db
from app.models import Folder, GalleryFile, User

router = APIRouter(prefix="/api/files", tags=["Gallery"])


class FolderInput(BaseModel):
    name: str = Field(min_length=1, max_length=255)
    parent_id: int | None = None
    is_public: bool = False


class RenameInput(BaseModel):
    name: str = Field(min_length=1, max_length=255)


class VisibilityInput(BaseModel):
    is_public: bool


class MoveInput(BaseModel):
    folder_id: int | None = None


def owned_folder(db, folder_id, owner_id):
    folder = db.scalar(select(Folder).where(
        Folder.id == folder_id,
        Folder.owner_id == owner_id,
    ))
    if folder is None:
        raise HTTPException(404, "Folder not found")
    return folder


def accessible_folder(db, folder_id, user_id):
    folder = db.get(Folder, folder_id)
    if folder is None or (
        folder.owner_id != user_id and not folder.is_public
    ):
        raise HTTPException(404, "Folder not found")
    return folder


def owned_file(db, file_id, owner_id):
    item = db.scalar(select(GalleryFile).where(
        GalleryFile.id == file_id,
        GalleryFile.owner_id == owner_id,
    ))
    if item is None:
        raise HTTPException(404, "File not found")
    return item


def accessible_file(db, file_id, user_id):
    item = db.get(GalleryFile, file_id)
    if item is None:
        raise HTTPException(404, "File not found")

    if item.owner_id == user_id:
        return item

    if item.folder_id is not None:
        folder = db.get(Folder, item.folder_id)
        if folder is not None and folder.is_public:
            return item

    raise HTTPException(404, "File not found")


def file_path(owner_id, storage_name):
    root = Path(settings.FILE_STORAGE_ROOT).resolve()
    path = (root / str(owner_id) / storage_name).resolve()
    if root not in path.parents:
        raise HTTPException(400, "Invalid storage path")
    return path


def set_descendant_visibility(db, parent_id, is_public):
    pending = [parent_id]
    while pending:
        current_id = pending.pop()
        children = db.scalars(
            select(Folder).where(Folder.parent_id == current_id)
        ).all()

        for child in children:
            child.is_public = is_public
            pending.append(child.id)


@router.post("/folders", status_code=201)
def create_folder(
    data: FolderInput,
    user: User = Depends(get_current_user),
    db: DbSession = Depends(get_db),
):
    name = data.name.strip()
    if not name:
        raise HTTPException(400, "Folder name cannot be empty")

    is_public = data.is_public

    if data.parent_id is not None:
        parent = owned_folder(db, data.parent_id, user.id)
        is_public = parent.is_public

    duplicate = db.scalar(select(Folder).where(
        Folder.owner_id == user.id,
        Folder.parent_id == data.parent_id,
        Folder.name == name,
    ))
    if duplicate:
        raise HTTPException(409, "Folder already exists")

    folder = Folder(
        owner_id=user.id,
        parent_id=data.parent_id,
        name=name,
        is_public=is_public,
    )
    db.add(folder)
    db.commit()
    db.refresh(folder)

    return {
        "id": folder.id,
        "name": folder.name,
        "parent_id": folder.parent_id,
        "is_public": folder.is_public,
        "owner_id": folder.owner_id,
        "is_owner": True,
    }


@router.get("/folders")
def list_folders(
    parent_id: int | None = None,
    user: User = Depends(get_current_user),
    db: DbSession = Depends(get_db),
):
    if parent_id is not None:
        accessible_folder(db, parent_id, user.id)

    folders = db.scalars(
        select(Folder).where(
            Folder.parent_id == parent_id,
            or_(
                Folder.owner_id == user.id,
                Folder.is_public.is_(True),
            ),
        ).order_by(Folder.name)
    ).all()

    return [
        {
            "id": f.id,
            "name": f.name,
            "parent_id": f.parent_id,
            "is_public": f.is_public,
            "owner_id": f.owner_id,
            "is_owner": f.owner_id == user.id,
        }
        for f in folders
    ]


@router.patch("/folders/{folder_id}")
def rename_folder(
    folder_id: int,
    data: RenameInput,
    user: User = Depends(get_current_user),
    db: DbSession = Depends(get_db),
):
    folder = owned_folder(db, folder_id, user.id)
    name = data.name.strip()
    if not name:
        raise HTTPException(400, "Folder name cannot be empty")

    duplicate = db.scalar(select(Folder).where(
        Folder.owner_id == user.id,
        Folder.parent_id == folder.parent_id,
        Folder.name == name,
        Folder.id != folder.id,
    ))
    if duplicate:
        raise HTTPException(409, "Folder already exists")

    folder.name = name
    db.commit()
    return {"message": "Folder renamed"}


@router.patch("/folders/{folder_id}/visibility")
def change_folder_visibility(
    folder_id: int,
    data: VisibilityInput,
    user: User = Depends(get_current_user),
    db: DbSession = Depends(get_db),
):
    folder = owned_folder(db, folder_id, user.id)

    if folder.parent_id is not None:
        raise HTTPException(
            400,
            "Nested folders inherit their parent's visibility",
        )

    folder.is_public = data.is_public
    set_descendant_visibility(db, folder.id, data.is_public)
    db.commit()

    return {
        "message": "Folder visibility updated",
        "is_public": folder.is_public,
    }


@router.delete("/folders/{folder_id}")
def delete_folder(
    folder_id: int,
    user: User = Depends(get_current_user),
    db: DbSession = Depends(get_db),
):
    folder = owned_folder(db, folder_id, user.id)

    db.query(GalleryFile).filter(
        GalleryFile.folder_id == folder.id,
    ).update({"folder_id": None}, synchronize_session=False)

    db.delete(folder)
    db.commit()
    return {"message": "Folder deleted"}


@router.post("/upload", status_code=201)
def upload_file(
    file: UploadFile = File(...),
    folder_id: int | None = None,
    user: User = Depends(get_current_user),
    db: DbSession = Depends(get_db),
):
    if folder_id is not None:
        accessible_folder(db, folder_id, user.id)

    name = Path(file.filename or "upload").name.strip()
    if not name or name in {".", ".."}:
        raise HTTPException(400, "Invalid filename")

    content = file.file.read(settings.MAX_FILE_SIZE_BYTES + 1)
    file.file.close()

    if len(content) > settings.MAX_FILE_SIZE_BYTES:
        raise HTTPException(413, "File exceeds upload limit")

    storage_name = uuid4().hex
    destination = file_path(user.id, storage_name)
    destination.parent.mkdir(parents=True, exist_ok=True)

    try:
        destination.write_bytes(content)
        item = GalleryFile(
            owner_id=user.id,
            folder_id=folder_id,
            original_name=name,
            storage_name=storage_name,
            content_type=file.content_type or "application/octet-stream",
            size_bytes=len(content),
        )
        db.add(item)
        db.commit()
        db.refresh(item)
    except Exception:
        db.rollback()
        destination.unlink(missing_ok=True)
        raise

    return {
        "id": item.id,
        "name": item.original_name,
        "folder_id": item.folder_id,
        "content_type": item.content_type,
        "size_bytes": item.size_bytes,
        "owner_id": item.owner_id,
    }


@router.get("")
def list_files(
    folder_id: int | None = None,
    user: User = Depends(get_current_user),
    db: DbSession = Depends(get_db),
):
    if folder_id is None:
        condition = GalleryFile.owner_id == user.id
    else:
        folder = accessible_folder(db, folder_id, user.id)
        condition = GalleryFile.folder_id == folder.id

    items = db.scalars(
        select(GalleryFile).where(condition)
        .order_by(GalleryFile.original_name)
    ).all()

    return [
        {
            "id": f.id,
            "name": f.original_name,
            "folder_id": f.folder_id,
            "content_type": f.content_type,
            "size_bytes": f.size_bytes,
            "owner_id": f.owner_id,
            "is_owner": f.owner_id == user.id,
        }
        for f in items
    ]


@router.get("/{file_id}/download")
def download_file(
    file_id: int,
    user: User = Depends(get_current_user),
    db: DbSession = Depends(get_db),
):
    item = accessible_file(db, file_id, user.id)
    path = file_path(item.owner_id, item.storage_name)

    if not path.is_file():
        raise HTTPException(404, "Stored file is missing")

    return FileResponse(
        path,
        media_type=item.content_type,
        filename=item.original_name,
    )


@router.patch("/{file_id}/move")
def move_file(
    file_id: int,
    data: MoveInput,
    user: User = Depends(get_current_user),
    db: DbSession = Depends(get_db),
):
    item = owned_file(db, file_id, user.id)

    if data.folder_id is not None:
        destination = owned_folder(db, data.folder_id, user.id)
        item.folder_id = destination.id
    else:
        item.folder_id = None

    db.commit()
    return {"message": "File moved", "folder_id": item.folder_id}


@router.delete("/{file_id}")
def delete_file(
    file_id: int,
    user: User = Depends(get_current_user),
    db: DbSession = Depends(get_db),
):
    item = accessible_file(db, file_id, user.id)
    path = file_path(item.owner_id, item.storage_name)

    db.delete(item)
    db.commit()
    path.unlink(missing_ok=True)

    return {"message": "File deleted"}

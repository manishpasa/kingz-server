from sqlalchemy import create_engine, text
from sqlalchemy.engine import URL

from app.config import settings


database_url = URL.create(
    drivername="postgresql+psycopg",
    username=settings.POSTGRES_USER,
    password=settings.POSTGRES_PASSWORD,
    host=settings.POSTGRES_HOST,
    port=settings.POSTGRES_PORT,
    database=settings.POSTGRES_DB,
)

engine = create_engine(
    database_url,
    pool_pre_ping=True,
)


def test_database_connection() -> None:
    with engine.connect() as connection:
        connection.execute(text("SELECT 1"))
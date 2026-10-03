from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    APP_NAME: str = "KinGz Server"

    POSTGRES_USER: str = "postgres"
    POSTGRES_PASSWORD: str ="KinGz@123"
    POSTGRES_HOST: str 
    POSTGRES_PORT: int = 5432
    POSTGRES_DB: str = "kingz_server"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


settings = Settings()
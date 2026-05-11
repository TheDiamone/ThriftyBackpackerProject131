import os
from dataclasses import dataclass

from dotenv import load_dotenv

load_dotenv()

@dataclass(frozen=True)
class Settings:
    amadeus_client_id: str | None = os.getenv("AMADEUS_CLIENT_ID")
    amadeus_client_secret: str | None = os.getenv("AMADEUS_CLIENT_SECRET")
    amadeus_hostname: str = os.getenv("AMADEUS_HOSTNAME", "test")
    database_url: str = os.getenv("DATABASE_URL", "sqlite:///./fallback.db")
    rapidapi_key: str | None = os.getenv("RAPIDAPI_KEY")

def get_settings() -> Settings:
    return Settings()

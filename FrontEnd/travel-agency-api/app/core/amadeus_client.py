from functools import lru_cache

from amadeus import Client

from app.core.config import get_settings


@lru_cache(maxsize=1)
def get_amadeus_client() -> Client:
    settings = get_settings()
    return Client(
        client_id=settings.amadeus_client_id,
        client_secret=settings.amadeus_client_secret,
        hostname=settings.amadeus_hostname,
    )

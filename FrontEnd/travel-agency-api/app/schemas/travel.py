from typing import Any

from pydantic import BaseModel


class CoordinatesSchema(BaseModel):
    lat: float
    lon: float


class ActivitiesByCityResponse(BaseModel):
    city: str
    coordinates: CoordinatesSchema
    activities: list[dict[str, Any]]

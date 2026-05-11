from fastapi import APIRouter, Depends, HTTPException, Query
from amadeus import ResponseError

from app.core.amadeus_client import get_amadeus_client
from app.schemas.travel import ActivitiesByCityResponse
from app.services.travel_service import TravelService

router = APIRouter(prefix="", tags=["travel"])

def get_travel_service() -> TravelService:
    return TravelService(get_amadeus_client())


@router.get("/flights/search")
async def search_flights(
    origin: str,
    destination: str,
    date: str,
    service: TravelService = Depends(get_travel_service),
):
    try:
        return service.search_flights(origin, destination, date)
    except ResponseError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error


@router.get("/hotels/by-city")
async def get_hotels_by_city(
    city_code: str,
    service: TravelService = Depends(get_travel_service),
):
    try:
        return service.get_hotels_by_city(city_code)
    except ResponseError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error


@router.get("/activities")
async def get_tours_and_activities(
    lat: float,
    lon: float,
    radius: int = 1,
    service: TravelService = Depends(get_travel_service),
):
    try:
        return service.get_activities(lat, lon, radius)
    except ResponseError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error


@router.get("/activities/by-city", response_model=ActivitiesByCityResponse)
async def get_activities_by_city(
    city_code: str,
    radius: int = 1,
    service: TravelService = Depends(get_travel_service),
):
    try:
        activities = service.get_activities_by_city(city_code, radius)
        if activities is None:
            raise HTTPException(status_code=404, detail="City code not found")
        return activities
    except ResponseError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error


@router.get("/hotels/offers")
async def get_hotel_offers(
    hotel_ids: list[str] = Query(..., alias="hotelIds"),
    check_in_date: str = Query(..., alias="checkInDate"),
    adults: int = 1,
    service: TravelService = Depends(get_travel_service),
):
    try:
        return service.get_hotel_offers(hotel_ids, check_in_date, adults)
    except ResponseError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error

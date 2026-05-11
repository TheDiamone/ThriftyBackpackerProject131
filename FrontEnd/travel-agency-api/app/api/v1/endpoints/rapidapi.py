from fastapi import APIRouter, Depends, HTTPException, Query

from app.core.rapidapi_client import RapidApiError, get_rapidapi_client
from app.services.rapidapi_service import RapidApiService

router = APIRouter(prefix="", tags=["rapidapi"])

def get_rapidapi_service() -> RapidApiService:
    return RapidApiService(get_rapidapi_client())


@router.get("/attractions/search")
async def search_attractions(
    start_date: str = Query(..., description="Format: YYYY-MM-DD"),
    end_date: str = Query(..., description="Format: YYYY-MM-DD"),
    dest_name: str = Query(..., description="Example: New York"),
    country_name: str = Query(..., description="Example: United States"),
    locale: str = Query("en-gb"),
    page_number: int = Query(0, ge=0),
    currency: str = Query("AED"),
    order_by: str = Query("attr_book_score"),
    service: RapidApiService = Depends(get_rapidapi_service),
):
    try:
        return service.search_attractions(
            start_date=start_date,
            end_date=end_date,
            dest_name=dest_name,
            country_name=country_name,
            locale=locale,
            page_number=page_number,
            currency=currency,
            order_by=order_by,
        )
    except RapidApiError as error:
        raise HTTPException(status_code=error.status_code, detail=error.detail) from error


@router.get("/hotels/search")
async def search_hotels(
    page_number: int = Query(0, ge=0),
    dest_type: str = Query("city"),
    dest_name: str = Query(..., description="Example: New York"),
    country_name: str = Query(..., description="Example: United States"),
    units: str = Query("metric"),
    children_number: int = Query(0, ge=0),
    locale: str = Query("en-gb"),
    categories_filter_ids: str | None = Query(None),
    children_ages: str | None = Query(None, description="Comma-separated ages, e.g. 5,0"),
    include_adjacency: bool = Query(True),
    filter_by_currency: str = Query("AED"),
    order_by: str = Query("popularity"),
    checkin_date: str = Query(..., description="Format: YYYY-MM-DD"),
    checkout_date: str = Query(..., description="Format: YYYY-MM-DD"),
    room_number: int = Query(1, ge=1),
    adults_number: int = Query(1, ge=1),
    service: RapidApiService = Depends(get_rapidapi_service),
):
    try:
        return service.search_hotels(
            page_number=page_number,
            dest_type=dest_type,
            dest_name=dest_name,
            country_name=country_name,
            units=units,
            children_number=children_number,
            locale=locale,
            categories_filter_ids=categories_filter_ids,
            children_ages=children_ages,
            include_adjacency=include_adjacency,
            filter_by_currency=filter_by_currency,
            order_by=order_by,
            checkin_date=checkin_date,
            checkout_date=checkout_date,
            room_number=room_number,
            adults_number=adults_number,
        )
    except RapidApiError as error:
        raise HTTPException(status_code=error.status_code, detail=error.detail) from error


@router.get("/flights/search")
async def search_flights(
    depart_date: str = Query(..., description="Format: YYYY-MM-DD"),
    from_code: str = Query(..., description="Example: ONT.AIRPORT"),
    to_code: str = Query(..., description="Example: NYC.CITY"),
    adults: int = Query(1, ge=1),
    locale: str = Query("en-gb"),
    page_number: int = Query(0, ge=0),
    currency: str = Query("AED"),
    order_by: str = Query("BEST"),
    flight_type: str = Query("ONEWAY"),
    cabin_class: str = Query("ECONOMY"),
    children_ages: str | None = Query(None, description="Comma-separated ages, e.g. 5,0"),
    return_date: str | None = Query(None, description="Format: YYYY-MM-DD"),
    service: RapidApiService = Depends(get_rapidapi_service),
):
    try:
        return service.search_flights(
            depart_date=depart_date,
            from_code=from_code,
            to_code=to_code,
            adults=adults,
            locale=locale,
            page_number=page_number,
            currency=currency,
            order_by=order_by,
            flight_type=flight_type,
            cabin_class=cabin_class,
            children_ages=children_ages,
            return_date=return_date,
        )
    except RapidApiError as error:
        raise HTTPException(status_code=error.status_code, detail=error.detail) from error

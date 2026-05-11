from functools import lru_cache
import json
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

from app.core.config import get_settings


class RapidApiError(Exception):
    def __init__(self, status_code: int, detail: str) -> None:
        super().__init__(detail)
        self.status_code = status_code
        self.detail = detail


class RapidApiClient:
    def __init__(self, api_key: str) -> None:
        self.api_key = api_key

    def search_attractions(
        self,
        start_date: str,
        end_date: str,
        dest_name: str,
        country_name: str,
        locale: str = "en-gb",
        page_number: int = 0,
        currency: str = "AED",
        order_by: str = "attr_book_score",
    ):
        dest_id = self._resolve_city_dest_id(dest_name=dest_name, country_name=country_name)

        querystring = {
            "start_date": start_date,
            "end_date": end_date,
            "locale": locale,
            "page_number": str(page_number),
            "currency": currency,
            "order_by": order_by,
            "dest_id": dest_id,
        }

        return self._get(
            host="booking-com.p.rapidapi.com",
            path="v1/attractions/search",
            params=querystring,
        )

    def search_hotels(
        self,
        page_number: int,
        dest_type: str,
        dest_name: str,
        country_name: str,
        units: str,
        children_number: int,
        locale: str,
        include_adjacency: bool,
        filter_by_currency: str,
        order_by: str,
        checkin_date: str,
        checkout_date: str,
        room_number: int,
        adults_number: int,
        categories_filter_ids: str | None = None,
        children_ages: str | None = None,
    ):
        dest_id = self._resolve_city_dest_id(dest_name=dest_name, country_name=country_name)

        querystring = {
            "page_number": str(page_number),
            "dest_type": dest_type,
            "dest_id": dest_id,
            "units": units,
            "locale": locale,
            "include_adjacency": str(include_adjacency).lower(),
            "filter_by_currency": filter_by_currency,
            "order_by": order_by,
            "checkin_date": checkin_date,
            "checkout_date": checkout_date,
            "room_number": str(room_number),
            "adults_number": str(adults_number),
        }

        if categories_filter_ids:
            querystring["categories_filter_ids"] = categories_filter_ids

        if children_number is not None and children_number >= 1:
            querystring["children_number"] = str(children_number)
            if children_ages:
                querystring["children_ages"] = children_ages

        return self._get(
            host="booking-com.p.rapidapi.com",
            path="v1/hotels/search",
            params=querystring,
        )

    def _resolve_city_dest_id(self, dest_name: str, country_name: str) -> str:
        country_code = self._resolve_country_code(country_name)
        response = self._get(
            host="booking-com.p.rapidapi.com",
            path="v1/static/cities",
            params={"country": country_code, "name": dest_name, "page": "0"},
        )

        cities: list[dict[str, Any]] = []
        if isinstance(response, list):
            cities = [item for item in response if isinstance(item, dict)]
        elif isinstance(response, dict):
            result = response.get("result")
            if isinstance(result, list):
                cities = [item for item in result if isinstance(item, dict)]

        if not cities:
            raise RapidApiError(
                404,
                f"No city match found for '{dest_name}' in '{country_name}'.",
            )

        normalized_dest_name = self._normalize(dest_name)
        for city in cities:
            city_name = city.get("name")
            dest_id = city.get("dest_id") or city.get("city_id")
            if not city_name or not dest_id:
                continue

            if self._normalize(str(city_name)) == normalized_dest_name:
                return str(dest_id)

        first_match = cities[0]
        first_match_dest_id = first_match.get("dest_id") or first_match.get("city_id")
        if first_match_dest_id:
            return str(first_match_dest_id)

        raise RapidApiError(
            404,
            f"No destination id found for '{dest_name}' in '{country_name}'.",
        )

    def _resolve_country_code(self, country_name: str) -> str:
        response = self._get(
            host="booking-com.p.rapidapi.com",
            path="v1/static/country",
            params={},
        )

        normalized_country_name = self._normalize(country_name)
        candidates: list[dict[str, Any]] = []
        if isinstance(response, list):
            candidates = [item for item in response if isinstance(item, dict)]
        elif isinstance(response, dict):
            for value in response.values():
                if isinstance(value, list):
                    candidates.extend(item for item in value if isinstance(item, dict))

        for country in candidates:
            code = country.get("code") or country.get("country")
            name = country.get("name") or country.get("country_name")
            if not code or not name:
                continue

            if self._normalize(str(code)) == normalized_country_name:
                return str(code).lower()

            if self._normalize(str(name)) == normalized_country_name:
                return str(code).lower()

        raise RapidApiError(404, f"Country not found: '{country_name}'.")

    @staticmethod
    def _normalize(value: str) -> str:
        return " ".join(value.strip().lower().split())

    def search_rental_cars(
        self,
        pick_up_date: str,
        drop_off_date: str,
        pick_up_time: str,
        drop_off_time: str,
    ):
        querystring = {
            "pick_up_latitude": "40.6397018432617",
            "pick_up_longitude": "-73.7791976928711",
            "drop_off_latitude": "40.6397018432617",
            "drop_off_longitude": "-73.7791976928711",
            "pick_up_date": pick_up_date,
            "drop_off_date": drop_off_date,
            "pick_up_time": pick_up_time,
            "drop_off_time": drop_off_time,
            "driver_age": "30",
            "currency_code": "USD",
            "location": "US",
        }
        return self._get(
            host="booking-com15.p.rapidapi.com",
            path="api/v1/cars/searchCarRentals",
            params=querystring,
        )

    def search_flights(
        self,
        depart_date: str,
        from_code: str,
        to_code: str,
        adults: int,
        locale: str = "en-gb",
        page_number: int = 0,
        currency: str = "AED",
        order_by: str = "BEST",
        flight_type: str = "ONEWAY",
        cabin_class: str = "ECONOMY",
        children_ages: str | None = None,
        return_date: str | None = None,
    ):
        querystring = {
            "depart_date": depart_date,
            "from_code": from_code,
            "to_code": to_code,
            "adults": str(adults),
            "locale": locale,
            "page_number": str(page_number),
            "currency": currency,
            "order_by": order_by,
            "flight_type": flight_type,
            "cabin_class": cabin_class,
        }

        if children_ages:
            querystring["children_ages"] = children_ages

        if return_date:
            querystring["return_date"] = return_date

        return self._get(
            host="booking-com.p.rapidapi.com",
            path="v1/flights/search",
            params=querystring,
        )

    def _get(self, host: str, path: str, params: dict[str, str]):
        request = Request(
            f"https://{host}/{path}?{urlencode(params)}",
            headers={
                "x-rapidapi-host": host,
                "x-rapidapi-key": self.api_key,
                "Content-Type": "application/json",
            },
            method="GET",
        )

        try:
            with urlopen(request, timeout=30) as response:
                payload = response.read().decode("utf-8")
                return json.loads(payload)
        except HTTPError as error:
            detail = error.read().decode("utf-8", errors="replace")
            raise RapidApiError(error.code, detail or str(error)) from error
        except URLError as error:
            raise RapidApiError(500, "Failed to connect to RapidAPI.") from error


@lru_cache(maxsize=1)
def get_rapidapi_client() -> RapidApiClient:
    settings = get_settings()
    if not settings.rapidapi_key:
        raise RapidApiError(500, "RAPIDAPI_KEY is not configured.")

    return RapidApiClient(api_key=settings.rapidapi_key)
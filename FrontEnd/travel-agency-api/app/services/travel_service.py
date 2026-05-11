from amadeus import Client, Location

from app.models.travel import Coordinates


class TravelService:
    def __init__(self, amadeus_client: Client) -> None:
        self.amadeus = amadeus_client

    def search_flights(self, origin: str, destination: str, date: str):
        response = self.amadeus.shopping.flight_offers_search.get(
            originLocationCode=origin,
            destinationLocationCode=destination,
            departureDate=date,
            adults=1,
            currencyCode="USD",
            max=5,
        )
        return response.data

    def get_hotels_by_city(self, city_code: str):
        response = self.amadeus.reference_data.locations.hotels.by_city.get(
            cityCode=city_code
        )
        return response.data

    def get_activities(self, lat: float, lon: float, radius: int = 1):
        response = self.amadeus.shopping.activities.get(
            latitude=lat,
            longitude=lon,
            radius=radius,
        )
        return response.data

    def get_activities_by_city(self, city_code: str, radius: int = 1):
        coords = self.get_city_coordinates(city_code)
        if coords is None:
            return None

        activities = self.get_activities(coords.lat, coords.lon, radius)
        return {
            "city": city_code,
            "coordinates": {"lat": coords.lat, "lon": coords.lon},
            "activities": activities,
        }

    def get_city_coordinates(self, city_code: str) -> Coordinates | None:
        response = self.amadeus.reference_data.locations.get(
            keyword=city_code,
            subType=Location.ANY,
        )

        if not response.data:
            return None

        location = response.data[0]
        geo = location.get("geoCode") or {}
        lat = geo.get("latitude")
        lon = geo.get("longitude")

        if lat is None or lon is None:
            return None

        return Coordinates(lat=lat, lon=lon)

    def get_hotel_offers(
        self,
        hotel_ids: list[str],
        check_in_date: str,
        adults: int = 1,
    ):
        response = self.amadeus.shopping.hotel_offers_search.get(
            hotelIds=",".join(hotel_ids),
            adults=adults,
            checkInDate=check_in_date,
            roomQuantity=1,
        )
        return response.data

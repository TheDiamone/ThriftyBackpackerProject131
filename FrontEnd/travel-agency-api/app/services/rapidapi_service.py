from app.core.rapidapi_client import RapidApiClient

class RapidApiService:
    def __init__(self, rapidapi_client: RapidApiClient) -> None:
        self.rapidapi = rapidapi_client

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
        return self.rapidapi.search_attractions(
            start_date=start_date,
            end_date=end_date,
            dest_name=dest_name,
            country_name=country_name,
            locale=locale,
            page_number=page_number,
            currency=currency,
            order_by=order_by,
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
        return self.rapidapi.search_hotels(
            page_number=page_number,
            dest_type=dest_type,
            dest_name=dest_name,
            country_name=country_name,
            units=units,
            children_number=children_number,
            locale=locale,
            include_adjacency=include_adjacency,
            filter_by_currency=filter_by_currency,
            order_by=order_by,
            checkin_date=checkin_date,
            checkout_date=checkout_date,
            room_number=room_number,
            adults_number=adults_number,
            categories_filter_ids=categories_filter_ids,
            children_ages=children_ages,
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
        return self.rapidapi.search_flights(
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

    def search_rental_cars(
        self,
        pick_up_date: str,
        drop_off_date: str,
        pick_up_time: str,
        drop_off_time: str,
    ):
        return self.rapidapi.search_rental_cars(
            pick_up_date=pick_up_date,
            drop_off_date=drop_off_date,
            pick_up_time=pick_up_time,
            drop_off_time=drop_off_time,
        )
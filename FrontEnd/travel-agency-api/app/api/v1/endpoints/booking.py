from typing import List
from fastapi import APIRouter, Body, Depends, HTTPException, status

from app.core.database import get_db
from app.models.booking import Booking, User, HotelReservation, FlightReservation
from app.schemas.booking import (
    BookingResponse,
    BookingDetailResponse,
    BookingCreate,
    BookingUpdate,
    HotelReservationResponse,
    FlightReservationResponse,
    HotelReservationUpdate,
    FlightReservationUpdate,
)

from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError
from sqlalchemy import text


router = APIRouter(prefix="", tags=["booking"])


def _exists(db: Session, sql: str, params: dict) -> bool:
    return db.execute(text(sql), params).scalar() is not None


def _validate_hotel_code(db: Session, hotel_code: int) -> None:
    if _exists(
        db,
        "SELECT 1 FROM Hotel_Master WHERE Hotel_Code = :hotel_code LIMIT 1",
        {"hotel_code": hotel_code},
    ):
        return

    # Auto-seed unknown hotel codes so booking creation can proceed.
    try:
        db.execute(
            text(
                """
                INSERT INTO Hotel_Master (Hotel_Code, Hotel_Name, City, Country)
                VALUES (:hotel_code, :hotel_name, :city, :country)
                """
            ),
            {
                "hotel_code": hotel_code,
                "hotel_name": f"Auto Hotel {hotel_code}",
                "city": "Unknown",
                "country": "Unknown",
            },
        )
    except IntegrityError as exc:
        db.rollback()
        if not _exists(
            db,
            "SELECT 1 FROM Hotel_Master WHERE Hotel_Code = :hotel_code LIMIT 1",
            {"hotel_code": hotel_code},
        ):
            raise HTTPException(status_code=400, detail=f"Unable to create Hotel_Code {hotel_code} in Hotel_Master.") from exc


def _validate_airline_code(db: Session, airline_code: str) -> None:
    if _exists(
        db,
        "SELECT 1 FROM Airline_Master WHERE Airline_Code = :airline_code LIMIT 1",
        {"airline_code": airline_code},
    ):
        return

    try:
        db.execute(
            text(
                """
                INSERT INTO Airline_Master (Airline_Code, Airline_Name)
                VALUES (:airline_code, :airline_name)
                """
            ),
            {
                "airline_code": airline_code,
                "airline_name": f"Auto Airline {airline_code}",
            },
        )
    except IntegrityError as exc:
        db.rollback()
        if not _exists(
            db,
            "SELECT 1 FROM Airline_Master WHERE Airline_Code = :airline_code LIMIT 1",
            {"airline_code": airline_code},
        ):
            raise HTTPException(status_code=400, detail=f"Unable to create Airline_Code '{airline_code}' in Airline_Master.") from exc


def _validate_airport_code(db: Session, airport_code: str, field_name: str) -> None:
    if _exists(
        db,
        "SELECT 1 FROM Airport_Master WHERE Airport_Code = :airport_code LIMIT 1",
        {"airport_code": airport_code},
    ):
        return

    try:
        db.execute(
            text(
                """
                INSERT INTO Airport_Master (Airport_Code, Airport_Name, City, Country)
                VALUES (:airport_code, :airport_name, :city, :country)
                """
            ),
            {
                "airport_code": airport_code,
                "airport_name": f"Auto Airport {airport_code}",
                "city": "Unknown",
                "country": "Unknown",
            },
        )
    except IntegrityError as exc:
        db.rollback()
        if not _exists(
            db,
            "SELECT 1 FROM Airport_Master WHERE Airport_Code = :airport_code LIMIT 1",
            {"airport_code": airport_code},
        ):
            raise HTTPException(status_code=400, detail=f"Unable to create {field_name} '{airport_code}' in Airport_Master.") from exc

# ==========================================
# ENDPOINTS (CRUD)
# ==========================================

# 1. READ: Get all Bookings (with pagination)
@router.get("/bookings/", response_model=List[BookingResponse], summary="List all bookings")
def read_bookings(skip: int = 0, limit: int = 100, db: Session = Depends(get_db)):
    """
    Retrieve a list of bookings. Use 'skip' and 'limit' for pagination.
    """
    bookings = db.query(Booking).offset(skip).limit(limit).all()
    return bookings

# 2. READ: Get saved bookings by agent and user
@router.get(
    "/bookings/by-agent-user",
    response_model=List[BookingDetailResponse],
    summary="List bookings by agent ID and user ID",
)
def read_bookings_by_agent_and_user(
    agent_id: int,
    user_id: int,
    db: Session = Depends(get_db),
):
    """
    Retrieve saved bookings for a specific agent and user combination.
    """
    return (
        db.query(Booking)
        .filter(Booking.Agent_Id == agent_id, Booking.User_Id == user_id)
        .all()
    )


# 3. READ: Get a specific Booking by ID (with full details)
@router.get("/bookings/{booking_id}", response_model=BookingDetailResponse, summary="Get booking details")
def read_booking(booking_id: int, db: Session = Depends(get_db)):
    """
    Retrieve detailed information about a specific booking, including user details.
    """
    db_booking = db.query(Booking).filter(Booking.Booking_Id == booking_id).first()
    if db_booking is None:
        raise HTTPException(status_code=404, detail=f"Booking with ID {booking_id} not found")
    return db_booking

# 4. READ: Get User ID by Email
@router.get("/users/by-email", summary="Get user ID by email")
def get_user_id_by_email(email: str, db: Session = Depends(get_db)):
    """
    Retrieve the User_ID for a given email address.
    """
    user = db.query(User).filter(User.Email == email).first()
    if user is None:
        raise HTTPException(status_code=404, detail=f"User with email '{email}' not found")
    return {"User_ID": user.User_ID, "email": user.Email}


_HARDCODED_PASSWORD = "CMPE-131@2026"

# 5. READ: Login with email and password
@router.get("/users/login", summary="Login with email and password")
def login_user(email: str, password: str, db: Session = Depends(get_db)):
    """
    Authenticate a user by email and password.
    Returns the User_ID on success.
    """
    if password != _HARDCODED_PASSWORD:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    user = db.query(User).filter(User.Email == email).first()
    if user is None:
        raise HTTPException(status_code=401, detail="Invalid credentials")
    return {"User_ID": user.User_ID, "email": user.Email}


# 6. CREATE: Add a new Booking
@router.get("/setup-seed-data/", include_in_schema=False)
def seed_data(db: Session = Depends(get_db)):
    """Helper endpoint to seed a test user."""
    # Check if seed user exists
    existing_user = db.query(User).filter(User.Email == "test@example.com").first()
    if existing_user:
        return {"message": f"Seed User already exists (ID: {existing_user.User_ID})"}

    # Create a test user needed for bookings
    new_user = User(
        First_Name="Test", 
        Last_Name="Subject", 
        Email="test@example.com", 
        Phone_Number="555-0100"
    )
    db.add(new_user)
    db.commit()
    db.refresh(new_user)
    return {"message": f"Test User created successfully (ID: {new_user.User_ID}). Use this User_Id to create bookings."}

@router.post("/bookings/", response_model=BookingDetailResponse, status_code=status.HTTP_201_CREATED, summary="Create a new booking")
def create_booking(booking: BookingCreate, db: Session = Depends(get_db)):
    """
    Create a new travel booking.
    * The 'User_Id' must refer to an existing User.
    * Optional 'hotel_reservations' and 'flight_reservations' can be created in the same request.
    * Use the /setup-seed-data/ endpoint first if you need a test User_Id.
    """
    # Verify the user exists (crucial validation)
    user_exists = db.query(User).filter(User.User_ID == booking.User_Id).first()
    if not user_exists:
        raise HTTPException(
            status_code=400, 
            detail=f"Cannot create booking. User_Id {booking.User_Id} does not exist."
        )

    booking_data = booking.model_dump(exclude={"hotel_reservations", "flight_reservations"})
    db_booking = Booking(**booking_data)

    db.add(db_booking)
    db.flush()

    for hotel in booking.hotel_reservations:
        _validate_hotel_code(db, hotel.Hotel_Code)
        db.add(
            HotelReservation(
                Booking_Id=db_booking.Booking_Id,
                **hotel.model_dump(),
            )
        )

    for flight in booking.flight_reservations:
        _validate_airline_code(db, flight.Airline_Code)
        _validate_airport_code(db, flight.Origin_Airport_Code, "Origin_Airport_Code")
        _validate_airport_code(db, flight.Destination_Airport_Code, "Destination_Airport_Code")
        db.add(
            FlightReservation(
                Booking_Id=db_booking.Booking_Id,
                **flight.model_dump(),
            )
        )

    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        raise HTTPException(status_code=400, detail="Invalid reservation reference data provided.") from exc

    db.refresh(db_booking)
    return db_booking

# 4. UPDATE: Modify an existing Booking
@router.patch("/bookings/{booking_id}", response_model=BookingResponse, summary="Update an existing booking")
def update_booking(booking_id: int, booking_update: BookingUpdate, db: Session = Depends(get_db)):
    """
    Update specific fields (dates or agent) of an existing booking. 
    Only fields provided in the request body will be changed.
    """
    db_booking = db.query(Booking).filter(Booking.Booking_Id == booking_id).first()
    if db_booking is None:
        raise HTTPException(status_code=404, detail=f"Booking with ID {booking_id} not found")

    # Extract update data, excluding fields set to None
    update_data = booking_update.model_dump(exclude_unset=True)
    
    # Apply updates to the model
    for key, value in update_data.items():
        setattr(db_booking, key, value)

    db.commit()
    db.refresh(db_booking)
    return db_booking

# 5. DELETE: Remove an existing Booking
@router.delete("/bookings/{booking_id}", status_code=status.HTTP_204_NO_CONTENT, summary="Delete a booking")
def delete_booking(booking_id: int, db: Session = Depends(get_db)):
    """
    permanently delete a booking from the system.
    """
    db_booking = db.query(Booking).filter(Booking.Booking_Id == booking_id).first()
    if db_booking is None:
        raise HTTPException(status_code=404, detail=f"Booking with ID {booking_id} not found")

    db.delete(db_booking)
    db.commit()
    return None # HTTP 204 requires no content


# 6. UPDATE: Modify a hotel reservation under a booking
@router.patch(
    "/bookings/{booking_id}/hotel-reservations/{reservation_no}",
    response_model=HotelReservationResponse,
    summary="Update a hotel reservation",
)
def update_hotel_reservation(
    booking_id: int,
    reservation_no: int,
    reservation_update: HotelReservationUpdate = Body(
        ...,
        examples={
            "change_stay_dates": {
                "summary": "Change check-in/check-out dates",
                "value": {
                    "Check_In_Date": "2026-06-11",
                    "Check_Out_Date": "2026-06-20"
                },
            },
            "change_rate_and_hotel": {
                "summary": "Move to a different hotel and update rate",
                "value": {
                    "Hotel_Code": 3,
                    "Rate": 349.99
                },
            },
        },
    ),
    db: Session = Depends(get_db),
):
    db_reservation = (
        db.query(HotelReservation)
        .filter(
            HotelReservation.Reservation_No == reservation_no,
            HotelReservation.Booking_Id == booking_id,
        )
        .first()
    )
    if db_reservation is None:
        raise HTTPException(
            status_code=404,
            detail=f"Hotel reservation {reservation_no} not found for booking {booking_id}",
        )

    update_data = reservation_update.model_dump(exclude_unset=True)
    if "Hotel_Code" in update_data:
        _validate_hotel_code(db, update_data["Hotel_Code"])

    for key, value in update_data.items():
        setattr(db_reservation, key, value)

    db.commit()
    db.refresh(db_reservation)
    return db_reservation


# 7. DELETE: Remove a hotel reservation under a booking
@router.delete(
    "/bookings/{booking_id}/hotel-reservations/{reservation_no}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a hotel reservation",
)
def delete_hotel_reservation(booking_id: int, reservation_no: int, db: Session = Depends(get_db)):
    db_reservation = (
        db.query(HotelReservation)
        .filter(
            HotelReservation.Reservation_No == reservation_no,
            HotelReservation.Booking_Id == booking_id,
        )
        .first()
    )
    if db_reservation is None:
        raise HTTPException(
            status_code=404,
            detail=f"Hotel reservation {reservation_no} not found for booking {booking_id}",
        )

    db.delete(db_reservation)
    db.commit()
    return None


# 8. UPDATE: Modify a flight reservation under a booking
@router.patch(
    "/bookings/{booking_id}/flight-reservations/{reservation_no}",
    response_model=FlightReservationResponse,
    summary="Update a flight reservation",
)
def update_flight_reservation(
    booking_id: int,
    reservation_no: int,
    reservation_update: FlightReservationUpdate = Body(
        ...,
        examples={
            "change_departure_time": {
                "summary": "Update departure date and time",
                "value": {
                    "Departure_Date": "2026-06-10",
                    "Departure_Time": "20:15"
                },
            },
            "change_route_and_airline": {
                "summary": "Switch airline and destination airport",
                "value": {
                    "Airline_Code": "DL",
                    "Destination_Airport_Code": "LAX"
                },
            },
        },
    ),
    db: Session = Depends(get_db),
):
    db_reservation = (
        db.query(FlightReservation)
        .filter(
            FlightReservation.Reservation_No == reservation_no,
            FlightReservation.Booking_Id == booking_id,
        )
        .first()
    )
    if db_reservation is None:
        raise HTTPException(
            status_code=404,
            detail=f"Flight reservation {reservation_no} not found for booking {booking_id}",
        )

    update_data = reservation_update.model_dump(exclude_unset=True)
    if "Airline_Code" in update_data:
        _validate_airline_code(db, update_data["Airline_Code"])
    if "Origin_Airport_Code" in update_data:
        _validate_airport_code(db, update_data["Origin_Airport_Code"], "Origin_Airport_Code")
    if "Destination_Airport_Code" in update_data:
        _validate_airport_code(db, update_data["Destination_Airport_Code"], "Destination_Airport_Code")

    for key, value in update_data.items():
        setattr(db_reservation, key, value)

    db.commit()
    db.refresh(db_reservation)
    return db_reservation


# 9. DELETE: Remove a flight reservation under a booking
@router.delete(
    "/bookings/{booking_id}/flight-reservations/{reservation_no}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete a flight reservation",
)
def delete_flight_reservation(booking_id: int, reservation_no: int, db: Session = Depends(get_db)):
    db_reservation = (
        db.query(FlightReservation)
        .filter(
            FlightReservation.Reservation_No == reservation_no,
            FlightReservation.Booking_Id == booking_id,
        )
        .first()
    )
    if db_reservation is None:
        raise HTTPException(
            status_code=404,
            detail=f"Flight reservation {reservation_no} not found for booking {booking_id}",
        )

    db.delete(db_reservation)
    db.commit()
    return None

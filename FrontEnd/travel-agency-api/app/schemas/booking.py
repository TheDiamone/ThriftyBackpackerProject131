# ==========================================
# PYDANTIC SCHEMAS (Data Validation)
# ==========================================

from datetime import date
from typing import Optional

from pydantic import BaseModel, EmailStr, Field


class HotelReservationCreate(BaseModel):
    Hotel_Code: int
    Check_In_Date: date
    Check_In_Time: Optional[str] = None
    Check_Out_Date: date
    Check_Out_Time: Optional[str] = None
    Rate: Optional[float] = None


class FlightReservationCreate(BaseModel):
    Airline_Code: str
    Flight_Number: str
    Departure_Date: date
    Departure_Time: str
    Arrive_Date: date
    Arrive_Time: str
    Rate: Optional[float] = None
    Origin_Airport_Code: str
    Destination_Airport_Code: str


class HotelReservationUpdate(BaseModel):
    Hotel_Code: Optional[int] = None
    Check_In_Date: Optional[date] = None
    Check_In_Time: Optional[str] = None
    Check_Out_Date: Optional[date] = None
    Check_Out_Time: Optional[str] = None
    Rate: Optional[float] = None


class FlightReservationUpdate(BaseModel):
    Airline_Code: Optional[str] = None
    Flight_Number: Optional[str] = None
    Departure_Date: Optional[date] = None
    Departure_Time: Optional[str] = None
    Arrive_Date: Optional[date] = None
    Arrive_Time: Optional[str] = None
    Rate: Optional[float] = None
    Origin_Airport_Code: Optional[str] = None
    Destination_Airport_Code: Optional[str] = None


class HotelReservationResponse(HotelReservationCreate):
    Reservation_No: int
    Hotel_Name: Optional[str] = None

    class Config:
        from_attributes = True


class FlightReservationResponse(FlightReservationCreate):
    Reservation_No: int

    class Config:
        from_attributes = True


# Base schema (fields shared by Create and Response)
class BookingBase(BaseModel):
    User_Id: int
    Agent_Id: Optional[int] = None
    Start_Date: date
    End_Date: date

# Schema used when CREATING a new Booking (Request Body)
class BookingCreate(BookingBase):
    hotel_reservations: list[HotelReservationCreate] = Field(default_factory=list)
    flight_reservations: list[FlightReservationCreate] = Field(default_factory=list)

# Schema used when UPDATING an existing Booking (Request Body)
class BookingUpdate(BaseModel):
    Start_Date: Optional[date] = None
    End_Date: Optional[date] = None
    Agent_Id: Optional[int] = None

# Schema used when RETURNING a Booking (Response Body)
class BookingResponse(BookingBase):
    Booking_Id: int

    class Config:
        # Crucial for SQLAlchemy compatibility
        from_attributes = True

# Helper schema to show User details alongside Bookings
class UserResponse(BaseModel):
    First_Name: str
    Last_Name: str
    Email: EmailStr

    class Config:
        from_attributes = True

# Advanced Response schema including User information
class BookingDetailResponse(BookingResponse):
    user: UserResponse
    hotel_reservations: list[HotelReservationResponse] = Field(default_factory=list)
    flight_reservations: list[FlightReservationResponse] = Field(default_factory=list)

from fastapi import FastAPI

from app.core.database import Base  # Usually imported from core/database though
import app.models # This triggers the registration of everything in the folder

from app.api.v1.router import api_router

app = FastAPI(title="Travel Agency API")
app.include_router(api_router, prefix="/api/v1")


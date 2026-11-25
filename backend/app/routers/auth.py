from fastapi import APIRouter, status

from app.core.security import create_access_token
from app.dependencies import AppSettings, DatabaseSession, unauthorized
from app.models import User
from app.schemas.auth import LoginRequest, TokenResponse
from app.schemas.user import UserRegistration, UserResponse
from app.services.users import authenticate_user, register_user

router = APIRouter(prefix="/auth", tags=["Authentification"])


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register(data: UserRegistration, session: DatabaseSession) -> User:
    return register_user(session, data)


@router.post("/login", response_model=TokenResponse)
def login(data: LoginRequest, session: DatabaseSession, settings: AppSettings) -> TokenResponse:
    user = authenticate_user(session, data)
    if user is None:
        raise unauthorized("Identifiant ou mot de passe incorrect.")
    return TokenResponse(access_token=create_access_token(user.id, settings))

from collections import Counter, deque
from contextlib import contextmanager
from hmac import compare_digest
from threading import BoundedSemaphore, Lock
from time import monotonic

from fastapi import APIRouter, HTTPException, Request, Response, status

from app.core.security import create_access_token
from app.dependencies import AppSettings, DatabaseSession, unauthorized
from app.models import User
from app.schemas.auth import LoginRequest, TokenResponse
from app.schemas.user import UserRegistration, UserResponse
from app.services.users import authenticate_user, register_user

router = APIRouter(prefix="/auth", tags=["Authentification"])


class AuthAttempts:
    def __init__(self) -> None:
        self.recent: deque[tuple[float, str]] = deque()
        self.by_account: Counter[str] = Counter()
        self.lock = Lock()
        self.workers = BoundedSemaphore(3)

    @contextmanager
    def allow(self, key: str):
        with self.lock:
            now = monotonic()
            while self.recent and self.recent[0][0] <= now - 60:
                _, expired_key = self.recent.popleft()
                self.by_account[expired_key] -= 1
                if not self.by_account[expired_key]:
                    del self.by_account[expired_key]
            if self.by_account[key] >= 15 or len(self.recent) >= 120:
                raise HTTPException(429, "Trop de demandes. Réessaie dans une minute.", headers={"Retry-After": "60"})
            if not self.workers.acquire(blocking=False):
                raise HTTPException(429, "Le serveur est occupé. Réessaie.", headers={"Retry-After": "3"})
            self.recent.append((now, key))
            self.by_account[key] += 1
        try:
            yield
        finally:
            self.workers.release()


@router.post("/register", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
def register(data: UserRegistration, session: DatabaseSession, settings: AppSettings, request: Request) -> User:
    required_code = settings.registration_code
    if required_code is not None:
        submitted_code = data.registration_code.get_secret_value() if data.registration_code else ""
        if not compare_digest(submitted_code.encode(), required_code.get_secret_value().encode()):
            raise HTTPException(403, "Code d'invitation absent ou incorrect.")
    with request.app.state.auth_attempts.allow("register"):
        return register_user(session, data)


@router.post("/login", response_model=TokenResponse)
def login(
    data: LoginRequest, response: Response, session: DatabaseSession, settings: AppSettings, request: Request,
) -> TokenResponse:
    with request.app.state.auth_attempts.allow(f"login:{data.username}"):
        user = authenticate_user(session, data)
    if user is None:
        raise unauthorized("Identifiant ou mot de passe incorrect.")
    response.headers["Cache-Control"] = "no-store"
    return TokenResponse(
        access_token=create_access_token(user.id, settings),
        expires_in=settings.jwt_access_token_minutes * 60,
        user_id=user.id,
        username=user.username,
    )

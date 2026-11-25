from hashlib import sha256
from secrets import token_urlsafe
from time import time
from uuid import UUID, uuid4

import jwt
from fastapi import APIRouter, HTTPException
from fastapi.responses import HTMLResponse, JSONResponse
from pydantic import BaseModel, Field
from sqlalchemy import delete, select, update

from app.core.security import create_access_token
from app.dependencies import AppSettings, DatabaseSession
from app.models import GoogleLogin, User
from app.services import google
from app.services.users import UserConflict

router = APIRouter(prefix="/auth/google", tags=["Connexion Google"])
NO_STORE = {"Cache-Control": "no-store", "Referrer-Policy": "no-referrer"}
LOGIN_SECONDS = 300


class PollRequest(BaseModel):
    id: UUID
    secret: str = Field(min_length=43, max_length=43, pattern=r"^[A-Za-z0-9_-]+$")


def browser_result(success: bool) -> HTMLResponse:
    message = (
        "Connexion réussie. Tu peux retourner dans Dev Quiz."
        if success else "Connexion interrompue. Retourne dans Dev Quiz pour réessayer."
    )
    return HTMLResponse(
        '<!doctype html><html lang="fr"><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width, initial-scale=1">'
        f"<title>Dev Quiz</title><h1>Dev Quiz</h1><p>{message}</p>",
        headers=NO_STORE,
    )


@router.post("/start")
def start(session: DatabaseSession, settings: AppSettings) -> JSONResponse:
    if not google.configured(settings):
        raise HTTPException(503, "La connexion Google doit être configurée sur le serveur.")
    now = int(time())
    identifier, secret = uuid4(), token_urlsafe(32)
    session.execute(delete(GoogleLogin).where(GoogleLogin.expires_at <= now))
    session.add(GoogleLogin(
        id=identifier, poll_hash=sha256(secret.encode()).hexdigest(),
        expires_at=now + LOGIN_SECONDS, status="pending",
    ))
    session.commit()
    return JSONResponse(
        {"id": str(identifier), "secret": secret, "authorization_url": google.authorization_url(settings, str(identifier))},
        headers=NO_STORE,
    )


@router.get("/callback", include_in_schema=False)
def callback(
    session: DatabaseSession, settings: AppSettings, state: UUID | None = None,
    code: str | None = None, error: str | None = None,
) -> HTMLResponse:
    if state is None or not google.configured(settings):
        return browser_result(False)
    claimed = session.execute(
        update(GoogleLogin).where(
            GoogleLogin.id == state, GoogleLogin.expires_at > int(time()), GoogleLogin.status == "pending",
        ).values(status="processing").returning(GoogleLogin.id)
    ).scalar_one_or_none()
    session.commit()
    if claimed is None:
        return browser_result(False)

    user, result = None, "failed"
    if code and not error:
        try:
            subject, email = google.exchange_code(settings, code, str(state))
            user = google.google_user(session, subject, email)
            result = "complete"
        except UserConflict:
            result = "conflict"
        except (OSError, ValueError, KeyError, TypeError, jwt.PyJWTError):
            pass

    saved = session.execute(
        update(GoogleLogin).where(
            GoogleLogin.id == state, GoogleLogin.expires_at > int(time()),
        ).values(status=result, user_id=user.id if user else None).returning(GoogleLogin.id)
    ).scalar_one_or_none()
    session.commit()
    return browser_result(saved is not None and result == "complete")


@router.post("/poll")
def poll(data: PollRequest, session: DatabaseSession, settings: AppSettings) -> JSONResponse:
    # Seule l’application qui détient le secret séparé reçoit le JWT.
    matches = (
        GoogleLogin.id == data.id,
        GoogleLogin.poll_hash == sha256(data.secret.encode()).hexdigest(),
        GoogleLogin.expires_at > int(time()),
    )
    finished = session.execute(
        delete(GoogleLogin).where(*matches, GoogleLogin.status.in_(("complete", "failed", "conflict")))
        .returning(GoogleLogin.status, GoogleLogin.user_id)
    ).one_or_none()
    if finished:
        result, user_id = finished
        user = session.get(User, user_id) if user_id else None
        session.commit()
        if user:
            return JSONResponse({
                "status": result,
                "id": str(user.id),
                "username": user.username,
                "access_token": create_access_token(user.id, settings),
                "expires_in": settings.jwt_access_token_minutes * 60,
            }, headers=NO_STORE)
        return JSONResponse({"status": result}, headers=NO_STORE)

    pending = session.scalar(select(GoogleLogin.id).where(*matches))
    return JSONResponse({"status": "pending" if pending else "expired"}, headers=NO_STORE)

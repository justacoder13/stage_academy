import json
import re
from base64 import urlsafe_b64encode
from hashlib import sha256
from hmac import digest
from urllib.parse import urlencode, urlsplit
from urllib.request import Request, urlopen

import jwt
from pydantic import TypeAdapter

from app.core.config import Settings
from app.models import User
from app.schemas.user import Email
from app.services.users import UserConflict, persist_user
from sqlalchemy import select
from sqlalchemy.orm import Session

GOOGLE_AUTH = "https://accounts.google.com/o/oauth2/v2/auth"
GOOGLE_TOKEN = "https://oauth2.googleapis.com/token"
google_keys = jwt.PyJWKClient("https://www.googleapis.com/oauth2/v3/certs", timeout=8)
email_validator = TypeAdapter(Email)


def configured(settings: Settings) -> bool:
    try:
        uri = urlsplit(settings.google_redirect_uri)
    except ValueError:
        return False
    secure = uri.scheme == "https" or (uri.scheme == "http" and uri.hostname in {"localhost", "127.0.0.1"})
    return bool(settings.google_client_id and settings.google_client_secret.get_secret_value()
                and secure and uri.hostname and not uri.query and not uri.fragment
                and not uri.username and uri.path.endswith("/auth/google/callback"))


def authorization_url(settings: Settings, state: str) -> str:
    return GOOGLE_AUTH + "?" + urlencode({
        "client_id": settings.google_client_id,
        "redirect_uri": settings.google_redirect_uri,
        "response_type": "code",
        "scope": "openid email",
        "state": state,
        "nonce": state,
        "code_challenge": base64url(sha256(code_verifier(settings, state).encode()).digest()),
        "code_challenge_method": "S256",
        "prompt": "select_account",
    })


def base64url(value: bytes) -> str:
    return urlsafe_b64encode(value).decode().rstrip("=")


def code_verifier(settings: Settings, state: str) -> str:
    key = settings.jwt_secret_key.get_secret_value().encode()
    return base64url(digest(key, f"google-pkce:{state}".encode(), "sha256"))


def exchange_code(settings: Settings, code: str, nonce: str) -> tuple[str, str]:
    form = urlencode({
        "code": code,
        "client_id": settings.google_client_id,
        "client_secret": settings.google_client_secret.get_secret_value(),
        "redirect_uri": settings.google_redirect_uri,
        "grant_type": "authorization_code",
        "code_verifier": code_verifier(settings, nonce),
    }).encode()
    request = Request(GOOGLE_TOKEN, data=form, headers={"Content-Type": "application/x-www-form-urlencoded"})
    with urlopen(request, timeout=8) as response:
        token = json.load(response)["id_token"]
    claims = jwt.decode(
        token,
        google_keys.get_signing_key_from_jwt(token).key,
        algorithms=["RS256"],
        audience=settings.google_client_id,
        issuer=["https://accounts.google.com", "accounts.google.com"],
        options={"require": ["sub", "exp", "iat", "iss", "aud", "nonce"]},
    )
    if (claims["nonce"] != nonce or claims.get("email_verified") is not True
            or claims.get("azp", settings.google_client_id) != settings.google_client_id
            or not 0 < len(claims["sub"]) <= 255):
        raise ValueError("Invalid Google identity")
    return claims["sub"], email_validator.validate_python(claims.get("email"))


def google_user(session: Session, subject: str, email: str) -> User:
    by_subject = select(User).where(User.google_sub == subject)
    user = session.scalar(by_subject)
    if user is not None:
        return user
    # Un compte avec mot de passe doit conserver sa méthode de connexion.
    # Une adresse identique ne suffit pas pour associer automatiquement les comptes.
    handle = re.sub(r"[^a-z0-9_.-]", "-", email.split("@", 1)[0])[:17].strip(".-")
    suffix = sha256(subject.encode()).hexdigest()[:12]
    try:
        return persist_user(session, User(username=f"{handle or 'joueur'}-{suffix}", email=email, google_sub=subject))
    except UserConflict:
        user = session.scalar(by_subject)
        if user is not None:
            return user
        raise

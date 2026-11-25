from io import BytesIO
from json import dumps
from time import time
from types import SimpleNamespace
from urllib.parse import parse_qs, urlsplit
from uuid import uuid4

import jwt
import pytest
from cryptography.hazmat.primitives.asymmetric import rsa
from pydantic import SecretStr

from app.services import google

from test_api import register


@pytest.fixture
def google_provider(client, monkeypatch):
    settings = client.app.state.settings
    settings.google_client_id = "test-client"
    settings.google_client_secret = SecretStr("test-secret")
    settings.google_redirect_uri = "https://quiz.example.com/auth/google/callback"
    monkeypatch.setattr(google, "exchange_code", lambda _settings, code, _nonce: (code, f"{code}@example.com"))
    return client


def start_login(client):
    response = client.post("/auth/google/start")
    assert response.status_code == 200
    body = response.json()
    params = parse_qs(urlsplit(body["authorization_url"]).query)
    assert params["state"] == params["nonce"] == [body["id"]]
    assert params["code_challenge_method"] == ["S256"]
    return {key: body[key] for key in ("id", "secret")}


def finish_login(client, flow, code):
    client.get("/auth/google/callback", params={"state": flow["id"], "code": code})
    return client.post("/auth/google/poll", json=flow).json()


def bearer(result):
    return {"Authorization": f"Bearer {result['access_token']}"}


def test_google_requires_server_configuration(client):
    assert client.post("/auth/google/start").status_code == 503


def test_google_signed_identity_checks_signature_audience_and_nonce(client, monkeypatch):
    settings = client.app.state.settings
    settings.google_client_id = "quiz-client"
    private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    monkeypatch.setattr(
        google.google_keys, "get_signing_key_from_jwt",
        lambda _token: SimpleNamespace(key=private_key.public_key()),
    )
    now = int(time())
    claims = {
        "iss": "https://accounts.google.com", "aud": "quiz-client", "sub": "stable-id",
        "email": "test@example.com", "email_verified": True, "iat": now,
        "exp": now + 120, "nonce": "expected",
    }

    def token_response(overrides, signing_key=private_key):
        token = jwt.encode(claims | overrides, signing_key, algorithm="RS256")
        monkeypatch.setattr(google, "urlopen", lambda *_args, **_kwargs: BytesIO(dumps({"id_token": token}).encode()))

    token_response({})
    assert google.exchange_code(settings, "code", "expected") == ("stable-id", "test@example.com")
    token_response({"aud": "other-client"})
    with pytest.raises(jwt.InvalidAudienceError):
        google.exchange_code(settings, "code", "expected")
    token_response({"nonce": "other"})
    with pytest.raises(ValueError):
        google.exchange_code(settings, "code", "expected")
    other_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    token_response({}, other_key)
    with pytest.raises(jwt.InvalidSignatureError):
        google.exchange_code(settings, "code", "expected")


def test_callback_requires_state_and_polling_secret_and_issues_one_token(google_provider):
    client = google_provider
    flow = start_login(client)
    client.get("/auth/google/callback", params={"state": str(uuid4()), "code": "player"})
    assert client.post("/auth/google/poll", json=flow).json() == {"status": "pending"}

    client.get("/auth/google/callback", params={"state": flow["id"], "code": "player"})
    assert client.post("/auth/google/poll", json=flow | {"secret": "a" * 43}).json() == {"status": "expired"}
    completed = client.post("/auth/google/poll", json=flow).json()
    assert completed["status"] == "complete"
    assert client.get("/users/me", headers=bearer(completed)).json()["email"] == "player@example.com"
    assert client.post("/auth/google/poll", json=flow).json() == {"status": "expired"}


def test_google_does_not_take_over_an_existing_password_account(google_provider):
    client = google_provider
    assert register(client, email="player@example.com").status_code == 201
    assert finish_login(client, start_login(client), "player") == {"status": "conflict"}


def test_google_cancel_is_returned_to_app(google_provider):
    client = google_provider
    flow = start_login(client)
    client.get("/auth/google/callback", params={"state": flow["id"], "error": "access_denied"})
    assert client.post("/auth/google/poll", json=flow).json() == {"status": "failed"}


def test_two_devices_share_one_score_and_duplicate_rounds_are_ignored(google_provider):
    client = google_provider
    phone = finish_login(client, start_login(client), "player")
    desktop = finish_login(client, start_login(client), "player")
    other = finish_login(client, start_login(client), "other")
    assert phone["id"] == desktop["id"] != other["id"]
    first_round = {"id": str(uuid4()), "points": 4}
    second_round = {"id": str(uuid4()), "points": 3}
    route = "/leaderboard/rounds"
    assert client.post(route, json={"rounds": [first_round]}).status_code == 401
    assert client.post(route, headers=bearer(phone), json={"rounds": [first_round]}).status_code == 200
    batch = {"rounds": [first_round | {"points": 99}, second_round]}
    assert client.post(route, headers=bearer(desktop), json=batch).status_code == 200
    assert client.post(route, headers=bearer(other), json={"rounds": [first_round]}).status_code == 200
    assert client.get("/leaderboard").json() == [{"name": phone["username"], "points": 7}]
    assert client.delete("/users/me", headers=bearer(phone)).status_code == 204
    assert client.get("/leaderboard").json() == []

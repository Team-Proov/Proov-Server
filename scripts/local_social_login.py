#!/usr/bin/env python3
"""Run a real provider login against the local Proov server without extra dependencies."""

import argparse
import base64
import binascii
import getpass
import json
import os
import secrets
import sys
import time
import webbrowser
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, urlencode, urlparse
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
PROVIDERS = {
    "KAKAO": ("https://kauth.kakao.com/oauth/authorize", "https://kauth.kakao.com/oauth/token"),
    "GOOGLE": ("https://accounts.google.com/o/oauth2/v2/auth", "https://oauth2.googleapis.com/token"),
}


def read_env_file(path: Path) -> dict[str, str]:
    values = {}
    if path.exists():
        for line in path.read_text().splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip().strip('"').strip("'")
    return values


def setting(name: str, file_values: dict[str, str]) -> str:
    return os.environ.get(name, file_values.get(name, "")).strip()


def claims_from_token(token: str) -> dict:
    try:
        payload = token.split(".")[1]
        return json.loads(base64.urlsafe_b64decode(payload + "=" * (-len(payload) % 4)))
    except (IndexError, ValueError, binascii.Error, UnicodeDecodeError) as error:
        raise RuntimeError("제공자가 반환한 ID 토큰 형식이 올바르지 않습니다.") from error


def request_json(url: str, data: dict[str, str], content_type: str) -> tuple[int, dict]:
    encoded = (json.dumps(data) if content_type == "application/json" else urlencode(data)).encode()
    request = Request(url, data=encoded, headers={"Content-Type": content_type}, method="POST")
    try:
        with urlopen(request, timeout=10) as response:
            return response.status, json.load(response)
    except HTTPError as error:
        try:
            return error.code, json.load(error)
        except (ValueError, UnicodeDecodeError):
            return error.code, {}
    except (URLError, TimeoutError) as error:
        raise RuntimeError(f"요청 실패: {error.reason if isinstance(error, URLError) else error}") from error


def authorization_code(provider: str, client_id: str, port: int, timeout: int) -> tuple[str, str, str]:
    redirect_uri = f"http://localhost:{port}/callback"
    state = secrets.token_urlsafe(32)
    nonce = secrets.token_urlsafe(32)
    parameters = {
        "response_type": "code",
        "client_id": client_id,
        "redirect_uri": redirect_uri,
        "state": state,
        "nonce": nonce,
    }
    if provider == "GOOGLE":
        parameters["scope"] = "openid"
    url = PROVIDERS[provider][0] + "?" + urlencode(parameters)
    result: dict[str, str] = {}

    class CallbackHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            parsed = urlparse(self.path)
            if parsed.path != "/callback":
                self.send_error(404)
                return
            result.update({key: values[0] for key, values in parse_qs(parsed.query).items()})
            body = "인가 코드를 받았습니다. 터미널에서 결과를 확인하세요.".encode()
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def log_message(self, format, *args):
            pass  # The callback URL contains a one-time authorization code.

    with HTTPServer(("127.0.0.1", port), CallbackHandler) as server:
        server.timeout = 1
        print(f"등록할 리다이렉트 URI: {redirect_uri}")
        print(f"인가 URL: {url}")
        webbrowser.open(url)
        deadline = time.monotonic() + timeout
        while not result and time.monotonic() < deadline:
            server.handle_request()

    if not result:
        raise RuntimeError("인가 코드 대기 시간이 지났습니다. 리다이렉트 URI 등록과 브라우저 로그인을 확인하세요.")
    if result.get("state") != state:
        raise RuntimeError("state가 일치하지 않아 인가 코드를 사용하지 않았습니다.")
    if "error" in result:
        raise RuntimeError(f"제공자 로그인 실패: {result['error']}")
    if "code" not in result:
        raise RuntimeError("리다이렉트 응답에 인가 코드가 없습니다.")
    return result["code"], nonce, redirect_uri


def exchange_code(provider: str, code: str, client_id: str, client_secret: str, redirect_uri: str) -> str:
    form = {
        "grant_type": "authorization_code",
        "client_id": client_id,
        "redirect_uri": redirect_uri,
        "code": code,
    }
    if client_secret:
        form["client_secret"] = client_secret
    status, response = request_json(PROVIDERS[provider][1], form, "application/x-www-form-urlencoded")
    if status != 200:
        raise RuntimeError(f"인가 코드 교환 실패 (HTTP {status}): {response.get('error', '제공자 설정을 확인하세요.')}")
    token = response.get("id_token")
    if not isinstance(token, str):
        raise RuntimeError("ID 토큰이 없습니다. 제공자 앱의 OIDC 설정(구글은 openid scope)을 확인하세요.")
    return token


def main() -> int:
    parser = argparse.ArgumentParser(description="실제 소셜 ID 토큰으로 로컬 로그인 API를 확인합니다.")
    parser.add_argument("provider", choices=["kakao", "google", "apple"])
    parser.add_argument("--port", type=int, default=8765, help="카카오·구글 로컬 콜백 포트 (기본값: 8765)")
    parser.add_argument("--timeout", type=int, default=180, help="브라우저 로그인 대기 초 (기본값: 180)")
    parser.add_argument("--api-url", default="http://localhost:8080", help="로컬 서버 주소")
    args = parser.parse_args()
    provider = args.provider.upper()
    api_url = urlparse(args.api_url)
    if api_url.scheme != "http" or api_url.hostname not in {"localhost", "127.0.0.1", "::1"}:
        parser.error("--api-url은 로컬 HTTP 주소만 사용할 수 있습니다.")
    if not 1 <= args.port <= 65535 or args.timeout <= 0:
        parser.error("--port와 --timeout은 양수여야 합니다.")

    values = read_env_file(ROOT / ".env")
    configured_ids = [item.strip() for item in setting(f"{provider}_CLIENT_IDS", values).split(",") if item.strip()]
    if not configured_ids:
        parser.error(f".env의 {provider}_CLIENT_IDS를 먼저 설정하세요.")

    if provider == "APPLE":
        print("Apple 앱/등록된 HTTPS 콜백에서 받은 identityToken을 붙여 넣으세요 (입력은 화면에 표시되지 않습니다).")
        id_token = getpass.getpass("ID token: ").strip()
        if not id_token:
            parser.error("ID 토큰이 비어 있습니다.")
    else:
        client_id = setting(f"LOCAL_TEST_{provider}_CLIENT_ID", values) or configured_ids[0]
        if client_id not in configured_ids:
            parser.error(f"{client_id!r}가 {provider}_CLIENT_IDS에 없습니다.")
        client_secret = setting(f"LOCAL_TEST_{provider}_CLIENT_SECRET", values)
        if provider == "GOOGLE" and not client_secret:
            parser.error("Google 웹 클라이언트의 LOCAL_TEST_GOOGLE_CLIENT_SECRET을 설정하세요.")
        code, nonce, redirect_uri = authorization_code(provider, client_id, args.port, args.timeout)
        id_token = exchange_code(provider, code, client_id, client_secret, redirect_uri)
        if claims_from_token(id_token).get("nonce") != nonce:
            raise RuntimeError("ID 토큰의 nonce가 요청과 일치하지 않습니다.")

    status, response = request_json(
        args.api_url.rstrip("/") + "/api/v1/auth/login",
        {"provider": provider, "idToken": id_token},
        "application/json",
    )
    print(f"로컬 로그인 응답: HTTP {status}, code={response.get('code', '(없음)')}")
    if status == 200:
        print(f"access token 만료 시각: {response.get('data', {}).get('accessTokenExpiresAt', '(없음)')}")
    elif status == 404 and response.get("code") == "SIGNUP_REQUIRED":
        print(f"ID 토큰 검증 성공. 아직 가입되지 않은 소셜 계정입니다. provider={provider}, sub={claims_from_token(id_token).get('sub')}")
    else:
        return 1
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (RuntimeError, OSError) as error:
        print(f"오류: {error}", file=sys.stderr)
        sys.exit(1)

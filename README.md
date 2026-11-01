# Proov Server

## 로컬 소셜 로그인 확인

카카오·구글의 실제 인가 코드를 로컬 콜백에서 받아 ID 토큰으로 교환하고, `POST /api/v1/auth/login`까지 호출하는 도구가 있습니다. Python 3 표준 라이브러리만 사용하며, 인가 코드·ID 토큰·클라이언트 시크릿은 출력하거나 파일에 저장하지 않습니다.

1. `.env.sample`을 `.env`로 복사하고 DB 설정, `JWT_SECRET`, 테스트할 제공자의 `*_CLIENT_IDS`를 채웁니다. `JWT_SECRET`은 `openssl rand -base64 48`로 만들 수 있습니다.
2. 카카오는 카카오 로그인과 OpenID Connect를 활성화하고 REST API 키에 `http://localhost:8765/callback`을 리다이렉트 URI로 등록합니다. 클라이언트 시크릿을 사용하는 앱이면 `.env`에 `LOCAL_TEST_KAKAO_CLIENT_SECRET`도 적습니다.
3. 구글은 **웹 애플리케이션** OAuth 클라이언트에 같은 리다이렉트 URI를 등록합니다. 해당 클라이언트 ID를 `GOOGLE_CLIENT_IDS`와 `LOCAL_TEST_GOOGLE_CLIENT_ID`에, 시크릿을 `LOCAL_TEST_GOOGLE_CLIENT_SECRET`에 적습니다. 여러 앱 ID를 서버에 등록했다면 `GOOGLE_CLIENT_IDS`에는 쉼표로 구분해 넣을 수 있습니다. OAuth 동의 화면이 테스트 상태라면 로그인할 계정을 테스트 사용자로 등록합니다.
4. 로컬 DB와 서버를 실행합니다.

   ```bash
   docker compose --env-file .env -f docker/compose.local.yaml up -d
   ./gradlew bootRun
   ```

5. 다른 터미널에서 원하는 제공자를 실행하고 브라우저 로그인을 완료합니다.

   ```bash
   python3 scripts/local_social_login.py kakao
   python3 scripts/local_social_login.py google
   ```

8765 포트를 이미 사용 중이면 `--port`로 바꿀 수 있습니다. 이때 제공자 콘솔의 리다이렉트 URI도 같은 포트로 등록해야 합니다.

스크립트는 콜백의 `state`와 ID 토큰의 `nonce`를 확인한 다음 서버에 ID 토큰을 보냅니다. `404 SIGNUP_REQUIRED`는 **ID 토큰 검증은 성공했지만 아직 users 테이블에 해당 소셜 계정이 없다**는 뜻입니다. 현재 회원가입 API가 없으므로, 발급까지 확인하려면 출력된 `provider`·`sub`로 로컬 DB에 테스트 회원을 넣고 다시 로그인합니다. 예를 들어 새 로컬 DB에서 첫 테스트 회원은 다음처럼 추가할 수 있습니다.

```bash
docker compose --env-file .env -f docker/compose.local.yaml exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

```sql
INSERT INTO users (id, social_provider, social_id, created_at, updated_at)
VALUES ('0ABCDEFGHJKMN', 'KAKAO', '<출력된 sub>', now(), now());
```

Apple 웹 로그인은 등록된 HTTPS 도메인과 반환 URL이 필요합니다. 로컬에서는 Apple 앱의 Sign in with Apple SDK 또는 등록된 콜백에서 받은 `identityToken`을 다음 명령에 붙여 넣어 동일한 서버 검증을 확인합니다. 입력값은 터미널에 표시되지 않습니다.

```bash
python3 scripts/local_social_login.py apple
```

이 도구의 `nonce` 검사는 로컬에서만 수행합니다. 실제 클라이언트 로그인 흐름에서 `nonce`를 사용한다면 서버에서도 요청과 ID 토큰의 값을 비교해야 합니다.

참고: [카카오 로그인 REST API](https://developers.kakao.com/docs/ko/kakaologin/rest-api), [Google 웹 서버 OAuth](https://developers.google.com/identity/protocols/oauth2/web-server), [Apple 웹 로그인 설정](https://developer.apple.com/documentation/signinwithapple/configuring-your-environment-for-sign-in-with-apple).

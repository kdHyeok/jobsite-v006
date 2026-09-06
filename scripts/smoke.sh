#!/usr/bin/env bash
# 배포·설정 변경 뒤 반드시 통과해야 하는 최소 검증.
#
#   bash scripts/smoke.sh                 # .env 의 PUBLIC_BASE_URL 사용
#   bash scripts/smoke.sh https://job.donhse.duckdns.org
#
# 이 스크립트가 잡는 것은 이 프로젝트에서 실제로 반복됐던 실패들이다:
#   - nginx 가 포트 없는 절대 URL 로 리다이렉트 (redirect_uri_mismatch, /admin 302)
#   - SecurityConfig 매처 누락으로 미인증 접근이 통과
#   - 끝 슬래시 경로가 500
#   - CSRF 토큰 없이 변경 요청이 통과
#   - 세션 쿠키 SameSite=Strict 로 OAuth 콜백이 깨짐
# 종료코드 0 = 전부 통과. 하나라도 실패하면 1.

set -u
cd "$(dirname "$0")/.."

BASE="${1:-}"
if [ -z "$BASE" ] && [ -f .env ]; then
  BASE="$(grep -E '^PUBLIC_BASE_URL=' .env | cut -d= -f2- | tr -d '\r')"
fi
BASE="${BASE:-http://127.0.0.1:8088}"
BASE="${BASE%/}"

fail=0
pass=0
check() { # name expected actual
  if [ "$2" = "$3" ]; then
    printf '  ok    %-48s %s\n' "$1" "$3"; pass=$((pass+1))
  else
    printf '  FAIL  %-48s expected=%s actual=%s\n' "$1" "$2" "$3"; fail=1
  fi
}
code()     { curl -sS -o /dev/null -w '%{http_code}' "$@"; }
header()   { curl -sS -o /dev/null -D - "$1" 2>/dev/null | tr -d '\r' | awk -v k="$(echo "$2" | tr 'A-Z' 'a-z'):" 'tolower($1)==k {sub(/^[^ ]+ /,""); print}'; }
urldecode(){ sed 's/%3A/:/gi; s/%2F/\//gi'; }

echo "smoke @ $BASE"
echo ""

echo "[가용성]"
check "GET /health"                         "200"  "$(code "$BASE/health")"
check "GET /api/auth/me"                    "200"  "$(code "$BASE/api/auth/me")"
check "  me.authenticated (쿠키 없음)"       "false" "$(curl -sS "$BASE/api/auth/me" | grep -o '"authenticated":[a-z]*' | cut -d: -f2)"

echo ""
echo "[미인증 차단]"
check "GET /api/companies"                  "401"  "$(code "$BASE/api/companies")"
check "GET /api/companies/{id}/contents"    "401"  "$(code "$BASE/api/companies/00000000-0000-0000-0000-000000000000/contents")"
check "GET /api/postings"                   "401"  "$(code "$BASE/api/postings")"
check "GET /api/positions"                  "401"  "$(code "$BASE/api/positions")"
check "GET /api/references"                 "401"  "$(code "$BASE/api/references")"
check "GET /api/admin/users"                "401"  "$(code "$BASE/api/admin/users")"
check "GET /api/admin/settings"             "401"  "$(code "$BASE/api/admin/settings")"
check "GET /swagger-ui/index.html"          "401"  "$(code "$BASE/swagger-ui/index.html")"
check "GET /v3/api-docs"                    "401"  "$(code "$BASE/v3/api-docs")"
check "POST /api/companies (CSRF 없음)"     "403"  "$(code -X POST -H 'Content-Type: application/json' -d '{}' "$BASE/api/companies")"
check "PATCH /api/auth/me (CSRF 없음)"       "403"  "$(code -X PATCH -H 'Content-Type: application/json' -d '{}' "$BASE/api/auth/me")"
check "DELETE /api/admin/users/x (CSRF 없음)" "403"  "$(code -X DELETE "$BASE/api/admin/users/00000000-0000-0000-0000-000000000000")"

echo ""
echo "[리다이렉트 — 절대 URL 금지]"
check "GET /admin -> status"                "302"  "$(code "$BASE/admin")"
check "GET /admin -> Location (상대 경로)"   "/"    "$(header "$BASE/admin" Location)"

echo ""
echo "[경로 견고성 — 500 이 나오면 안 된다]"
# 미인증이라 보안 필터가 먼저 401 을 낸다. 인증 세션에서는 NoResourceFoundException 핸들러가 404 를 낸다.
check "GET /api/companies/ (끝 슬래시)"     "401"  "$(code "$BASE/api/companies/")"
check "GET /api/nope"                       "401"  "$(code "$BASE/api/nope")"

echo ""
echo "[Google OAuth]"
google_enabled="$(curl -sS "$BASE/api/auth/login-options" | grep -o '"googleEnabled":[a-z]*' | cut -d: -f2)"
if [ "$google_enabled" = "true" ]; then
  loc="$(header "$BASE/oauth2/authorization/google" Location)"
  redirect_uri="$(printf '%s' "$loc" | tr '&?' '\n\n' | grep '^redirect_uri=' | cut -d= -f2- | urldecode)"
  check "GET /oauth2/authorization/google"    "302"  "$(code "$BASE/oauth2/authorization/google")"
  check "  redirect_uri == PUBLIC_BASE_URL+callback" "$BASE/login/oauth2/code/google" "$redirect_uri"
  cookies="$(curl -sS -o /dev/null -D - "$BASE/oauth2/authorization/google" 2>/dev/null | tr -d '\r' | grep -i '^set-cookie:' | grep -i 'JSESSIONID')"
  case "$cookies" in
    *SameSite=Lax*) check "  JSESSIONID SameSite" "Lax" "Lax" ;;
    *SameSite=Strict*) check "  JSESSIONID SameSite" "Lax" "Strict" ;;
    *) check "  JSESSIONID SameSite" "Lax" "(없음)" ;;
  esac
else
  printf '  skip  %-48s %s\n' "Google 미설정 (login-options.googleEnabled=false)" "-"
  check "GET /oauth2/authorization/google (미설정)" "404" "$(code "$BASE/oauth2/authorization/google")"
fi

echo ""
if [ "$fail" -eq 0 ]; then
  echo "PASS  ($pass checks)"
else
  echo "FAIL  — 위의 FAIL 항목을 고치기 전에는 배포하지 않는다."
fi
exit "$fail"

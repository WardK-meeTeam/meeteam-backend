#!/usr/bin/env bash
# 단건 지원 요청 — 경합 없는 상태에서 락 소유 시간 1건 측정용
#
# 사용법:
#   ./loadtest/apply-one.sh <PROJECT_ID> [TOKEN_INDEX]
#
# 요청 전후의 project.apply.lock.held 누적값 차이로 "이번 1건"의 락 소유 시간을 출력한다.
# 첫 요청은 콜드 스타트(JIT, 쿼리 플랜 캐시, 커넥션 준비)로 크게 나오므로,
# 웜업 후 측정하려면 여러 번 반복 실행하고 값이 안정된 뒤의 결과를 쓴다:
#   for i in {1..20}; do ./loadtest/apply-one.sh 302; done
#
# 회원당 1회 지원 제약 때문에 토큰을 1회성으로 소비한다.
# k6(apply-load.js)가 토큰을 0번부터 쓰므로, 이 스크립트는 마지막 인덱스부터 거꾸로 소비해
# 웜업/단건 측정이 부하 테스트의 토큰과 겹치지 않게 한다. (커서: .apply-one-cursor, -1씩 전진)

set -euo pipefail
cd "$(dirname "$0")"

BASE_URL="${BASE_URL:-http://localhost:8080}"
POSITION="${POSITION:-WEB_FRONTEND}"
CURSOR_FILE=".apply-one-cursor"

PROJECT_ID="${1:?사용법: ./apply-one.sh <PROJECT_ID> [TOKEN_INDEX]}"

if [[ $# -ge 2 ]]; then
  IDX="$2"
elif [[ -f "$CURSOR_FILE" ]]; then
  IDX=$(cat "$CURSOR_FILE")
else
  # 첫 실행: 마지막 토큰부터 시작 (k6는 0번부터 올라오므로 충돌 방지)
  IDX=$(python3 -c "import json; print(len(json.load(open('tokens.json'))) - 1)")
fi

if [[ "$IDX" -lt 0 ]]; then
  echo "토큰 소진 — .apply-one-cursor 삭제 후 reset.sql을 돌리고 다시 시작하세요" >&2
  exit 1
fi

read -r EMAIL TOKEN < <(python3 -c "
import json, sys
tokens = json.load(open('tokens.json'))
i = int('$IDX')
if i >= len(tokens):
    sys.exit(f'토큰 소진 (총 {len(tokens)}개, 요청 인덱스 {i})')
print(tokens[i]['email'], tokens[i]['token'])
")

# 누적 메트릭 스냅샷: "count total_ms max_ms" (메트릭 미등록 시 0 0 0)
metric_snapshot() {
  curl -s "$BASE_URL/actuator/metrics/project.apply.lock.held" | python3 -c "
import json, sys
try:
    d = json.load(sys.stdin)
    m = {x['statistic']: x['value'] for x in d['measurements']}
    print(int(m.get('COUNT', 0)), m.get('TOTAL_TIME', 0) * 1000, m.get('MAX', 0) * 1000)
except Exception:
    print(0, 0, 0)
"
}

read -r BEFORE_COUNT BEFORE_TOTAL _ <<< "$(metric_snapshot)"

echo "토큰 #$IDX ($EMAIL) → POST /api/v1/projects/$PROJECT_ID/application"

HTTP_CODE=$(curl -s -o /tmp/apply-one-body.json -w '%{http_code} (전체 왕복 %{time_total}s)' \
  -X POST "$BASE_URL/api/v1/projects/$PROJECT_ID/application" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"jobPositionCode\": \"$POSITION\", \"motivation\": \"단건 측정 #$IDX\"}")

echo "응답: $HTTP_CODE"
cat /tmp/apply-one-body.json; echo

# 성공했을 때만 커서 전진 — 뒤에서 앞으로 (인덱스 직접 지정 시에는 건드리지 않음)
if [[ $# -lt 2 && "$HTTP_CODE" == 200* ]]; then
  echo $((IDX - 1)) > "$CURSOR_FILE"
fi

# 카운터 증가는 커밋 후 비동기(@Async)로 돌므로, 메트릭 반영을 최대 3초까지 기다린다
for _ in $(seq 1 30); do
  read -r AFTER_COUNT AFTER_TOTAL AFTER_MAX <<< "$(metric_snapshot)"
  [[ "$AFTER_COUNT" -gt "$BEFORE_COUNT" ]] && break
  sleep 0.1
done

echo
python3 -c "
bc, bt = int('$BEFORE_COUNT'), float('$BEFORE_TOTAL')
ac, at, mx = int('$AFTER_COUNT'), float('$AFTER_TOTAL'), float('$AFTER_MAX')
if ac == bc + 1:
    print(f'이번 1건 락 소유 시간: {at - bt:.2f}ms')
elif ac == bc:
    print('메트릭 증가 없음 — 요청이 실패했거나 서버가 측정 코드 이전 빌드입니다')
else:
    print(f'이번 요청 사이에 다른 지원 {ac - bc}건이 섞임 — 단건 값 분리 불가')
if ac:
    print(f'누적: {ac}건, 평균 {at / ac:.2f}ms, 최대 {mx:.2f}ms')
"
// 프로젝트 지원 API 부하 테스트 — project 행 락 경합 측정 (1단계: 단일 카운터 기준선)
//
// 실행:
//   k6 run -e PROJECT_ID=<hot_project_id> loadtest/apply-load.js
//
// 선행 조건:
//   1. loadtest/seed.sql 적용 (회원 N명 + 핫 프로젝트)
//   2. ./gradlew generateLoadTestTokens  →  loadtest/tokens.json 생성
//   3. 서버 기동 (local 프로필, 8080)
//
// 설계 노트:
//   - 한 회원은 한 프로젝트에 1회만 지원 가능하므로(중복 검증) 토큰을 1회성으로 소비한다.
//     iterationInTest(테스트 전체에서 유일한 순번)로 토큰을 인덱싱해 중복 사용을 막는다.
//   - ramping-arrival-rate(open model)를 사용해, 서버가 느려져도 k6가 요청 투입 속도를
//     유지하게 한다. 락 대기로 응답이 밀리는 상황을 숨기지 않기 위함(coordinated omission 방지).
//   - 재실행 전에는 loadtest/reset.sql 로 지원서를 비워야 한다.

import http from 'k6/http';
import { check } from 'k6';
import exec from 'k6/execution';
import { SharedArray } from 'k6/data';

const tokens = new SharedArray('tokens', () => JSON.parse(open('./tokens.json')));

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PROJECT_ID = __ENV.PROJECT_ID;
const POSITION = __ENV.POSITION || 'WEB_FRONTEND';

// PROFILE=baseline(기본): 5→100 req/s, 3분, 약 6,500건 — 경합 발생 직전까지
// PROFILE=peak:           50→250 req/s, 1분40초, 약 15,700건 — 직렬화 상한 돌파용 (토큰 20,000개 필요)
// PROFILE=burst:          100→1000 req/s, 35초, 약 16,000건 — RTT 주입 상태에서 전면 붕괴 관찰용
//   burst는 임계 초과가 목적이므로 threshold를 걸지 않는다(타임아웃·에러 자체가 측정 대상).
const STAGE_PROFILES = {
  baseline: {
    startRate: 5,
    maxVUs: 1000,
    stages: [
      { target: 10, duration: '30s' },  // 워밍업
      { target: 30, duration: '1m' },   // 점진 증가
      { target: 60, duration: '1m' },
      { target: 100, duration: '30s' }, // 피크
    ],
    thresholds: {
      http_req_failed: ['rate<0.01'],
      http_req_duration: ['p(95)<1000', 'p(99)<3000'],
    },
  },
  peak: {
    startRate: 50,
    maxVUs: 1000,
    stages: [
      { target: 100, duration: '20s' }, // 무릎 직전까지 빠르게
      { target: 150, duration: '30s' }, // 상한 돌파 시작
      { target: 250, duration: '40s' }, // 대기 행렬 폭증 구간
      { target: 250, duration: '10s' },
    ],
    thresholds: {
      http_req_failed: ['rate<0.01'],
      http_req_duration: ['p(95)<1000', 'p(99)<3000'],
    },
  },
  burst: {
    startRate: 100,
    maxVUs: 3000, // 대기가 수십 초로 늘어나므로 VU가 폭증한다. 그래도 모자라면 dropped_iterations로 집계됨
    stages: [
      { target: 300, duration: '10s' },
      { target: 1000, duration: '15s' },
      { target: 1000, duration: '6s' }, // 총 ~17,800건 — 토큰 20,000개 내
    ],
    thresholds: {},
  },
};

const profile = STAGE_PROFILES[__ENV.PROFILE || 'baseline'];

export const options = {
  scenarios: {
    apply: {
      executor: 'ramping-arrival-rate',
      startRate: profile.startRate,
      timeUnit: '1s',
      preAllocatedVUs: 100,
      maxVUs: profile.maxVUs,
      stages: profile.stages,
    },
  },
  thresholds: profile.thresholds,
};

export default function () {
  if (!PROJECT_ID) {
    exec.test.abort('PROJECT_ID 환경변수가 필요합니다: k6 run -e PROJECT_ID=...');
  }

  // 테스트 전체에서 유일한 순번 → 토큰(=회원) 1회성 소비
  const idx = exec.scenario.iterationInTest;
  if (idx >= tokens.length) {
    exec.test.abort(`토큰 소진 (${tokens.length}개) — generateLoadTestTokens -Pcount 를 늘리세요`);
  }

  const res = http.post(
    `${BASE_URL}/api/v1/projects/${PROJECT_ID}/application`,
    JSON.stringify({
      jobPositionCode: POSITION,
      motivation: `k6 부하 테스트 지원 #${idx}`,
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${tokens[idx].token}`,
      },
      tags: { name: 'apply' },
    },
  );

  check(res, {
    'status is 200': (r) => r.status === 200,
  }) || console.error(`#${idx} ${tokens[idx].email} → ${res.status} ${res.body}`);
}
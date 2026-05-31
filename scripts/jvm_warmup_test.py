import time
import csv
import requests
from concurrent.futures import ThreadPoolExecutor

# ================= [설정 영역] =================
# 로컬 테스트
# BASE_URL = "http://localhost:8080"

# 운영서버 테스트
BASE_URL = "https://api.meeteam.alom-sejong.com"



LOGIN_URL = f"{BASE_URL}/api/v1/auth/login/sejong"

# 실제 학번/비밀번호로 변경하세요
STUDENT_ID = "21013220"
PASSWORD = "19980611"

TOTAL_RUNS = 10                         # 총 10번의 측정 (Run 1 ~ Run 10)
CONCURRENT_REQUESTS = 2                 # 한 번에 동시에 보낼 요청 수 (2개)
INTERVAL = 2                            # 각 Run 사이의 간격 (초)
WARMUP_COUNT = 50                       # 웜업 시 보낼 요청 횟수 (로그인은 무거우므로 적게)
# ===============================================

def send_login_request(session, debug=False):
    """세종대 포털 로그인 요청을 보내고 걸린 시간(ms)을 반환합니다."""
    try:
        payload = {
            "studentId": STUDENT_ID,
            "password": PASSWORD
        }
        headers = {
            "Content-Type": "application/json"
        }
        response = session.post(LOGIN_URL, json=payload, headers=headers, timeout=30)

        if debug or response.status_code != 200:
            print(f"  [DEBUG] URL: {LOGIN_URL}")
            print(f"  [DEBUG] Status: {response.status_code}")
            print(f"  [DEBUG] Response: {response.text[:500]}")

        # response.elapsed는 서버 처리+네트워크 시간만 정밀 측정합니다.
        return response.elapsed.total_seconds() * 1000, response.status_code
    except Exception as e:
        print(f"  [에러] {e}")
        return None, None

def execute_benchmark(session, test_name):
    """동시 요청을 총 N번 수행하며 평균 레이턴시를 수집합니다."""
    print(f"\n{'='*60}")
    print(f"  [{test_name}] 테스트 시작...")
    print(f"{'='*60}")
    print(f"{'Run':<6} | {'Avg Latency (ms)':<18} | {'Status':<10}")
    print("-" * 60)

    latencies_over_runs = []

    for run in range(1, TOTAL_RUNS + 1):
        # ThreadPoolExecutor를 사용해 요청을 '동시'에 쏩니다.
        with ThreadPoolExecutor(max_workers=CONCURRENT_REQUESTS) as executor:
            futures = [executor.submit(send_login_request, session) for _ in range(CONCURRENT_REQUESTS)]

            # 응답 시간 취합
            results = [f.result() for f in futures]
            run_latencies = [r[0] for r in results if r[0] is not None]
            status_codes = [r[1] for r in results if r[1] is not None]

        if run_latencies:
            avg_latency = sum(run_latencies) / len(run_latencies)
            status = status_codes[0] if status_codes else "N/A"
            print(f"Run {run:<3} | {avg_latency:>14.2f} ms   | {status}")
            latencies_over_runs.append(round(avg_latency, 2))
        else:
            print(f"Run {run:<3} | {'Failed':>14}      | -")
            latencies_over_runs.append(0)

        if run < TOTAL_RUNS:
            time.sleep(INTERVAL)

    return latencies_over_runs

def run_compare_test():
    print("\n" + "=" * 60)
    print("  JVM Warm-up 성능 비교 테스트 (세종대 포털 로그인)")
    print("=" * 60)
    print(f"  - 로그인 URL: {LOGIN_URL}")
    print(f"  - 학번: {STUDENT_ID}")
    print(f"  - 동시 요청 수: {CONCURRENT_REQUESTS}")
    print(f"  - 총 측정 횟수: {TOTAL_RUNS}")
    print(f"  - 웜업 요청 횟수: {WARMUP_COUNT}")
    print("=" * 60)

    # 1. 웜업이 없는 상태 (No Warm-up) 테스트
    session_no_warmup = requests.Session()
    print("\n[No Warm-up 테스트]")
    print("서버(스프링부트)를 '재기동'한 직후에 이 스크립트를 실행하면 효과가 극대화됩니다!")
    input("서버가 재기동되었다면 엔터(Enter)를 누르세요...")

    no_warmup_results = execute_benchmark(session_no_warmup, "No Warm-up (콜드 스타트)")

    # 2. 웜업을 진행하는 상태 (With Warm-up) 테스트
    session_warmup = requests.Session()
    print(f"\n[Warm-up 진행 중] 서버에 요청 {WARMUP_COUNT}개를 보내 JIT 최적화를 시킵니다...")

    for i in range(WARMUP_COUNT):
        send_login_request(session_warmup)
        if (i + 1) % 10 == 0:
            print(f"   > {i + 1}개 완료...")

    print("웜업 완료! 잠시 후 웜업된 상태의 본 측정을 시작합니다.")
    time.sleep(2)

    warmup_results = execute_benchmark(session_warmup, "With Warm-up (웜업 완료)")

    # 3. 결과 요약
    print("\n" + "=" * 60)
    print("  결과 요약")
    print("=" * 60)

    avg_no_warmup = sum(no_warmup_results) / len([x for x in no_warmup_results if x > 0]) if any(no_warmup_results) else 0
    avg_warmup = sum(warmup_results) / len([x for x in warmup_results if x > 0]) if any(warmup_results) else 0

    print(f"  No Warm-up 평균: {avg_no_warmup:.2f} ms")
    print(f"  With Warm-up 평균: {avg_warmup:.2f} ms")
    if avg_no_warmup > 0 and avg_warmup > 0:
        improvement = ((avg_no_warmup - avg_warmup) / avg_no_warmup) * 100
        print(f"  성능 향상: {improvement:.1f}%")
    print("=" * 60)

    # 4. CSV 결과 저장
    csv_filename = "warmup_compare_result.csv"
    with open(csv_filename, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["Run Number", "No Warm-up (ms)", "With Warm-up (ms)"])
        for idx in range(TOTAL_RUNS):
            writer.writerow([idx + 1, no_warmup_results[idx], warmup_results[idx]])
    print(f"\n결과가 '{csv_filename}'로 저장되었습니다.")

    # 5. 그래프 그리기 (선택적)
    try:
        import matplotlib.pyplot as plt
        runs = list(range(1, TOTAL_RUNS + 1))
        max_y = max(max(no_warmup_results), max(warmup_results)) * 1.1

        # 그래프 1: No Warm-up (빨간색) - 별도 파일
        fig1, ax1 = plt.subplots(figsize=(8, 5))
        ax1.plot(runs, no_warmup_results, marker='o', color='red', linestyle='-', linewidth=2)
        ax1.fill_between(runs, no_warmup_results, alpha=0.3, color='red')
        ax1.set_xlabel('Run Number', fontsize=11)
        ax1.set_ylabel('Latency (ms)', fontsize=11)
        ax1.set_title(f'No Warm-up (Cold Start)\nAvg: {avg_no_warmup:.2f} ms', fontsize=14, fontweight='bold', color='red')
        ax1.set_xlim(0, TOTAL_RUNS + 1)
        ax1.set_ylim(0, max_y)
        ax1.set_xticks(runs)
        ax1.grid(True, linestyle='--', alpha=0.5)
        plt.tight_layout()
        plt.savefig("no_warmup_chart.png", dpi=300)
        print("그래프 저장: 'no_warmup_chart.png'")
        plt.close(fig1)

        # 그래프 2: With Warm-up (파란색) - 별도 파일
        fig2, ax2 = plt.subplots(figsize=(8, 5))
        ax2.plot(runs, warmup_results, marker='s', color='blue', linestyle='-', linewidth=2)
        ax2.fill_between(runs, warmup_results, alpha=0.3, color='blue')
        ax2.set_xlabel('Run Number', fontsize=11)
        ax2.set_ylabel('Latency (ms)', fontsize=11)
        ax2.set_title(f'With Warm-up (Optimized)\nAvg: {avg_warmup:.2f} ms', fontsize=14, fontweight='bold', color='blue')
        ax2.set_xlim(0, TOTAL_RUNS + 1)
        ax2.set_ylim(0, max_y)
        ax2.set_xticks(runs)
        ax2.grid(True, linestyle='--', alpha=0.5)
        plt.tight_layout()
        plt.savefig("with_warmup_chart.png", dpi=300)
        print("그래프 저장: 'with_warmup_chart.png'")
        plt.close(fig2)

        # 그래프 3: 비교 차트 (두 개 함께) - 별도 파일
        fig3, ax3 = plt.subplots(figsize=(10, 6))
        ax3.plot(runs, no_warmup_results, marker='o', color='red', linestyle='-', linewidth=2, label=f'No Warm-up (Avg: {avg_no_warmup:.2f}ms)')
        ax3.plot(runs, warmup_results, marker='s', color='blue', linestyle='-', linewidth=2, label=f'With Warm-up (Avg: {avg_warmup:.2f}ms)')
        ax3.set_xlabel('Run Number', fontsize=11)
        ax3.set_ylabel('Latency (ms)', fontsize=11)
        ax3.set_title('JVM Warm-up Performance Comparison', fontsize=14, fontweight='bold')
        ax3.set_xlim(0, TOTAL_RUNS + 1)
        ax3.set_ylim(0, max_y)
        ax3.set_xticks(runs)
        ax3.grid(True, linestyle='--', alpha=0.5)
        ax3.legend(fontsize=11)
        plt.tight_layout()
        plt.savefig("warmup_comparison_chart.png", dpi=300)
        print("그래프 저장: 'warmup_comparison_chart.png'")
        plt.close(fig3)

        print("\n총 3개의 그래프 파일이 저장되었습니다!")
    except ImportError:
        print("\nmatplotlib가 설치되지 않아 그래프를 그리지 못했습니다.")
        print("설치하려면: pip install matplotlib")
    except Exception as e:
        print(f"\n그래프 그리기 실패: {e}")

if __name__ == "__main__":
    run_compare_test()
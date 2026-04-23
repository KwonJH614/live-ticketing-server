-- ============================================================
--  k6 부하 테스트용 데이터 생성 SQL
--  - 이메일 인증을 거치지 않고 직접 DB에 테스트 유저 INSERT
--  - 실행: psql -d ticketing -f k6/scripts/setup-test-data.sql
-- ============================================================

-- 테스트 유저 생성 (1000명)
-- BCrypt 해시는 Spring BCryptPasswordEncoder 로 생성한 값 사용
DO $$
DECLARE
  i INTEGER;
  hashed_password TEXT := '$2a$10$OUo16F86JPIWPo99HlZySOe2QfElzasxfMRHtWL1sFf/98lz3XTuG';
BEGIN
  FOR i IN 1..1000 LOOP
    INSERT INTO users (username, password, email, role)
    VALUES (
      'loadtest_user_' || i,
      hashed_password,
      'loadtest' || i || '@test.com',
      'USER'
    )
    ON CONFLICT (email) DO NOTHING;
  END LOOP;
END $$;

-- 생성 확인
SELECT COUNT(*) AS test_user_count
FROM users
WHERE username LIKE 'loadtest_user_%';

-- ============================================================
--  테스트 이벤트 & 좌석 생성 (없을 경우)
-- ============================================================

-- 테스트 이벤트
INSERT INTO events (title, description, start_time, end_time, venue, price)
SELECT 'K6 부하테스트 이벤트', '부하 테스트용 이벤트입니다',
       NOW() + INTERVAL '7 days', NOW() + INTERVAL '7 days 3 hours',
       '테스트 홀', 50000
WHERE NOT EXISTS (SELECT 1 FROM events WHERE title = 'K6 부하테스트 이벤트');

-- 테스트 좌석 (100석)
DO $$
DECLARE
  test_event_id BIGINT;
  i INTEGER;
BEGIN
  SELECT id INTO test_event_id FROM events WHERE title = 'K6 부하테스트 이벤트' LIMIT 1;

  IF test_event_id IS NOT NULL THEN
    FOR i IN 1..100 LOOP
      INSERT INTO seats (event_id, seat_number, is_reserved)
      VALUES (test_event_id, 'A-' || i, false)
      ON CONFLICT (event_id, seat_number) DO NOTHING;
    END LOOP;
    RAISE NOTICE '이벤트 ID: %, 좌석 100개 생성 완료', test_event_id;
  END IF;
END $$;

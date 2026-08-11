INSERT INTO companies (
    id, name, industry, location, website_url, status, summary, memo, created_at, updated_at
) VALUES
(
    '10000000-0000-0000-0000-000000000001',
    '루멘 로보틱스 데모',
    '산업용 로봇·비전',
    '경기 성남',
    'https://example.invalid/lumen',
    'PREPARING',
    '제품 검증용 합성 기업입니다. 실제 기업 정보가 아닙니다.',
    'RGB-D와 ROS2 프로젝트 경험 연결 검토',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '10000000-0000-0000-0000-000000000002',
    '노바그리드 데이터 데모',
    '데이터 플랫폼',
    '서울 영등포',
    'https://example.invalid/novagrid',
    'INTERESTED',
    '제품 검증용 합성 기업입니다. 실제 기업 정보가 아닙니다.',
    '데이터 파이프라인 운영 경험 정리',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
),
(
    '10000000-0000-0000-0000-000000000003',
    '오비트 모빌리티 데모',
    '모빌리티 소프트웨어',
    '대전 유성',
    'https://example.invalid/orbit',
    'ARCHIVED',
    '제품 검증용 합성 기업입니다. 실제 기업 정보가 아닙니다.',
    NULL,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

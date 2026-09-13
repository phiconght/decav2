-- =====================================================================
-- V49: Hero cua Trang chu cong khai Web (khach chua dang nhap) + quyen
-- quan tri + va loi thieu FILE:WRITE cua EMPLOYEE.
-- Xem KEHOACH_WEB_TrangChuCongKhai_HeroContent.md.
-- =====================================================================

-- (a) Bang singleton home_hero — cung khuon voi trust_stats (V41).
CREATE TABLE home_hero (
    id                    BIGINT PRIMARY KEY,
    badge_text            VARCHAR(60),
    title                 VARCHAR(150) NOT NULL,
    subtitle              VARCHAR(400),
    primary_cta_label     VARCHAR(60),
    primary_cta_href      VARCHAR(255),
    secondary_cta_label   VARCHAR(60),
    secondary_cta_href    VARCHAR(255),
    background_image_url  VARCHAR(500),
    visible               BOOLEAN      NOT NULL DEFAULT true,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    CONSTRAINT home_hero_singleton CHECK (id = 1)
);

-- (b) Noi dung mac dinh — de Trang chu khong trong khi moi deploy.
INSERT INTO home_hero
    (id, badge_text, title, subtitle, primary_cta_label, primary_cta_href,
     secondary_cta_label, secondary_cta_href, visible, created_at)
VALUES
    (1, 'Trung tâm giáo dục DecaMath',
     'Học chắc kiến thức — Tiến bộ từng buổi học',
     'Lộ trình học Toán cá nhân hóa cho học viên từ lớp 6 đến lớp 12, cùng đội ngũ giáo viên đồng hành sát sao từng buổi học.',
     'Đăng nhập', '/login',
     'Khám phá khóa học', '/catalog',
     true, now());

-- (c) EMPLOYEE duoc dung MARKETING:WRITE de sua Hero (quyen MARKETING:*
-- da co san tu V41 cho ADMIN + EMPLOYEE — khong can them quyen moi).

-- (d) Va loi thieu FILE:WRITE cua EMPLOYEE — dang chi co FILE:READ (V3)
-- trong khi da co POST:WRITE (V32), khien EMPLOYEE bi 403 khi upload anh
-- bia bai viet / anh nen Hero.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.name = 'EMPLOYEE' AND p.code = 'FILE:WRITE'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

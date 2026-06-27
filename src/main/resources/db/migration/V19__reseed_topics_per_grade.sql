-- =====================================================================
-- V19: Re-seed CHUYÊN ĐỀ theo TỪNG MÔN-KHỐI (mỗi subject có danh sách riêng)
-- Thay thế seed "generic" của V17 (vốn lặp lại y hệt giữa các khối).
-- Schema KHÔNG đổi: topics.subject_id vốn đã trỏ tới 1 môn-khối cụ thể.
-- Các bảng liên quan (exercises.topic_id, exams.topic_id) được map lại.
-- Join theo subjects.code (vd LTO06T = Toán Khối 6) để chắc chắn đúng môn-khối.
-- =====================================================================

-- 1) Gỡ tham chiếu để có thể xóa topics cũ (FK exercises/exams.topic_id)
UPDATE exercises SET topic_id = NULL WHERE topic_id IS NOT NULL;
UPDATE exams     SET topic_id = NULL WHERE topic_id IS NOT NULL;

-- 2) Xóa toàn bộ chuyên đề cũ (seed generic của V17)
DELETE FROM topics;

-- 3) Chèn chuyên đề RIÊNG cho từng môn-khối
INSERT INTO topics (subject_id, name, sort_order)
SELECT s.id, v.topic_name, v.sort_order
FROM subjects s
JOIN (VALUES
    -- ===================== TOÁN =====================
    ('LTO06T','Số tự nhiên',1),('LTO06T','Số nguyên',2),('LTO06T','Phân số & số thập phân',3),('LTO06T','Hình học trực quan',4),('LTO06T','Thống kê & xác suất',5),
    ('LTO07T','Số hữu tỉ',1),('LTO07T','Số thực',2),('LTO07T','Biểu thức đại số',3),('LTO07T','Tam giác',4),('LTO07T','Thống kê & xác suất',5),
    ('LTO08T','Đa thức',1),('LTO08T','Hằng đẳng thức & phân tích đa thức',2),('LTO08T','Phương trình bậc nhất một ẩn',3),('LTO08T','Tứ giác',4),('LTO08T','Định lý Pythagore',5),('LTO08T','Xác suất',6),
    ('LTO09T','Phương trình & hệ phương trình bậc nhất',1),('LTO09T','Hàm số bậc nhất',2),('LTO09T','Hàm số y=ax² và phương trình bậc hai',3),('LTO09T','Hệ thức lượng trong tam giác vuông',4),('LTO09T','Đường tròn',5),('LTO09T','Thống kê & xác suất',6),
    ('LTO10T','Mệnh đề & tập hợp',1),('LTO10T','Bất phương trình & hệ bậc nhất hai ẩn',2),('LTO10T','Hàm số bậc hai & đồ thị',3),('LTO10T','Hệ thức lượng trong tam giác',4),('LTO10T','Vectơ',5),('LTO10T','Thống kê & xác suất',6),
    ('LTO11T','Hàm số lượng giác & phương trình lượng giác',1),('LTO11T','Dãy số - Cấp số cộng - Cấp số nhân',2),('LTO11T','Giới hạn & hàm số liên tục',3),('LTO11T','Đạo hàm',4),('LTO11T','Quan hệ song song & vuông góc trong không gian',5),('LTO11T','Xác suất',6),
    ('LTO12T','Ứng dụng đạo hàm & khảo sát hàm số',1),('LTO12T','Hàm số mũ & logarit',2),('LTO12T','Nguyên hàm - Tích phân',3),('LTO12T','Số phức',4),('LTO12T','Khối đa diện & thể tích',5),('LTO12T','Phương pháp tọa độ trong không gian',6),

    -- ===================== VẬT LÝ =====================
    ('LLY06T','Các phép đo',1),('LLY06T','Lực',2),('LLY06T','Khối lượng & trọng lượng',3),('LLY06T','Năng lượng',4),
    ('LLY07T','Tốc độ & chuyển động',1),('LLY07T','Âm thanh',2),('LLY07T','Ánh sáng',3),('LLY07T','Từ',4),
    ('LLY08T','Khối lượng riêng & áp suất',1),('LLY08T','Lực & moment lực',2),('LLY08T','Năng lượng nhiệt',3),('LLY08T','Điện',4),
    ('LLY09T','Năng lượng cơ học',1),('LLY09T','Điện & mạch điện',2),('LLY09T','Điện từ',3),('LLY09T','Khúc xạ ánh sáng',4),
    ('LLY10T','Động học',1),('LLY10T','Động lực học - Định luật Newton',2),('LLY10T','Công - Năng lượng - Công suất',3),('LLY10T','Động lượng',4),('LLY10T','Chuyển động tròn & biến dạng',5),
    ('LLY11T','Điện trường',1),('LLY11T','Dòng điện không đổi',2),('LLY11T','Từ trường & cảm ứng điện từ',3),('LLY11T','Dao động',4),('LLY11T','Sóng',5),
    ('LLY12T','Dao động cơ',1),('LLY12T','Sóng cơ & âm',2),('LLY12T','Dòng điện xoay chiều',3),('LLY12T','Dao động & sóng điện từ',4),('LLY12T','Sóng ánh sáng',5),('LLY12T','Lượng tử ánh sáng',6),('LLY12T','Hạt nhân nguyên tử',7),

    -- ===================== HÓA HỌC =====================
    ('LHO06T','Chất & sự biến đổi của chất',1),('LHO06T','Hỗn hợp & tách chất',2),('LHO06T','Oxygen & không khí',3),
    ('LHO07T','Nguyên tử & nguyên tố hóa học',1),('LHO07T','Sơ lược bảng tuần hoàn',2),('LHO07T','Phân tử & liên kết hóa học',3),
    ('LHO08T','Phản ứng hóa học',1),('LHO08T','Mol & tính toán hóa học',2),('LHO08T','Acid - Base - Oxide - Muối',3),('LHO08T','Dung dịch & nồng độ',4),
    ('LHO09T','Kim loại',1),('LHO09T','Phi kim & bảng tuần hoàn',2),('LHO09T','Hydrocarbon',3),('LHO09T','Dẫn xuất hydrocarbon & ứng dụng',4),
    ('LHO10T','Cấu tạo nguyên tử',1),('LHO10T','Bảng tuần hoàn & định luật tuần hoàn',2),('LHO10T','Liên kết hóa học',3),('LHO10T','Phản ứng oxi hóa - khử',4),('LHO10T','Năng lượng hóa học',5),
    ('LHO11T','Cân bằng hóa học',1),('LHO11T','Nitrogen & sulfur',2),('LHO11T','Đại cương hóa học hữu cơ',3),('LHO11T','Hydrocarbon',4),('LHO11T','Alcohol - Phenol - Dẫn xuất halogen',5),
    ('LHO12T','Ester - Lipid',1),('LHO12T','Carbohydrate',2),('LHO12T','Amine - Amino acid - Protein',3),('LHO12T','Polymer',4),('LHO12T','Đại cương kim loại',5),('LHO12T','Kim loại nhóm IA, IIA & nhôm',6),

    -- ===================== NGỮ VĂN =====================
    ('LVA06T','Truyền thuyết & cổ tích',1),('LVA06T','Thơ',2),('LVA06T','Ký & du ký',3),('LVA06T','Văn bản nghị luận',4),('LVA06T','Văn bản thông tin',5),
    ('LVA07T','Truyện ngắn',1),('LVA07T','Thơ bốn chữ, năm chữ',2),('LVA07T','Truyện ngụ ngôn & tục ngữ',3),('LVA07T','Nghị luận văn học',4),('LVA07T','Văn bản thông tin',5),
    ('LVA08T','Truyện ngắn',1),('LVA08T','Thơ Đường luật',2),('LVA08T','Hài kịch',3),('LVA08T','Văn nghị luận',4),('LVA08T','Văn bản thông tin',5),
    ('LVA09T','Truyện thơ Nôm',1),('LVA09T','Truyện hiện đại',2),('LVA09T','Thơ tám chữ',3),('LVA09T','Nghị luận xã hội',4),('LVA09T','Văn bản thông tin',5),
    ('LVA10T','Thần thoại & sử thi',1),('LVA10T','Thơ trung đại',2),('LVA10T','Chèo & tuồng',3),('LVA10T','Văn nghị luận',4),('LVA10T','Văn bản thông tin',5),
    ('LVA11T','Truyện ngắn & tiểu thuyết hiện đại',1),('LVA11T','Thơ trữ tình',2),('LVA11T','Kịch',3),('LVA11T','Văn nghị luận',4),('LVA11T','Văn bản thông tin',5),
    ('LVA12T','Truyện hiện đại',1),('LVA12T','Thơ hiện đại',2),('LVA12T','Bi kịch',3),('LVA12T','Nghị luận văn học & xã hội',4),('LVA12T','Phong cách ngôn ngữ',5),

    -- ===================== TIẾNG ANH (theo unit chủ đề) =====================
    ('LAN06T','My new school',1),('LAN06T','My home',2),('LAN06T','My friends',3),('LAN06T','Communities',4),('LAN06T','Natural wonders',5),
    ('LAN07T','Hobbies',1),('LAN07T','Health',2),('LAN07T','Community service',3),('LAN07T','Music and arts',4),('LAN07T','Food and drink',5),
    ('LAN08T','Leisure activities',1),('LAN08T','Life in the countryside',2),('LAN08T','Environment',3),('LAN08T','Customs and traditions',4),('LAN08T','Science and technology',5),
    ('LAN09T','Local environment',1),('LAN09T','City life',2),('LAN09T','Healthy living',3),('LAN09T','Life in the past',4),('LAN09T','Wonders of the world',5),
    ('LAN10T','Family life',1),('LAN10T','Humans and the environment',2),('LAN10T','Music',3),('LAN10T','For a better community',4),('LAN10T','Gender equality',5),
    ('LAN11T','Generation gap',1),('LAN11T','Relationships',2),('LAN11T','Becoming independent',3),('LAN11T','Caring for those in need',4),('LAN11T','Global warming',5),
    ('LAN12T','Life stories',1),('LAN12T','Urbanisation',2),('LAN12T','Green living',3),('LAN12T','Cultural identity',4),('LAN12T','Career paths',5),

    -- ===================== SINH HỌC =====================
    ('LSI06T','Tế bào - đơn vị của sự sống',1),('LSI06T','Đa dạng thế giới sống',2),('LSI06T','Nấm, thực vật & động vật',3),('LSI06T','Virus & vi khuẩn',4),
    ('LSI07T','Trao đổi chất & năng lượng ở sinh vật',1),('LSI07T','Cảm ứng ở sinh vật',2),('LSI07T','Sinh trưởng & phát triển',3),('LSI07T','Sinh sản ở sinh vật',4),
    ('LSI08T','Dinh dưỡng & tiêu hóa',1),('LSI08T','Tuần hoàn & hô hấp',2),('LSI08T','Thần kinh & giác quan',3),('LSI08T','Nội tiết & sinh sản ở người',4),
    ('LSI09T','Di truyền học Mendel',1),('LSI09T','Nhiễm sắc thể & ADN',2),('LSI09T','Tiến hóa',3),('LSI09T','Sinh thái & môi trường',4),
    ('LSI10T','Thành phần hóa học của tế bào',1),('LSI10T','Cấu trúc tế bào',2),('LSI10T','Trao đổi chất & chuyển hóa năng lượng ở tế bào',3),('LSI10T','Phân bào',4),('LSI10T','Vi sinh vật & virus',5),
    ('LSI11T','Trao đổi chất & chuyển hóa năng lượng ở sinh vật',1),('LSI11T','Cảm ứng ở sinh vật',2),('LSI11T','Sinh trưởng & phát triển',3),('LSI11T','Sinh sản ở sinh vật',4),
    ('LSI12T','Di truyền phân tử',1),('LSI12T','Di truyền học quần thể',2),('LSI12T','Ứng dụng di truyền & tiến hóa',3),('LSI12T','Sinh thái học & môi trường',4),

    -- ===================== TIN HỌC =====================
    ('LTI06T','Thông tin & dữ liệu',1),('LTI06T','Mạng máy tính & Internet',2),('LTI06T','Soạn thảo văn bản',3),('LTI06T','Sơ đồ tư duy & trình chiếu',4),
    ('LTI07T','Bảng tính điện tử',1),('LTI07T','Phần mềm trình chiếu',2),('LTI07T','Mạng xã hội & an toàn thông tin',3),('LTI07T','Thuật toán sắp xếp & tìm kiếm',4),
    ('LTI08T','Xử lý & trực quan hóa dữ liệu',1),('LTI08T','Soạn thảo văn bản nâng cao',2),('LTI08T','Lập trình trực quan Scratch',3),('LTI08T','Đạo đức & văn hóa số',4),
    ('LTI09T','Bảng tính nâng cao',1),('LTI09T','Trình bày thông tin đa phương tiện',2),('LTI09T','Thuật toán & lập trình',3),('LTI09T','An toàn thông tin',4),
    ('LTI10T','Máy tính & xã hội tri thức',1),('LTI10T','Mạng máy tính & Internet',2),('LTI10T','Lập trình Python cơ bản',3),('LTI10T','Thiết kế đồ họa',4),
    ('LTI11T','Hệ điều hành & phần mềm',1),('LTI11T','Cơ sở dữ liệu',2),('LTI11T','Lập trình Python nâng cao',3),('LTI11T','Mạng & bảo mật',4),
    ('LTI12T','Trí tuệ nhân tạo & máy học',1),('LTI12T','Cơ sở dữ liệu quan hệ',2),('LTI12T','Phát triển ứng dụng web',3),('LTI12T','An toàn & pháp lý số',4)
) AS v(subject_code, topic_name, sort_order)
  ON s.code = v.subject_code;

-- 4) Map lại chuyên đề cho BÀI TẬP (cùng môn-khối, phân bổ theo id)
UPDATE exercises e
SET topic_id = pick.topic_id
FROM (
    SELECT e2.id AS exercise_id, ranked.topic_id
    FROM exercises e2
    JOIN LATERAL (
        SELECT t.id AS topic_id,
               ROW_NUMBER() OVER (ORDER BY t.sort_order, t.id) AS rn,
               COUNT(*)     OVER ()                            AS cnt
        FROM topics t
        WHERE t.subject_id = e2.subject_id
    ) ranked ON ranked.rn = (e2.id % ranked.cnt) + 1
    WHERE e2.topic_id IS NULL
) pick
WHERE e.id = pick.exercise_id;

-- 5) Map lại chuyên đề cho ĐỀ THI (cùng môn-khối)
UPDATE exams ex
SET topic_id = pick.topic_id
FROM (
    SELECT ex2.id AS exam_id, ranked.topic_id
    FROM exams ex2
    JOIN LATERAL (
        SELECT t.id AS topic_id,
               ROW_NUMBER() OVER (ORDER BY t.sort_order, t.id) AS rn,
               COUNT(*)     OVER ()                            AS cnt
        FROM topics t
        WHERE t.subject_id = ex2.subject_id
    ) ranked ON ranked.rn = (ex2.id % ranked.cnt) + 1
    WHERE ex2.topic_id IS NULL
) pick
WHERE ex.id = pick.exam_id;

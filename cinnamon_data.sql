-- ====================================================================
-- CINNAMON ENGLISH MASTERY - DỮ LIỆU ĐẦY ĐỦ TOÀN BỘ HỆ THỐNG
-- Sử dụng: MySQL 8.0+ / 9.0+
-- Tương thích Hibernate Entity mapping (spring.jpa.hibernate.ddl-auto=update)
-- ====================================================================

USE `cinnamon_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------------
-- DỌN DẸP DỮ LIỆU CŨ TRƯỚC KHI NẠP LẠI TOÀN BỘ
-- --------------------------------------------------------------------
TRUNCATE TABLE `user_badges`;
TRUNCATE TABLE `user_progress`;
TRUNCATE TABLE `quiz_options`;
TRUNCATE TABLE `quiz_questions`;
TRUNCATE TABLE `quizzes`;
TRUNCATE TABLE `vocabularies`;
TRUNCATE TABLE `lessons`;
TRUNCATE TABLE `courses`;
TRUNCATE TABLE `badges`;
TRUNCATE TABLE `user_xp`;
TRUNCATE TABLE `users`;

-- ====================================================================
-- 1. USERS (Tài khoản người dùng)
-- Mật khẩu mặc định:
--   admin@cinnamon.com   / admin123
--   teacher@cinnamon.com / teacher123
--   student@cinnamon.com / student123
-- ====================================================================
INSERT INTO `users` (`id`, `username`, `email`, `password`, `full_name`, `avatar_url`, `role`, `is_active`, `streak_days`, `last_login_date`) VALUES
(1, 'admin', 'admin@cinnamon.com', '$2a$10$aBDGu978ItSoEkQCv8maBu0B..E2FBhRqhcAfo4Nn/3aLSvsB2GbG', 'Cinnamon Administrator', 'https://api.dicebear.com/7.x/bottts/svg?seed=admin', 'ADMIN', 1, 15, CURDATE()),
(2, 'teacher', 'teacher@cinnamon.com', '$2a$10$8qDR75TH0Xbv1YlI.NjWb.v.VKAXHs4.cOqucINltl.IeX6DSDyve', 'Ms. Sarah Jenkins', 'https://api.dicebear.com/7.x/bottts/svg?seed=sarah', 'TEACHER', 1, 28, CURDATE()),
(3, 'student', 'student@cinnamon.com', '$2a$10$dW1LXGmYbHpDVDbzClmAVuRaLCU2PEFu2ylfcUr9emf306GmCau0K', 'Alex Nguyen', 'https://api.dicebear.com/7.x/bottts/svg?seed=alex', 'STUDENT', 1, 7, CURDATE()),
(4, 'elenarose', 'elena@example.com', '$2a$10$dW1LXGmYbHpDVDbzClmAVuRaLCU2PEFu2ylfcUr9emf306GmCau0K', 'Elena Rose', 'https://api.dicebear.com/7.x/bottts/svg?seed=elena', 'STUDENT', 1, 42, CURDATE()),
(5, 'davidbeck', 'david@example.com', '$2a$10$dW1LXGmYbHpDVDbzClmAVuRaLCU2PEFu2ylfcUr9emf306GmCau0K', 'David Beck', 'https://api.dicebear.com/7.x/bottts/svg?seed=david', 'STUDENT', 1, 21, CURDATE()),
(6, 'charlotte', 'charlotte@example.com', '$2a$10$dW1LXGmYbHpDVDbzClmAVuRaLCU2PEFu2ylfcUr9emf306GmCau0K', 'Charlotte Vu', 'https://api.dicebear.com/7.x/bottts/svg?seed=charlotte', 'STUDENT', 1, 14, CURDATE());

-- ====================================================================
-- 2. USER_XP (Điểm kinh nghiệm & Cấp độ)
-- ====================================================================
INSERT INTO `user_xp` (`id`, `user_id`, `total_xp`, `level`, `weekly_xp`) VALUES
(1, 1, 2500, 26, 550),
(2, 2, 800, 9, 90),
(3, 3, 420, 5, 180),
(4, 4, 1850, 19, 420),
(5, 5, 1100, 12, 280),
(6, 6, 750, 8, 190);

-- ====================================================================
-- 3. BADGES (Huy hiệu thành tích hệ thống)
-- ====================================================================
INSERT INTO `badges` (`id`, `name`, `description`, `icon`, `condition_type`, `condition_value`) VALUES
(1, 'Tân Binh Xuất Sắc', 'Hoàn thành bài học đầu tiên trên Cinnamon', '🌱', 'LESSONS_COMPLETED', 1),
(2, 'Thắp Lửa Streak', 'Duy trì chuỗi học tập liên tiếp 7 ngày', '🔥', 'STREAK_DAYS', 7),
(3, 'Chiến Binh Cấp 5', 'Đạt cấp độ Level 5 trong hành trình học tập', '⚔️', 'LEVEL_REACHED', 5),
(4, 'Bậc Thầy Tích Lũy', 'Tích lũy tối thiểu 100 điểm kinh nghiệm XP', '👑', 'XP_REACHED', 100),
(5, 'Học Bá Chăm Chỉ', 'Hoàn thành từ 5 bài học trở lên', '📚', 'LESSONS_COMPLETED', 5),
(6, 'Chuỗi Siêu Cấp 30 Ngày', 'Duy trì chuỗi học tập kiên trì 30 ngày', '⚡', 'STREAK_DAYS', 30),
(7, 'Huyền Thoại Cinnamon', 'Đạt tổng cộng 500 điểm XP trên hệ thống', '🏆', 'XP_REACHED', 500);

-- ====================================================================
-- 4. USER_BADGES (Huy hiệu người dùng đã đạt được)
-- ====================================================================
INSERT INTO `user_badges` (`id`, `user_id`, `badge_id`, `earned_at`) VALUES
(1, 3, 1, NOW()),
(2, 3, 2, NOW()),
(3, 3, 4, NOW()),
(4, 4, 1, NOW()),
(5, 4, 2, NOW()),
(6, 4, 3, NOW()),
(7, 4, 4, NOW()),
(8, 4, 6, NOW()),
(9, 4, 7, NOW());

-- ====================================================================
-- 5. COURSES (Khóa học tiêu chuẩn CEFR)
-- ====================================================================
INSERT INTO `courses` (`id`, `title`, `description`, `level`, `thumbnail_url`, `teacher_id`, `is_published`) VALUES
(1, 'English for Beginners: Essential Daily Foundations', 'Khóa học nền tảng cho người mới bắt đầu hoặc mất gốc. Làm quen với từ vựng thông dụng, phát âm chuẩn IPA và các mẫu câu giao tiếp cơ bản mỗi ngày.', 'A1', 'https://images.unsplash.com/photo-1546410531-bb4caa6b424d?w=800&auto=format&fit=crop&q=60', 2, 1),
(2, 'Pre-Intermediate: Real-world Conversational Fluency', 'Phát triển phản xạ tự nhiên trong các tình huống thực tế: du lịch, mua sắm, đặt phòng khách sạn, giao lưu bạn bè quốc tế.', 'A2', 'https://images.unsplash.com/photo-1522202176988-66273c2fd55f?w=800&auto=format&fit=crop&q=60', 2, 1),
(3, 'Intermediate: Business & Professional Workplace English', 'Nâng tầm tiếng Anh trong môi trường công sở: viết email chuẩn mực, thuyết trình ý tưởng, đàm phán hợp đồng và phỏng vấn xin việc.', 'B1', 'https://images.unsplash.com/photo-1551836022-d5d88e9218df?w=800&auto=format&fit=crop&q=60', 2, 1),
(4, 'Upper-Intermediate: Academic Reading & Critical Debate', 'Mở rộng vốn từ vựng học thuật, phân tích các bài luận phức tạp và xây dựng khả năng lập luận sắc bén.', 'B2', 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=800&auto=format&fit=crop&q=60', 2, 1);

-- ====================================================================
-- 6. LESSONS (Danh sách bài học chi tiết)
-- ====================================================================
INSERT INTO `lessons` (`id`, `course_id`, `title`, `type`, `content`, `audio_url`, `video_url`, `order_index`, `xp_reward`) VALUES
(1, 1, 'Unit 1: Greetings & Making New Friends', 'VOCABULARY', 
'Trong bài học này, bạn sẽ học cách chào hỏi lịch sự, tự giới thiệu bản thân và hỏi thăm sức khỏe người đối diện trong các ngữ cảnh trang trọng và thân mật.

Cấu trúc cơ bản:
- Hello, nice to meet you!
- It is a pleasure to make your acquaintance.
- How have you been lately?
- What do you do in your free time?', 
NULL, NULL, 1, 25),

(2, 1, 'Unit 2: Present Simple Tense & Habits', 'GRAMMAR', 
'Thì hiện tại đơn (Present Simple) diễn tả thói quen, chân lý và sự thật hiển nhiên.

1. Cấu trúc khẳng định:
   S + V(s/es) + O
   Ví dụ:
   - I drink coffee every morning.
   - She studies English at Cinnamon.

2. Cấu trúc phủ định:
   S + do/does not + V_inf
   Ví dụ:
   - He does not wake up early on Sundays.
   - They do not eat fast food.

3. Cấu trúc nghi vấn:
   Do/Does + S + V_inf?
   Ví dụ:
   - Do you study English daily? -> Yes, I do.
   - Does she live in Hanoi? -> No, she doesn''t.', 
NULL, NULL, 2, 30),

(3, 1, 'Unit 3: Ordering Coffee & Dining Out', 'SPEAKING', 
'Học các câu giao tiếp thông dụng tại quán cà phê và nhà hàng:

1. Khi gọi đồ uống:
   - "Can I get an iced latte with oat milk, please?"
   - "Could I have a hot cappuccino and a cinnamon roll?"

2. Khi thanh toán:
   - "Could I have the bill/check, please?"
   - "Do you accept credit card payments?"
   - "Keep the change, thank you!"', 
NULL, NULL, 3, 35),

(4, 2, 'Unit 1: Talking About Past Holidays & Travel', 'VOCABULARY', 
'Chia sẻ những chuyến đi đáng nhớ trong quá khứ sử dụng thì quá khứ đơn (Past Simple) và từ vựng về điểm đến du lịch.

Các mẫu câu diễn đạt:
- "Last summer, I had an unforgettable holiday in Da Nang."
- "The scenery was breathtaking and the local food was delicious."
- "We explored many historical sites and made wonderful memories."', 
NULL, NULL, 1, 25),

(5, 2, 'Unit 2: Airport Check-in & Boarding Flight', 'LISTENING', 
'Luyện nghe và giao tiếp tại sân bay quốc tế:

1. Tại quầy thủ tục (Check-in counter):
   - "May I see your passport and booking reference?"
   - "Would you prefer a window seat or an aisle seat?"
   - "Do you have any luggage to check in?"

2. Tại cổng lên máy bay (Boarding gate):
   - "Flight VN123 is now boarding at Gate 14."
   - "Please have your boarding pass and ID ready."', 
NULL, NULL, 2, 30),

(6, 3, 'Unit 1: Professional Workplace Email Etiquette', 'WRITING', 
'Quy chuẩn viết email chuyên nghiệp trong môi trường làm việc quốc tế:

1. Lời chào trang trọng:
   - "Dear Mr. / Ms. [Last Name],"
   - "I hope this email finds you well."

2. Nêu mục đích rõ ràng:
   - "I am writing to inquire about the project timeline."
   - "Please find the attached weekly analytics report."

3. Lời kết lịch sự:
   - "Looking forward to hearing from you."
   - "Best regards, / Sincerely,"', 
NULL, NULL, 1, 30);

-- ====================================================================
-- 7. VOCABULARIES (Thẻ từ vựng Flashcard cho từng bài học)
-- ====================================================================
INSERT INTO `vocabularies` (`id`, `lesson_id`, `word`, `pronunciation`, `definition`, `example_sentence`, `image_url`, `audio_url`) VALUES
-- Unit 1 (Lesson 1)
(1, 1, 'Enthusiastic', '/ɪnˌθjuː.ziˈæs.tɪk/', 'Showing a lot of excitement and interest about something.', 'The students were very enthusiastic about the new English learning game.', 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&auto=format&fit=crop&q=60', NULL),
(2, 1, 'Acquaintance', '/əˈkweɪn.təns/', 'A person that you know but who is not a close friend.', 'He is not a close friend, just a business acquaintance.', 'https://images.unsplash.com/photo-1511632765486-a01980e01a18?w=600&auto=format&fit=crop&q=60', NULL),
(3, 1, 'Perseverance', '/ˌpɜː.sɪˈvɪə.rəns/', 'Continued effort to do or achieve something despite difficulties.', 'Learning a foreign language requires daily practice and great perseverance.', 'https://images.unsplash.com/photo-1454165804606-c3d57bc86b40?w=600&auto=format&fit=crop&q=60', NULL),
(4, 1, 'Fluency', '/ˈfluː.ən.si/', 'The ability to speak or write a language easily, well, and quickly.', 'She achieved remarkable fluency in English after studying abroad.', 'https://images.unsplash.com/photo-1455849318743-b2233052fcff?w=600&auto=format&fit=crop&q=60', NULL),
(5, 1, 'Vocabulary', '/vəˈkæb.jə.lər.i/', 'All the words known and used by a particular person.', 'Reading books regularly expands your English vocabulary quickly.', 'https://images.unsplash.com/photo-1457369804613-52c61a468e7d?w=600&auto=format&fit=crop&q=60', NULL),

-- Unit 2 (Lesson 2)
(6, 2, 'Habit', '/ˈhæb.ɪt/', 'A settled or regular tendency or practice, especially one that is hard to give up.', 'Drinking a glass of warm water in the morning is a healthy daily habit.', 'https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=600&auto=format&fit=crop&q=60', NULL),
(7, 2, 'Routine', '/ruːˈtiːn/', 'A sequence of actions regularly followed; a fixed program.', 'Her morning routine includes yoga and listening to English podcasts.', 'https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=600&auto=format&fit=crop&q=60', NULL),
(8, 2, 'Frequently', '/ˈfriː.kwənt.li/', 'Regularly or habitually; often.', 'Students who practice frequently achieve high fluency much faster.', 'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=600&auto=format&fit=crop&q=60', NULL),
(9, 2, 'Permanent', '/ˈpɜː.mə.nənt/', 'Lasting or intended to last or remain unchanged indefinitely.', 'The present simple tense expresses permanent facts and general truths.', 'https://images.unsplash.com/photo-1497215728101-856f4ea42174?w=600&auto=format&fit=crop&q=60', NULL),
(10, 2, 'Occasional', '/əˈkeɪ.ʒən.əl/', 'Occurring, appearing, or done infrequently and irregularly.', 'He enjoys an occasional cup of hot cinnamon tea in the evening.', 'https://images.unsplash.com/photo-1544717305-2782549b5136?w=600&auto=format&fit=crop&q=60', NULL),

-- Unit 3 (Lesson 3)
(11, 3, 'Beverage', '/ˈbev.ər.ɪdʒ/', 'A drink of any type, usually other than water.', 'Hot beverages like cappuccino and herbal tea are served daily.', 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=600&auto=format&fit=crop&q=60', NULL),
(12, 3, 'Receipt', '/rɪˈsiːt/', 'A piece of paper that proves that money, goods, or information have been received.', 'Could you please give me the receipt for my corporate expense report?', 'https://images.unsplash.com/photo-1554415707-9e4466b8852e?w=600&auto=format&fit=crop&q=60', NULL),
(13, 3, 'Delicious', '/dɪˈlɪʃ.əs/', 'Having a very pleasant taste or smell.', 'The cinnamon roll and fresh iced latte taste delicious.', 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=600&auto=format&fit=crop&q=60', NULL),
(14, 3, 'Recommendation', '/ˌrek.ə.menˈdeɪ.ʃən/', 'A suggestion that something is good or suitable for a particular purpose.', 'Do you have any recommendations for a light breakfast meal?', 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=600&auto=format&fit=crop&q=60', NULL),

-- Course 2 - Unit 1 (Lesson 4)
(15, 4, 'Breathtaking', '/ˈbreθˌteɪ.kɪŋ/', 'Extremely exciting, beautiful, or surprising.', 'The sunrise over the mountain was absolutely breathtaking.', 'https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600&auto=format&fit=crop&q=60', NULL),
(16, 4, 'Destination', '/ˌdes.tɪˈneɪ.ʃən/', 'The place where someone is going or where something is being sent.', 'Paris remains one of the most popular travel destinations in the world.', 'https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=600&auto=format&fit=crop&q=60', NULL),
(17, 4, 'Souvenir', '/ˌsuː.vənˈɪər/', 'Something you buy or keep to help you remember a holiday or special event.', 'I bought a handmade ceramic mug as a souvenir from Vietnam.', 'https://images.unsplash.com/photo-1526778548025-fa2f459cd5c1?w=600&auto=format&fit=crop&q=60', NULL),

-- Course 2 - Unit 2 (Lesson 5)
(18, 5, 'Departure', '/dɪˈpɑː.tʃər/', 'The act of leaving a place, especially to start a journey to another place.', 'Passengers should check the departure board for gate changes.', 'https://images.unsplash.com/photo-1436491865332-7a61a109cc05?w=600&auto=format&fit=crop&q=60', NULL),
(19, 5, 'Luggage', '/ˈlʌɡ.ɪdʒ/', 'The bags, suitcases, etc. that contain your possessions and that you take with you when you are traveling.', 'Please make sure your luggage has a clear name tag attached.', 'https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&auto=format&fit=crop&q=60', NULL),
(20, 5, 'Boarding Pass', '/ˈbɔː.dɪŋ ˌpɑːs/', 'A card that a passenger must have to be allowed to enter an aircraft.', 'You can download your mobile boarding pass to your smartphone.', 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=600&auto=format&fit=crop&q=60', NULL),

-- Course 3 - Unit 1 (Lesson 6)
(21, 6, 'Correspondence', '/ˌkɒr.ɪˈspɒn.dəns/', 'Letters, emails, etc., or the process of writing them.', 'All business correspondence should maintain a polite and professional tone.', 'https://images.unsplash.com/photo-1586769852044-692d6e3703f0?w=600&auto=format&fit=crop&q=60', NULL),
(22, 6, 'Attachment', '/əˈtætʃ.mənt/', 'A computer file that is sent together with an email message.', 'Please review the financial summary in the email attachment.', 'https://images.unsplash.com/photo-1450133064473-71024230f91b?w=600&auto=format&fit=crop&q=60', NULL),
(23, 6, 'Promptly', '/ˈprɒmpt.li/', 'Quickly, without delay, or at the arranged time.', 'We aim to reply to all client inquiries promptly within 24 hours.', 'https://images.unsplash.com/photo-1507679799987-c73779587ccf?w=600&auto=format&fit=crop&q=60', NULL);

-- ====================================================================
-- 8. QUIZZES (Bộ đề kiểm tra Checkpoint)
-- ====================================================================
INSERT INTO `quizzes` (`id`, `lesson_id`, `title`, `type`, `time_limit_seconds`, `xp_reward`) VALUES
(1, 1, 'Checkpoint Quiz: Unit 1 Foundations', 'MULTIPLE_CHOICE', 180, 30),
(2, 2, 'Checkpoint Quiz: Present Simple Mastery', 'MULTIPLE_CHOICE', 180, 30),
(3, 3, 'Checkpoint Quiz: Dining & Café Expressions', 'MULTIPLE_CHOICE', 180, 35),
(4, 4, 'Checkpoint Quiz: Travel & Holidays', 'MULTIPLE_CHOICE', 180, 30),
(5, 5, 'Checkpoint Quiz: Airport Vocabulary', 'MULTIPLE_CHOICE', 180, 30),
(6, 6, 'Checkpoint Quiz: Business Email Skills', 'MULTIPLE_CHOICE', 180, 35);

-- ====================================================================
-- 9. QUIZ_QUESTIONS (Câu hỏi trắc nghiệm)
-- ====================================================================
INSERT INTO `quiz_questions` (`id`, `quiz_id`, `question_text`, `question_type`, `order_index`) VALUES
-- Quiz 1
(1, 1, 'What is the meaning of the word \'Perseverance\'?', 'MULTIPLE_CHOICE', 1),
(2, 1, 'Which phrase is the most natural greeting when meeting someone for the first time in a professional meeting?', 'MULTIPLE_CHOICE', 2),
(3, 1, 'Complete the sentence: \'She was extremely ________ about joining the Cinnamon English club.\'', 'MULTIPLE_CHOICE', 3),

-- Quiz 2
(4, 2, 'She ________ English every day to improve her communication skills.', 'MULTIPLE_CHOICE', 1),
(5, 2, 'They ________ wake up early on Sundays because they don\'t have classes.', 'MULTIPLE_CHOICE', 2),
(6, 2, 'Water ________ at 100 degrees Celsius under standard atmospheric pressure.', 'MULTIPLE_CHOICE', 3),
(7, 2, '________ your brother play guitar in a music band?', 'MULTIPLE_CHOICE', 4),

-- Quiz 3
(8, 3, 'Which phrase is the most polite and natural way to order a drink?', 'MULTIPLE_CHOICE', 1),
(9, 3, 'What does the phrase \'Keep the change\' imply when paying the bill?', 'MULTIPLE_CHOICE', 2),

-- Quiz 4
(10, 4, 'Complete the sentence: \'The mountain view at sunrise was absolutely ________!\'', 'MULTIPLE_CHOICE', 1),
(11, 4, 'What is the correct past form: \'Last summer, we ________ (go) to Da Nang on vacation.\'?', 'MULTIPLE_CHOICE', 2),

-- Quiz 5
(12, 5, 'Which document is strictly required to get onto an airplane at the boarding gate?', 'MULTIPLE_CHOICE', 1),
(13, 5, 'What is the place called where flights take off and leave the airport?', 'MULTIPLE_CHOICE', 2),

-- Quiz 6
(14, 6, 'Which is the most appropriate closing for a formal business email?', 'MULTIPLE_CHOICE', 1),
(15, 6, 'Complete the sentence: \'Please find ________ the updated project proposal document.\'', 'MULTIPLE_CHOICE', 2);

-- ====================================================================
-- 10. QUIZ_OPTIONS (Các đáp án lựa chọn trắc nghiệm)
-- ====================================================================
INSERT INTO `quiz_options` (`id`, `question_id`, `option_text`, `is_correct`) VALUES
-- Câu 1 (Quiz 1)
(1, 1, 'The ability to give up quickly when facing obstacles', 0),
(2, 1, 'Continued effort and determination despite difficulties', 1),
(3, 1, 'A feeling of extreme tiredness after work', 0),
(4, 1, 'An accidental mistake in pronunciation', 0),

-- Câu 2 (Quiz 1)
(5, 2, 'Hey dude, what\'s up?', 0),
(6, 2, 'It is a pleasure to make your acquaintance.', 1),
(7, 2, 'Catch you later, bye!', 0),
(8, 2, 'Who are you anyway?', 0),

-- Câu 3 (Quiz 1)
(9, 3, 'enthusiastic', 1),
(10, 3, 'enthusiastically', 0),
(11, 3, 'enthusiasm', 0),
(12, 3, 'enthuse', 0),

-- Câu 4 (Quiz 2)
(13, 4, 'study', 0),
(14, 4, 'studies', 1),
(15, 4, 'studying', 0),
(16, 4, 'studied', 0),

-- Câu 5 (Quiz 2)
(17, 5, 'doesn\'t', 0),
(18, 5, 'don\'t', 1),
(19, 5, 'aren\'t', 0),
(20, 5, 'isn\'t', 0),

-- Câu 6 (Quiz 2)
(21, 6, 'boil', 0),
(22, 6, 'boils', 1),
(23, 6, 'boiling', 0),
(24, 6, 'is boil', 0),

-- Câu 7 (Quiz 2)
(25, 7, 'Do', 0),
(26, 7, 'Does', 1),
(27, 7, 'Is', 0),
(28, 7, 'Are', 0),

-- Câu 8 (Quiz 3)
(29, 8, 'Give me coffee right now!', 0),
(30, 8, 'Could I please get an iced latte?', 1),
(31, 8, 'I want drink!', 0),
(32, 8, 'Coffee bring here.', 0),

-- Câu 9 (Quiz 3)
(33, 9, 'You want every single coin returned to you', 0),
(34, 9, 'The remaining balance is given as a tip to the staff', 1),
(35, 9, 'You forgot your debit card', 0),
(36, 9, 'You want to change your menu order', 0),

-- Câu 10 (Quiz 4)
(37, 10, 'breathtaking', 1),
(38, 10, 'boring', 0),
(39, 10, 'breathe', 0),
(40, 10, 'breathless', 0),

-- Câu 11 (Quiz 4)
(41, 11, 'goed', 0),
(42, 11, 'went', 1),
(43, 11, 'gone', 0),
(44, 11, 'are go', 0),

-- Câu 12 (Quiz 5)
(45, 12, 'Boarding Pass & Valid ID/Passport', 1),
(46, 12, 'Coffee receipt', 0),
(47, 12, 'Gym membership card', 0),
(48, 12, 'Library card', 0),

-- Câu 13 (Quiz 5)
(49, 13, 'Arrival lounge', 0),
(50, 13, 'Departure gate', 1),
(51, 13, 'Baggage claim belt', 0),
(52, 13, 'Parking lot', 0),

-- Câu 14 (Quiz 6)
(53, 14, 'Cya later mate!', 0),
(54, 14, 'Best regards,', 1),
(55, 14, 'Bye now', 0),
(56, 14, 'Talk later', 0),

-- Câu 15 (Quiz 6)
(57, 15, 'attached', 1),
(58, 15, 'attachment', 0),
(59, 15, 'attaching', 0),
(60, 15, 'attach', 0);

-- ====================================================================
-- 11. USER_PROGRESS (Tiến trình bài học mẫu của học viên)
-- ====================================================================
INSERT INTO `user_progress` (`id`, `user_id`, `lesson_id`, `is_completed`, `score`, `completed_at`) VALUES
(1, 3, 1, 1, 100, NOW()),
(2, 3, 2, 1, 100, NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- ====================================================================
-- CINNAMON - NẠP DỮ LIỆU THÀNH CÔNG!
-- ====================================================================

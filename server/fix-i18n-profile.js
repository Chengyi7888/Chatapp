'use strict';
const fs = require('fs');
const p = 'C:/Users/cheng/Documents/Codex/Chatapp/android/src/com/chatapp/app/I18n.java';
let t = fs.readFileSync(p, 'utf8');
const anchor = '        put("install_go_settings", "去设置", "前往設定", "Go to settings");';
const add = `
        put("profile", "个人名片", "個人名片", "Profile card");
        put("avatar", "头像", "頭像", "Avatar");
        put("nickname", "昵称", "暱稱", "Nickname");
        put("gender", "性别", "性別", "Gender");
        put("gender_male", "男", "男", "Male");
        put("gender_female", "女", "女", "Female");
        put("gender_hidden", "隐藏", "隱藏", "Hidden");
        put("gender_other", "其他", "其他", "Other");
        put("birthday", "生日", "生日", "Birthday");
        put("tags", "个性标签", "個性標籤", "Tags");
        put("my_photos", "我的照片", "我的照片", "My photos");
        put("job", "职业", "職業", "Job");
        put("company", "公司", "公司", "Company");
        put("location", "所在地", "所在地", "Location");
        put("birthplace", "出生地", "出生地", "Birthplace");
        put("email", "邮箱", "郵箱", "Email");
        put("add_tag", "添加标签", "新增標籤", "Add tag");
        put("tag_hint", "输入标签内容（自动加#）", "輸入標籤內容（自動加#）", "Enter tag (auto #)");
        put("add_photo", "添加照片", "新增照片", "Add photo");
        put("max_photos", "最多9张照片", "最多9張照片", "Max 9 photos");
        put("job_computer", "计算机", "計算機", "Computer");
        put("job_manufacturing", "生产", "生產", "Manufacturing");
        put("job_medical", "医疗", "醫療", "Medical");
        put("job_finance", "金融", "金融", "Finance");
        put("job_business", "商业", "商業", "Business");
        put("job_culture", "文化", "文化", "Culture");
        put("job_entertainment", "娱乐", "娛樂", "Entertainment");
        put("job_legal", "法务", "法務", "Legal");
        put("job_education", "教育", "教育", "Education");
        put("job_admin", "行政", "行政", "Admin");
        put("job_student", "学生", "學生", "Student");
        put("job_other", "其他职业", "其他職業", "Other");
        put("location_hint", "格式：广东-深圳-南山", "格式：廣東-深圳-南山", "Format: Guangdong-Shenzhen-Nanshan");
        put("birthplace_hint", "格式：广东-深圳-南山", "格式：廣東-深圳-南山", "Format: Guangdong-Shenzhen-Nanshan");
        put("company_hint", "输入公司名称", "輸入公司名稱", "Enter company");
        put("email_hint", "输入邮箱地址", "輸入郵箱地址", "Enter email");`;
if (t.includes(anchor)) {
  t = t.split(anchor).join(anchor + add);
  fs.writeFileSync(p, t);
  console.log('I18n profile keys added');
} else {
  console.log('ANCHOR NOT FOUND');
}

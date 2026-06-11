# UP-SELF 接口文档 V1.0

这是我根据你目前 **Student Demo范围 + 后续正式开发可扩展性** 设计的接口清单。

原则：

```text
Demo必须能跑
+
以后不用推翻重做
```

------

# Module 0 Authentication

## 0.1 Login

### POST /api/auth/login

功能：

```text
学生登录
```

请求：

```json
{
  "email": "student@upself.com",
  "password": "123456"
}
```

返回：

```json
{
  "code": 200,
  "msg": "Login Successful",
  "data": {
    "userId": 1,
    "name": "Sarah",
    "role": "STUDENT"
  }
}
```

------

## 0.2 Current User

### GET /api/auth/me

功能：

```text
获取当前用户信息
```

------

# Module 1 Dashboard

## 1.1 Dashboard Summary

### GET /api/dashboard

功能：

```text
获取首页数据
```

返回：

```json
{
  "name": "Sarah",
  "mood": "Happy",
  "goal": "Finish Chapter 3",
  "progress": 65,
  "badge": "First Step",
  "currentCourse": "Entrepreneurship Basics",
  "currentChapter": "Identifying Opportunities"
}
```

------

# Module 2 Learning

------

## 2.1 Course List

### GET /api/courses

功能：

```text
获取课程列表
```

参数：

```text
keyword
category
pageNum
pageSize
```

------

## 2.2 Course Detail

### GET /api/courses/{courseId}

功能：

```text
获取课程详情
```

------

## 2.3 Chapter Detail

### GET /api/chapters/{chapterId}

功能：

```text
获取章节内容
```

返回：

```json
{
  "chapterId": 3,
  "title": "Identifying Opportunities",
  "content": "..."
}
```

------

## 2.4 Mark Chapter Complete

### PUT /api/progress/chapter/{chapterId}

功能：

```text
完成章节
更新进度
```

------

## 2.5 Learning Progress

### GET /api/progress

功能：

```text
获取学习进度
```

------

## 2.6 Continue Learning

### GET /api/progress/current

功能：

```text
获取当前课程和章节
```

------

# Module 3 Goals

------

## 3.1 Goal List

### GET /api/goals

功能：

```text
获取目标列表
```

------

## 3.2 Create Goal

### POST /api/goals

请求：

```json
{
  "title": "Finish Chapter 3"
}
```

------

## 3.3 Update Goal

### PUT /api/goals/{goalId}

------

## 3.4 Delete Goal

### DELETE /api/goals/{goalId}

------

## 3.5 Complete Goal

### PUT /api/goals/{goalId}/complete

功能：

```text
完成目标
```

------

# Module 4 Mood Tracker

------

## 4.1 Save Mood

### POST /api/moods

请求：

```json
{
  "mood": "Happy",
  "note": "Feeling productive today"
}
```

------

## 4.2 Latest Mood

### GET /api/moods/latest

功能：

```text
Dashboard显示
```

------

## 4.3 Mood History

### GET /api/moods

功能：

```text
查看历史记录
```

------

## 4.4 Mood Analytics

### GET /api/moods/analytics

功能：

```text
统计图
```

后期做。

------

# Module 5 Assignments

------

## 5.1 Assignment List

### GET /api/assignments

功能：

```text
获取作业列表
```

------

## 5.2 Assignment Detail

### GET /api/assignments/{assignmentId}

功能：

```text
获取作业详情
```

------

## 5.3 Upload File

### POST /api/files/upload

功能：

```text
上传文件
```

返回：

```json
{
  "filePath": "/uploads/report.pdf"
}
```

------

## 5.4 Submit Assignment

### POST /api/submissions

请求：

```json
{
  "assignmentId": 1,
  "filePath": "/uploads/report.pdf"
}
```

------

## 5.5 Submission History

### GET /api/submissions

功能：

```text
查看提交历史
```

------

## 5.6 Submission Detail

### GET /api/submissions/{submissionId}

------

# Module 6 Achievement

------

## 6.1 Badge List

### GET /api/badges

功能：

```text
查看所有徽章
```

------

## 6.2 Latest Badge

### GET /api/badges/latest

功能：

```text
Dashboard展示
```

------

# Module 7 AI Assistant

------

## 7.1 Chat

### POST /api/ai/chat

请求：

```json
{
  "message": "What is a business model canvas?"
}
```

返回：

```json
{
  "reply": "A business model canvas is..."
}
```

------

## 7.2 Generate Summary

### POST /api/ai/summary

功能：

```text
总结课程内容
```

------

## 7.3 Generate Quiz

### POST /api/ai/quiz

功能：

```text
生成测验
```

------

# Student Demo 第一版实际开发

先做下面这些：

```text
POST /api/auth/login

GET /api/dashboard

GET /api/courses

GET /api/courses/{id}

GET /api/chapters/{id}

PUT /api/progress/chapter/{id}

GET /api/goals

POST /api/goals

POST /api/moods

GET /api/assignments

POST /api/submissions

POST /api/ai/chat
```

总共：

```text
12个API
```

这 12 个接口已经可以完整演示：

```text
Login
↓
Dashboard
↓
Learning
↓
Mark Complete
↓
Goal
↓
Mood
↓
Assignment
↓
AI Assistant
```

并且以后扩展 Mentor/Admin 时，这套接口基本不用推翻。
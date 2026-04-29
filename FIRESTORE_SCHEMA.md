# BetterTick Firestore Database Schema

## Collection Structure

```
firestore/
├── users/{userId}                          # 유저 프로필
│   ├── id: string
│   ├── email: string
│   ├── displayName: string
│   ├── createdAt: timestamp
│   └── settings: map
│
├── users/{userId}/tasks/{taskId}           # 할 일
│   ├── id: string
│   ├── title: string
│   ├── notes: string
│   ├── listId: string                      # → lists/{listId}
│   ├── tagIds: array<string>               # → tags/{tagId}
│   ├── dueDate: timestamp | null
│   ├── isCompleted: boolean
│   ├── completedAt: timestamp | null
│   ├── isAbandoned: boolean                  # 계획 취소 — rendered with blue X
│   ├── abandonedAt: timestamp | null
│   ├── priority: number (0=none, 1=low, 2=med, 3=high)
│   ├── repeatRule: string | null           # RRULE format
│   ├── createdAt: timestamp
│   ├── updatedAt: timestamp
│   └── sortOrder: number
│
├── users/{userId}/lists/{listId}           # 할 일 목록
│   ├── id: string
│   ├── name: string
│   ├── color: string (#hex)
│   ├── icon: string
│   ├── isDefault: boolean
│   ├── sortOrder: number
│   └── createdAt: timestamp
│
├── users/{userId}/tags/{tagId}             # 태그
│   ├── id: string
│   ├── name: string
│   ├── color: string (#hex)
│   └── createdAt: timestamp
│
├── users/{userId}/habits/{habitId}         # 습관 정의
│   ├── id: string
│   ├── name: string
│   ├── icon: string
│   ├── color: string (#hex)
│   ├── frequency: string (daily/weekly/custom)
│   ├── targetDays: array<number> (1=Mon..7=Sun)
│   ├── reminderTime: string | null (HH:mm)
│   ├── sortOrder: number
│   ├── isArchived: boolean
│   └── createdAt: timestamp
│
├── users/{userId}/habitLogs/{logId}        # 습관 완료 기록
│   ├── id: string
│   ├── habitId: string                     # → habits/{habitId}
│   ├── date: string (yyyy-MM-dd)
│   ├── isCompleted: boolean
│   └── completedAt: timestamp | null
│
├── users/{userId}/focusCategories/{catId}  # 포커스 활동 카테고리
│   ├── id: string
│   ├── name: string
│   ├── icon: string
│   ├── color: string (#hex)
│   ├── sortOrder: number
│   └── createdAt: timestamp
│
└── users/{userId}/focusSessions/{sesId}    # 포커스 타이머 기록
    ├── id: string
    ├── activityName: string
    ├── activityIcon: string
    ├── activityColor: string (#hex)
    ├── durationSeconds: number
    ├── startedAt: timestamp
    ├── endedAt: timestamp | null
    └── isCompleted: boolean
```

## Security Rules

각 유저는 자신의 `/users/{userId}` 하위 컬렉션만 읽기/쓰기 가능.
인증되지 않은 요청은 모두 거부.

## Indexes (자동 생성)

Firestore가 단일 필드 인덱스를 자동 생성합니다.
복합 쿼리가 필요한 경우 앱 실행 시 에러 로그에서 인덱스 생성 링크가 제공됩니다.

## Offline Caching

Android SDK는 기본적으로 100MB 오프라인 캐시를 사용합니다.
별도 설정 없이 오프라인에서 읽기/쓰기 가능, 온라인 복귀 시 자동 동기화.

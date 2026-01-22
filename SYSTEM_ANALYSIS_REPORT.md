# System Analysis Report

## 1. API Endpoints & Functionality Summary

The system is organized into domain-specific controllers. Below is a summary of the key endpoints identified:

### Authentication (`/api/auth`)
- **Register**: Standard and OAuth2 (Google, GitHub) registration.
- **Login/Refresh**: JWT-based authentication with token refresh mechanism.
- **Logout**: Token invalidation.

### Project Management (`/api/projects`)
- **Create/Update/Delete**: Full CRUD for projects. Updates include status, skills, and recruitments.
- **Search**: Advanced search with filters (category, skills, etc.).
- **Details**: `V2` endpoint provides detailed project info with like counts.
- **Status Management**: Endpoints to complete or end a project.

### Project Members & Applications (`/api/projects/{projectId}`)
- **Apply**: Users can apply to projects (`/application`).
- **Manage Applications**: Project leaders can view list, approve, or reject applications.
- **Members**: View list of current members, withdraw, or kick members (leader only).

### Member Profile (`/api/members`)
- **Profile**: View and update user profile (skills, portfolio).
- **Search**: Find other members.

### Notification (`/api/notifications`)
- **Subscribe**: SSE subscription for real-time updates.
- **Read**: Mark notifications as read.

### Code Review & PR (`/api/codereview`, `/api/prs`)
- **Manage**: Create and manage code reviews linked to GitHub PRs.
- **Chat**: Specific chat functionality for code reviews.

### Other
- **File**: S3 file upload/delete.
- **Webhook**: GitHub webhook processing.
- **Chat**: Real-time chat (WebSocket).

---

## 2. Core Business Rules

### Project & Recruitment
- **Date Validity**: Project end date must be after the start date and the current date (`INVALID_PROJECT_DATE`).
- **Status Constraints**:
  - Completed projects cannot be modified, deleted, or applied to (`PROJECT_ALREADY_COMPLETED`).
  - Reviews can only be written for completed projects (`PROJECT_NOT_COMPLETED`).
- **Recruitment Limits**: Applications are rejected if the recruitment count for a specific position is full (`RECRUITMENT_FULL`).
- **Double application**: Users cannot apply to the same project twice (`PROJECT_APPLICATION_ALREADY_EXISTS`) or if they are already a member (`PROJECT_MEMBER_ALREADY_EXISTS`).

### Authorization & Ownership
- **Leader Only**: Only the project creator can update/delete the project or manage members/applications (`PROJECT_MEMBER_FORBIDDEN`).
- **Creator Protection**: The project creator cannot be removed or withdraw from the project (`CREATOR_DELETE_FORBIDDEN`).
- **Review Rights**: Only project members can write reviews (`PROJECT_MEMBER_FORBIDDEN` / `PROJECT_MEMBER_NOT_FOUND`).

### File Management
- **Validation**: Strict checks on file extension (`INVALID_FILE_EXTENSION`) and size (`FILE_SIZE_EXCEEDED`).

### Authentication
- **Uniqueness**: Email must be unique (`DUPLICATE_MEMBER`).
- **OAuth2**: Essential attributes (email, provider ID) must be present in the OAuth response.

---

## 3. Potential Bugs & Code Smells

### 3.1 N+1 Query Problem (Performance Risk)
**Location:** `ProjectServiceImpl.searchProject` & `searchMainPageProject`
**Analysis:**
Inside the `content.map(project -> ...)` lambda, the code calls:
1. `projectCategoryApplicationRepository.findTotalCountsByProject(project)`
2. `projectMemberServiceImpl.getProjectMembers(project.getId())`
3. `projectLikeRepository.existsByMemberIdAndProjectId(...)`

**Impact:** If a page size is 20, this results in **1 (main query) + 20 * 3 (sub-queries) = 61 database queries** per request. This will severely degrade performance as data grows.
**Recommendation:** Use `JOIN FETCH` or `DTO Projection` in the initial repository query to fetch all necessary data (counts, members, like status) in a single query.

### 3.2 Concurrency Handling
**Location:** `ProjectMemberServiceImpl.addMember`
**Analysis:**
The method uses `@Retry`, which suggests that `ObjectOptimisticLockingFailureException` or similar conflicts are expected when updating recruitment counts (`projectCategoryApplication.increaseCurrentCount()`).
**Risk:** While `@Retry` helps, it is a reactive measure. High contention on popular projects could still lead to failures.
**Recommendation:** Consider using a direct update query (`UPDATE ... SET count = count + 1 WHERE ...`) or Redis atomic counters to handle recruitment limits more robustly.

### 3.3 Hardcoded Configuration (Code Smell)
**Location:** `AuthController` (and potentially others)
**Analysis:**
Domain strings (e.g., `.meeteam.alom-sejong.com`) seem to be hardcoded or tightly coupled in cookie generation.
**Impact:** This makes it difficult to deploy the application to different environments (e.g., dev, staging, production) without code changes.
**Recommendation:** Move all domain and URL configurations to `application.yml` and inject them via `@Value` or `@ConfigurationProperties`.

### 3.4 Dead/Commented Code
**Location:** `ChatController.java`
**Analysis:**
The entire class appears to be commented out.
**Impact:** Creates confusion about the status of the Chat feature.
**Recommendation:** Either restore the functionality if needed or delete the file to keep the codebase clean.

# Anime Streaming Application – Frontend Integration Guide

This document provides complete details for the frontend team to build the UI and consume the backend services.

## Architecture Overview

The backend uses a **Microservice Architecture**. The frontend should **NEVER** call individual microservices directly. Instead, all traffic must go through the **API Gateway**.

**API Gateway Base URL:** `http://localhost:8080`

The API Gateway routes requests to specific microservices based on the URL path:
- `/v1/user/apis/**` ➡️ Routed to **User Service**
- `/v1/upload/**` ➡️ Routed to **Video Upload Service**

*(Note: The Video Encoding Service runs asynchronously via Kafka and does not expose HTTP routes to the frontend).*

---

## 1. User & Authentication Service

**Base Path:** `http://localhost:8080/v1/user/apis`

### 1.1 Sign Up
Creates a new user account.

- **Route:** `POST /auth/signup`
- **Headers:** `Content-Type: application/json`
- **Body:**
  ```json
  {
    "name": "John Doe",
    "username": "johndoe123",
    "email": "john@example.com",
    "password": "securepassword123"
  }
  ```
- **Expected Response (200 OK):**
  ```text
  user created successfully
  ```

### 1.2 Login
Authenticates the user and returns access and refresh tokens.

- **Route:** `POST /auth/login`
- **Headers:** `Content-Type: application/json`
- **Body:**
  ```json
  {
    "Username": "johndoe123",  // Note the capital 'U'
    "password": "securepassword123"
  }
  ```
- **Expected Response (200 OK):**
  ```json
  {
    "accessToken": "ey...",
    "refreshToken": "ey..."
  }
  ```

### 1.3 Get User Profile
Fetches the logged-in user's profile details.

- **Route:** `GET /profile`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Expected Response (200 OK):**
  ```json
  {
    "username": "johndoe123",
    "email": "john@example.com",
    "name": "John Doe"
  }
  ```

### 1.4 Edit Profile
Updates the user's display name.

- **Route:** `PATCH /edit-profile`
- **Headers:** 
  - `Authorization: Bearer <accessToken>`
  - `Content-Type: application/json`
- **Body:**
  ```json
  {
    "name": "John New Name"
  }
  ```
- **Expected Response (200 OK):**
  ```text
  edited successfully
  ```

### 1.5 Logout & Refresh Tokens
*(Note: These routes use query parameters based on current controller implementation)*
- **Refresh Token:** `GET /auth/refresh?refreshToken={token}&accessToken={token}`
- **Logout:** `POST /auth/logout?refreshToken={token}&accessToken={token}`

---

## 2. Video Upload Service (Direct-to-S3 Multipart)

**Base Path:** `http://localhost:8080/v1/upload`

To optimize bandwidth and memory, videos are uploaded in 3 steps: 
1. Initialize the upload with our backend.
2. Upload chunks directly to AWS S3 (Client-side).
3. Complete the upload with our backend.

### Step 2.1: Initialize Upload
Tells the backend a file is coming. The backend will return AWS S3 pre-signed URLs.

- **Route:** `POST /init`
- **Headers:** 
  - `Authorization: Bearer <accessToken>`
  - `Content-Type: application/json`
- **Body:**
  ```json
  {
    "fileName": "my_anime_video.mp4",
    "contentType": "video/mp4",
    "fileSize": 104857600  // Size in bytes
  }
  ```
- **Expected Response (201 Created):**
  ```json
  {
    "videoId": "uuid-string",
    "uploadId": "aws-multipart-upload-id",
    "s3Key": "videos/uuid/raw/my_anime_video.mp4",
    "presignedUrls": [
      {
        "partNumber": 1,
        "url": "https://s3.amazonaws.com/..."
      },
      {
        "partNumber": 2,
        "url": "https://s3.amazonaws.com/..."
      }
    ]
  }
  ```

### Step 2.2: Upload Chunks to S3 (Frontend-only action)
Using the `presignedUrls` array from Step 1, the frontend must loop through and upload the video chunks.

- **Action:** For each chunk, make an HTTP `PUT` request directly to the presigned `url`.
- **Headers Required on PUT:** None (S3 auth is in the URL).
- **CRITICAL:** When S3 responds with a `200 OK`, you **must** read the `ETag` header from the response. Store this ETag along with the `partNumber`.

### Step 2.3: Complete Upload
Once all chunks are successfully uploaded to S3, notify our backend to assemble the final video and trigger the Encoding process.

- **Route:** `POST /complete`
- **Headers:** 
  - `Authorization: Bearer <accessToken>`
  - `Content-Type: application/json`
- **Body:**
  ```json
  {
    "videoId": "uuid-string",          // From Step 1
    "uploadId": "aws-multipart-upload-id", // From Step 1
    "parts": [
      { "partNumber": 1, "etag": "\"xyz123\"" },  // ETag from S3 Response Headers
      { "partNumber": 2, "etag": "\"abc456\"" }
    ]
  }
  ```
- **Expected Response (200 OK):**
  ```json
  {
    "videoId": "uuid-string",
    "s3Key": "videos/uuid-string/raw/...",
    "status": "COMPLETED"
  }
  ```

### Step 2.4: Cancel/Abort Upload (Optional)
If a user cancels the upload mid-way, call this route so we can clean up orphaned chunks on S3.

- **Route:** `DELETE /{uploadId}`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Expected Response (200 OK):**
  ```json
  {
    "uploadId": "aws-multipart-upload-id",
    "status": "FAILED"
  }
  ```

---

## Data Flow & State Management Expectations for Frontend

1. **Auth State:** Store `accessToken` securely (e.g., in memory or HttpOnly cookies if migrated later) and attach it as a `Bearer` token to all secure requests.
2. **Video Uploading UI:** 
    - Because large files are uploaded in chunks (multipart), the frontend should calculate overall progress by keeping track of how many parts (out of the total `presignedUrls` length) have been successfully uploaded to S3.
    - If a single chunk `PUT` request to S3 fails, the frontend should retry that specific chunk before failing the entire upload.
3. **Video Processing:**
    - After Step 2.3 (Complete Upload) succeeds, the video is placed in a `PROCESSING` state by our backend workers. It will not be immediately streamable.

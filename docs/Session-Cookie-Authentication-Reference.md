# Session Cookie Authentication Reference

## Overview

This document explains how session-based authentication works between the React frontend and Spring Security backend in the Trade Capture System.

**Backend DOES use session cookies** with Spring Security. Here's how it works:

## How Session Cookie Authentication Works

### Frontend Side (`api.ts`)

```typescript
const api = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 10000,
  withCredentials: true, // ← This is the key!
  headers: {
    "Content-Type": "application/json",
  },
});
```

**`withCredentials: true`** tells the browser to:

- **Send cookies** (including session cookies) with every request to `localhost:8080`
- **Store cookies** that the server sends back
- **Include the `JSESSIONID` cookie** in subsequent requests

### Backend Side (Spring Security)

The `AuthorizationController` creates and manages sessions:

```java
// After successful login, Spring Security creates a session
request.getSession(true).setAttribute(
    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
    SecurityContextHolder.getContext()
);
```

## The Complete Authentication Flow

### 1. Login Process

1. **Frontend** calls `/api/login/{username}` with credentials
2. **Backend** authenticates using `AuthenticationManager`
3. **Backend** stores authentication in HTTP session
4. **Backend** sends `JSESSIONID` cookie back to browser
5. **Browser** automatically stores the session cookie

### 2. Subsequent Requests

1. **Browser** automatically includes `JSESSIONID` cookie (because `withCredentials: true`)
2. **Spring Security** reads the session cookie
3. **Spring Security** loads the authentication from the session
4. **Request proceeds** as authenticated user

## 🎯 Why This Setup Works

### Session Management

- Spring Security **automatically creates sessions** when authentication succeeds
- The **`JSESSIONID` cookie** is sent with every response
- Your frontend **automatically includes it** in future requests

### CORS Configuration

Your backend must have CORS configured to allow credentials:

```java
.csrf(csrf -> csrf.disable())  // Allows cross-origin requests
// Plus CORS config somewhere allowing credentials from localhost:3000
```

### Security Context Persistence

```java
HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
```

This stores the **entire authentication context** in the HTTP session, so subsequent requests don't need to re-authenticate.

## 🔧 What `withCredentials: true` Actually Does

Without it:

- Cookies ignored
- Each request treated as anonymous
- Would need to send Authorization header every time

With it:

- Browser sends `JSESSIONID` automatically
- Spring Security recognizes authenticated session
- Seamless authenticated experience

## 💡 Key Benefits

1. **Stateful Authentication** - Server remembers who you are
2. **Automatic Cookie Handling** - No manual token management needed
3. **Spring Security Integration** - Leverages built-in session management
4. **Cross-Origin Support** - Works between frontend (3000) and backend (8080)

This is a **traditional session-based authentication** approach, which is simpler than JWT tokens but requires the server to maintain session state. Perfect for single-server development setup!

## Key Questions & Answers

### 1. Does backend set JSESSIONID?

**YES** - The backend automatically sets `JSESSIONID` cookie when authentication succeeds.

**Code Location:** `AuthorizationController.java`

```java
@PostMapping("/{userName}")
public ResponseEntity<?> login(@PathVariable(name = "userName") String userName,
        @RequestParam(name = "Authorization") String authorization,
        HttpServletRequest request) {

    try {
        // Authenticate user credentials
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userName, authorization);
        Authentication authentication = authenticationManager.authenticate(token);

        // Set authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Store authentication in HTTP session - this triggers JSESSIONID cookie creation
        if (request != null) {
            request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
            );
        }

        return ResponseEntity.ok("Login successful");
    } catch (Exception e) {
        // Handle authentication failure
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Login failed");
    }
}
```

**What happens:**

- `request.getSession(true)` creates a new HTTP session
- Spring Boot automatically sends `Set-Cookie: JSESSIONID=ABC123...` in response headers
- Browser stores the cookie automatically

### 2. Is login stored in a cookie?

**YES** - But indirectly through session storage.

**Storage Architecture:**

- **Server-side**: Full authentication details stored in HTTP session memory
- **Client-side**: Only session ID stored as `JSESSIONID` cookie

**What's stored where:**

| Location                | Data                                                    |
| ----------------------- | ------------------------------------------------------- |
| **Server HTTP Session** | Username, roles, authentication status, SecurityContext |
| **Browser Cookie**      | Session identifier only (`JSESSIONID=ABC123...`)        |

**Code that stores authentication:**

```java
// Store complete SecurityContext in server session
request.getSession(true).setAttribute(
    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
    SecurityContextHolder.getContext()
);
```

**Security Benefits:**

- Sensitive data (roles, permissions) never leaves the server
- Cookie only contains session identifier
- Session expires automatically on server timeout

### 3. Does CORS require credentials?

**YES** - CORS must explicitly allow credentials for cookie-based authentication.

**Backend CORS Configuration:** `WebConfig.java`

```java
@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:3000", "http://localhost:5173")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);  // ← CRITICAL for session cookies
            }
        };
    }
}
```

**Frontend Configuration:** `api.ts`

```typescript
const api = axios.create({
  baseURL: "http://localhost:8080/api",
  timeout: 10000,
  withCredentials: true, // ← CRITICAL for sending cookies
  headers: {
    "Content-Type": "application/json",
  },
});
```

## Complete Authentication Flow

### Login Process

1. **Frontend** → `POST /api/login/username` with credentials
2. **Backend** → Validates credentials via `AuthenticationManager`
3. **Backend** → Creates HTTP session with `request.getSession(true)`
4. **Backend** → Stores `SecurityContext` in session
5. **Backend** → Responds with `Set-Cookie: JSESSIONID=...`
6. **Browser** → Automatically stores `JSESSIONID` cookie

### Subsequent API Requests

1. **Browser** → Automatically includes `Cookie: JSESSIONID=...` header
2. **Backend** → Spring Security reads `JSESSIONID` from request
3. **Backend** → Loads `SecurityContext` from server session storage
4. **Backend** → Request proceeds with authenticated user context

### Session Validation

```java
// Spring Security automatically handles this:
// 1. Extract JSESSIONID from Cookie header
// 2. Look up HTTP session in server memory
// 3. Load SecurityContext from session attribute
// 4. Set authentication in current request context
```

## Configuration Requirements

### Frontend Requirements

```typescript
// In api.ts or axios configuration
withCredentials: true; // Enables cookie sending/receiving
```

### Backend Requirements

```java
// In WebConfig.java
.allowCredentials(true)  // Allows cookies from specified origins

// In SecurityConfig.java (implicit session management)
// Spring Security automatically manages sessions when authentication is set
```

### Browser Behavior

- Automatically stores cookies when `Set-Cookie` header received
- Automatically sends cookies when `withCredentials: true`
- Respects `SameSite` and domain policies
- Handles cookie expiration automatically

## Security Considerations

### Advantages of Session Authentication

- **Server-controlled**: Server can invalidate sessions immediately
- **Secure storage**: Sensitive data never sent to client
- **Automatic expiration**: Sessions timeout without client action
- **Spring Security integration**: Leverages built-in security features

### CORS Security

- `allowCredentials(true)` requires explicit origin specification
- Cannot use `allowedOrigins("*")` with credentials enabled
- Must specify exact frontend URLs (`localhost:3000`, `localhost:5173`)

### Session Security

```java
// Session is created only after successful authentication
request.getSession(true)  // Creates session if none exists

// Authentication stored in session-scoped attribute
HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
```

## Troubleshooting

### Common Issues

**Cookies not being sent:**

- Check `withCredentials: true` in frontend
- Verify `allowCredentials(true)` in backend CORS
- Ensure origins match exactly (no trailing slashes)

**Authentication not persisting:**

- Verify session creation in `AuthorizationController`
- Check `SecurityContext` is stored in session
- Confirm `JSESSIONID` cookie is being set

**CORS errors with credentials:**

- Cannot use wildcard origins with credentials
- Must specify exact frontend URLs
- Check preflight OPTIONS requests

### Debugging Commands

**Check if cookies are set:**

```javascript
// In browser console
document.cookie; // Should show JSESSIONID
```

**Verify CORS headers:**

```bash
curl -H "Origin: http://localhost:3000" \
     -H "Access-Control-Request-Method: POST" \
     -H "Access-Control-Request-Headers: Content-Type" \
     -X OPTIONS \
     http://localhost:8080/api/login/test
```

**Check session storage:**

```java
// Add logging in AuthorizationController
HttpSession session = request.getSession(false);
logger.info("Session ID: {}", session != null ? session.getId() : "none");
```

## Alternative Approaches

### JWT Token Authentication (Not Used)

```typescript
// Would require manual token management
headers: {
  'Authorization': `Bearer ${token}`
}
// No session state on server
```

### HTTP Basic Authentication (Available but not primary)

```java
// Enabled in SecurityConfig for testing
.httpBasic(org.springframework.security.config.Customizer.withDefaults())
```

## File Locations

| Component                     | File Path                                                           |
| ----------------------------- | ------------------------------------------------------------------- |
| **Authentication Controller** | `backend/src/main/java/.../controller/AuthorizationController.java` |
| **CORS Configuration**        | `backend/src/main/java/.../config/WebConfig.java`                   |
| **Security Configuration**    | `backend/src/main/java/.../config/SecurityConfig.java`              |
| **Frontend API Setup**        | `frontend/src/utils/api.ts`                                         |

## Summary

The Trade Capture System uses **session-based authentication** with:

- **Backend**: Spring Security with HTTP sessions and `JSESSIONID` cookies
- **Frontend**: Axios with `withCredentials: true` for automatic cookie handling
- **CORS**: Explicit credential allowance for cross-origin cookie sharing
- **Security**: Server-side session storage with client-side session identifiers

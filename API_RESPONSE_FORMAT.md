# API response format

Every controller returns the same top-level structure.

## Successful response

```json
{
  "success": true,
  "message": "Signed in.",
  "data": {
    "id": "user-id",
    "email": "user@example.com",
    "role": "ADMIN"
  },
  "timestamp": "2026-07-26T14:30:00Z"
}
```

Controllers return `ResponseEntity<ApiResponse<T>>`. Use
`ResponseEntity.ok(ApiResponse.success(message, data))` for a normal `200 OK`
response, or `ResponseEntity.status(HttpStatus.CREATED)` for a `201 Created`
response. Services do not depend on HTTP status.

## Error response

```json
{
  "success": false,
  "message": "Please correct the invalid fields.",
  "errors": {
    "email": "must be a well-formed email address"
  },
  "timestamp": "2026-07-26T14:30:00Z"
}
```

`GlobalExceptionHandler` converts service and validation exceptions into this
format. `RestAuthenticationEntryPoint` uses the same format for authentication
errors.

## Package responsibilities

- `dto/request`: validated request-body types accepted by controllers.
- `dto/response`: public response data and the shared success/error templates.
- `dto/internal`: data passed between the service and controller but not
  serialized directly.
- `exception`: reusable exceptions and global HTTP error handling.
- `controller`: HTTP routes, cookies, status, and response wrapping.
- `service`: authentication and business rules.

DTOs use only the Lombok annotations they need. Request DTOs have getters,
setters, and constructors because Jackson creates and fills them from JSON.
Response and internal DTOs have getters and an all-arguments constructor, but
no setters, `equals`, `hashCode`, or `toString` are generated. IntelliJ must
have annotation processing enabled to recognize the generated methods.

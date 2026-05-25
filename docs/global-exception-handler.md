# Global Exception Handler

This project uses a centralized exception handler to return consistent JSON error responses from the API.

## Location

- `src/main/java/com/ashish/saas/multitanantsaasapp/exception/GlobalExceptionHandler.java`

## Response Format

```json
{
  "timestamp": "2026-05-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/tenants",
  "traceId": "1f73a2b6f9c14e10",
  "details": {
    "fieldErrors": [
      { "field": "name", "message": "must not be blank" }
    ]
  }
}
```

## Handled Cases

- Validation errors: `MethodArgumentNotValidException`, `BindException`, `ConstraintViolationException`
- Missing/invalid request params: `MissingServletRequestParameterException`, `MissingPathVariableException`,
  `MethodArgumentTypeMismatchException`
- Malformed JSON: `HttpMessageNotReadableException`
- Unsupported media type: `HttpMediaTypeNotSupportedException`
- Method not allowed: `HttpRequestMethodNotSupportedException`
- Access denied: `AccessDeniedException`
- Data conflicts: `DataIntegrityViolationException`, `DuplicateKeyException`
- Transaction failures: `TransactionSystemException`
- Explicit status exceptions: `ResponseStatusException`, `ErrorResponseException`
- Fallback: `Exception`

## Notes

- `traceId` is populated from the logging MDC key `traceId` if available.
- For validation failures, `details.fieldErrors` and `details.violations` provide structured error data.


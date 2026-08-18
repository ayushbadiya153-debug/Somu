# Auth-Gated App Testing Playbook

## Step 1: Create Test User & Session
```
mongosh --eval "
use('test_database');
var userId = 'test-user-' + Date.now();
var sessionToken = 'test_session_' + Date.now();
db.users.insertOne({
  user_id: userId,
  email: 'test.user.' + Date.now() + '@example.com',
  name: 'Test User',
  picture: 'https://via.placeholder.com/150',
  created_at: new Date().toISOString(),
  updated_at: new Date().toISOString()
});
db.user_sessions.insertOne({
  user_id: userId,
  session_token: sessionToken,
  expires_at: new Date(Date.now() + 7*24*60*60*1000).toISOString(),
  created_at: new Date().toISOString()
});
print('Session token: ' + sessionToken);
print('User ID: ' + userId);
"
```

## Step 2: Test Backend API
```
curl -X GET "$BACKEND_URL/api/auth/me" -H "Authorization: Bearer $SESSION_TOKEN"
curl -X GET "$BACKEND_URL/api/dashboard" -H "Authorization: Bearer $SESSION_TOKEN"
curl -X GET "$BACKEND_URL/api/settings" -H "Authorization: Bearer $SESSION_TOKEN"
curl -X GET "$BACKEND_URL/api/strategies" -H "Authorization: Bearer $SESSION_TOKEN"
```

## Step 3: Browser Testing (Playwright)
```
await page.context.add_cookies([{
  "name": "session_token",
  "value": SESSION_TOKEN,
  "domain": HOST,
  "path": "/",
  "httpOnly": True,
  "secure": True,
  "sameSite": "None"
}])
await page.goto(f"{BASE_URL}/dashboard")
```

## Checklist
- users.user_id present, no exposure of MongoDB `_id`
- session_token cookie set httpOnly, secure, SameSite=None
- /api/auth/me returns 401 when unauth, user object when auth
- All protected routes return 401 without cookie/header

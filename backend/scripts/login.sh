# Usage (from backend/):  . scripts/login.sh
#                         . scripts/login.sh someone@example.com theirpassword
EMAIL=${1:-other@example.com}
PASSWORD=${2:-otherpass123}

RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")

TOKEN=$(echo "$RESPONSE" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
REFRESH=$(echo "$RESPONSE" | sed -n 's/.*"refreshToken":"\([^"]*\)".*/\1/p')

if [ -n "$TOKEN" ]; then
  echo "Logged in as $EMAIL. TOKEN and REFRESH are set."
else
  echo "Login failed: $RESPONSE"
fi
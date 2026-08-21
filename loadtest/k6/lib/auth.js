// 카카오 OAuth 콜백을 거치지 않고, 앱과 동일한 HS256 서명으로 JWT를 직접 발급한다.
// JwtTokenProvider.generateToken()과 동일한 형식(subject=user_id, provider/providerId
// claim, iat/exp)으로 만들어 JwtAuthenticationFilter 검증을 그대로 통과시킨다.
//
// 서명 키는 jwt.secret 문자열을 UTF-8 바이트 그대로 HMAC 키로 쓴다(base64 디코딩 안 함) -
// JwtTokenProvider.getSigningKey()와 동일하게 맞춘 것.

import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { JWT_SECRET, JWT_EXPIRATION_MS } from './config.js';

function base64url(input) {
  return encoding.b64encode(input, 'rawurl');
}

function pad3(n) {
  const s = String(n);
  return s.length >= 3 ? s : ('000' + s).slice(-3);
}

/** userId(users.user_id)를 subject로 하는 JWT를 즉시 발급한다. */
export function mintToken(userId) {
  const nowSec = Math.floor(Date.now() / 1000);
  const expSec = nowSec + Math.floor(JWT_EXPIRATION_MS / 1000);

  const header = { alg: 'HS256', typ: 'JWT' };
  const payload = {
    sub: String(userId),
    provider: 'LOCAL',
    providerId: `k6-loadtest-${pad3(userId - 3)}`,
    iat: nowSec,
    exp: expSec,
  };

  const signingInput = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(payload))}`;
  const signature = crypto.hmac('sha256', JWT_SECRET, signingInput, 'base64rawurl');

  return `${signingInput}.${signature}`;
}

export function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  };
}

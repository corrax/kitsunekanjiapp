'use strict';

const crypto = require('node:crypto');
const {
  DynamoDBClient
} = require('@aws-sdk/client-dynamodb');
const {
  DynamoDBDocumentClient,
  GetCommand,
  PutCommand,
  UpdateCommand
} = require('@aws-sdk/lib-dynamodb');

const MODEL_NAME = 'gemini-2.5-flash';
const PROMPT = [
  'You are a Japanese language tutor.',
  'Extract all Japanese vocabulary terms visible in this image.',
  'Return strict JSON only with this shape:',
  '{"terms":[{"kanji":"...","reading":"...","meaning":"...","notes":"..."}]}',
  'No markdown fences. No prose. No explanations.',
  'Max 15 terms.',
  'Skip full sentences and focus on individual words or short phrases.',
  'If nothing useful is found, return {"terms":[]}.'
].join(' ');

const DEVICE_ID_HEADER = 'x-device-id';
const DEVICE_ID_REGEX = /^[A-Za-z0-9._:-]{8,128}$/;
const MAX_BASE64_IMAGE_LENGTH = 8 * 1024 * 1024;

const countersTable = process.env.USAGE_COUNTERS_TABLE;
const blacklistTable = process.env.DEVICE_BLACKLIST_TABLE;
const profilesTable = process.env.DEVICE_PROFILES_TABLE;
const devicePerMinuteLimit = Number(process.env.DEVICE_PER_MINUTE_LIMIT || 15);
const devicePerDayLimit = Number(process.env.DEVICE_PER_DAY_LIMIT || 300);
const ipPerMinuteLimit = Number(process.env.IP_PER_MINUTE_LIMIT || 60);
const deviceHardBlockThresholdPerMinute = Number(
  process.env.DEVICE_HARD_BLOCK_THRESHOLD_PER_MINUTE || 120
);
const freeDeviceWeeklyLimit = Number(process.env.FREE_DEVICE_WEEKLY_LIMIT || 3);
const disableAbuseProtection = process.env.DISABLE_ABUSE_PROTECTION === 'true';

// Minimum token length for a plausible Google Play purchase token
const MIN_PURCHASE_TOKEN_LENGTH = 100;

const ddb = DynamoDBDocumentClient.from(new DynamoDBClient({}));

function jsonResponse(statusCode, body) {
  return {
    statusCode,
    headers: {
      'content-type': 'application/json',
      'cache-control': 'no-store'
    },
    body: JSON.stringify(body)
  };
}

function noStoreError(statusCode, body, retryAfterSeconds) {
  const response = jsonResponse(statusCode, body);
  if (retryAfterSeconds && retryAfterSeconds > 0) {
    response.headers['retry-after'] = String(Math.ceil(retryAfterSeconds));
  }
  return response;
}

function extractJsonBlock(text) {
  const trimmed = text.trim();

  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    return trimmed;
  }

  const fenced = trimmed.match(/```(?:json)?\s*([\s\S]*?)\s*```/i);
  if (fenced && fenced[1]) {
    return fenced[1].trim();
  }

  const firstBrace = trimmed.indexOf('{');
  const lastBrace = trimmed.lastIndexOf('}');
  if (firstBrace >= 0 && lastBrace > firstBrace) {
    return trimmed.slice(firstBrace, lastBrace + 1);
  }

  throw new Error('Model did not return JSON.');
}

function normalizeTerms(payload) {
  const terms = Array.isArray(payload?.terms) ? payload.terms : [];

  return {
    terms: terms
      .map((term) => ({
        kanji: typeof term?.kanji === 'string' ? term.kanji.trim() : '',
        reading: typeof term?.reading === 'string' ? term.reading.trim() : '',
        meaning: typeof term?.meaning === 'string' ? term.meaning.trim() : '',
        notes: typeof term?.notes === 'string' ? term.notes.trim() : ''
      }))
      .filter((term) => term.kanji.length > 0)
      .slice(0, 15)
  };
}

function parseRequestBody(event) {
  if (!event.body) {
    throw new Error('Request body is required.');
  }

  const rawBody = event.isBase64Encoded
    ? Buffer.from(event.body, 'base64').toString('utf8')
    : event.body;

  return JSON.parse(rawBody);
}

function normalizedHeader(headers, key) {
  if (!headers) return '';

  const direct = headers[key];
  if (typeof direct === 'string') return direct.trim();

  const lower = headers[key.toLowerCase()];
  if (typeof lower === 'string') return lower.trim();

  return '';
}

function sha256(value) {
  return crypto.createHash('sha256').update(value).digest('hex');
}

function utcDayKey(nowMs) {
  return Math.floor(nowMs / 86400000);
}

function minuteWindowStart(epochSeconds) {
  return Math.floor(epochSeconds / 60) * 60;
}

function dayWindowStart(epochSeconds) {
  return Math.floor(epochSeconds / 86400) * 86400;
}

function weekWindowStart(epochSeconds) {
  return Math.floor(epochSeconds / (86400 * 7)) * (86400 * 7);
}

function buildRetryAfter(windowStart, windowSeconds, nowSeconds) {
  return Math.max(1, windowStart + windowSeconds - nowSeconds);
}

async function incrementCounter(counterKey, nowSeconds, windowSeconds) {
  const windowStart =
    windowSeconds === 86400 * 7
      ? weekWindowStart(nowSeconds)
      : windowSeconds === 86400
        ? dayWindowStart(nowSeconds)
        : minuteWindowStart(nowSeconds);
  const expiresAt = windowStart + windowSeconds + 120;

  const result = await ddb.send(
    new UpdateCommand({
      TableName: countersTable,
      Key: { counterKey },
      UpdateExpression:
        'SET expiresAt = :exp, windowStart = :ws, updatedAt = :updatedAt ADD callCount :inc',
      ExpressionAttributeValues: {
        ':exp': expiresAt,
        ':ws': windowStart,
        ':updatedAt': nowSeconds,
        ':inc': 1
      },
      ReturnValues: 'UPDATED_NEW'
    })
  );

  return {
    count: Number(result?.Attributes?.callCount || 0),
    windowStart
  };
}

async function getBlacklist(deviceId, nowSeconds) {
  const response = await ddb.send(
    new GetCommand({
      TableName: blacklistTable,
      Key: { deviceId }
    })
  );

  const item = response?.Item;
  if (!item) return null;

  if (item.expiresAt && Number(item.expiresAt) < nowSeconds) {
    return null;
  }

  return item;
}

async function addBlacklist(deviceId, nowSeconds, reason, secondsToExpire) {
  await ddb.send(
    new PutCommand({
      TableName: blacklistTable,
      Item: {
        deviceId,
        reason,
        createdAt: nowSeconds,
        expiresAt: nowSeconds + secondsToExpire
      }
    })
  );
}

function resolveClientInfo(event) {
  const sourceIp =
    event?.requestContext?.http?.sourceIp ||
    normalizedHeader(event?.headers, 'x-forwarded-for').split(',')[0].trim() ||
    '0.0.0.0';
  const userAgent =
    event?.requestContext?.http?.userAgent ||
    normalizedHeader(event?.headers, 'user-agent') ||
    '';
  const rawDeviceId = normalizedHeader(event?.headers, DEVICE_ID_HEADER);
  const validDeviceId =
    DEVICE_ID_REGEX.test(rawDeviceId) ? rawDeviceId : null;
  const fallbackFingerprint = sha256(`${sourceIp}|${userAgent}`).slice(0, 32);
  const deviceId = validDeviceId || `fp_${fallbackFingerprint}`;
  const ipHash = sha256(sourceIp).slice(0, 32);
  const userAgentHash = sha256(userAgent).slice(0, 32);

  return { sourceIp, userAgent, deviceId, ipHash, userAgentHash };
}

async function trackDeviceProfile(deviceInfo, nowSeconds) {
  const dayKey = utcDayKey(nowSeconds * 1000);
  await ddb.send(
    new UpdateCommand({
      TableName: profilesTable,
      Key: { deviceId: deviceInfo.deviceId },
      UpdateExpression:
        'SET lastSeenAt = :now, lastIpHash = :ipHash, lastUserAgentHash = :uaHash, lastDayKey = :dayKey, firstSeenAt = if_not_exists(firstSeenAt, :now) ADD totalCalls :inc',
      ExpressionAttributeValues: {
        ':now': nowSeconds,
        ':ipHash': deviceInfo.ipHash,
        ':uaHash': deviceInfo.userAgentHash,
        ':dayKey': dayKey,
        ':inc': 1
      }
    })
  );
}

function isPlusToken(purchaseToken) {
  return (
    typeof purchaseToken === 'string' &&
    purchaseToken.trim().length >= MIN_PURCHASE_TOKEN_LENGTH
  );
}

async function checkFreeWeeklyQuota(deviceInfo, nowSeconds, purchaseToken) {
  if (isPlusToken(purchaseToken)) {
    return { exceeded: false };
  }

  const weekKey = `device_week#${deviceInfo.deviceId}#${weekWindowStart(nowSeconds)}`;
  const deviceWeek = await incrementCounter(weekKey, nowSeconds, 86400 * 7);

  if (deviceWeek.count > freeDeviceWeeklyLimit) {
    const retryAfter = buildRetryAfter(deviceWeek.windowStart, 86400 * 7, nowSeconds);
    return {
      exceeded: true,
      response: noStoreError(
        402,
        {
          error: 'Free capture limit reached for this week. Upgrade to Plus for unlimited captures.',
          errorCode: 'free_quota_exceeded'
        },
        retryAfter
      )
    };
  }

  return { exceeded: false };
}

async function enforceRateLimits(deviceInfo, nowSeconds) {
  const blacklist = await getBlacklist(deviceInfo.deviceId, nowSeconds);
  if (blacklist) {
    const retryAfter = blacklist.expiresAt
      ? Number(blacklist.expiresAt) - nowSeconds
      : 86400;
    return {
      blocked: true,
      response: noStoreError(
        429,
        { error: 'This device is temporarily blocked.', reason: blacklist.reason || 'blocked' },
        retryAfter
      )
    };
  }

  const deviceMinuteKey = `device_minute#${deviceInfo.deviceId}#${minuteWindowStart(nowSeconds)}`;
  const deviceMinute = await incrementCounter(deviceMinuteKey, nowSeconds, 60);
  if (deviceMinute.count > deviceHardBlockThresholdPerMinute) {
    await addBlacklist(
      deviceInfo.deviceId,
      nowSeconds,
      'excessive_requests_per_minute',
      24 * 3600
    );
    return {
      blocked: true,
      response: noStoreError(
        429,
        { error: 'This device is temporarily blocked due to abuse.' },
        24 * 3600
      )
    };
  }
  if (deviceMinute.count > devicePerMinuteLimit) {
    const retryAfter = buildRetryAfter(deviceMinute.windowStart, 60, nowSeconds);
    return {
      blocked: true,
      response: noStoreError(
        429,
        { error: 'Device rate limit exceeded. Please try again shortly.' },
        retryAfter
      )
    };
  }

  const deviceDayKey = `device_day#${deviceInfo.deviceId}#${dayWindowStart(nowSeconds)}`;
  const deviceDay = await incrementCounter(deviceDayKey, nowSeconds, 86400);
  if (deviceDay.count > devicePerDayLimit) {
    const retryAfter = buildRetryAfter(deviceDay.windowStart, 86400, nowSeconds);
    return {
      blocked: true,
      response: noStoreError(
        429,
        { error: 'Daily device limit exceeded. Please try again tomorrow.' },
        retryAfter
      )
    };
  }

  const ipMinuteKey = `ip_minute#${deviceInfo.ipHash}#${minuteWindowStart(nowSeconds)}`;
  const ipMinute = await incrementCounter(ipMinuteKey, nowSeconds, 60);
  if (ipMinute.count > ipPerMinuteLimit) {
    const retryAfter = buildRetryAfter(ipMinute.windowStart, 60, nowSeconds);
    return {
      blocked: true,
      response: noStoreError(
        429,
        { error: 'IP rate limit exceeded. Please try again shortly.' },
        retryAfter
      )
    };
  }

  return { blocked: false };
}

exports.handler = async (event) => {
  try {
    const { image, mimeType } = parseRequestBody(event);
    if (!image || typeof image !== 'string') {
      return jsonResponse(400, { error: 'image must be a base64 string.' });
    }
    if (image.length > MAX_BASE64_IMAGE_LENGTH) {
      return jsonResponse(413, { error: 'image payload too large.' });
    }

    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
      throw new Error('GEMINI_API_KEY is not configured.');
    }
    if (!disableAbuseProtection && (!countersTable || !blacklistTable || !profilesTable)) {
      throw new Error('Required DynamoDB table env vars are not configured.');
    }

    const purchaseToken = normalizedHeader(event?.headers, 'x-purchase-token');

    if (!disableAbuseProtection) {
      const nowSeconds = Math.floor(Date.now() / 1000);
      const deviceInfo = resolveClientInfo(event);
      const limitResult = await enforceRateLimits(deviceInfo, nowSeconds);
      if (limitResult.blocked) {
        return limitResult.response;
      }
      const quotaResult = await checkFreeWeeklyQuota(deviceInfo, nowSeconds, purchaseToken);
      if (quotaResult.exceeded) {
        return quotaResult.response;
      }
      await trackDeviceProfile(deviceInfo, nowSeconds);
    }

    const { GoogleGenAI } = require('@google/genai');

    const resolvedMimeType =
      typeof mimeType === 'string' && mimeType.trim().length > 0
        ? mimeType.trim()
        : 'image/jpeg';

    const client = new GoogleGenAI({ apiKey });
    const result = await client.models.generateContent({
      model: MODEL_NAME,
      contents: [
        {
          role: 'user',
          parts: [
            { text: PROMPT },
            {
              inlineData: {
                data: image,
                mimeType: resolvedMimeType
              }
            }
          ]
        }
      ],
      config: {
        responseMimeType: 'application/json',
        temperature: 0.1
      }
    });

    const text = result.text;
    const parsed = JSON.parse(extractJsonBlock(text));
    return jsonResponse(200, normalizeTerms(parsed));
  } catch (error) {
    const message =
      error instanceof SyntaxError
        ? 'Malformed JSON body.'
        : error?.message || 'Internal server error.';

    const statusCode =
      message === 'Malformed JSON body.' || message.includes('Request body')
        ? 400
        : 500;

    if (statusCode >= 500) {
      console.error('analyze-japanese failed', error);
    }

    return jsonResponse(statusCode, { error: message });
  }
};

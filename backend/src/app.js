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
  'You are a Japanese language tutor analyzing an image for vocabulary learning.',
  'Identify up to 30 Japanese vocabulary items from this image.',
  'Include ALL of the following that apply:',
  '1. Text visible in the image (signs, labels, menus, packaging, etc.)',
  '2. Actions being performed by people in the image (verbs, e.g. 歩く for walking, 食べる for eating)',
  '3. Objects and concepts depicted',
  'For each item provide:',
  'kanji: the Japanese word or phrase (kanji/kana as natural Japanese),',
  'reading: hiragana or katakana reading,',
  'meaning: a single English word or short compound noun (e.g. "walk", "red", "station", "exit sign"). Must be concise — one or two words maximum. Do NOT include explanations, parentheses, or phrases here.',
  'definition: a brief English definition or extra context only when the single-word meaning is ambiguous or insufficient (e.g. "to walk slowly and casually" for 散歩). Leave blank or omit if the meaning field is self-explanatory.',
  'jlptLevel: one of N5/N4/N3/N2/N1/unknown based on JLPT classification,',
  'partOfSpeech: one of noun/verb/adjective/phrase/counter/other,',
  'exampleSentence: a short natural Japanese sentence (max 15 words) using the word,',
  'exampleTranslation: English translation of the example sentence,',
  'confidence: 0.0–1.0 (1.0 = text directly visible in image; 0.7 = clearly depicted object/action; 0.4 = inferred from context).',
  'Assign higher confidence to words directly visible as text.',
  'Focus on words most useful for a learner.'
].join(' ');

const RESPONSE_SCHEMA = {
  type: 'object',
  properties: {
    terms: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          kanji: { type: 'string' },
          reading: { type: 'string' },
          meaning: { type: 'string' },
          definition: { type: 'string' },
          jlptLevel: { type: 'string', enum: ['N5', 'N4', 'N3', 'N2', 'N1', 'unknown'] },
          partOfSpeech: { type: 'string', enum: ['noun', 'verb', 'adjective', 'phrase', 'counter', 'other'] },
          exampleSentence: { type: 'string' },
          exampleTranslation: { type: 'string' },
          confidence: { type: 'number' }
        },
        required: ['kanji', 'reading', 'meaning', 'definition', 'jlptLevel', 'partOfSpeech', 'exampleSentence', 'exampleTranslation', 'confidence']
      }
    }
  },
  required: ['terms']
};

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

function lowercaseFirst(str) {
  if (!str) return str;
  return str.charAt(0).toLowerCase() + str.slice(1);
}

function normalizeTerms(payload) {
  const validLevels = new Set(['N5', 'N4', 'N3', 'N2', 'N1', 'unknown']);
  const validPos = new Set(['noun', 'verb', 'adjective', 'phrase', 'counter', 'other']);
  const terms = Array.isArray(payload?.terms) ? payload.terms : [];

  return {
    terms: terms
      .map((term) => {
        const rawLevel = typeof term?.jlptLevel === 'string' ? term.jlptLevel.trim().toUpperCase() : '';
        const rawPos = typeof term?.partOfSpeech === 'string' ? term.partOfSpeech.trim().toLowerCase() : '';
        const confidence = typeof term?.confidence === 'number' ? Math.max(0, Math.min(1, term.confidence)) : 0.5;
        const rawMeaning = typeof term?.meaning === 'string' ? term.meaning.trim() : '';
        return {
          kanji: typeof term?.kanji === 'string' ? term.kanji.trim() : '',
          reading: typeof term?.reading === 'string' ? term.reading.trim() : '',
          meaning: lowercaseFirst(rawMeaning),
          definition: typeof term?.definition === 'string' ? term.definition.trim() : '',
          jlptLevel: validLevels.has(rawLevel) ? rawLevel : 'unknown',
          partOfSpeech: validPos.has(rawPos) ? rawPos : 'other',
          exampleSentence: typeof term?.exampleSentence === 'string' ? term.exampleSentence.trim() : '',
          exampleTranslation: typeof term?.exampleTranslation === 'string' ? term.exampleTranslation.trim() : '',
          confidence
        };
      })
      .filter((term) => term.kanji.length > 0)
      .sort((a, b) => b.confidence - a.confidence)
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
    console.info('Gemini request', {
      model: MODEL_NAME,
      mimeType: resolvedMimeType,
      imageLength: image.length
    });

    let result;
    try {
      result = await client.models.generateContent({
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
          responseSchema: RESPONSE_SCHEMA,
          temperature: 0.1
        }
      });
    } catch (geminiError) {
      console.error('Gemini API call failed', {
        message: geminiError?.message,
        name: geminiError?.name,
        status: geminiError?.status,
        statusCode: geminiError?.statusCode,
        code: geminiError?.code,
        cause: geminiError?.cause ? String(geminiError.cause) : undefined,
        stack: geminiError?.stack
      });
      throw geminiError;
    }

    const rawText = result?.text;
    if (rawText == null || typeof rawText !== 'string') {
      console.error('Gemini response missing or invalid text', {
        hasResult: !!result,
        resultKeys: result ? Object.keys(result) : [],
        rawResult: JSON.stringify(result)?.slice(0, 500)
      });
      throw new Error('Invalid response from analysis service.');
    }

    console.info('Gemini response received', { responseLength: rawText.length });

    let parsed;
    try {
      parsed = JSON.parse(rawText);
    } catch (parseError) {
      console.error('Gemini response JSON parse failed', {
        message: parseError?.message,
        rawTextLength: rawText.length,
        rawTextPreview: rawText.slice(0, 300)
      });
      throw new Error('Analysis service returned invalid data.');
    }

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
      console.error('analyze-japanese failed', {
        message: error?.message,
        name: error?.name,
        stack: error?.stack,
        status: error?.status,
        statusCode: error?.statusCode,
        code: error?.code
      });
    }

    return jsonResponse(statusCode, { error: message });
  }
};

const MODEL = 'minimax/minimax-m3.1-flash';
const GATEWAY_URL = 'https://ai-gateway.vercel.sh/v1/chat/completions';

function stripThinkTags(raw: string): string {
  return raw
    .replace(/<think>[\s\S]*?<\/think>/gi, '')
    .replace(/<think>[\s\S]*$/gi, '')
    .trim();
}

function clamp(num: number, min: number, max: number): number {
  return Math.min(Math.max(num, min), max);
}

async function callGateway(
  apiKey: string,
  payload: Record<string, unknown>,
  timeoutMs = 50000
): Promise<{ ok: boolean; status: number; data: any }> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const resp = await fetch(GATEWAY_URL, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${apiKey}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
      signal: controller.signal,
    });
    const text = await resp.text();
    let parsed: any = null;
    try {
      parsed = text ? JSON.parse(text) : null;
    } catch {
      parsed = null;
    }
    return { ok: resp.ok, status: resp.status, data: parsed };
  } finally {
    clearTimeout(timer);
  }
}

export default async function handler(req: any, res: any) {
  if (req.method === 'GET') {
    return res.status(200).json({ ok: true, status: 'ready' });
  }

  if (req.method !== 'POST') {
    res.setHeader('Allow', 'GET, POST');
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const apiKey = process.env.AI_GATEWAY_API_KEY;
  if (!apiKey) {
    return res.status(503).json({ error: 'AI service not configured' });
  }

  let body: any = {};
  try {
    body = typeof req.body === 'string' ? JSON.parse(req.body || '{}') : req.body || {};
  } catch {
    return res.status(400).json({ error: 'Invalid JSON request body' });
  }

  const messages = body?.messages;
  if (!Array.isArray(messages) || messages.length === 0) {
    return res.status(400).json({ error: 'messages array is required' });
  }

  const requestedTemp = Number(body?.temperature);
  const temperature = Number.isFinite(requestedTemp) ? clamp(requestedTemp, 0, 1) : 0.5;

  const requestedMax = Number(body?.max_tokens);
  const maxTokens = Number.isFinite(requestedMax)
    ? clamp(Math.round(requestedMax), 1200, 4096)
    : 2048;

  const buildPayload = (
    overrideTokens: number,
    extraReminder = false
  ): Record<string, unknown> => {
    const msgs = extraReminder
      ? [
          ...messages,
          { role: 'system', content: 'Provide the final concise answer directly in content.' },
        ]
      : messages;

    const payload: Record<string, unknown> = {
      model: MODEL,
      temperature,
      max_tokens: overrideTokens,
      messages: msgs,
    };

    if (Array.isArray(body?.tools) && body.tools.length > 0) {
      payload.tools = body.tools;
      payload.tool_choice = body.tool_choice || 'auto';
    }

    return payload;
  };

  try {
    let result = await callGateway(apiKey, buildPayload(maxTokens, false));

    if (!result.ok && (result.status === 429 || result.status >= 500)) {
      await new Promise((r) => setTimeout(r, 350));
      result = await callGateway(apiKey, buildPayload(maxTokens, false));
    }

    if (!result.ok) {
      const code = result.status >= 400 ? result.status : 502;
      return res.status(code).json({ error: 'AI service temporarily unavailable' });
    }

    let choice = result.data?.choices?.[0];
    let msg = choice?.message || {};
    let toolCalls = Array.isArray(msg.tool_calls) ? msg.tool_calls : [];
    let content = stripThinkTags(String(msg.content || ''));
    let finishReason = String(choice?.finish_reason || 'stop');

    if (toolCalls.length === 0 && !content) {
      const boostedTokens = Math.min(maxTokens + 1200, 4096);
      const retry = await callGateway(apiKey, buildPayload(boostedTokens, true));
      if (retry.ok) {
        choice = retry.data?.choices?.[0];
        msg = choice?.message || {};
        toolCalls = Array.isArray(msg.tool_calls) ? msg.tool_calls : [];
        content = stripThinkTags(String(msg.content || ''));
        finishReason = String(choice?.finish_reason || finishReason);
      }
    }

    if (toolCalls.length === 0 && !content) {
      return res.status(502).json({ error: 'AI service returned an empty response' });
    }

    return res.status(200).json({
      content,
      tool_calls: toolCalls,
      finish_reason: finishReason,
    });
  } catch {
    return res.status(502).json({ error: 'Unable to reach AI service' });
  }
}
